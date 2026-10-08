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
        get() = metadata

    public val metadata: PluginManifest
        get() = manifest

    public val settingsSchema: SettingsSchema?
        get() = manifest.toSettingsSchema().takeIf { it.items.isNotEmpty() }

    public fun init(context: ProviderContext) {}

    public suspend fun test(context: ProviderContext): ProviderTestResult = ProviderTestResult.Success("Провайдер доступен и готов к работе")

    public suspend fun executeAction(
        actionId: String,
        context: ProviderContext,
    ): ProviderResult<String> = ProviderResult.Failure("Действие $actionId не поддерживается")

    public suspend fun probeStatus(
        context: ProviderContext,
    ): ProviderResult<ProviderStatusInfo> = ProviderResult.Success(ProviderStatusInfo(status = "OK"))
}
