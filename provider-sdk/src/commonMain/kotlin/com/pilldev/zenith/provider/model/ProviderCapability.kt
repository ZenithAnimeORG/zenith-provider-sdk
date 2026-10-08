package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

@Serializable
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
}
