package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

@Serializable
public data class ProviderSubtitle(
    val fileId: Long = 0L,
    val language: String,
    val releaseName: String,
    val isHearingImpaired: Boolean = false,
    val isAiTranslated: Boolean = false,
    val downloadCount: Int = 0,
    val directUrl: String? = null,
    val provider: String = "",
)
