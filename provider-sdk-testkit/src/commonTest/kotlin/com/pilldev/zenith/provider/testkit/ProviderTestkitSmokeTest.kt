package com.pilldev.zenith.provider.testkit

import com.pilldev.zenith.provider.BaseZenithProvider
import com.pilldev.zenith.provider.CatalogMediaDetails
import com.pilldev.zenith.provider.CatalogProvider
import com.pilldev.zenith.provider.CatalogSearchResult
import com.pilldev.zenith.provider.CatalogSection
import com.pilldev.zenith.provider.MediaSourceProvider
import com.pilldev.zenith.provider.PosterSourceProvider
import com.pilldev.zenith.provider.SkipTimingsProvider
import com.pilldev.zenith.provider.SubtitleSourceProvider
import com.pilldev.zenith.provider.TorrentSourceProvider
import com.pilldev.zenith.provider.TrackerProvider
import com.pilldev.zenith.provider.ZenithProvider
import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderId
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.ProviderSubtitle
import com.pilldev.zenith.provider.model.ProviderTorrentSource
import com.pilldev.zenith.provider.model.ProviderVideoSource
import com.pilldev.zenith.provider.model.SkipInterval
import com.pilldev.zenith.provider.model.UserMediaStatus
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProviderTestkitSmokeTest : ProviderContractTestBase() {
    private class SamplePlugin(
        override val manifest: PluginManifest
    ) : BaseZenithProvider()

    private class LifecyclePlugin : BaseZenithProvider() {
        override val manifest = PluginManifest(
            id = ProviderId("lifecycle"),
            name = "Lifecycle",
            version = "1.0.0",
        )
        var disposalCount = 0

        override suspend fun onDispose() {
            disposalCount += 1
        }

        fun readSetting(key: String): String? = getSetting(key)
    }

    private class FullCapabilityProvider :
        BaseZenithProvider(),
        CatalogProvider,
        MediaSourceProvider,
        PosterSourceProvider,
        TorrentSourceProvider,
        SkipTimingsProvider,
        SubtitleSourceProvider,
        TrackerProvider {
        override val manifest =
            PluginManifest(
                id = ProviderId("contract-provider"),
                name = "Contract Provider",
                version = "1.0.0",
                capabilities = setOf(
                    ProviderCapability.CATALOG,
                    ProviderCapability.MEDIA_SOURCE,
                    ProviderCapability.POSTER_SOURCE,
                    ProviderCapability.TORRENT_SOURCE,
                    ProviderCapability.SKIP_TIMINGS,
                    ProviderCapability.SUBTITLES,
                    ProviderCapability.TRACKER,
                ),
            )

        override suspend fun search(
            query: String,
            page: Int,
            pageSize: Int
        ): ProviderResult<CatalogSearchResult> = ProviderResult.Success(CatalogSearchResult())

        override suspend fun getDetails(mediaRef: MediaRef): ProviderResult<CatalogMediaDetails> = ProviderResult.Success(CatalogMediaDetails(mediaRef = mediaRef, title = mediaRef.title))

        override suspend fun getHomeSections(): ProviderResult<List<CatalogSection>> = ProviderResult.Success(emptyList())

        override suspend fun getSources(mediaRef: MediaRef): ProviderResult<List<ProviderVideoSource>> = ProviderResult.Success(emptyList())

        override suspend fun getPoster(mediaRef: MediaRef): ProviderResult<String?> = ProviderResult.Success(null)

        override suspend fun search(mediaRef: MediaRef): ProviderResult<List<ProviderTorrentSource>> = ProviderResult.Success(emptyList())

        override suspend fun getSkipTimes(
            mediaRef: MediaRef,
            episodeNumber: Int,
            episodeLength: Double?,
            translationName: String?,
        ): ProviderResult<List<SkipInterval>> = ProviderResult.Success(emptyList())

        override suspend fun searchSubtitles(
            query: String,
            languages: String,
            episodeNumber: Int?,
            seasonNumber: Int?,
        ): ProviderResult<List<ProviderSubtitle>> = ProviderResult.Success(emptyList())

        override suspend fun getDownloadLink(fileId: Long): ProviderResult<String?> = ProviderResult.Success(null)

        override suspend fun onEpisodeWatched(
            mediaRef: MediaRef,
            episode: Int
        ): ProviderResult<Unit> = ProviderResult.Success(Unit)

        override suspend fun syncStatus(
            mediaRef: MediaRef,
            status: UserMediaStatus
        ): ProviderResult<Unit> = ProviderResult.Success(Unit)
    }

    @Test
    fun testContractVerification() =
        runTest {
            val manifest = PluginManifest(
                id = ProviderId("sample"),
                name = "Sample",
                version = "1.0.0",
                capabilities = setOf(ProviderCapability.MEDIA_SOURCE),
                hosts = listOf("sample.org"),
            )
            verifyManifestCompliance(manifest)

            val plugin = SamplePlugin(manifest)
            verifyRequiredCapabilities(plugin, setOf(ProviderCapability.MEDIA_SOURCE))
            verifyBasicLifecycle(plugin)
            assertEquals("sample", plugin.manifest.id.value)
            assertTrue(plugin.manifest.capabilities.contains(ProviderCapability.MEDIA_SOURCE))
        }

    @Test
    fun testManifestIsRequiredForDirectImplementations() {
        val manifest = PluginManifest(
            id = ProviderId("direct"),
            name = "Direct",
            version = "1.0.0",
        )
        val provider = object : ZenithProvider {
            override val manifest: PluginManifest = manifest

            override suspend fun dispose() {}
        }
        assertEquals(manifest, provider.manifest)
    }

    @Test
    fun baseProviderDisposalReleasesContextAndRunsOnce() =
        runTest {
            val provider = LifecyclePlugin()
            val context = FakeProviderContext(initialSettings = mapOf("key" to "value"))
            try {
                provider.initialize(context)
                assertEquals("value", provider.readSetting("key"))

                provider.dispose()
                provider.dispose()

                assertEquals(1, provider.disposalCount)
                assertFailsWith<IllegalStateException> { provider.readSetting("key") }
            } finally {
                context.httpClient.close()
            }
        }

    @Test
    fun testProviderCapabilityContracts() =
        runTest {
            val provider = FullCapabilityProvider()
            val context = FakeProviderContext()
            try {
                assertEquals(ProviderId("contract-provider"), provider.manifest.id)
                verifyCatalogProvider(provider, context)
                verifyMediaProvider(provider, context)
                verifyPosterProvider(provider, context)
                verifyTorrentProvider(provider, context)
                verifySubtitleProvider(provider, context)
                verifySkipProvider(provider, context)
                verifyTrackerProvider(provider, context)
            } finally {
                context.httpClient.close()
            }
        }

    @Test
    fun testFakeProviderContextAndMockClient() =
        runTest {
            val client = MockProviderHttpClient.withStaticResponses(
                mapOf("sample.org/data" to "{\"status\":\"ok\"}"),
            )
            val context = FakeProviderContext(httpClient = client)
            try {
                context.setSetting("key1", "val1")
                assertEquals("val1", context.getSetting("key1"))

                val response = context.httpClient.get("https://sample.org/data").bodyAsText()
                assertTrue(response.contains("\"status\":\"ok\""))
            } finally {
                client.close()
            }
        }
}
