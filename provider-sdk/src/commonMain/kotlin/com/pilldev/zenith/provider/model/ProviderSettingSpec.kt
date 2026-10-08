package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

@Serializable
public enum class SettingType {
    STRING,
    URL,
    PASSWORD,
    BOOLEAN,
    SELECT,
}

@Serializable
public data class ProviderSettingSpec(
    val key: String,
    val label: String,
    val type: SettingType = SettingType.STRING,
    val required: Boolean = false,
    val defaultValue: String? = null,
    val description: String? = null,
    val options: List<String> = emptyList(),
)
