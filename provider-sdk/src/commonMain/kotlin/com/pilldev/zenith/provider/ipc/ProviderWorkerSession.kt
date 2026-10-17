package com.pilldev.zenith.provider.ipc

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

public interface ProviderWorkerFrameTransport {
    public suspend fun send(frame: ProviderWorkerFrame)

    public suspend fun receive(): ProviderWorkerFrame

    public suspend fun close()
}

public fun interface ProviderWorkerBroker {
    public suspend fun handle(
        operation: ProviderBrokerOperation,
        requestBody: ByteArray?,
    ): ProviderWorkerBrokerResponse
}

public data class ProviderWorkerBrokerResponse(
    public val result: ProviderBrokerResult,
    public val responseBody: ByteArray? = null,
)

public class ProviderWorkerBlobAssembler(
    private val digest: ProviderWorkerDigest,
) {
    private val mutex = Mutex()
    private val transfers = mutableMapOf<String, Transfer>()
    private var reservedBytes = 0L

    public suspend fun expect(
        blob: ProviderWorkerBlob,
        parentRequestId: String,
    ): Unit =
        mutex.withLock {
            require(blob.sizeBytes in 0..PROVIDER_WORKER_MAX_BLOB_BYTES) { "Provider worker blob exceeds the size limit" }
            require(blob.transferId !in transfers) { "Provider worker blob transfer ID is already active" }
            require(reservedBytes + blob.sizeBytes <= MAX_ACTIVE_BLOB_BYTES) {
                "Concurrent provider worker blobs exceed the aggregate size limit"
            }
            if (blob.sizeBytes == 0L) {
                require(digest.sha256(byteArrayOf()).equals(blob.sha256, ignoreCase = true)) {
                    "Provider worker blob checksum mismatch"
                }
            }
            val transfer = Transfer(blob, parentRequestId)
            transfers[blob.transferId] = transfer
            reservedBytes += blob.sizeBytes
            if (blob.sizeBytes == 0L) {
                transfer.result.complete(byteArrayOf())
            }
        }

    public suspend fun append(frame: ProviderWorkerFrame) {
        val chunk = frame.message as? ProviderWorkerMessage.BlobChunk
            ?: throw IllegalArgumentException("Expected a provider worker blob chunk")
        mutex.withLock {
            val transfer = transfers[chunk.transferId]
                ?: throw IllegalArgumentException("Provider worker sent an unrequested blob chunk")
            require(frame.inReplyTo == transfer.parentRequestId) {
                "Provider worker blob chunk has an invalid correlation ID"
            }
            require(!transfer.result.isCompleted) { "Provider worker sent data after the final blob chunk" }
            require(chunk.sequence == transfer.nextSequence) { "Provider worker blob chunks are out of order" }
            require(transfer.receivedBytes + chunk.bytes.size <= transfer.blob.sizeBytes) {
                "Provider worker blob exceeds its declared size"
            }
            chunk.bytes.copyInto(
                transfer.bytes,
                destinationOffset = transfer.receivedBytes.toInt(),
            )
            transfer.receivedBytes += chunk.bytes.size
            transfer.nextSequence += 1
            if (chunk.isLast) {
                require(transfer.receivedBytes == transfer.blob.sizeBytes) {
                    "Provider worker blob ended before its declared size"
                }
                require(digest.sha256(transfer.bytes).equals(transfer.blob.sha256, ignoreCase = true)) {
                    "Provider worker blob checksum mismatch"
                }
                transfer.result.complete(transfer.bytes)
            } else {
                require(transfer.receivedBytes < transfer.blob.sizeBytes) {
                    "Provider worker blob is missing its final chunk marker"
                }
            }
        }
    }

    public suspend fun read(blob: ProviderWorkerBlob): ByteArray =
        mutex.withLock {
            transfers[blob.transferId]?.takeIf { it.blob == blob }
                ?: throw IllegalArgumentException("Provider worker blob was not registered")
        }
            .result
            .await()

    public suspend fun discard(transferId: String) {
        mutex.withLock {
            transfers.remove(transferId)?.let { reservedBytes -= it.blob.sizeBytes }
        }
    }

    public suspend fun clear() {
        mutex.withLock {
            transfers.clear()
            reservedBytes = 0
        }
    }

    private class Transfer(
        val blob: ProviderWorkerBlob,
        val parentRequestId: String,
    ) {
        val bytes = ByteArray(blob.sizeBytes.toInt())
        val result = CompletableDeferred<ByteArray>()
        var nextSequence = 0
        var receivedBytes = 0L
    }

    private companion object {
        const val MAX_ACTIVE_BLOB_BYTES = 64L * 1024 * 1024
    }
}

