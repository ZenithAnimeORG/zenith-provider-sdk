package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderId
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.SettingItemSpec
import com.pilldev.zenith.provider.model.SettingType
import com.pilldev.zenith.provider.model.SettingsSchema
import com.pilldev.zenith.provider.model.localizedDescription
import com.pilldev.zenith.provider.model.localizedName
import com.pilldev.zenith.provider.model.toSettingsSchema
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProviderSdkTest {
    @Test
    fun testProviderIdEquality() {
        val id1 = ProviderId("kodik")
        val id2 = ProviderId("kodik")
        assertEquals(id1, id2)
        assertEquals("kodik", id1.value)
    }

    @Test
    fun testProviderResultSuccess() {
        val result = ProviderResult.Success("test_data")
        assertEquals("test_data", result.data)
        assertEquals("test_data", result.getOrNull())
        val mapped = result.map { it.length }
        assertTrue(mapped is ProviderResult.Success)
        assertEquals(9, mapped.data)
    }

    @Test
    fun testProviderResultFailure() {
        val result: ProviderResult<String> = ProviderResult.Failure("network_error")
        assertTrue(result.getOrNull() == null)
        assertEquals("fallback", result.getOrDefault("fallback"))
    }

    @Test
    fun streamExtractorFailuresDoNotPreventLaterMatches() = kotlinx.coroutines.test.runTest {
        val matchingExtractor = object : StreamExtractor {
            override val name: String = "matching"

            override suspend fun canExtract(url: String): ProviderResult<Boolean> =
                ProviderResult.Success(true)

            override suspend fun extract(
                url: String,
                referer: String?,
                context: com.pilldev.zenith.provider.context.ProviderContext?,
            ): ProviderResult<List<com.pilldev.zenith.provider.model.ProviderMediaStream>> =
                ProviderResult.Success(listOf(com.pilldev.zenith.provider.model.ProviderMediaStream(url)))
        }
        val failingExtractor = object : StreamExtractor {
            override val name: String = "failing"

            override suspend fun canExtract(url: String): ProviderResult<Boolean> =
                ProviderResult.Failure("Worker unavailable")

            override suspend fun extract(
                url: String,
                referer: String?,
                context: com.pilldev.zenith.provider.context.ProviderContext?,
            ): ProviderResult<List<com.pilldev.zenith.provider.model.ProviderMediaStream>> =
                ProviderResult.Failure("Not expected to be called")
        }
        val registry = DefaultStreamExtractorRegistry(listOf(failingExtractor, matchingExtractor))

        val result = registry.extract("https://example.com/video")

        assertTrue(result is ProviderResult.Success)
        assertEquals("https://example.com/video", result.data.single().url)
    }

    @Test
    fun testPluginManifestSerialization() {
        val manifest = PluginManifest(
            id = ProviderId("sample"),
            name = "Sample Provider",
            version = "1.0.0",
            capabilities = setOf(ProviderCapability.MEDIA_SOURCE, ProviderCapability.CATALOG),
            hosts = listOf("example.com", "*.example.com"),
        )
        val json = Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(PluginManifest.serializer(), manifest)
        val decoded = json.decodeFromString(PluginManifest.serializer(), encoded)
        assertEquals(manifest.id, decoded.id)
        assertEquals(manifest.name, decoded.name)
        assertTrue(decoded.capabilities.contains(ProviderCapability.CATALOG))
        assertEquals(2, decoded.hosts.size)
    }

    @Test
    fun testSettingsSchemaPolymorphism() {
        val schema = SettingsSchema(
            version = 1,
            items = listOf(
                SettingItemSpec.Text(key = "token", title = "API Token", isSecret = true),
                SettingItemSpec.Toggle(key = "enable_cache", title = "Enable Cache", defaultValue = true),
                SettingItemSpec.Action(key = "ping", title = "Check Connection", buttonLabel = "Test", actionId = "ping_action"),
            )
        )
        val json = Json { ignoreUnknownKeys = true }
        val encoded = json.encodeToString(SettingsSchema.serializer(), schema)
        val decoded = json.decodeFromString(SettingsSchema.serializer(), encoded)
        assertEquals(3, decoded.items.size)
        assertTrue(decoded.items[0] is SettingItemSpec.Text)
        assertTrue((decoded.items[0] as SettingItemSpec.Text).isSecret)
    }

    @Test
    fun testProviderTestResult() {
        val success = com.pilldev.zenith.provider.model.ProviderTestResult
            .Success("OK", 42L)
        assertEquals("OK", success.message)
        assertEquals(42L, success.latencyMs)

        val failure = com.pilldev.zenith.provider.model.ProviderTestResult
            .Failure("Timeout", "Details")
        assertEquals("Timeout", failure.error)
        assertEquals("Details", failure.message)
    }

    @Test
    fun testProviderSettingSpec() {
        val spec = com.pilldev.zenith.provider.model.ProviderSettingSpec(
            key = "mirror",
            label = "Зеркало",
            type = com.pilldev.zenith.provider.model.SettingType.SELECT,
            options = listOf("mirror1.com", "mirror2.com"),
        )
        assertEquals("mirror", spec.key)
        assertEquals(com.pilldev.zenith.provider.model.SettingType.SELECT, spec.type)
        assertEquals(2, spec.options.size)
    }

    @Test
    fun testManifestSettingsSchemaPreservesMirrorAndActionTypes() {
        val manifest = PluginManifest(
            id = ProviderId("sample"),
            name = "Sample Provider",
            version = "1.0.0",
            settingsSchema = listOf(
                com.pilldev.zenith.provider.model.ProviderSettingSpec(
                    key = "mirror",
                    label = "API Mirror",
                    type = SettingType.MIRROR_LIST,
                    defaultMirrors = listOf("https://api.example.com"),
                ),
                com.pilldev.zenith.provider.model.ProviderSettingSpec(
                    key = "ping",
                    label = "Test Connection",
                    type = SettingType.ACTION,
                    actionId = "ping",
                ),
            ),
            mirrors = listOf(
                com.pilldev.zenith.provider.model.ProviderMirrorSpec(
                    url = "https://api.example.com",
                    displayName = "Primary",
                ),
            ),
        )

        val settings = manifest.toSettingsSchema()

        assertEquals(2, settings.items.size)
        assertEquals("mirror", (settings.items[0] as SettingItemSpec.MirrorList).key)
        assertEquals("ping", (settings.items[1] as SettingItemSpec.Action).actionId)
    }

    @Test
    fun testAnimeTitleMatcherNormalizationAndSeasons() {
        val season = com.pilldev.zenith.provider.matcher.AnimeTitleMatcher
            .extractSeasonNumber("Клинок, рассекающий демонов 2 сезон")
        assertEquals(2, season)

        val romanSeason = com.pilldev.zenith.provider.matcher.AnimeTitleMatcher
            .extractSeasonNumber("Jujutsu Kaisen Season 2")
        assertEquals(2, romanSeason)

        val norm = com.pilldev.zenith.provider.matcher.AnimeTitleMatcher
            .normalize("   Sousou no Frieren! (2023)   ")
        assertEquals("sousou no frieren 2023", norm)

        val distance = com.pilldev.zenith.provider.matcher.AnimeTitleMatcher
            .levenshteinDistance("kitten", "sitting")
        assertEquals(3, distance)
    }

    @Test
    fun testTorrentTitleParser() {
        val parsed = com.pilldev.zenith.provider.matcher.TorrentTitleParser
            .parse("[SubsPlease] Bocchi the Rock! - 01 (1080p) [x265] [Dual Audio].mkv")
        assertEquals("Bocchi the Rock! - 01", parsed.cleanTitle)
        assertEquals("SubsPlease", parsed.releaseGroup)
        assertEquals(1, parsed.episode)
        assertEquals("1080P", parsed.resolution)
        assertTrue(parsed.tags.any { it.name.contains("Dual Audio") })
        assertTrue(parsed.tags.any { it.name.contains("x265") })
    }

    @Test
    fun testBrowserHeaders() {
        val headers = com.pilldev.zenith.provider.net.BrowserHeaders
            .standardHeaders(referer = "https://example.com")
        assertEquals("https://example.com", headers["Referer"])
        assertEquals(com.pilldev.zenith.provider.net.BrowserHeaders.CHROME_DESKTOP_UA, headers["User-Agent"])
        assertTrue(headers.containsKey("Accept"))
    }

    @Test
    fun testUrlNormalizer() {
        assertEquals(
            "https://cdn.example.com/poster.jpg",
            com.pilldev.zenith.provider.net.UrlNormalizer
                .resolve("//cdn.example.com/poster.jpg")
        )
        assertEquals(
            "https://example.com/media/poster.jpg",
            com.pilldev.zenith.provider.net.UrlNormalizer
                .resolve("/media/poster.jpg", "https://example.com/page")
        )
        assertEquals(
            "https://example.com/image.jpg",
            com.pilldev.zenith.provider.net.UrlNormalizer
                .resolve("image.jpg", "https://example.com/")
        )
        assertEquals(
            "https://direct.com/img.jpg",
            com.pilldev.zenith.provider.net.UrlNormalizer
                .resolve("https://direct.com/img.jpg")
        )
    }

    @Test
    fun testCleanForSearch() {
        val cleaned = com.pilldev.zenith.provider.matcher.AnimeTitleMatcher
            .cleanForSearch("Клинок, рассекающий демонов: 2 сезон (AniLibria)")
        assertEquals("Клинок, рассекающий демонов", cleaned)

        val cleaned2 = com.pilldev.zenith.provider.matcher.AnimeTitleMatcher
            .cleanForSearch("Spy x Family Part 2!")
        assertEquals("Spy x Family", cleaned2)
    }

    @Test
    fun testTorrentTitleParserByteSizeAndQuality() {
        assertEquals(
            1503238553L,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .parseByteSize("1.4 GB")
        )
        assertEquals(
            1610612736L,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .parseByteSize("1.5 GiB")
        )
        assertEquals(
            786432000L,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .parseByteSize("750 MB")
        )
        assertEquals(
            102400L,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .parseByteSize("100 KB")
        )
        assertEquals(
            0L,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .parseByteSize("invalid")
        )

        val parsed = com.pilldev.zenith.provider.matcher.TorrentTitleParser
            .parse("[SubsPlease] Bocchi the Rock! - 01 (1080p).mkv")
        assertEquals("1080p", parsed.normalizedQuality)

        val parsedNoRes = com.pilldev.zenith.provider.matcher.TorrentTitleParser
            .parse("[Group] Anime - 01.mkv")
        assertEquals("1080p", parsedNoRes.normalizedQuality)

        assertEquals(
            2160,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .qualityScore("2160p")
        )
        assertEquals(
            2160,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .qualityScore("4k")
        )
        assertEquals(
            1080,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .qualityScore("1080p")
        )
        assertEquals(
            720,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .qualityScore("720p")
        )
        assertEquals(
            480,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .qualityScore("480p")
        )
        assertEquals(
            0,
            com.pilldev.zenith.provider.matcher.TorrentTitleParser
                .qualityScore("unknown")
        )
    }

    @Test
    fun testMeasureTestSuccessAndFailure() =
        kotlinx.coroutines.test.runTest {
            val success = com.pilldev.zenith.provider.model.measureTest {
                "API Online"
            }
            assertTrue(success is com.pilldev.zenith.provider.model.ProviderTestResult.Success)
            assertEquals("API Online", success.message)

            val failure = com.pilldev.zenith.provider.model.measureTest {
                throw IllegalStateException("Connection failed")
            }
            assertTrue(failure is com.pilldev.zenith.provider.model.ProviderTestResult.Failure)
            assertEquals("Connection failed", failure.error)
        }

    @Test
    fun testProviderCapabilityDeserializationResilience() {
        val json = Json { ignoreUnknownKeys = true }

        val standard = json.decodeFromString<ProviderCapability>("\"TORRENT_SOURCE\"")
        assertEquals(ProviderCapability.TORRENT_SOURCE, standard)

        val alias = json.decodeFromString<ProviderCapability>("\"TORRENT\"")
        assertEquals(ProviderCapability.TORRENT_SOURCE, alias)

        val lowercaseAlias = json.decodeFromString<ProviderCapability>("\"torrent\"")
        assertEquals(ProviderCapability.TORRENT_SOURCE, lowercaseAlias)

        val unknown = json.decodeFromString<ProviderCapability>("\"FUTURE_UNSUPPORTED\"")
        assertEquals(ProviderCapability.UNKNOWN, unknown)

        val manifestJson =
            """
            {
                "id": "nyaa",
                "name": "Nyaa",
                "version": "1.0.0",
                "capabilities": ["TORRENT", "FUTURE_FEATURE", "MEDIA_SOURCE"]
            }
            """.trimIndent()
        val manifest = json.decodeFromString<PluginManifest>(manifestJson)
        assertTrue(manifest.capabilities.contains(ProviderCapability.TORRENT_SOURCE))
        assertTrue(manifest.capabilities.contains(ProviderCapability.MEDIA_SOURCE))
        assertTrue(manifest.capabilities.contains(ProviderCapability.UNKNOWN))
    }

    @Test
    fun testLocalizedManifestFields() {
        val manifest = PluginManifest(
            id = ProviderId("test-id"),
            name = "Test Provider",
            version = "1.0.0",
            description = "Default description",
            localizedNames = mapOf(
                "ru" to "Тестовый провайдер",
                "en-US" to "Test Provider US",
            ),
            localizedDescriptions = mapOf(
                "ru" to "Описание на русском",
                "en" to "English description",
            ),
        )

        assertEquals("Тестовый провайдер", manifest.localizedName("ru"))
        assertEquals("Тестовый провайдер", manifest.localizedName("ru_RU"))
        assertEquals("Test Provider US", manifest.localizedName("en-US"))
        assertEquals("Test Provider", manifest.localizedName("fr"))
        assertEquals("Test Provider", manifest.localizedName(null))

        assertEquals("Описание на русском", manifest.localizedDescription("ru"))
        assertEquals("English description", manifest.localizedDescription("en-GB"))
        assertEquals("Default description", manifest.localizedDescription("es"))
    }
}
