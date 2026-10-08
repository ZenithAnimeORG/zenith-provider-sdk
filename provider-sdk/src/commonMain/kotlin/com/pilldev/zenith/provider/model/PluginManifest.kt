package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

@Serializable
public data class PluginArtifactEntry(
    val jvm: String = "provider.jar",
    val android: String = "provider.dex",
)

@Serializable
public data class PluginManifest(
    val id: ProviderId,
    val name: String,
    val version: String,
    val sdkVersion: String = "2.0",
    val description: String = "",
    val author: String? = null,
    val homepage: String? = null,
    val icon: String? = null,
    val capabilities: Set<ProviderCapability> = emptySet(),
    val hosts: List<String> = emptyList(),
    val entry: PluginArtifactEntry = PluginArtifactEntry(),
    val entryClass: String = "",
    val publicKey: String? = null,
    val settings: String? = null,
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    val settingsSchema: List<ProviderSettingSpec> = emptyList(),
    val mirrors: List<ProviderMirrorSpec> = emptyList(),
)

public typealias ProviderMetadata = PluginManifest

public fun PluginManifest.toSettingsSchema(): SettingsSchema {
    val items = mutableListOf<SettingItemSpec>()
    if (mirrors.isNotEmpty()) {
        items.add(
            SettingItemSpec.MirrorList(
                key = "base_url",
                title = "Зеркала",
                description = "Список доступных рабочих зеркал провайдера",
                defaultMirrors = mirrors.map { it.url },
                allowCustom = true,
            ),
        )
    }
    for (spec in settingsSchema) {
        if (mirrors.isNotEmpty() && (spec.key == "base_url" || spec.key == "mirror")) continue
        when (spec.type) {
            SettingType.BOOLEAN -> {
                items.add(
                    SettingItemSpec.Toggle(
                        key = spec.key,
                        title = spec.label,
                        description = spec.description,
                        defaultValue = spec.defaultValue?.toBooleanStrictOrNull() ?: false,
                    ),
                )
            }
            SettingType.PASSWORD -> {
                items.add(
                    SettingItemSpec.Text(
                        key = spec.key,
                        title = spec.label,
                        description = spec.description,
                        defaultValue = spec.defaultValue.orEmpty(),
                        placeholder = spec.defaultValue.orEmpty(),
                        isSecret = true,
                        required = spec.required,
                    ),
                )
            }
            SettingType.SELECT -> {
                items.add(
                    SettingItemSpec.Choice(
                        key = spec.key,
                        title = spec.label,
                        description = spec.description,
                        options = spec.options.map { ChoiceOption(value = it, label = it) },
                        defaultValue = spec.defaultValue.orEmpty(),
                    ),
                )
            }
            SettingType.STRING, SettingType.URL -> {
                items.add(
                    SettingItemSpec.Text(
                        key = spec.key,
                        title = spec.label,
                        description = spec.description,
                        defaultValue = spec.defaultValue.orEmpty(),
                        placeholder = spec.defaultValue.orEmpty(),
                        isSecret = false,
                        required = spec.required,
                    ),
                )
            }
        }
    }
    return SettingsSchema(items = items)
}
