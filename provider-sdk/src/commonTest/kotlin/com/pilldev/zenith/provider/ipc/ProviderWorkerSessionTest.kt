package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ProviderWorkerSessionTest {
    @Test
    fun sessionInitializesCorrelatesInvocationAndServicesBrokerRequests() = runTest {
        val manifest = PluginManifest(id = ProviderId("session-test"), name = "Session test", version = "1")
        val (hostTransport, workerTransport) = inMemoryTransports()
        val worker = launch {
            val initialize = workerTransport.receive()
            workerTransport.send(
                response(
                    requestId = "worker_ready",
                    inReplyTo = initialize.requestId,
                    message = ProviderWorkerMessage.Ready(manifest),
                ),
            )

            val invoke = workerTransport.receive()
            workerTransport.send(
                response(
                    requestId = "worker_broker",
                    inReplyTo = invoke.requestId,
                    message = ProviderWorkerMessage.BrokerRequest(ProviderBrokerOperation.GetSetting("token")),
                ),
            )
            val brokerComplete = workerTransport.receive()
            assertEquals("worker_broker", brokerComplete.inReplyTo)
            assertIs<ProviderWorkerMessage.BrokerComplete>(brokerComplete.message)
            workerTransport.send(
                response(
                    requestId = "worker_complete",
                    inReplyTo = invoke.requestId,
                    message = ProviderWorkerMessage.Complete(
                        ProviderWorkerResult.Success(ProviderWorkerValue.UnitResult),
                    ),
                ),
            )

            val dispose = workerTransport.receive()
            workerTransport.send(
                response(
                    requestId = "worker_disposed",
                    inReplyTo = dispose.requestId,
                    message = ProviderWorkerMessage.Disposed,
                ),
            )
        }
        val session = ProviderWorkerSession.start(
            transport = hostTransport,
            manifest = manifest,
            broker = ProviderWorkerBroker { operation, _ ->
                assertEquals(ProviderBrokerOperation.GetSetting("token"), operation)
                ProviderWorkerBrokerResponse(ProviderBrokerResult.Setting("secret"))
            },
            digest = ProviderWorkerDigest { "a".repeat(64) },
            scope = this,
        )

        assertEquals(manifest, session.ready.manifest)
        assertEquals(
            ProviderWorkerResult.Success(ProviderWorkerValue.UnitResult),
            session.invoke(ProviderWorkerOperation.ProbeStatus),
        )
        session.dispose()
        worker.join()
    }

    @Test
    fun sessionRejectsWorkerManifestMismatch() = runTest {
        val expectedManifest = PluginManifest(id = ProviderId("expected"), name = "Expected", version = "1")
        val otherManifest = PluginManifest(id = ProviderId("other"), name = "Other", version = "1")
        val (hostTransport, workerTransport) = inMemoryTransports()
        val worker = launch {
            val initialize = workerTransport.receive()
            workerTransport.send(
                response(
                    requestId = "worker_ready",
                    inReplyTo = initialize.requestId,
                    message = ProviderWorkerMessage.Ready(otherManifest),
                ),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            ProviderWorkerSession.start(
                transport = hostTransport,
                manifest = expectedManifest,
                broker = ProviderWorkerBroker { _, _ ->
                    ProviderWorkerBrokerResponse(ProviderBrokerResult.UnitResult)
                },
                digest = ProviderWorkerDigest { "a".repeat(64) },
                scope = this,
            )
        }
        worker.cancel()
    }

    @Test
    fun sessionRejectsWorkerResponseWithUnknownCorrelationId() = runTest {
        val manifest = PluginManifest(id = ProviderId("correlation-test"), name = "Correlation test", version = "1")
        val (hostTransport, workerTransport) = inMemoryTransports()
        val worker = launch {
            workerTransport.receive()
            workerTransport.send(
                response(
                    requestId = "worker_ready",
                    inReplyTo = "unknown_request",
                    message = ProviderWorkerMessage.Ready(manifest),
                ),
            )
        }

        assertFailsWith<IllegalStateException> {
            ProviderWorkerSession.start(
                transport = hostTransport,
                manifest = manifest,
                broker = ProviderWorkerBroker { _, _ ->
                    ProviderWorkerBrokerResponse(ProviderBrokerResult.UnitResult)
                },
                digest = ProviderWorkerDigest { "a".repeat(64) },
                scope = this,
            )
        }
        worker.cancel()
    }

    @Test
    fun sessionTransfersBrokerRequestAndResponseBodiesInBoundedChunks() = runTest {
        val manifest = PluginManifest(id = ProviderId("blob-session"), name = "Blob session", version = "1")
        val requestBytes = ByteArray(PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES + 3) { it.toByte() }
        val responseBytes = ByteArray(PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES + 5) { (it * 2).toByte() }
        val requestHash = "a".repeat(64)
        val responseHash = "b".repeat(64)
        val digest = ProviderWorkerDigest { bytes ->
            when {
                bytes.contentEquals(requestBytes) -> requestHash
                bytes.contentEquals(responseBytes) -> responseHash
                else -> "c".repeat(64)
            }
        }
        val (hostTransport, workerTransport) = inMemoryTransports()
        val worker = launch {
            val initialize = workerTransport.receive()
            workerTransport.send(
                response(
                    requestId = "worker_ready",
                    inReplyTo = initialize.requestId,
                    message = ProviderWorkerMessage.Ready(manifest),
                ),
            )
            val invoke = workerTransport.receive()
            val brokerRequestId = "worker_broker"
            workerTransport.send(
                response(
                    requestId = brokerRequestId,
                    inReplyTo = invoke.requestId,
                    message = ProviderWorkerMessage.BrokerRequest(
                        ProviderBrokerOperation.HttpRequest(
                            url = "https://provider.example/data",
                            method = "POST",
                            body = ProviderWorkerBlob("worker_blob", requestBytes.size.toLong(), requestHash),
                        ),
                    ),
                ),
            )
            sendBlob(workerTransport, "worker_blob", brokerRequestId, requestBytes, "worker_chunk")
            val brokerComplete = workerTransport.receive()
            val complete = assertIs<ProviderWorkerMessage.BrokerComplete>(brokerComplete.message)
            val httpResponse = assertIs<ProviderBrokerResult.HttpResponse>(complete.result)
            val responseBlob = assertNotNull(httpResponse.body)
            val responseChunks = mutableListOf<ByteArray>()
            var isLastChunk = false
            while (!isLastChunk) {
                val responseChunk = workerTransport.receive()
                val chunk = assertIs<ProviderWorkerMessage.BlobChunk>(responseChunk.message)
                assertEquals(brokerRequestId, responseChunk.inReplyTo)
                assertEquals(responseBlob.transferId, chunk.transferId)
                responseChunks += chunk.bytes
                isLastChunk = chunk.isLast
            }
            val assembledResponse = ByteArray(responseChunks.sumOf { it.size })
            var responseOffset = 0
            responseChunks.forEach { chunk ->
                chunk.copyInto(assembledResponse, responseOffset)
                responseOffset += chunk.size
            }
            assertEquals(responseBytes.toList(), assembledResponse.toList())
            workerTransport.send(
                response(
                    requestId = "worker_complete",
                    inReplyTo = invoke.requestId,
                    message = ProviderWorkerMessage.Complete(
                        ProviderWorkerResult.Success(ProviderWorkerValue.UnitResult),
                    ),
                ),
            )
            val dispose = workerTransport.receive()
            workerTransport.send(
                response(
                    requestId = "worker_disposed",
                    inReplyTo = dispose.requestId,
                    message = ProviderWorkerMessage.Disposed,
                ),
            )
        }

        val session = ProviderWorkerSession.start(
            transport = hostTransport,
            manifest = manifest,
            broker = ProviderWorkerBroker { operation, body ->
                assertIs<ProviderBrokerOperation.HttpRequest>(operation)
                assertEquals(requestBytes.toList(), body?.toList())
                ProviderWorkerBrokerResponse(
                    result = ProviderBrokerResult.HttpResponse(
                        statusCode = 200,
                    ),
                    responseBody = responseBytes,
                )
            },
            digest = digest,
            scope = this,
        )

        assertEquals(
            ProviderWorkerResult.Success(ProviderWorkerValue.UnitResult),
            session.invoke(ProviderWorkerOperation.ProbeStatus),
        )
        session.dispose()
        worker.join()
    }

    private fun inMemoryTransports(): Pair<ChannelTransport, ChannelTransport> {
        val hostToWorker = Channel<ProviderWorkerFrame>(Channel.UNLIMITED)
        val workerToHost = Channel<ProviderWorkerFrame>(Channel.UNLIMITED)
        return ChannelTransport(workerToHost, hostToWorker) to ChannelTransport(hostToWorker, workerToHost)
    }

    private fun response(
        requestId: String,
        inReplyTo: String,
        message: ProviderWorkerMessage,
    ): ProviderWorkerFrame =
        ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = requestId,
            inReplyTo = inReplyTo,
            message = message,
        )

    private suspend fun sendBlob(
        transport: ProviderWorkerFrameTransport,
        transferId: String,
        inReplyTo: String,
        bytes: ByteArray,
        requestIdPrefix: String,
    ) {
        var offset = 0
        var sequence = 0
        while (offset < bytes.size) {
            val end = minOf(offset + PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES, bytes.size)
            transport.send(
                response(
                    requestId = "${requestIdPrefix}_$sequence",
                    inReplyTo = inReplyTo,
                    message = ProviderWorkerMessage.BlobChunk(
                        transferId = transferId,
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

    private class ChannelTransport(
        private val incoming: Channel<ProviderWorkerFrame>,
        private val outgoing: Channel<ProviderWorkerFrame>,
    ) : ProviderWorkerFrameTransport {
        override suspend fun send(frame: ProviderWorkerFrame) {
            outgoing.send(frame)
        }

        override suspend fun receive(): ProviderWorkerFrame = incoming.receive()

        override suspend fun close() {
            incoming.close()
            outgoing.close()
        }
    }
}
