package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
public value class MediaId(
    public val value: String,
) {
    override fun toString(): String = value
}

@Serializable
public data class MediaRef(
    val id: MediaId,
    val externalIds: Map<ProviderId, String> = emptyMap(),
    val title: String = "",
    val alternateTitles: List<String> = emptyList(),
)

@Serializable
public enum class UserMediaStatus {
    WATCHING,
    PLANNED,
    COMPLETED,
    ON_HOLD,
    DROPPED,
}
