package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.ProviderTorrentSource

public interface TorrentSourceProvider : ZenithProvider {
    public suspend fun search(mediaRef: MediaRef): ProviderResult<List<ProviderTorrentSource>>
}
