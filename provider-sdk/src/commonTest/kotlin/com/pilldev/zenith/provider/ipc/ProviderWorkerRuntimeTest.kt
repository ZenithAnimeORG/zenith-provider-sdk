package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ProviderWorkerRuntimeTest {
    @Test
    fun runtimeServicesBrokerCallsAndTransfersRequestAndResponseBodies() = runTest {
        val manifest = PluginManifest(id = ProviderId("runtime-test"), name = "Runtime test", version = "1")
        val requestBody = ByteArray(PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES + 5) { it.toByte() }
        val responseBody = ByteArray(PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES + 9) { (it * 2).toByte() }
        val digest = ProviderWorkerDigest { bytes ->
            when {
                bytes.contentEquals(requestBody) -> "a".repeat(64)
                bytes.contentEquals(responseBody) -> "b".repeat(64)
                bytes.isEmpty() -> "c".repeat(64)
                else -> "d".repeat(64)
            }
        }
        val (host, worker) = inMemoryTransports()
        var brokerRequestBody: ByteArray? = null
        val runtime = ProviderWorkerRuntime(
            transport = worker,
            digest = digest,
            scope = this,
            createHandler = { context ->
                ProviderWorkerFrameHandler { frame ->
                    when (val message = frame.message) {
                        is ProviderWorkerMessage.Initialize ->
                            response(
                                requestId = "ready",
                                inReplyTo = frame.requestId,
                                message = ProviderWorkerMessage.Ready(message.manifest),
                            )
                        is ProviderWorkerMessage.Invoke -> {
                            assertEquals("initial", context.settings.get("existing"))
                            val broker = context.broker.request(
                                ProviderBrokerOperation.HttpRequest(
                                    url = "https://provider.example/api",
                                    method = "POST",
                                ),
                                requestBody,
                            )
                            assertEquals(requestBody.toList(), brokerRequestBody?.toList())
                            assertEquals(responseBody.toList(), broker.responseBody?.toList())
                            context.settings.set("new-setting", "value")
                            response(
                                requestId = "complete",
                                inReplyTo = frame.requestId,
                                message = ProviderWorkerMessage.Complete(
                                    ProviderWorkerResult.Success(ProviderWorkerValue.UnitResult),
                                ),
                            )
                        }
                        ProviderWorkerMessage.Dispose ->
                            response(
                                requestId = "disposed",
                                inReplyTo = frame.requestId,
                                message = ProviderWorkerMessage.Disposed,
                            )
                        else -> error("Unexpected request")
                    }
                }
            },
        )
        val runtimeJob = launch { runtime.run() }
        host.send(request("initialize", ProviderWorkerMessage.Initialize(manifest, mapOf("existing" to "initial"))))
        assertIs<ProviderWorkerMessage.Ready>(host.receive().message)

        host.send(request("invoke", ProviderWorkerMessage.Invoke(ProviderWorkerOperation.ProbeStatus)))
        val brokerFrame = host.receive()
        val brokerRequest = assertIs<ProviderWorkerMessage.BrokerRequest>(brokerFrame.message)
        val httpRequest = assertIs<ProviderBrokerOperation.HttpRequest>(brokerRequest.operation)
        val incomingBlob = assertNotNull(httpRequest.body)
        val incomingAssembler = ProviderWorkerBlobAssembler(digest)
        incomingAssembler.expect(incomingBlob, brokerFrame.requestId)
        var requestSequence = 0
        var requestIsLast = false
        while (!requestIsLast) {
            val chunkFrame = host.receive()
            val chunk = assertIs<ProviderWorkerMessage.BlobChunk>(chunkFrame.message)
            assertEquals(brokerFrame.requestId, chunkFrame.inReplyTo)
            incomingAssembler.append(chunkFrame)
            requestSequence += 1
            requestIsLast = chunk.isLast
        }
        assertEquals(2, requestSequence)
        brokerRequestBody = incomingAssembler.read(incomingBlob)
        incomingAssembler.discard(incomingBlob.transferId)

        val responseBlob = ProviderWorkerBlob("host_response", responseBody.size.toLong(), "b".repeat(64))
        host.send(
            response(
                requestId = "broker_complete",
                inReplyTo = brokerFrame.requestId,
                message = ProviderWorkerMessage.BrokerComplete(
                    ProviderBrokerResult.HttpResponse(200, body = responseBlob),
                ),
            ),
        )
        sendBlob(host, responseBlob.transferId, brokerFrame.requestId, responseBody)

        val settingFrame = host.receive()
        val settingRequest = assertIs<ProviderWorkerMessage.BrokerRequest>(settingFrame.message)
        assertEquals(
            ProviderBrokerOperation.SetSetting("new-setting", "value"),
            settingRequest.operation,
        )
        host.send(
            response(
                requestId = "setting_complete",
                inReplyTo = settingFrame.requestId,
                message = ProviderWorkerMessage.BrokerComplete(ProviderBrokerResult.UnitResult),
            ),
        )

        val complete = host.receive()
        val result = assertIs<ProviderWorkerMessage.Complete>(complete.message)
        assertEquals("invoke", complete.inReplyTo)
        assertEquals(ProviderWorkerValue.UnitResult, (result.result as ProviderWorkerResult.Success).value)

        host.send(request("dispose", ProviderWorkerMessage.Dispose))
        assertIs<ProviderWorkerMessage.Disposed>(host.receive().message)
        runtimeJob.cancelAndJoin()
    }

    @Test
    fun runtimeServicesBrokerHttpClientRequests() = kotlinx.coroutines.runBlocking {
        val manifest = PluginManifest(id = ProviderId("runtime-http-test"), name = "Runtime HTTP test", version = "1")
        val responseBody = "http-response-body".encodeToByteArray()
        val digest = ProviderWorkerDigest { "e".repeat(64) }
        val (host, worker) = inMemoryTransports()
        val runtime = ProviderWorkerRuntime(
            transport = worker,
            digest = digest,
            scope = this,
            createHandler = { context ->
                ProviderWorkerFrameHandler { frame ->
                    when (val message = frame.message) {
                        is ProviderWorkerMessage.Initialize ->
                            response(
                                requestId = "ready",
                                inReplyTo = frame.requestId,
                                message = ProviderWorkerMessage.Ready(message.manifest),
                            )
                        is ProviderWorkerMessage.Invoke -> {
                            val brokerContext = ProviderWorkerBrokerContext(context.broker, context.settings)
                            val httpResponse = brokerContext.httpClient.get("https://provider.example/test")
                            assertEquals("http-response-body", httpResponse.bodyAsText())
                            response(
                                requestId = "complete",
                                inReplyTo = frame.requestId,
                                message = ProviderWorkerMessage.Complete(
                                    ProviderWorkerResult.Success(ProviderWorkerValue.UnitResult),
                                ),
                            )
                        }
                        ProviderWorkerMessage.Dispose ->
                            response(
                                requestId = "disposed",
                                inReplyTo = frame.requestId,
                                message = ProviderWorkerMessage.Disposed,
                            )
                        else -> error("Unexpected request")
                    }
                }
            },
        )
        val runtimeJob = launch { runtime.run() }
        host.send(request("initialize", ProviderWorkerMessage.Initialize(manifest, emptyMap())))
        assertIs<ProviderWorkerMessage.Ready>(host.receive().message)

        host.send(request("invoke", ProviderWorkerMessage.Invoke(ProviderWorkerOperation.ProbeStatus)))
        val brokerFrame = host.receive()
        val brokerRequest = assertIs<ProviderWorkerMessage.BrokerRequest>(brokerFrame.message)
        val httpRequest = assertIs<ProviderBrokerOperation.HttpRequest>(brokerRequest.operation)
        assertEquals("https://provider.example/test", httpRequest.url)

        val responseBlob = ProviderWorkerBlob("host_response", responseBody.size.toLong(), "e".repeat(64))
        host.send(
            response(
                requestId = "broker_complete",
                inReplyTo = brokerFrame.requestId,
                message = ProviderWorkerMessage.BrokerComplete(
                    ProviderBrokerResult.HttpResponse(200, body = responseBlob),
                ),
            ),
        )
        sendBlob(host, responseBlob.transferId, brokerFrame.requestId, responseBody)

        val complete = host.receive()
        val result = assertIs<ProviderWorkerMessage.Complete>(complete.message)
        assertEquals("invoke", complete.inReplyTo)
        assertEquals(ProviderWorkerValue.UnitResult, (result.result as ProviderWorkerResult.Success).value)

        host.send(request("dispose", ProviderWorkerMessage.Dispose))
        assertIs<ProviderWorkerMessage.Disposed>(host.receive().message)
        runtimeJob.cancelAndJoin()
    }

    private fun inMemoryTransports(): Pair<ChannelTransport, ChannelTransport> {
        val hostToWorker = Channel<ProviderWorkerFrame>(Channel.UNLIMITED)
        val workerToHost = Channel<ProviderWorkerFrame>(Channel.UNLIMITED)
        return ChannelTransport(workerToHost, hostToWorker) to ChannelTransport(hostToWorker, workerToHost)
    }

    private fun request(
        requestId: String,
        message: ProviderWorkerMessage,
    ): ProviderWorkerFrame =
        ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = requestId,
            message = message,
        )

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
    ) {
        var offset = 0
        var sequence = 0
        while (offset < bytes.size) {
            val end = minOf(offset + PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES, bytes.size)
            transport.send(
                response(
                    requestId = "host_chunk_$sequence",
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
