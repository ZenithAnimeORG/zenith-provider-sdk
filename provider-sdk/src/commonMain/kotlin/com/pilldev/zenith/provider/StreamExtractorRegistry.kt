package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public interface StreamExtractorRegistry {
    public suspend fun extractors(): List<StreamExtractor>

    public suspend fun register(extractor: StreamExtractor)

    public suspend fun registerAll(extractors: Collection<StreamExtractor>)

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

    override suspend fun extractors(): List<StreamExtractor> = mutex.withLock { extractorsList.toList() }

    override suspend fun register(extractor: StreamExtractor) {
        mutex.withLock {
            extractorsList.add(extractor)
        }
    }

    override suspend fun registerAll(extractors: Collection<StreamExtractor>) {
        mutex.withLock {
            extractorsList.addAll(extractors)
        }
    }

    override suspend fun extract(
        url: String,
        referer: String?,
        context: ProviderContext?,
    ): ProviderResult<List<ProviderMediaStream>> {
        val currentExtractors = mutex.withLock { extractorsList.toList() }
        var firstFailure: ProviderResult.Failure? = null
        for (extractor in currentExtractors) {
            when (val match = extractor.canExtract(url)) {
                is ProviderResult.Success -> {
                    if (!match.data) continue
                    when (val result = extractor.extract(url, referer, context)) {
                        is ProviderResult.Success -> if (result.data.isNotEmpty()) return result
                        is ProviderResult.Failure -> if (firstFailure == null) firstFailure = result
                    }
                }
                is ProviderResult.Failure -> if (firstFailure == null) firstFailure = match
            }
        }
        return firstFailure ?: ProviderResult.Failure("No matching extractor found for $url")
    }
}
