package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

public const val PACKAGE_FORMAT_V1: String = "zpk/v1"
public const val PACKAGE_FORMAT_V2: String = "zpk/v2"
public const val CURRENT_PACKAGE_FORMAT: String = PACKAGE_FORMAT_V2
public const val CURRENT_MODULE_API_VERSION: String = "1.0.0"

@Serializable
public data class PluginArtifactEntry(
    val jvm: String = "provider.jar",
    val android: String = "provider.dex",
    val js: String = "index.js",
)

@Serializable
public data class PluginManifest(
    val id: ProviderId,
    val name: String,
    val version: String,
    val sdkVersion: String = ProviderSdkInfo.VERSION,
    val packageFormat: String = PACKAGE_FORMAT_V2,
    val moduleApiVersion: String = CURRENT_MODULE_API_VERSION,
    val entrypoint: String = "index.js",
    val description: String = "",
    val author: String? = null,
    val homepage: String? = null,
    val icon: String? = null,
    val capabilities: Set<ProviderCapability> = emptySet(),
    val hosts: List<String> = emptyList(),
    val entry: PluginArtifactEntry = PluginArtifactEntry(),
    val entryClass: String = "",
    val settings: String? = null,
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    val settingsSchema: List<ProviderSettingSpec> = emptyList(),
    val mirrors: List<ProviderMirrorSpec> = emptyList(),
    val localizedNames: Map<String, String> = emptyMap(),
    val localizedDescriptions: Map<String, String> = emptyMap(),
)

public fun PluginManifest.localizedName(locale: String? = null): String {
    if (locale.isNullOrBlank()) return name
    val exact = localizedNames[locale]
    if (exact != null) return exact
    val lang = locale.substringBefore('-').substringBefore('_')
    return localizedNames[lang] ?: name
}

public fun PluginManifest.localizedDescription(locale: String? = null): String {
    if (locale.isNullOrBlank()) return description
    val exact = localizedDescriptions[locale]
    if (exact != null) return exact
    val lang = locale.substringBefore('-').substringBefore('_')
    return localizedDescriptions[lang] ?: description
}

public fun PluginManifest.toSettingsSchema(): SettingsSchema {
    val items = mutableListOf<SettingItemSpec>()
    val mirrorSetting = settingsSchema.firstOrNull { it.type == SettingType.MIRROR_LIST }
    if (mirrors.isNotEmpty() && mirrorSetting == null) {
        items.add(
            SettingItemSpec.MirrorList(
                key = "base_url",
                title = "Mirrors",
                description = "List of available working provider mirrors",
                defaultMirrors = mirrors.map { it.url },
                allowCustom = true,
            ),
        )
    }
    for (spec in settingsSchema) {
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
            SettingType.MIRROR_LIST -> {
                items.add(
                    SettingItemSpec.MirrorList(
                        key = spec.key,
                        title = spec.label,
                        description = spec.description,
                        defaultMirrors = spec.defaultMirrors.ifEmpty { mirrors.map { it.url } },
                        allowCustom = spec.allowCustom,
                    ),
                )
            }
            SettingType.ACTION -> {
                items.add(
                    SettingItemSpec.Action(
                        key = spec.key,
                        title = spec.label,
                        description = spec.description,
                        buttonLabel = spec.buttonLabel ?: spec.label,
                        actionId = spec.actionId ?: spec.key,
                    ),
                )
            }
            SettingType.AUTH_FORM -> {
                items.add(
                    SettingItemSpec.AuthForm(
                        key = spec.key,
                        title = spec.label,
                        description = spec.description,
                        authType = spec.authType,
                        loginUrl = spec.loginUrl,
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
