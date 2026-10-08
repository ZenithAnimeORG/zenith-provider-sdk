package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.SkipInterval

public interface SkipTimingsProvider : ZenithProvider {
    public suspend fun getSkipTimes(
        malId: Int,
        episodeNumber: Int,
        episodeLength: Double? = null,
        shikimoriId: Int = 0,
        translationName: String? = null,
    ): ProviderResult<List<SkipInterval>>
}
