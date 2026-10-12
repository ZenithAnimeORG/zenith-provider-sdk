package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.CatalogMediaDetails
import com.pilldev.zenith.provider.CatalogMediaItem
import com.pilldev.zenith.provider.CatalogProvider
import com.pilldev.zenith.provider.CatalogSearchResult
import com.pilldev.zenith.provider.CatalogSection
import com.pilldev.zenith.provider.ZenithProvider
import com.pilldev.zenith.provider.ZenithProviderFactory
import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.MediaId
import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderId
import com.pilldev.zenith.provider.model.ProviderResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProviderWorkerDispatcherTest {
    @Test
    fun initializesDispatchesOnlyDeclaredCapabilitiesAndDisposesProvider() = runTest {
        val manifest = PluginManifest(
            id = ProviderId("worker-test"),
            name = "Worker test",
            version = "0.1.0",
            capabilities = setOf(ProviderCapability.CATALOG),
        )
        var disposed = false
        val provider = TestCatalogProvider(manifest) { disposed = true }
        val context = TestContext()
        val dispatcher = ProviderWorkerDispatcher(
            factory = object : ZenithProviderFactory {
                override fun create(manifest: PluginManifest): ZenithProvider = provider
            },
            context = context,
        )

        val readyFrame = dispatcher.handle(request("initialize", ProviderWorkerMessage.Initialize(manifest)))
        val ready = assertIs<ProviderWorkerMessage.Ready>(readyFrame.message)
        assertEquals("initialize", readyFrame.inReplyTo)
        assertEquals(manifest, ready.manifest)

        val searchFrame = dispatcher.handle(
            request(
                "search",
                ProviderWorkerMessage.Invoke(ProviderWorkerOperation.CatalogSearch("Example", 1, 20)),
            ),
        )
        val searchResult = assertIs<ProviderWorkerMessage.Complete>(searchFrame.message).result
        val searchValue = assertIs<ProviderWorkerResult.Success>(searchResult).value
        val results = assertIs<ProviderWorkerValue.CatalogSearch>(searchValue).result
        assertEquals("Example", results.items.single().title)

        val deniedFrame = dispatcher.handle(
            request(
                "poster",
                ProviderWorkerMessage.Invoke(
                    ProviderWorkerOperation.Poster(MediaRef(MediaId("media-1"))),
                ),
            ),
        )
        val deniedResult = assertIs<ProviderWorkerMessage.Complete>(deniedFrame.message).result
        assertEquals(
            ProviderWorkerFailureCode.INVALID_REQUEST,
            assertIs<ProviderWorkerResult.Failed>(deniedResult).code,
        )

        val disposedFrame = dispatcher.handle(request("dispose", ProviderWorkerMessage.Dispose))
        assertIs<ProviderWorkerMessage.Disposed>(disposedFrame.message)
        assertEquals(true, disposed)
        context.httpClient.close()
    }

    @Test
    fun failedInitializationDisposesPartiallyCreatedProvider() = runTest {
        val manifest = PluginManifest(id = ProviderId("broken"), name = "Broken", version = "0.1.0")
        var disposed = false
        val provider = object : ZenithProvider {
            override val manifest: PluginManifest = manifest

            override suspend fun initialize(context: ProviderContext) {
                error("synthetic initialization failure")
            }

            override suspend fun dispose() {
                disposed = true
            }
        }
        val context = TestContext()
        val dispatcher = ProviderWorkerDispatcher(
            factory = object : ZenithProviderFactory {
                override fun create(manifest: PluginManifest): ZenithProvider = provider
            },
            context = context,
        )

        val response = dispatcher.handle(request("initialize", ProviderWorkerMessage.Initialize(manifest)))

        assertEquals(ProviderWorkerFailureCode.PROVIDER_FAILURE, assertIs<ProviderWorkerMessage.Failure>(response.message).code)
        assertEquals(true, disposed)
        context.httpClient.close()
    }

    private fun request(
        id: String,
        message: ProviderWorkerMessage,
    ) = ProviderWorkerFrame(
        protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
        requestId = id,
        message = message,
    )

    private class TestContext : ProviderContext {
        override val httpClient = HttpClient(MockEngine { respond("") })
        private val values = mutableMapOf<String, String>()

        override fun getSetting(key: String): String? = values[key]

        override fun setSetting(key: String, value: String?) {
            if (value == null) values.remove(key) else values[key] = value
        }
    }

    private class TestCatalogProvider(
        override val manifest: PluginManifest,
        private val onDispose: () -> Unit,
    ) : CatalogProvider {
        override suspend fun search(query: String, page: Int, pageSize: Int): ProviderResult<CatalogSearchResult> =
            ProviderResult.Success(
                CatalogSearchResult(
                    items = listOf(
                        CatalogMediaItem(MediaRef(MediaId("media-1")), query),
                    ),
                ),
            )

        override suspend fun getDetails(mediaRef: MediaRef): ProviderResult<CatalogMediaDetails> =
            ProviderResult.Failure("Not needed by this test")

        override suspend fun getHomeSections(): ProviderResult<List<CatalogSection>> =
            ProviderResult.Success(emptyList())

        override suspend fun dispose() = onDispose()
    }
}
