package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.ZenithProviderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.util.ServiceLoader
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

public object JvmProviderWorkerMain {
    @JvmStatic
    public fun main(args: Array<String>) {
        require(args.isEmpty()) { "Provider worker does not accept command-line arguments" }
        val protocolOutput = System.out
        System.setOut(System.err)
        val ioDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        val workerDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        val workerScope = CoroutineScope(SupervisorJob() + workerDispatcher)
        val transport = JvmProviderWorkerFrameTransport(System.`in`, protocolOutput, ioDispatcher)
        val brokerContext = AtomicReference<ProviderWorkerBrokerContext?>()
        try {
            runBlocking {
                val runtime =
                    ProviderWorkerRuntime(
                        transport = transport,
                        digest = JvmWorkerDigest,
                        scope = workerScope,
                        createHandler = { context ->
                            val factory = ServiceLoader.load(ZenithProviderFactory::class.java).singleOrNull()
                                ?: throw IllegalStateException("Provider package must declare exactly one worker factory")
                            val providerContext = ProviderWorkerBrokerContext(context.broker, context.settings)
                            brokerContext.set(providerContext)
                            val dispatcher = ProviderWorkerDispatcher(factory, providerContext)
                            ProviderWorkerFrameHandler { frame -> dispatcher.handle(frame) }
                        },
                    )
                runtime.run()
            }
        } finally {
            brokerContext.getAndSet(null)?.close()
            workerScope.cancel()
            workerDispatcher.close()
            ioDispatcher.close()
        }
    }

    private object JvmWorkerDigest : ProviderWorkerDigest {
        override fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { byte -> "%02x".format(byte) }
    }
}
