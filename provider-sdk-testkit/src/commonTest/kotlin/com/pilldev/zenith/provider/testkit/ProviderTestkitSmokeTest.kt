package com.pilldev.zenith.provider.testkit

import com.pilldev.zenith.provider.BaseZenithProvider
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderId
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProviderTestkitSmokeTest : ProviderContractTestBase() {
    private class SamplePlugin(
        manifest: PluginManifest,
    ) : BaseZenithProvider(manifest)

    @Test
    fun testContractVerification() {
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
    fun testFakeProviderContextAndMockClient() =
        runTest {
            val client = MockProviderHttpClient.withStaticResponses(
                mapOf("sample.org/data" to "{\"status\":\"ok\"}"),
            )
            val context = FakeProviderContext(httpClient = client)
            context.setSetting("key1", "val1")
            assertEquals("val1", context.getSetting("key1"))

            val response = context.httpClient.get("https://sample.org/data").bodyAsText()
            assertTrue(response.contains("\"status\":\"ok\""))
        }
}
