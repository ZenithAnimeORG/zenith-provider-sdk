package com.pilldev.zenith.provider.testkit

import com.pilldev.zenith.provider.CatalogMediaDetails
import com.pilldev.zenith.provider.CatalogSearchResult
import com.pilldev.zenith.provider.ImportBatch
import com.pilldev.zenith.provider.ipc.ProviderWorkerFailureCode
import com.pilldev.zenith.provider.ipc.ProviderWorkerResult
import com.pilldev.zenith.provider.ipc.ProviderWorkerValue
import com.pilldev.zenith.provider.model.PACKAGE_FORMAT_V2
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderId
import com.pilldev.zenith.provider.model.ProviderTestResult
import com.pilldev.zenith.provider.model.ProviderTranslationType
import com.pilldev.zenith.provider.model.ProviderVideoSource
import com.pilldev.zenith.provider.model.UserMediaStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrossLanguageGoldenTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val manifestGoldenJson = """{
  "id": "shikimori",
  "name": "Shikimori",
  "version": "1.0.0",
  "sdkVersion": "0.1.0",
  "packageFormat": "zpk/v2",
  "moduleApiVersion": "1.0.0",
  "entrypoint": "index.js",
  "description": "Comprehensive anime catalog and tracker provider",
  "author": "Zenith Community",
  "homepage": "https://shikimori.one",
  "icon": "icon.png",
  "capabilities": [
    "CATALOG",
    "TRACKER"
  ],
  "hosts": [
    "shikimori.one",
    "*.shikimori.one"
  ],
  "entry": {
    "js": "index.js"
  },
  "settings": "settings.json",
  "localizedNames": {
    "ru": "Шикимори",
    "en": "Shikimori"
  },
  "localizedDescriptions": {
    "ru": "Каталог аниме и трекер прогресса",
    "en": "Anime catalog and progress tracker"
  }
}"""

    private val searchResultGoldenJson = """{
  "items": [
    {
      "mediaRef": {
        "id": "1",
        "externalIds": {
          "shikimori": "1"
        },
        "title": "Cowboy Bebop",
        "alternateTitles": [
          "Кавбой Бибоп"
        ]
      },
      "title": "Cowboy Bebop",
      "originalTitle": "Cowboy Bebop",
      "posterUrl": "https://shikimori.one/system/animes/original/1.jpg",
      "score": 8.75,
      "year": 1998,
      "totalEpisodes": 26
    }
  ],
  "hasNextPage": true
}"""

    private val detailsGoldenJson = """{
  "mediaRef": {
    "id": "1",
    "externalIds": {
      "shikimori": "1"
    },
    "title": "Cowboy Bebop",
    "alternateTitles": []
  },
  "title": "Cowboy Bebop",
  "originalTitle": "カウボーイビバップ",
  "description": "In 2071, roughly fifty years after an accident with a hyperspace gateway made the Earth almost uninhabitable...",
  "posterUrl": "https://shikimori.one/system/animes/original/1.jpg",
  "bannerUrl": "https://shikimori.one/system/animes/banner/1.jpg",
  "score": 8.75,
  "year": 1998,
  "episodesCount": 26,
  "genres": [
    "Action",
    "Sci-Fi",
    "Space"
  ],
  "studios": [
    "Sunrise"
  ],
  "status": "released"
}"""

    private val sourcesGoldenJson = """[
  {
    "name": "Kodik",
    "translationName": "AniLibria",
    "translationType": "DUB",
    "episodes": [
      {
        "number": 1,
        "url": "https://kodik.info/seria/123/ep1",
        "quality": {
          "720p": "https://stream.example.com/720.m3u8",
          "1080p": "https://stream.example.com/1080.m3u8"
        },
        "providerId": "kodik"
      }
    ],
    "qualities": {
      "1080p": "1080p Full HD",
      "720p": "720p HD"
    }
  }
]"""

    private val importBatchGoldenJson = """{
  "sourceName": "anixart",
  "entries": [
    {
      "title": "Steins;Gate",
      "originalTitle": "Steins;Gate",
      "releaseYear": 2011,
      "watchedEpisodes": 24,
      "totalEpisodes": 24,
      "targetStatus": "COMPLETED",
      "rating": 10
    }
  ]
}"""

    @Test
    fun testManifestGoldenDeserializationAndRoundtrip() {
        val manifest = json.decodeFromString(PluginManifest.serializer(), manifestGoldenJson)

        assertEquals(ProviderId("shikimori"), manifest.id)
        assertEquals("Shikimori", manifest.name)
        assertEquals("1.0.0", manifest.version)
        assertEquals(PACKAGE_FORMAT_V2, manifest.packageFormat)
        assertEquals("1.0.0", manifest.moduleApiVersion)
        assertEquals("index.js", manifest.entrypoint)
        assertTrue(manifest.capabilities.contains(ProviderCapability.CATALOG))
        assertTrue(manifest.capabilities.contains(ProviderCapability.TRACKER))
        assertEquals("Шикимори", manifest.localizedNames["ru"])

        // Roundtrip re-serialization
        val reserialized = json.encodeToString(PluginManifest.serializer(), manifest)
        val decoded = json.decodeFromString(PluginManifest.serializer(), reserialized)
        assertEquals(manifest, decoded)
    }

    @Test
    fun testCatalogSearchGoldenDeserialization() {
        val search = json.decodeFromString(CatalogSearchResult.serializer(), searchResultGoldenJson)

        assertTrue(search.hasNextPage)
        assertEquals(1, search.items.size)
        val item = search.items[0]
        assertEquals("1", item.mediaRef.id.value)
        assertEquals("Cowboy Bebop", item.title)
        assertEquals(8.75, item.score)
        assertEquals(1998, item.year)
        assertEquals(26, item.totalEpisodes)
    }

    @Test
    fun testCatalogDetailsGoldenDeserialization() {
        val details = json.decodeFromString(CatalogMediaDetails.serializer(), detailsGoldenJson)

        assertEquals("Cowboy Bebop", details.title)
        assertEquals("カウボーイビバップ", details.originalTitle)
        assertEquals(listOf("Action", "Sci-Fi", "Space"), details.genres)
        assertEquals(listOf("Sunrise"), details.studios)
        assertEquals(26, details.episodesCount)
        assertEquals("released", details.status)
    }

    @Test
    fun testVideoSourcesGoldenDeserialization() {
        val sources = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(ProviderVideoSource.serializer()),
            sourcesGoldenJson,
        )

        assertEquals(1, sources.size)
        val source = sources[0]
        assertEquals("Kodik", source.name)
        assertEquals("AniLibria", source.translationName)
        assertEquals(ProviderTranslationType.DUB, source.translationType)
        assertEquals(1, source.episodes.size)
        assertEquals(1, source.episodes[0].number)
        assertEquals("https://stream.example.com/1080.m3u8", source.episodes[0].quality?.get("1080p"))
    }

    @Test
    fun testImportBatchGoldenDeserialization() {
        val batch = json.decodeFromString(ImportBatch.serializer(), importBatchGoldenJson)

        assertEquals("anixart", batch.sourceName)
        assertEquals(1, batch.entries.size)
        val entry = batch.entries[0]
        assertEquals("Steins;Gate", entry.title)
        assertEquals(UserMediaStatus.COMPLETED, entry.targetStatus)
        assertEquals(10, entry.rating)
        assertEquals(24, entry.watchedEpisodes)
        assertEquals(24, entry.totalEpisodes)
    }

    @Test
    fun testProviderWorkerResultSuccessGoldenDeserialization() {
        val successJson = """{
            "type": "Success",
            "value": {
                "type": "CatalogSearch",
                "result": {
                    "items": [
                        {
                            "mediaRef": {
                                "id": "1",
                                "title": "Cowboy Bebop"
                            },
                            "title": "Cowboy Bebop"
                        }
                    ],
                    "hasNextPage": false
                }
            }
        }"""
        val result = json.decodeFromString(ProviderWorkerResult.serializer(), successJson)
        assertTrue(result is ProviderWorkerResult.Success)
        val value = result.value
        assertTrue(value is ProviderWorkerValue.CatalogSearch)
        assertEquals("Cowboy Bebop", value.result.items[0].title)
    }

    @Test
    fun testProviderWorkerResultFailedGoldenDeserialization() {
        val failedJson = """{
            "type": "Failed",
            "code": "PROVIDER_FAILURE",
            "message": "Upstream timeout"
        }"""
        val result = json.decodeFromString(ProviderWorkerResult.serializer(), failedJson)
        assertTrue(result is ProviderWorkerResult.Failed)
        assertEquals(ProviderWorkerFailureCode.PROVIDER_FAILURE, result.code)
        assertEquals("Upstream timeout", result.message)
    }

    @Test
    fun testProviderTestResultSuccessGoldenDeserialization() {
        val testJson = """{
            "type": "Success",
            "message": "All systems operational",
            "latencyMs": 42
        }"""
        val result = json.decodeFromString(ProviderTestResult.serializer(), testJson)
        assertTrue(result is ProviderTestResult.Success)
        assertEquals("All systems operational", result.message)
        assertEquals(42L, result.latencyMs)
    }
}
