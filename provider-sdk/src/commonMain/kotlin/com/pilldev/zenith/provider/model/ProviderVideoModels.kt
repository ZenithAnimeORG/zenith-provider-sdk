package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

@Serializable
public enum class ProviderTranslationType {
    DUB,
    VO,
    SUB,
    UNKNOWN,
}

@Serializable
public data class ProviderMediaStream(
    val url: String,
    val quality: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val isHls: Boolean = false,
)

@Serializable
public data class ProviderEpisode(
    val number: Int = 0,
    val url: String = "",
    val quality: Map<String, String>? = null,
    val providerId: ProviderId? = null,
)

@Serializable
public data class ProviderVideoSource(
    val name: String = "",
    val translationName: String = "",
    val translationType: ProviderTranslationType = ProviderTranslationType.VO,
    val episodes: List<ProviderEpisode> = emptyList(),
    val qualities: Map<String, String>? = null,
)
