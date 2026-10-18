package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.model.PluginManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import co.touchlab.kermit.Logger

private val logger = Logger.withTag("ProviderWorkerRuntime")

internal class WorkerRequestContext(
    val requestId: String,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<WorkerRequestContext>
}

public fun interface ProviderWorkerFrameHandler {
    public suspend fun handle(frame: ProviderWorkerFrame): ProviderWorkerFrame
}

public data class ProviderWorkerRuntimeContext(
    public val broker: ProviderWorkerBrokerClient,
    public val readBlob: suspend (ProviderWorkerBlob) -> ByteArray,
    public val settings: ProviderWorkerSettings,
)

public class ProviderWorkerSettings(
    initial: Map<String, String> = emptyMap(),
) {
    private val state = MutableStateFlow(SettingsState(values = initial, changes = emptyMap()))

    public fun get(key: String): String? = state.value.values[key]

    public fun set(
        key: String,
        value: String?,
    ) {
        require(key.isNotBlank()) { "Provider setting key must not be blank" }
        state.update { current ->
            if (current.values[key] == value) {
                current
            } else {
                val values = current.values.toMutableMap()
                if (value == null) values.remove(key) else values[key] = value
                current.copy(values = values, changes = current.changes + (key to value))
            }
        }
    }

    internal fun reset(initial: Map<String, String>) {
        state.value = SettingsState(values = initial, changes = emptyMap())
    }

    internal fun consumeChanges(): Map<String, String?> {
        while (true) {
            val current = state.value
            if (current.changes.isEmpty()) return emptyMap()
            if (state.compareAndSet(current, current.copy(changes = emptyMap()))) {
                return current.changes
            }
        }
    }

    private data class SettingsState(
        val values: Map<String, String?>,
        val changes: Map<String, String?>,
    )
}

public class ProviderWorkerBrokerClient internal constructor(
    private val sendFrame: suspend (ProviderWorkerFrame) -> Unit,
    private val digest: ProviderWorkerDigest,
    private val blobs: ProviderWorkerBlobAssembler,
    private val nextId: suspend (String) -> String,
    private val timeoutMillis: Long,
) {
    private val mutex = Mutex()
    private val slots = Semaphore(MAX_CONCURRENT_BROKER_REQUESTS)
    private val pending = mutableMapOf<String, CompletableDeferred<ProviderWorkerBrokerResponse>>()

    public suspend fun request(
        operation: ProviderBrokerOperation,
        requestBody: ByteArray? = null,
    ): ProviderWorkerBrokerResponse =
        slots.withPermit {
            val parentRequestId = currentCoroutineContext()[WorkerRequestContext]?.requestId
                ?: throw IllegalStateException("Broker request was made outside a worker operation")
            require(requestBody == null || requestBody.size <= PROVIDER_WORKER_MAX_HTTP_BODY_BYTES) {
                "Provider HTTP request exceeds the broker body limit"
            }
            val requestId = nextId("worker_broker")
            val bodyBlob = requestBody?.let { bytes ->
                ProviderWorkerBlob(
                    transferId = nextId("worker_blob"),
                    sizeBytes = bytes.size.toLong(),
                    sha256 = digest.sha256(bytes),
                )
            }
            val brokerOperation = when (operation) {
                is ProviderBrokerOperation.HttpRequest -> {
                    require(operation.body == null) { "HTTP request body metadata is created by the worker broker client" }
                    operation.copy(body = bodyBlob)
                }
                else -> {
                    require(requestBody == null) { "Request body is only supported for HTTP broker operations" }
                    operation
                }
            }
            val response = CompletableDeferred<ProviderWorkerBrokerResponse>()
            mutex.withLock { pending[requestId] = response }
            var responseTransferId: String? = null
            try {
                sendFrame(
                    ProviderWorkerFrame(
                        protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                        requestId = requestId,
                        inReplyTo = parentRequestId,
                        message = ProviderWorkerMessage.BrokerRequest(brokerOperation),
                    ),
                )
                if (bodyBlob != null && requestBody != null) {
                    sendBlob(bodyBlob, requestBody, requestId)
                }
                val brokerResponse =
                    try {
                        withTimeout(timeoutMillis) { response.await() }
                    } catch (cancelled: CancellationException) {
                        runCatching {
                            sendFrame(
                                ProviderWorkerFrame(
                                    protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                                    requestId = nextId("worker_cancel"),
                                    inReplyTo = parentRequestId,
                                    message = ProviderWorkerMessage.Cancel(requestId),
                                ),
                            )
                        }
                        throw cancelled
                    }
                val responseBlob = (brokerResponse.result as? ProviderBrokerResult.HttpResponse)?.body
                if (responseBlob == null) {
                    require(brokerResponse.responseBody == null) {
                        "Worker broker response contains bytes without blob metadata"
                    }
                    brokerResponse
                } else {
                    responseTransferId = responseBlob.transferId
                    brokerResponse.copy(responseBody = blobs.read(responseBlob)).also {
                        blobs.discard(responseBlob.transferId)
                        responseTransferId = null
                    }
                }
            } finally {
                mutex.withLock { pending.remove(requestId) }
                responseTransferId?.let { blobs.discard(it) }
            }
        }

    internal suspend fun accept(frame: ProviderWorkerFrame) {
        val requestId = frame.inReplyTo
            ?: throw IllegalArgumentException("Broker response is missing its correlation ID")
        val message = frame.message
        if (message is ProviderWorkerMessage.BlobChunk) {
            blobs.append(frame)
            return
        }
        val complete = message as? ProviderWorkerMessage.BrokerComplete
            ?: throw IllegalArgumentException("Worker expected a broker completion")
        val response = mutex.withLock { pending[requestId] }
            ?: throw IllegalArgumentException("Broker completion does not match an active request")
        val blob = (complete.result as? ProviderBrokerResult.HttpResponse)?.body
        if (blob != null && blob.sizeBytes > PROVIDER_WORKER_MAX_HTTP_BODY_BYTES) {
            response.complete(
                ProviderWorkerBrokerResponse(
                    ProviderBrokerResult.Failed(
                        ProviderWorkerFailureCode.RESOURCE_LIMIT,
                        "Provider HTTP response exceeds the broker body limit",
                    ),
                ),
            )
            return
        }
        if (blob != null) blobs.expect(blob, requestId)
        response.complete(ProviderWorkerBrokerResponse(complete.result))
    }

    private suspend fun sendBlob(
        blob: ProviderWorkerBlob,
        bytes: ByteArray,
        inReplyTo: String,
    ) {
        var offset = 0
        var sequence = 0
        while (offset < bytes.size) {
            val end = minOf(offset + PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES, bytes.size)
            sendFrame(
                ProviderWorkerFrame(
                    protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                    requestId = nextId("worker_chunk"),
                    inReplyTo = inReplyTo,
                    message = ProviderWorkerMessage.BlobChunk(
                        transferId = blob.transferId,
                        sequence = sequence,
                        bytes = bytes.copyOfRange(offset, end),
                        isLast = end == bytes.size,
                    ),
                ),
            )
            offset = end
            sequence += 1
        }
    }

    private companion object {
        const val MAX_CONCURRENT_BROKER_REQUESTS = 8
    }
}

