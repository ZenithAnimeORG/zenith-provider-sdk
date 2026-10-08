package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.UserMediaStatus

public interface TrackerProvider : ZenithProvider {
    public suspend fun onEpisodeWatched(
        mediaRef: MediaRef,
        episode: Int,
    ): ProviderResult<Unit>

    public suspend fun syncStatus(
        mediaRef: MediaRef,
        status: UserMediaStatus,
    ): ProviderResult<Unit>
}
