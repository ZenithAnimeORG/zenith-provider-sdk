package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.ProviderResult

public interface PosterSourceProvider : ZenithProvider {
    public suspend fun getPoster(mediaRef: MediaRef): ProviderResult<String?>
}
