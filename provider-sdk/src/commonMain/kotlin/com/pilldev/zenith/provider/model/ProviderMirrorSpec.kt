package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

@Serializable
public data class ProviderMirrorSpec(
    val url: String,
    val displayName: String,
    val isOfficial: Boolean = false,
    val description: String? = null,
    val testPath: String? = null,
    val headers: Map<String, String> = emptyMap(),
)
