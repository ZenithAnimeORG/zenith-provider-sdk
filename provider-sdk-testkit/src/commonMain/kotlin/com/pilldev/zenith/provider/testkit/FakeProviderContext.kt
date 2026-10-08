package com.pilldev.zenith.provider.testkit

import com.pilldev.zenith.provider.context.JsEvaluator
import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond

public class FakeProviderContext(
    override val httpClient: HttpClient = HttpClient(MockEngine { respond("") }),
    override val jsEvaluator: JsEvaluator? = null,
    private val initialSettings: Map<String, String> = emptyMap(),
    private val effectiveMirrorValue: String = "",
) : ProviderContext {
    private val settings = initialSettings.toMutableMap()
    public val streamResolutions: MutableList<String> = mutableListOf()

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
        streamResolutions.add(url)
        return super.resolveStream(url, referer)
    }
}
