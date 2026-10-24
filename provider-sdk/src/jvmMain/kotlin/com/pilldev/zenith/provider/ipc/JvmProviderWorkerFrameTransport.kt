package com.pilldev.zenith.provider.ipc

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.CharacterCodingException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

public class JvmProviderWorkerFrameTransport(
    private val input: InputStream,
    private val output: OutputStream,
    private val ioDispatcher: CoroutineDispatcher,
) : ProviderWorkerFrameTransport {
    private val closed = AtomicBoolean(false)
    private val receiving = AtomicBoolean(false)
    private val sendLock = Any()

    override suspend fun send(frame: ProviderWorkerFrame) {
        check(!closed.get()) { "Provider worker transport is closed" }
        val encoded = ProviderWorkerProtocol.encode(frame).encodeToByteArray()
        require(encoded.size <= PROVIDER_WORKER_MAX_FRAME_BYTES) {
            "Provider worker frame exceeds the transport limit"
        }
        withContext(ioDispatcher) {
            synchronized(sendLock) {
                check(!closed.get()) { "Provider worker transport is closed" }
                output.write(encoded)
                output.write(NEW_LINE)
                output.flush()
            }
        }
    }

    override suspend fun receive(): ProviderWorkerFrame {
        check(!closed.get()) { "Provider worker transport is closed" }
        check(receiving.compareAndSet(false, true)) { "Provider worker transport supports one reader" }
        try {
            return withContext(ioDispatcher) {
                val line = java.io.ByteArrayOutputStream(16 * 1024)
                var length = 0
                while (true) {
                    val value = input.read()
                    if (value < 0) {
                        if (length == 0) throw kotlinx.coroutines.CancellationException("Provider worker transport reached end of stream")
                        throw IllegalArgumentException("Provider worker transport ended inside a frame")
                    }
                    if (value == NEW_LINE) break
                    if (length >= PROVIDER_WORKER_MAX_FRAME_BYTES) {
                        throw IllegalArgumentException("Provider worker frame exceeds the transport limit")
                    }
                    line.write(value)
                    length += 1
                }
                val bytes = line.toByteArray()
                var effectiveLength = bytes.size
                if (effectiveLength > 0 && bytes[effectiveLength - 1] == CARRIAGE_RETURN.toByte()) effectiveLength -= 1
                require(effectiveLength > 0) { "Provider worker transport received an empty frame" }
                val encoded =
                    try {
                        bytes.copyOf(effectiveLength).decodeToString(throwOnInvalidSequence = true)
                    } catch (error: CharacterCodingException) {
                        throw IllegalArgumentException("Provider worker frame is not valid UTF-8", error)
                    }
                ProviderWorkerProtocol.decode(encoded)
            }
        } finally {
            receiving.set(false)
        }
    }

    override suspend fun close() {
        if (!closed.compareAndSet(false, true)) return
        synchronized(sendLock) {
            input.close()
            output.close()
        }
    }

    private companion object {
        const val NEW_LINE = 0x0A
        const val CARRIAGE_RETURN = 0x0D
    }
}
