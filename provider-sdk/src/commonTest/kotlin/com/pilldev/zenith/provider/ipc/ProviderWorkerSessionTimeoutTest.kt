package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderId
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProviderWorkerSessionTimeoutTest {
    @Test
    fun requestDeadlineClosesTransportForAnUnresponsiveWorker() = runTest {
        val manifest = PluginManifest(id = ProviderId("timeout-test"), name = "Timeout test", version = "1")
        val hostToWorker = Channel<ProviderWorkerFrame>(Channel.UNLIMITED)
        val workerToHost = Channel<ProviderWorkerFrame>(Channel.UNLIMITED)
        val hostTransport = TrackingTransport(workerToHost, hostToWorker)
        val workerTransport = TrackingTransport(hostToWorker, workerToHost)
        val worker = launch {
            val initialize = workerTransport.receive()
            workerTransport.send(
                response(
                    requestId = "worker_ready",
                    inReplyTo = initialize.requestId,
                    message = ProviderWorkerMessage.Ready(manifest),
                ),
            )
            workerTransport.receive()
            awaitCancellation()
        }
        val session = ProviderWorkerSession.start(
            transport = hostTransport,
            manifest = manifest,
            broker = ProviderWorkerBroker { _, _ ->
                ProviderWorkerBrokerResponse(ProviderBrokerResult.UnitResult)
            },
            digest = ProviderWorkerDigest { "a".repeat(64) },
            scope = this,
            timeoutMillis = TIMEOUT_MILLIS,
        )
        val invocation = async { session.invoke(ProviderWorkerOperation.ProbeStatus) }

        runCurrent()
        advanceTimeBy(TIMEOUT_MILLIS)

        assertFailsWith<TimeoutCancellationException> { invocation.await() }
        assertTrue(hostTransport.closed)
        worker.cancelAndJoin()
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

    private class TrackingTransport(
        private val incoming: Channel<ProviderWorkerFrame>,
        private val outgoing: Channel<ProviderWorkerFrame>,
    ) : ProviderWorkerFrameTransport {
        var closed = false
            private set

        override suspend fun send(frame: ProviderWorkerFrame) {
            outgoing.send(frame)
        }

        override suspend fun receive(): ProviderWorkerFrame = incoming.receive()

        override suspend fun close() {
            closed = true
            incoming.close()
            outgoing.close()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 100L
    }
}
