package com.pilldev.zenith.provider.testkit

import com.pilldev.zenith.provider.CatalogProvider
import com.pilldev.zenith.provider.MediaSourceProvider
import com.pilldev.zenith.provider.PosterSourceProvider
import com.pilldev.zenith.provider.SkipTimingsProvider
import com.pilldev.zenith.provider.SubtitleSourceProvider
import com.pilldev.zenith.provider.TorrentSourceProvider
import com.pilldev.zenith.provider.TrackerProvider
import com.pilldev.zenith.provider.ZenithProvider
import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.MediaId
import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderEpisode
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.UserMediaStatus
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

public abstract class ProviderContractTestBase {
    protected fun verifyManifestCompliance(manifest: PluginManifest) {
        assertFalse(manifest.id.value.isBlank(), "Provider ID must not be blank")
        assertFalse(manifest.name.isBlank(), "Provider name must not be blank")
        assertFalse(manifest.version.isBlank(), "Provider version must not be blank")
        assertFalse(manifest.sdkVersion.isBlank(), "Provider sdkVersion must not be blank")
        assertFalse(manifest.capabilities.isEmpty(), "Provider must declare at least one capability")
        assertFalse(manifest.hosts.isEmpty(), "Provider must declare at least one allowed host in 'hosts'")

        for (host in manifest.hosts) {
            assertFalse(host.isBlank(), "Declared host cannot be blank")
            assertFalse(host.contains("://"), "Host pattern should not include protocol (e.g. use 'example.com' not 'https://example.com')")
        }
    }

    protected fun verifyRequiredCapabilities(
        provider: ZenithProvider,
        expected: Set<ProviderCapability>,
    ) {
        val manifestCaps = provider.manifest.capabilities
        for (cap in expected) {
            assertTrue(
                manifestCaps.contains(cap),
                "Provider ${provider.manifest.id} is missing expected capability $cap",
            )
        }
    }

    protected suspend fun verifyBasicLifecycle(
        provider: ZenithProvider,
        context: ProviderContext = FakeProviderContext(),
    ) {
        try {
            provider.initialize(context)
            assertNotNull(provider.manifest)
        } finally {
            provider.dispose()
            context.httpClient.close()
        }
    }

    protected suspend fun verifyCatalogProvider(
        provider: CatalogProvider,
        context: ProviderContext,
    ) {
        verifyRequiredCapabilities(provider, setOf(ProviderCapability.CATALOG))
        provider.initialize(context)
        val search = provider.search("Zenith provider contract fixture", page = 1, pageSize = 5)
        assertTrue(search is ProviderResult.Success, "Catalog search must return Success for the contract fixture")
        val mediaRef = contractMediaRef()
        assertTypedResult(provider.getDetails(mediaRef), "Catalog details")
        assertTypedResult(provider.getHomeSections(), "Catalog home sections")
    }

    protected suspend fun verifyMediaProvider(
        provider: MediaSourceProvider,
        context: ProviderContext,
    ) {
        verifyRequiredCapabilities(provider, setOf(ProviderCapability.MEDIA_SOURCE))
        provider.initialize(context)
        assertTypedResult(provider.getSources(contractMediaRef()), "Media sources")
        val directStream = provider.resolveStream(ProviderEpisode(url = "https://example.com/contract-fixture.m3u8"))
        assertTrue(directStream is ProviderResult.Success, "Direct HLS URLs must resolve without an extractor")
        assertTrue(directStream.data.any { it.url == "https://example.com/contract-fixture.m3u8" }, "Resolved stream must preserve the direct URL")
    }

    protected suspend fun verifyPosterProvider(
        provider: PosterSourceProvider,
        context: ProviderContext,
    ) {
        verifyRequiredCapabilities(provider, setOf(ProviderCapability.POSTER_SOURCE))
        provider.initialize(context)
        assertTypedResult(provider.getPoster(contractMediaRef()), "Poster lookup")
    }

    protected suspend fun verifyTorrentProvider(
        provider: TorrentSourceProvider,
        context: ProviderContext,
    ) {
        verifyRequiredCapabilities(provider, setOf(ProviderCapability.TORRENT_SOURCE))
        provider.initialize(context)
        assertTypedResult(provider.search(contractMediaRef()), "Torrent search")
    }

    protected suspend fun verifySubtitleProvider(
        provider: SubtitleSourceProvider,
        context: ProviderContext,
    ) {
        verifyRequiredCapabilities(provider, setOf(ProviderCapability.SUBTITLES))
        provider.initialize(context)
        assertTypedResult(provider.searchSubtitles("Zenith provider contract fixture"), "Subtitle search")
        assertTypedResult(provider.getDownloadLink(1L), "Subtitle download link")
    }

    protected suspend fun verifySkipProvider(
        provider: SkipTimingsProvider,
        context: ProviderContext,
    ) {
        verifyRequiredCapabilities(provider, setOf(ProviderCapability.SKIP_TIMINGS))
        provider.initialize(context)
        assertTypedResult(provider.getSkipTimes(contractMediaRef(), episodeNumber = 1), "Skip timings")
    }

    protected suspend fun verifyTrackerProvider(
        provider: TrackerProvider,
        context: ProviderContext,
    ) {
        verifyRequiredCapabilities(provider, setOf(ProviderCapability.TRACKER))
        provider.initialize(context)
        val mediaRef = contractMediaRef()
        assertTypedResult(provider.onEpisodeWatched(mediaRef, episode = 1), "Tracker episode update")
        assertTypedResult(provider.syncStatus(mediaRef, UserMediaStatus.WATCHING), "Tracker status update")
    }

    private fun contractMediaRef(): MediaRef =
        MediaRef(
            id = MediaId("zenith-provider-contract"),
            title = "Zenith provider contract fixture",
            alternateTitles = listOf("Фикстура контракта провайдера"),
        )

    private fun assertTypedResult(
        result: ProviderResult<*>,
        operation: String,
    ) {
        assertTrue(
            result is ProviderResult.Success || result is ProviderResult.Failure,
            "$operation must return a typed ProviderResult",
        )
    }
}
