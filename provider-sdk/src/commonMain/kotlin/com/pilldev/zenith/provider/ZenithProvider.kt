package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.ProviderStatusInfo
import com.pilldev.zenith.provider.model.ProviderTestResult
import com.pilldev.zenith.provider.model.SettingsSchema
import com.pilldev.zenith.provider.model.toSettingsSchema

public interface ZenithProvider {
    public val manifest: PluginManifest

    public val icon: String?
        get() = manifest.icon

    public val settingsSchema: SettingsSchema?
        get() = manifest.toSettingsSchema().takeIf { it.items.isNotEmpty() }

    public suspend fun initialize(context: ProviderContext) {}

    public suspend fun dispose()

    public suspend fun test(context: ProviderContext): ProviderTestResult = ProviderTestResult.Success("Provider is available and ready")

    public suspend fun executeAction(
        actionId: String,
        context: ProviderContext,
    ): ProviderResult<String> = ProviderResult.Failure("Action $actionId is not supported")

    public suspend fun probeStatus(
        context: ProviderContext,
    ): ProviderResult<ProviderStatusInfo> = ProviderResult.Success(ProviderStatusInfo(status = "OK"))
}