public fun interface ProviderWorkerDigest {
    public fun sha256(bytes: ByteArray): String
}

public class ProviderWorkerSession private constructor(
    private val transport: ProviderWorkerFrameTransport,
    private val broker: ProviderWorkerBroker,
    private val digest: ProviderWorkerDigest,
    private val scope: CoroutineScope,
    private val timeoutMillis: Long,
    private val maxConcurrentRequests: Int,
) : ProviderWorkerClient {
    private val logger = Logger.withTag("ProviderWorkerSession")
    private val sendMutex = Mutex()
    private val stateMutex = Mutex()
    private val requestSlots = Semaphore(maxConcurrentRequests)
    private val brokerSlots = Semaphore(MAX_CONCURRENT_BROKER_REQUESTS)
    private val httpBrokerSlots = Semaphore(MAX_CONCURRENT_HTTP_BROKER_REQUESTS)
    private val pending = mutableMapOf<String, CompletableDeferred<ProviderWorkerFrame>>()
    private val activeBrokerRequests = mutableMapOf<String, ActiveBrokerRequest>()
    private val cancelledRequests = mutableSetOf<String>()
    private val blobAssembler = ProviderWorkerBlobAssembler(digest)
    private var sequence = 0L
    private var closed = false
    private var readyValue: ProviderWorkerMessage.Ready? = null
    private val reader: Job = scope.launch { readFrames() }

    override val ready: ProviderWorkerMessage.Ready
        get() = readyValue ?: throw IllegalStateException("Provider worker session has not initialized")

    private suspend fun initialize(
        manifest: com.pilldev.zenith.provider.model.PluginManifest,
        settings: Map<String, String> = emptyMap(),
    ): ProviderWorkerMessage.Ready {
        val response = request(ProviderWorkerMessage.Initialize(manifest, settings))
        val ready = response.message as? ProviderWorkerMessage.Ready
            ?: throw IllegalStateException("Provider worker did not return Ready")
        require(ready.manifest == manifest) { "Provider worker returned a different manifest" }
        readyValue = ready
        return ready
    }

    override suspend fun invoke(operation: ProviderWorkerOperation): ProviderWorkerResult {
        val response = request(ProviderWorkerMessage.Invoke(operation))
        return when (val message = response.message) {
            is ProviderWorkerMessage.Complete -> message.result
            is ProviderWorkerMessage.Failure -> ProviderWorkerResult.Failed(message.code, message.message)
            else -> ProviderWorkerResult.Failed(
                ProviderWorkerFailureCode.INVALID_REQUEST,
                "Provider worker returned an invalid response",
            )
        }
    }

    override suspend fun importBackup(data: ByteArray): ProviderWorkerResult {
        if (data.size > PROVIDER_WORKER_MAX_BLOB_BYTES) {
            return resourceLimit("Provider backup exceeds the maximum blob size")
        }
        val transferId = nextId("blob")
        val blob = ProviderWorkerBlob(
            transferId = transferId,
            sizeBytes = data.size.toLong(),
            sha256 = digest.sha256(data),
        )
        val response = request(ProviderWorkerMessage.Invoke(ProviderWorkerOperation.ImportBackup(blob))) {
            var offset = 0
            var chunkIndex = 0
            while (offset < data.size) {
                val end = minOf(offset + PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES, data.size)
                val chunk = data.copyOfRange(offset, end)
                send(
                    ProviderWorkerMessage.BlobChunk(
                        transferId = transferId,
                        sequence = chunkIndex,
                        bytes = chunk,
                        isLast = end == data.size,
                    ),
                    inReplyTo = it,
                )
                offset = end
                chunkIndex += 1
            }
        }
        return when (val message = response.message) {
            is ProviderWorkerMessage.Complete -> message.result
            is ProviderWorkerMessage.Failure -> ProviderWorkerResult.Failed(message.code, message.message)
            else -> ProviderWorkerResult.Failed(
                ProviderWorkerFailureCode.INVALID_REQUEST,
                "Provider worker returned an invalid import response",
            )
        }
    }

    override suspend fun dispose() {
        if (closed) return
        var failure: Throwable? = null
        try {
            val response = request(ProviderWorkerMessage.Dispose)
            if (response.message !is ProviderWorkerMessage.Disposed &&
                response.message !is ProviderWorkerMessage.Failure
            ) {
                throw IllegalStateException("Provider worker returned an invalid disposal response")
            }
        } catch (error: Throwable) {
            failure = error
        }
        try {
            close(ProviderWorkerFailureCode.WORKER_CRASHED, "Provider worker session closed", failure)
        } catch (cleanupError: Throwable) {
            failure?.addSuppressed(cleanupError) ?: run { failure = cleanupError }
        }
        failure?.let { throw it }
    }

    private suspend fun request(
        message: ProviderWorkerMessage,
        sendPayload: suspend (String) -> Unit = {},
    ): ProviderWorkerFrame {
        val requestId = nextId("host")
        val response = CompletableDeferred<ProviderWorkerFrame>()
        var slotAcquired = false
        var requestSent = false
        try {
            return withTimeout(timeoutMillis) {
                requestSlots.acquire()
                slotAcquired = true
                stateMutex.withLock {
                    check(!closed) { "Provider worker session is closed" }
                    pending[requestId] = response
                }
                requestSent = true
                send(message, requestId = requestId)
                sendPayload(requestId)
                response.await()
            }
        } catch (cancelled: CancellationException) {
            val requestTimedOut = cancelled is TimeoutCancellationException && currentCoroutineContext().isActive
            if (requestTimedOut) {
                close(
                    ProviderWorkerFailureCode.RESOURCE_LIMIT,
                    "Provider worker operation exceeded its deadline",
                    cancelled,
                )
            } else if (requestSent) {
                val shouldClose = stateMutex.withLock {
                    if (cancelledRequests.size >= MAX_CANCELLED_REQUESTS) {
                        true
                    } else {
                        cancelledRequests.add(requestId)
                        false
                    }
                }
                if (shouldClose) {
                    close(
                        ProviderWorkerFailureCode.RESOURCE_LIMIT,
                        "Provider worker exceeded the outstanding cancellation limit",
                        cancelled,
                    )
                }
                runCatching { send(ProviderWorkerMessage.Cancel(requestId), inReplyTo = requestId) }
            }
            throw cancelled
        } finally {
            if (slotAcquired) {
                stateMutex.withLock { pending.remove(requestId) }
                requestSlots.release()
            }
        }
    }

    private suspend fun send(
        message: ProviderWorkerMessage,
        requestId: String? = null,
        inReplyTo: String? = null,
    ) {
        val frame = ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = requestId ?: nextId("host"),
            inReplyTo = inReplyTo,
            message = message,
        )
        ProviderWorkerProtocol.encode(frame)
        sendMutex.withLock { transport.send(frame) }
    }

    private suspend fun readFrames() {
        try {
            while (true) {
                val frame = transport.receive()
                ProviderWorkerProtocol.encode(frame)
                val correlation = frame.inReplyTo
                    ?: throw IllegalArgumentException("Provider worker response is missing its correlation ID")
                if (frame.message is ProviderWorkerMessage.BlobChunk) {
                    blobAssembler.append(frame)
                    continue
                }
                if (frame.message is ProviderWorkerMessage.Cancel) {
                    cancelBrokerRequest(frame)
                    continue
                }
                if (frame.message is ProviderWorkerMessage.BrokerRequest) {
                    val body = (frame.message.operation as? ProviderBrokerOperation.HttpRequest)?.body
                    if (body != null && body.sizeBytes <= PROVIDER_WORKER_MAX_HTTP_BODY_BYTES) {
                        blobAssembler.expect(body, frame.requestId)
                    }
                    val job = scope.launch(start = CoroutineStart.LAZY) { handleBrokerRequest(frame) }
                    val accepted = stateMutex.withLock {
                        correlation in pending &&
                            activeBrokerRequests.size < MAX_CONCURRENT_BROKER_REQUESTS &&
                            frame.requestId !in activeBrokerRequests
                    }
                    if (!accepted) {
                        job.cancel()
                        body?.let { blobAssembler.discard(it.transferId) }
                        throw IllegalArgumentException("Provider worker broker request has no active parent request")
                    }
                    stateMutex.withLock {
                        activeBrokerRequests[frame.requestId] = ActiveBrokerRequest(correlation, job)
                    }
                    job.start()
                    continue
                }
                val (response, wasCancelled) = stateMutex.withLock {
                    pending[correlation] to cancelledRequests.remove(correlation)
                }
                if (response == null && wasCancelled) continue
                if (response == null) {
                    throw IllegalArgumentException("Provider worker response does not match an active request")
                }
                response.complete(frame)
            }
        } catch (cancelled: CancellationException) {
            if (scope.coroutineContext[Job]?.isActive == true) {
                close(
                    ProviderWorkerFailureCode.WORKER_CRASHED,
                    cancelled.message ?: "Provider worker reader cancelled",
                    cancelled,
                )
            }
            throw cancelled
        } catch (error: Exception) {
            close(
                ProviderWorkerFailureCode.WORKER_CRASHED,
                error.message ?: "Provider worker transport failed",
                error,
            )
        }
    }

    private suspend fun handleBrokerRequest(frame: ProviderWorkerFrame) {
        val operation = (frame.message as? ProviderWorkerMessage.BrokerRequest)?.operation ?: return
        val requestBlob = (operation as? ProviderBrokerOperation.HttpRequest)?.body
        try {
            if (requestBlob != null && requestBlob.sizeBytes > PROVIDER_WORKER_MAX_HTTP_BODY_BYTES) {
                sendBrokerComplete(
                    ProviderBrokerResult.Failed(
                        ProviderWorkerFailureCode.RESOURCE_LIMIT,
                        "Provider HTTP request exceeds the broker body limit",
                    ),
                    frame.requestId,
                )
                return
            }
            val response = try {
                withTimeout(timeoutMillis) {
                    brokerSlots.withPermit {
                        val handle = suspend {
                            val requestBody = requestBlob?.let { blobAssembler.read(it) }
                            broker.handle(operation, requestBody)
                        }
                        if (operation is ProviderBrokerOperation.HttpRequest) {
                            httpBrokerSlots.withPermit { handle() }
                        } else {
                            handle()
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                if (cancelled !is TimeoutCancellationException) throw cancelled
                ProviderWorkerBrokerResponse(
                    ProviderBrokerResult.Failed(
                        ProviderWorkerFailureCode.BROKER_FAILURE,
                        "Provider broker request exceeded its deadline",
                    ),
                )
            } catch (error: Exception) {
                ProviderWorkerBrokerResponse(
                    ProviderBrokerResult.Failed(
                        ProviderWorkerFailureCode.BROKER_FAILURE,
                        error.message ?: "Provider broker request failed",
                    ),
                )
            }
            val preparedResponse = try {
                prepareBrokerResponse(response)
            } catch (error: Exception) {
                sendBrokerComplete(
                    ProviderBrokerResult.Failed(
                        ProviderWorkerFailureCode.BROKER_FAILURE,
                        error.message ?: "Provider broker returned an invalid response",
                    ),
                    frame.requestId,
                )
                return
            }
            if (sendBrokerComplete(preparedResponse.first, frame.requestId)) {
                preparedResponse.second?.let { (blob, bytes) ->
                    sendBlob(blob, bytes, frame.requestId)
                }
            }
        } finally {
            requestBlob?.let { blobAssembler.discard(it.transferId) }
            stateMutex.withLock { activeBrokerRequests.remove(frame.requestId) }
        }
    }

    private suspend fun cancelBrokerRequest(frame: ProviderWorkerFrame) {
        val cancel = frame.message as ProviderWorkerMessage.Cancel
        val active = stateMutex.withLock {
            activeBrokerRequests[cancel.targetRequestId]
                ?.takeIf { it.parentRequestId == frame.inReplyTo }
        } ?: throw IllegalArgumentException("Cancellation does not match an active broker request")
        active.job.cancel(CancellationException("Provider worker cancelled its broker request"))
    }

    private suspend fun sendBrokerComplete(
        result: ProviderBrokerResult,
        inReplyTo: String,
    ): Boolean {
        val message = ProviderWorkerMessage.BrokerComplete(result)
        val frame = ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = "broker_response",
            inReplyTo = inReplyTo,
            message = message,
        )
        val boundedResult =
            try {
                ProviderWorkerProtocol.encode(frame)
                result
            } catch (_: IllegalArgumentException) {
                ProviderBrokerResult.Failed(
                    ProviderWorkerFailureCode.RESOURCE_LIMIT,
                    "Provider broker response metadata exceeds the frame size limit",
                )
            }
        send(ProviderWorkerMessage.BrokerComplete(boundedResult), inReplyTo = inReplyTo)
        return boundedResult is ProviderBrokerResult.HttpResponse
    }

    private suspend fun prepareBrokerResponse(
        response: ProviderWorkerBrokerResponse,
    ): Pair<ProviderBrokerResult, Pair<ProviderWorkerBlob, ByteArray>?> {
        val httpResponse = response.result as? ProviderBrokerResult.HttpResponse
        val bytes = response.responseBody
        if (bytes == null) {
            require(httpResponse?.body == null) { "Broker returned blob metadata without body bytes" }
            return response.result to null
        }
        require(httpResponse != null) { "Broker returned body bytes for a non-HTTP result" }
        require(bytes.size.toLong() <= PROVIDER_WORKER_MAX_BLOB_BYTES) {
            "Broker response blob exceeds the size limit"
        }
        require(bytes.size <= PROVIDER_WORKER_MAX_HTTP_BODY_BYTES) {
            "Broker response blob exceeds the HTTP body limit"
        }
        val blob = ProviderWorkerBlob(
            transferId = nextId("host_blob"),
            sizeBytes = bytes.size.toLong(),
            sha256 = digest.sha256(bytes),
        )
        return httpResponse.copy(body = blob) to (blob to bytes)
    }

    private suspend fun sendBlob(
        blob: ProviderWorkerBlob,
        bytes: ByteArray,
        inReplyTo: String,
    ) {
        var offset = 0
        var chunkIndex = 0
        while (offset < bytes.size) {
            val end = minOf(offset + PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES, bytes.size)
            send(
                ProviderWorkerMessage.BlobChunk(
                    transferId = blob.transferId,
                    sequence = chunkIndex,
                    bytes = bytes.copyOfRange(offset, end),
                    isLast = end == bytes.size,
                ),
                inReplyTo = inReplyTo,
            )
            offset = end
            chunkIndex += 1
        }
    }

    private suspend fun close(
        code: ProviderWorkerFailureCode,
        message: String,
        cause: Throwable? = null,
    ) {
        val (active, brokerJobs) = stateMutex.withLock {
            if (closed) return
            closed = true
            val pendingRequests = pending.values.toList()
            pending.clear()
            cancelledRequests.clear()
            val jobs = activeBrokerRequests.values.map { it.job }
            activeBrokerRequests.clear()
            pendingRequests to jobs
        }
        brokerJobs.forEach { it.cancel() }
        active.forEach { deferred ->
            deferred.complete(
                ProviderWorkerFrame(
                    protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                    requestId = nextId("closed"),
                    message = ProviderWorkerMessage.Failure(code, message),
                ),
            )
        }
        blobAssembler.clear()
        reader.cancel()
        try {
            transport.close()
        } catch (cleanupError: Throwable) {
            if (cause == null) throw cleanupError
            cause.addSuppressed(cleanupError)
            logger.e(cleanupError) { "Failed to close provider worker transport after $message" }
        }
    }

    private suspend fun nextId(prefix: String): String =
        stateMutex.withLock {
            sequence += 1
            "$prefix$sequence"
        }

    private fun resourceLimit(message: String): ProviderWorkerResult =
        ProviderWorkerResult.Failed(ProviderWorkerFailureCode.RESOURCE_LIMIT, message)

    private data class ActiveBrokerRequest(
        val parentRequestId: String,
        val job: Job,
    )

    public companion object {
        public suspend fun start(
            transport: ProviderWorkerFrameTransport,
            manifest: com.pilldev.zenith.provider.model.PluginManifest,
            broker: ProviderWorkerBroker,
            digest: ProviderWorkerDigest,
            scope: CoroutineScope,
            settings: Map<String, String> = emptyMap(),
            timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
            maxConcurrentRequests: Int = DEFAULT_CONCURRENT_REQUESTS,
        ): ProviderWorkerSession {
            require(timeoutMillis > 0) { "Provider worker timeout must be positive" }
            require(maxConcurrentRequests in 1..MAX_CONCURRENT_REQUESTS) {
                "Provider worker concurrency is outside the supported range"
            }
            val session = ProviderWorkerSession(
                transport = transport,
                broker = broker,
                digest = digest,
                scope = scope,
                timeoutMillis = timeoutMillis,
                maxConcurrentRequests = maxConcurrentRequests,
            )
            try {
                session.initialize(manifest, settings)
                return session
            } catch (error: Throwable) {
                session.close(
                    ProviderWorkerFailureCode.WORKER_CRASHED,
                    "Provider worker initialization failed",
                    error,
                )
                throw error
            }
        }

        private const val DEFAULT_TIMEOUT_MILLIS = 30_000L
        private const val DEFAULT_CONCURRENT_REQUESTS = 4
        private const val MAX_CONCURRENT_REQUESTS = 16
        private const val MAX_CONCURRENT_BROKER_REQUESTS = 8
        private const val MAX_CONCURRENT_HTTP_BROKER_REQUESTS = 2
        private const val MAX_CANCELLED_REQUESTS = 128
    }
}
