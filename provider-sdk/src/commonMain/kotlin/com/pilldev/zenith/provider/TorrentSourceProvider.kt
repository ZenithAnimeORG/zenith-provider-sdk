package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.ProviderTorrentSource

public interface TorrentSourceProvider : ZenithProvider {
    public suspend fun search(
        query: String,
        russianName: String? = null,
        shikimoriId: Int = 0,
    ): ProviderResult<List<ProviderTorrentSource>>
}
