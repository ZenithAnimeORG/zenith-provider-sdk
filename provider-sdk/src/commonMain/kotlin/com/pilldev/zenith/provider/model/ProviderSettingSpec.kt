package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

@Serializable
public enum class SettingType {
    STRING,
    URL,
    PASSWORD,
    BOOLEAN,
    SELECT,
    MIRROR_LIST,
    ACTION,
    AUTH_FORM,
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
    val defaultMirrors: List<String> = emptyList(),
    val allowCustom: Boolean = true,
    val buttonLabel: String? = null,
    val actionId: String? = null,
    val authType: AuthType = AuthType.LOGIN_PASSWORD,
    val loginUrl: String? = null,
)
