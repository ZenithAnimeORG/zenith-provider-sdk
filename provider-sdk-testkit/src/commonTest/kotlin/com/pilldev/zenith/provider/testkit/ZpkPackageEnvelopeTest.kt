package com.pilldev.zenith.provider.testkit

import com.pilldev.zenith.provider.model.PACKAGE_FORMAT_V2
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderId
import com.pilldev.zenith.provider.model.ZpkPackageEnvelope
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZpkPackageEnvelopeTest {
    private val baseManifest = PluginManifest(
        id = ProviderId("portable_provider"),
        name = "Portable Provider",
        version = "1.0.0",
        packageFormat = PACKAGE_FORMAT_V2,
        moduleApiVersion = "1.0.0",
        entrypoint = "index.js",
        capabilities = setOf(ProviderCapability.CATALOG, ProviderCapability.MEDIA_SOURCE),
    )

    @Test
    fun testValidEnvelopePassesValidation() {
        val entries = mapOf(
            "manifest.json" to 512L,
            "index.js" to 150_000L,
            "icon.png" to 16_000L,
            "settings.json" to 1024L,
        )

        val result = ZpkPackageEnvelope.validateEnvelope(baseManifest, entries)
        assertTrue(result.isValid)
    }

    @Test
    fun testRejectsForbiddenBinariesInPortableV2Package() {
        val jarEntries = mapOf(
            "manifest.json" to 512L,
            "index.js" to 50_000L,
            "provider.jar" to 200_000L,
        )
        val dexEntries = mapOf(
            "manifest.json" to 512L,
            "index.js" to 50_000L,
            "provider.dex" to 200_000L,
        )
        val soEntries = mapOf(
            "manifest.json" to 512L,
            "index.js" to 50_000L,
            "native.so" to 200_000L,
        )

        assertFalse(ZpkPackageEnvelope.validateEnvelope(baseManifest, jarEntries).isValid)
        assertFalse(ZpkPackageEnvelope.validateEnvelope(baseManifest, dexEntries).isValid)
        assertFalse(ZpkPackageEnvelope.validateEnvelope(baseManifest, soEntries).isValid)
    }

    @Test
    fun testRejectsEnvelopeMissingManifestOrEntrypoint() {
        val missingManifest = mapOf(
            "index.js" to 50_000L,
        )
        val missingEntrypoint = mapOf(
            "manifest.json" to 512L,
        )

        assertFalse(ZpkPackageEnvelope.validateEnvelope(baseManifest, missingManifest).isValid)
        assertFalse(ZpkPackageEnvelope.validateEnvelope(baseManifest, missingEntrypoint).isValid)
    }

    @Test
    fun testRejectsExceedingEntrypointSizeLimit() {
        val oversized = mapOf(
            "manifest.json" to 512L,
            "index.js" to ZpkPackageEnvelope.MAX_ENTRYPOINT_BYTES + 1L,
        )

        assertFalse(ZpkPackageEnvelope.validateEnvelope(baseManifest, oversized).isValid)
    }

    @Test
    fun testSafeEntryNameDetection() {
        assertTrue(ZpkPackageEnvelope.isSafeEntryName("manifest.json"))
        assertTrue(ZpkPackageEnvelope.isSafeEntryName("index.js"))
        assertTrue(ZpkPackageEnvelope.isSafeEntryName("resources/logo.png"))
        assertFalse(ZpkPackageEnvelope.isSafeEntryName("../secret.txt"))
        assertFalse(ZpkPackageEnvelope.isSafeEntryName("/absolute/path"))
        assertFalse(ZpkPackageEnvelope.isSafeEntryName("\\windows\\path"))
        assertFalse(ZpkPackageEnvelope.isSafeEntryName("foo/../../bar"))
    }
}
