package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderResult

public interface StreamExtractor {
    public val name: String

    public fun canExtract(url: String): Boolean

    public suspend fun extract(
        url: String,
        referer: String? = null,
        context: ProviderContext? = null,
    ): ProviderResult<List<ProviderMediaStream>>
}
