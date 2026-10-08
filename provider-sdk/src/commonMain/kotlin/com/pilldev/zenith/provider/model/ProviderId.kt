package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
public value class ProviderId(
    public val value: String
) {
    override fun toString(): String = value
}
