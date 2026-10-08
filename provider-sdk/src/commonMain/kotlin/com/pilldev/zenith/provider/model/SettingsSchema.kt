package com.pilldev.zenith.provider.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class SettingsSchema(
    val version: Int = 1,
    val items: List<SettingItemSpec> = emptyList(),
)

@Serializable
public sealed interface SettingItemSpec {
    public val key: String
    public val title: String
    public val description: String?

    @Serializable
    @SerialName("text")
    public data class Text(
        override val key: String,
        override val title: String,
        override val description: String? = null,
        val defaultValue: String = "",
        val placeholder: String = "",
        val isSecret: Boolean = false,
        val required: Boolean = false,
    ) : SettingItemSpec

    @Serializable
    @SerialName("toggle")
    public data class Toggle(
        override val key: String,
        override val title: String,
        override val description: String? = null,
        val defaultValue: Boolean = false,
    ) : SettingItemSpec

    @Serializable
    @SerialName("choice")
    public data class Choice(
        override val key: String,
        override val title: String,
        override val description: String? = null,
        val options: List<ChoiceOption> = emptyList(),
        val defaultValue: String = "",
    ) : SettingItemSpec

    @Serializable
    @SerialName("action")
    public data class Action(
        override val key: String,
        override val title: String,
        override val description: String? = null,
        val buttonLabel: String,
        val actionId: String,
    ) : SettingItemSpec

    @Serializable
    @SerialName("mirror_list")
    public data class MirrorList(
        override val key: String,
        override val title: String,
        override val description: String? = null,
        val defaultMirrors: List<String> = emptyList(),
        val allowCustom: Boolean = true,
    ) : SettingItemSpec

    @Serializable
    @SerialName("auth_form")
    public data class AuthForm(
        override val key: String,
        override val title: String,
        override val description: String? = null,
        val authType: AuthType = AuthType.LOGIN_PASSWORD,
        val loginUrl: String? = null,
    ) : SettingItemSpec
}

@Serializable
public data class ChoiceOption(
    val value: String,
    val label: String,
)

@Serializable
public enum class AuthType {
    LOGIN_PASSWORD,
    WEB_OAUTH,
    API_TOKEN,
}

@Serializable
public data class ProviderStatusInfo(
    val status: String,
    val details: Map<String, String> = emptyMap(),
    val latencyMs: Long? = null,
)
