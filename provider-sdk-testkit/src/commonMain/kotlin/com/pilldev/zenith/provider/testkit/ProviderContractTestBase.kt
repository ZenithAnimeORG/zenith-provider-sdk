package com.pilldev.zenith.provider.testkit

import com.pilldev.zenith.provider.ZenithProvider
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
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

    protected fun verifyBasicLifecycle(provider: ZenithProvider) {
        val context = FakeProviderContext()
        provider.init(context)
        assertNotNull(provider.manifest)
    }
}
