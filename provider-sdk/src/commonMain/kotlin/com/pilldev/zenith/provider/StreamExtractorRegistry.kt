package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public interface StreamExtractorRegistry {
    public fun extractors(): List<StreamExtractor>

    public fun register(extractor: StreamExtractor)

    public fun registerAll(extractors: Collection<StreamExtractor>)

    public suspend fun extract(
        url: String,
        referer: String? = null,
        context: ProviderContext? = null,
    ): ProviderResult<List<ProviderMediaStream>>
}

public class DefaultStreamExtractorRegistry(
    initialExtractors: Collection<StreamExtractor> = emptyList(),
) : StreamExtractorRegistry {
    private val mutex = Mutex()
    private val extractorsList = initialExtractors.toMutableList()

    override fun extractors(): List<StreamExtractor> = extractorsList.toList()

    override fun register(extractor: StreamExtractor) {
        extractorsList.add(extractor)
    }

    override fun registerAll(extractors: Collection<StreamExtractor>) {
        extractorsList.addAll(extractors)
    }

    override suspend fun extract(
        url: String,
        referer: String?,
        context: ProviderContext?,
    ): ProviderResult<List<ProviderMediaStream>> {
        val currentExtractors = mutex.withLock { extractorsList.toList() }
        for (extractor in currentExtractors) {
            if (extractor.canExtract(url)) {
                val result = extractor.extract(url, referer, context)
                if (result is ProviderResult.Success && result.data.isNotEmpty()) {
                    return result
                }
            }
        }
        return ProviderResult.Failure("No matching extractor found for $url")
    }
}
