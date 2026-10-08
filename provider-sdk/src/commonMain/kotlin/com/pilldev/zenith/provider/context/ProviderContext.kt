package com.pilldev.zenith.provider.context

import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderResult
import io.ktor.client.HttpClient

public interface ProviderContext {
    public val httpClient: HttpClient

    public val jsEvaluator: JsEvaluator? get() = null

    public fun getSetting(key: String): String?

    public fun setSetting(
        key: String,
        value: String?
    )

    /**
     * Returns the active mirror URL for the provider.
     * In Auto mode, returns the lowest-latency responsive mirror from the declared mirrors pool.
     * In Manual mode, returns the user-selected or custom URL.
     */
    public suspend fun getEffectiveMirror(): String = ""

    /**
     * Attempts to resolve an embed/iframe media stream URL using the registered stream extractors.
     */
    public suspend fun resolveStream(
        url: String,
        referer: String? = null,
    ): ProviderResult<List<ProviderMediaStream>> = ProviderResult.Failure("Stream extractor not found for URL: $url")
}
