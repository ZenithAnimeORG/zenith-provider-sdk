package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.SkipInterval

public interface SkipTimingsProvider : ZenithProvider {
    public suspend fun getSkipTimes(
        mediaRef: MediaRef,
        episodeNumber: Int,
        episodeLength: Double? = null,
        translationName: String? = null,
    ): ProviderResult<List<SkipInterval>>
}
