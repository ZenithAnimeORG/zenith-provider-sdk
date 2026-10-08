package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.ProviderEpisode
import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.ProviderVideoSource

public interface MediaSourceProvider : ZenithProvider {
    public suspend fun getSources(
        shikimoriId: Int,
        animeName: String,
        russianName: String?,
    ): ProviderResult<List<ProviderVideoSource>>

    public suspend fun resolveStream(
        episode: ProviderEpisode,
    ): ProviderResult<List<ProviderMediaStream>> =
        if (episode.url.contains(".m3u8") || episode.url.contains(".mp4") || episode.url.startsWith("file://")) {
            ProviderResult.Success(
                listOf(
                    ProviderMediaStream(
                        url = episode.url,
                        quality = "Auto",
                        isHls = episode.url.contains(".m3u8"),
                    ),
                ),
            )
        } else {
            ProviderResult.Failure("URL is not a direct stream, needs extractor: ${episode.url}")
        }
}
