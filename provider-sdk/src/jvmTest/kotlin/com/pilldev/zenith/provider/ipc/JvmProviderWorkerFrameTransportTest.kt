package com.pilldev.zenith.provider.ipc

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmProviderWorkerFrameTransportTest {
    @Test
    fun transportReadsBoundedUtf8JsonLinesAndWritesNewlineDelimitedFrames() = runTest {
        val incoming = frame("initialize", ProviderWorkerMessage.Dispose)
        val input = ByteArrayInputStream("$incoming\r\n".encodeToByteArray())
        val output = ByteArrayOutputStream()
        val transport = JvmProviderWorkerFrameTransport(input, output, Dispatchers.IO)
        val expected = ProviderWorkerProtocol.decode(incoming)

        assertEquals(expected, transport.receive())
        transport.send(expected)
        assertEquals("$incoming\n", output.toString(Charsets.UTF_8.name()))
        transport.close()
    }

    @Test
    fun transportRejectsAnOversizedFrameBeforeDecoding() = runTest {
        val input = ByteArrayInputStream(
            ByteArray(PROVIDER_WORKER_MAX_FRAME_BYTES + 2) { ' '.code.toByte() } + byteArrayOf('\n'.code.toByte()),
        )
        val transport = JvmProviderWorkerFrameTransport(input, ByteArrayOutputStream(), Dispatchers.IO)

        assertFailsWith<IllegalArgumentException> { transport.receive() }
        transport.close()
    }

    @Test
    fun transportRejectsPartialFramesAndInvalidUtf8() = runTest {
        val partial = JvmProviderWorkerFrameTransport(
            ByteArrayInputStream("{\"partial\"".encodeToByteArray()),
            ByteArrayOutputStream(),
            Dispatchers.IO,
        )
        assertFailsWith<IllegalArgumentException> { partial.receive() }
        partial.close()

        val invalidUtf8 = JvmProviderWorkerFrameTransport(
            ByteArrayInputStream(byteArrayOf(0xC3.toByte(), 0x28, '\n'.code.toByte())),
            ByteArrayOutputStream(),
            Dispatchers.IO,
        )
        assertFailsWith<IllegalArgumentException> { invalidUtf8.receive() }
        invalidUtf8.close()
    }

    private fun frame(
        requestId: String,
        message: ProviderWorkerMessage,
    ): String =
        ProviderWorkerProtocol.encode(
            ProviderWorkerFrame(
                protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                requestId = requestId,
                message = message,
            ),
        )
}
