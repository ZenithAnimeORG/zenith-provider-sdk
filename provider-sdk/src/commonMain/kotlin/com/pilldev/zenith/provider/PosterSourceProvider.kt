package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.ProviderResult

public interface PosterSourceProvider : ZenithProvider {
    public suspend fun getPoster(
        animeId: Int,
        animeName: String,
    ): ProviderResult<String?>
}
