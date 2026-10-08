package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.PluginManifest

public interface ZenithProviderFactory {
    public fun create(manifest: PluginManifest): ZenithProvider
}
