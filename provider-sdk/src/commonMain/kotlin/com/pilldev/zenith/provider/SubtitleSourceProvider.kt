package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.ProviderSubtitle

public interface SubtitleSourceProvider : ZenithProvider {
    public suspend fun searchSubtitles(
        query: String,
        languages: String = "ru,en",
        episodeNumber: Int? = null,
        seasonNumber: Int? = null,
    ): ProviderResult<List<ProviderSubtitle>>

    public suspend fun getDownloadLink(fileId: Long): ProviderResult<String?>
}