public class ProviderWorkerRuntime(
    private val transport: ProviderWorkerFrameTransport,
    digest: ProviderWorkerDigest,
    private val scope: CoroutineScope,
    private val createHandler: (ProviderWorkerRuntimeContext) -> ProviderWorkerFrameHandler,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : Closeable {
    private val runtimeScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private val sendMutex = Mutex()
    private val stateMutex = Mutex()
    private val blobs = ProviderWorkerBlobAssembler(digest)
    private val settings = ProviderWorkerSettings()
    private var sequence = 0L
    private var closed = false
    private var initializationRequested = false
    private val activeRequests = mutableMapOf<String, Job>()
    private val hostCancelledRequests = mutableSetOf<String>()
    private val initialized = CompletableDeferred<Boolean>()
    private val broker = ProviderWorkerBrokerClient(
        sendFrame = ::send,
        digest = digest,
        blobs = blobs,
        nextId = ::nextId,
        timeoutMillis = timeoutMillis,
    )
    private val handler = createHandler(
        ProviderWorkerRuntimeContext(
            broker = broker,
            readBlob = ::readBlob,
            settings = settings,
        ),
    )

    init {
        require(timeoutMillis > 0) { "Provider worker runtime timeout must be positive" }
    }

    public suspend fun run() {
        try {
            while (true) {
                val frame = transport.receive()
                ProviderWorkerProtocol.encode(frame)
                when (frame.message) {
                    is ProviderWorkerMessage.BlobChunk -> blobs.append(frame)
                    is ProviderWorkerMessage.BrokerComplete -> broker.accept(frame)
                    is ProviderWorkerMessage.Cancel -> cancelRequest(frame)
                    is ProviderWorkerMessage.Initialize,
                    is ProviderWorkerMessage.Invoke,
                    ProviderWorkerMessage.Dispose,
                    -> dispatch(frame)
                    else -> throw IllegalArgumentException("Unexpected host message in provider worker runtime")
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            close()
            blobs.clear()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        activeRequests.values.forEach(Job::cancel)
        activeRequests.clear()
        runtimeScope.cancel("Provider worker runtime closed")
    }

    private suspend fun dispatch(frame: ProviderWorkerFrame) {
        require(frame.inReplyTo == null) { "Host operation frame must not contain a correlation ID" }
        val shouldDispatch = stateMutex.withLock {
            if (closed || activeRequests.containsKey(frame.requestId) ||
                (frame.message is ProviderWorkerMessage.Initialize && initializationRequested)
            ) {
                false
            } else {
                if (frame.message is ProviderWorkerMessage.Initialize) initializationRequested = true
                true
            }
        }
        require(shouldDispatch) { "Provider worker received a duplicate active request ID" }

        val inputBlob = (frame.message as? ProviderWorkerMessage.Invoke)
            ?.operation.let { it as? ProviderWorkerOperation.ImportBackup }
            ?.input
        if (inputBlob != null) blobs.expect(inputBlob, frame.requestId)
        if (frame.message is ProviderWorkerMessage.Initialize) {
            settings.reset(frame.message.settings)
        }

        val job = runtimeScope.launch(start = CoroutineStart.LAZY) {
            try {
                if (frame.message is ProviderWorkerMessage.Invoke || frame.message == ProviderWorkerMessage.Dispose) {
                    if (!initialized.await()) {
                        sendFailure(frame, ProviderWorkerFailureCode.INVALID_REQUEST, "Provider worker is not initialized")
                        return@launch
                    }
                }
                val response = withTimeout(timeoutMillis) {
                    withContext(WorkerRequestContext(frame.requestId)) { handler.handle(frame) }
                }
                withContext(WorkerRequestContext(frame.requestId)) { flushSettings() }
                require(response.inReplyTo == frame.requestId) {
                    "Worker handler returned a response with invalid correlation"
                }
                require(response.requestId != frame.requestId) { "Worker handler reused the host request ID" }
                ProviderWorkerProtocol.encode(response)
                if (frame.message is ProviderWorkerMessage.Initialize) {
                    initialized.complete(response.message is ProviderWorkerMessage.Ready)
                }
                send(response)
            } catch (cancelled: CancellationException) {
                if (frame.message is ProviderWorkerMessage.Initialize && !initialized.isCompleted) {
                    initialized.complete(false)
                }
                if (cancelled is kotlinx.coroutines.TimeoutCancellationException) {
                    logger.e { "Provider worker operation exceeded deadline for ${frame.message::class.simpleName} (requestId=${frame.requestId})" }
                    withContext(NonCancellable) {
                        sendFailure(
                            frame,
                            ProviderWorkerFailureCode.WORKER_CRASHED,
                            "Provider worker operation exceeded its deadline",
                        )
                    }
                } else {
                    val cancelledByHost = stateMutex.withLock { hostCancelledRequests.remove(frame.requestId) }
                    if (!cancelledByHost && runtimeScope.coroutineContext[Job]?.isActive == true) {
                        withContext(NonCancellable) {
                            sendFailure(frame, ProviderWorkerFailureCode.CANCELLED, "Provider worker operation was cancelled")
                        }
                    }
                }
            } catch (error: Exception) {
                logger.e(error) { "Provider worker operation failed for ${frame.message::class.simpleName} (requestId=${frame.requestId})" }
                if (frame.message is ProviderWorkerMessage.Initialize && !initialized.isCompleted) {
                    initialized.complete(false)
                }
                sendFailure(
                    frame,
                    ProviderWorkerFailureCode.PROVIDER_FAILURE,
                    error.message ?: "Provider worker operation failed",
                )
            } finally {
                inputBlob?.let { blobs.discard(it.transferId) }
                stateMutex.withLock { activeRequests.remove(frame.requestId) }
            }
        }
        stateMutex.withLock { activeRequests[frame.requestId] = job }
        job.start()
    }

    private suspend fun readBlob(blob: ProviderWorkerBlob): ByteArray =
        try {
            blobs.read(blob)
        } finally {
            blobs.discard(blob.transferId)
        }

    private suspend fun flushSettings() {
        for ((key, value) in settings.consumeChanges()) {
            val result = broker.request(ProviderBrokerOperation.SetSetting(key, value)).result
            when (result) {
                ProviderBrokerResult.UnitResult -> Unit
                is ProviderBrokerResult.Failed -> throw ProviderWorkerBrokerException(result)
                else -> throw IllegalStateException("Provider settings broker returned an invalid response")
            }
        }
    }

    private suspend fun cancelRequest(frame: ProviderWorkerFrame) {
        val cancel = frame.message as ProviderWorkerMessage.Cancel
        require(frame.inReplyTo == cancel.targetRequestId) { "Invalid worker cancellation correlation" }
        val job = stateMutex.withLock {
            activeRequests[cancel.targetRequestId]?.also { hostCancelledRequests.add(cancel.targetRequestId) }
        } ?: throw IllegalArgumentException("Cancellation does not match an active provider operation")
        job.cancel(CancellationException("Provider operation was cancelled by the host"))
    }

    private suspend fun sendFailure(
        request: ProviderWorkerFrame,
        code: ProviderWorkerFailureCode,
        message: String,
    ) {
        send(
            ProviderWorkerFrame(
                protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                requestId = nextId("worker_failure"),
                inReplyTo = request.requestId,
                message = ProviderWorkerMessage.Failure(code, message),
            ),
        )
    }

    private suspend fun send(frame: ProviderWorkerFrame) {
        ProviderWorkerProtocol.encode(frame)
        sendMutex.withLock { transport.send(frame) }
    }

    private suspend fun nextId(prefix: String): String =
        stateMutex.withLock {
            sequence += 1
            "$prefix$sequence"
        }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L
    }
}
