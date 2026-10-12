package com.pilldev.zenith.provider.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ProviderCapabilitySerializer::class)
public enum class ProviderCapability {
    MEDIA_SOURCE,
    POSTER_SOURCE,
    SKIP_TIMINGS,
    SUBTITLES,
    TORRENT_SOURCE,
    STREAM_EXTRACTOR,
    CATALOG,
    TRACKER,
    IMPORT,
    UNKNOWN;

    public companion object {
        public fun fromString(raw: String): ProviderCapability =
            when (raw.trim().uppercase()) {
                "MEDIA_SOURCE" -> MEDIA_SOURCE
                "POSTER_SOURCE" -> POSTER_SOURCE
                "SKIP_TIMINGS" -> SKIP_TIMINGS
                "SUBTITLES" -> SUBTITLES
                "TORRENT_SOURCE", "TORRENT" -> TORRENT_SOURCE
                "STREAM_EXTRACTOR" -> STREAM_EXTRACTOR
                "CATALOG" -> CATALOG
                "TRACKER" -> TRACKER
                "IMPORT" -> IMPORT
                else -> UNKNOWN
            }
    }
}

public object ProviderCapabilitySerializer : KSerializer<ProviderCapability> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ProviderCapability", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ProviderCapability {
        val raw = decoder.decodeString()
        return ProviderCapability.fromString(raw)
    }

    override fun serialize(
        encoder: Encoder,
        value: ProviderCapability
    ) {
        encoder.encodeString(value.name)
    }
}
