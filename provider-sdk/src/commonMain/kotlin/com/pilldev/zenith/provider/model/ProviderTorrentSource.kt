package com.pilldev.zenith.provider.model

import com.pilldev.zenith.provider.matcher.TorrentTitleParser
import kotlinx.serialization.Serializable

@Serializable
public data class ProviderTorrentSource(
    val title: String,
    val infoHash: String? = null,
    val magnetUri: String? = null,
    val size: Long = 0,
    val seeders: Int = 0,
    val leechers: Int = 0,
    val quality: String = "1080p",
    val trackerName: String = "",
    val translationName: String = "",
    val translationType: ProviderTranslationType = ProviderTranslationType.VO,
    val tags: List<TorrentTitleParser.Tag> = emptyList(),
)
