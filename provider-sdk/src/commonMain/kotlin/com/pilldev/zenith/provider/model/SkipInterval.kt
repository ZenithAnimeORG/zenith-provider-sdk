package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

@Serializable
public data class SkipInterval(
    val startTime: Double,
    val endTime: Double,
    val skipType: String,
    val episodeLength: Double? = null,
)
