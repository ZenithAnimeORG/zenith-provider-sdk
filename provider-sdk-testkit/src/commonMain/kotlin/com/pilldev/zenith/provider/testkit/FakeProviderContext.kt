package com.pilldev.zenith.provider.testkit

import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderResult
import io.ktor.client.HttpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class FakeProviderContext(
    override val httpClient: HttpClient = MockProviderHttpClient.withCanonicalFixtures(),
    private val initialSettings: Map<String, String> = emptyMap(),
    private val effectiveMirrorValue: String = "",
) : ProviderContext {
    private val settings = initialSettings.toMutableMap()
    private val streamResolutionsMutex = Mutex()
    private val mutableStreamResolutions: MutableList<String> = mutableListOf()

    public suspend fun streamResolutionsSnapshot(): List<String> = streamResolutionsMutex.withLock { mutableStreamResolutions.toList() }

    override fun getSetting(key: String): String? = settings[key]

    override fun setSetting(
        key: String,
        value: String?
    ) {
        if (value != null) {
            settings[key] = value
        } else {
            settings.remove(key)
        }
    }

    override suspend fun getEffectiveMirror(): String = effectiveMirrorValue

    override suspend fun resolveStream(
        url: String,
        referer: String?,
    ): ProviderResult<List<ProviderMediaStream>> {
        streamResolutionsMutex.withLock { mutableStreamResolutions.add(url) }
        return super.resolveStream(url, referer)
    }
}
