package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.PluginManifest

public abstract class BaseZenithProvider(
    manifest: PluginManifest? = null,
) : ZenithProvider {
    private val explicitManifest: PluginManifest? = manifest

    override val manifest: PluginManifest
        get() = explicitManifest ?: metadata

    protected var context: ProviderContext? = null

    override fun init(context: ProviderContext) {
        this.context = context
    }

    protected val providerContext: ProviderContext?
        get() = context

    protected fun getSetting(key: String): String? = context?.getSetting(key)

    protected fun setSetting(
        key: String,
        value: String?,
    ) {
        context?.setSetting(key, value)
    }

    protected suspend fun effectiveMirror(): String = context?.getEffectiveMirror().orEmpty()
}
