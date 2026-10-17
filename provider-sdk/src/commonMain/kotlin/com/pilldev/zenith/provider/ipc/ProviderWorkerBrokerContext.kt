package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderResult
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException

public class ProviderWorkerBrokerContext(
    private val broker: ProviderWorkerBrokerClient,
    private val settings: ProviderWorkerSettings,
    maxRequestBytes: Int = PROVIDER_WORKER_MAX_HTTP_BODY_BYTES,
    maxResponseBytes: Int = PROVIDER_WORKER_MAX_HTTP_BODY_BYTES,
) : ProviderContext,
    AutoCloseable {
    override val httpClient: HttpClient =
        createProviderWorkerBrokerHttpClient(broker, maxRequestBytes, maxResponseBytes)

    override fun getSetting(key: String): String? = settings.get(key)

    override fun setSetting(
        key: String,
        value: String?,
    ) {
        settings.set(key, value)
    }

    override suspend fun getEffectiveMirror(): String =
        when (val result = broker.request(ProviderBrokerOperation.GetEffectiveMirror).result) {
            is ProviderBrokerResult.Mirror -> result.url
            is ProviderBrokerResult.Failed -> throw ProviderWorkerBrokerException(result)
            else -> throw IllegalStateException("Provider mirror broker returned an invalid response")
        }

    override suspend fun resolveStream(
        url: String,
        referer: String?,
    ): ProviderResult<List<ProviderMediaStream>> =
        try {
            when (val result = broker.request(ProviderBrokerOperation.ResolveStream(url, referer)).result) {
                is ProviderBrokerResult.Streams -> ProviderResult.Success(result.values)
                is ProviderBrokerResult.Failed -> ProviderResult.Failure(result.message)
                else -> ProviderResult.Failure("Provider stream broker returned an invalid response")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ProviderResult.Failure(error.message ?: "Provider stream broker request failed", error)
        }

    override fun close() {
        httpClient.close()
    }
}
