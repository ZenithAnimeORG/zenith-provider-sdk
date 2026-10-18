package com.pilldev.zenith.provider.ipc

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.util.AttributeKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.write

public fun createProviderWorkerBrokerHttpClient(
    broker: ProviderWorkerBrokerClient,
    maxRequestBytes: Int = DEFAULT_MAX_HTTP_BODY_BYTES,
    maxResponseBytes: Int = DEFAULT_MAX_HTTP_BODY_BYTES,
): HttpClient =
    createProviderWorkerBrokerHttpClient(
        exchange = { operation, requestBody -> broker.request(operation, requestBody) },
        maxRequestBytes = maxRequestBytes,
        maxResponseBytes = maxResponseBytes,
    )

public fun createProviderWorkerBrokerHttpClient(
    exchange: suspend (
        operation: ProviderBrokerOperation.HttpRequest,
        requestBody: ByteArray?,
    ) -> ProviderWorkerBrokerResponse,
    maxRequestBytes: Int = DEFAULT_MAX_HTTP_BODY_BYTES,
    maxResponseBytes: Int = DEFAULT_MAX_HTTP_BODY_BYTES,
): HttpClient {
    require(maxRequestBytes in 1..PROVIDER_WORKER_MAX_BLOB_BYTES.toInt()) {
        "Provider HTTP request limit must be between 1 byte and the worker blob limit"
    }
    require(maxResponseBytes in 1..PROVIDER_WORKER_MAX_BLOB_BYTES.toInt()) {
        "Provider HTTP response limit must be between 1 byte and the worker blob limit"
    }
    val client = HttpClient(ProviderWorkerBrokerHttpEngine(exchange, maxRequestBytes, maxResponseBytes))
    client.requestPipeline.intercept(HttpRequestPipeline.State) {
        val workerContext = currentCoroutineContext()[WorkerRequestContext]
        if (workerContext != null) {
            context.attributes.put(WorkerRequestContextAttributeKey, workerContext)
        }
    }
    return client
}

internal val WorkerRequestContextAttributeKey = AttributeKey<WorkerRequestContext>("WorkerRequestContext")

private class ProviderWorkerBrokerHttpEngine(
    private val exchange: suspend (
        operation: ProviderBrokerOperation.HttpRequest,
        requestBody: ByteArray?,
    ) -> ProviderWorkerBrokerResponse,
    private val maxRequestBytes: Int,
    private val maxResponseBytes: Int,
) : HttpClientEngineBase("ProviderWorkerBrokerHttpEngine") {
    override val config = io.ktor.client.engine.HttpClientEngineConfig()

    @OptIn(io.ktor.utils.io.InternalAPI::class)
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val requestBody = data.body.readBounded(maxRequestBytes)
        val headers = linkedMapOf<String, MutableList<String>>()
        data.headers.appendTo(headers)
        data.body.headers.appendTo(headers)
        data.body.contentType?.let { contentType ->
            if (headers.keys.none { it.equals(HttpHeaders.ContentType, ignoreCase = true) }) {
                headers.getOrPut(HttpHeaders.ContentType) { mutableListOf() }.add(contentType.toString())
            }
        }
        val workerContext = data.attributes.getOrNull(WorkerRequestContextAttributeKey)
            ?: currentCoroutineContext()[WorkerRequestContext]
        val response = if (workerContext != null) {
            withContext(workerContext) {
                exchange(
                    ProviderBrokerOperation.HttpRequest(
                        url = data.url.toString(),
                        method = data.method.value,
                        headers = headers,
                        body = null,
                    ),
                    requestBody.takeIf { it.isNotEmpty() },
                )
            }
        } else {
            exchange(
                ProviderBrokerOperation.HttpRequest(
                    url = data.url.toString(),
                    method = data.method.value,
                    headers = headers,
                    body = null,
                ),
                requestBody.takeIf { it.isNotEmpty() },
            )
        }
        val httpResponse = response.result as? ProviderBrokerResult.HttpResponse
            ?: throw ProviderWorkerBrokerException(response.result)
        val responseBody = response.responseBody ?: ByteArray(0)
        require(responseBody.size <= maxResponseBytes) {
            "Provider HTTP response exceeds the configured body limit"
        }
        val responseHeaders = Headers.build {
            httpResponse.headers.forEach { (name, values) -> values.forEach { append(name, it) } }
        }
        return HttpResponseData(
            statusCode = HttpStatusCode.fromValue(httpResponse.statusCode),
            requestTime = io.ktor.util.date.GMTDate(),
            headers = responseHeaders,
            version = HttpProtocolVersion.HTTP_1_1,
            body = ByteReadChannel(responseBody),
            callContext = callContext(),
        )
    }

    private suspend fun OutgoingContent.readBounded(limit: Int): ByteArray =
        when (this) {
            is OutgoingContent.NoContent -> byteArrayOf()
            is OutgoingContent.ByteArrayContent -> bytes().also {
                require(it.size <= limit) { "Provider HTTP request exceeds the configured body limit" }
            }
            is OutgoingContent.ReadChannelContent -> readFrom().readBounded(limit)
            is OutgoingContent.WriteChannelContent -> {
                val channel = ByteChannel(autoFlush = true)
                coroutineScope {
                    var writerFailure: Throwable? = null
                    val writer = launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            writeTo(channel)
                            channel.close()
                        } catch (error: Throwable) {
                            writerFailure = error
                            channel.close()
                        }
                    }
                    try {
                        channel.readBounded(limit).also {
                            writer.join()
                            writerFailure?.let { error -> throw error }
                        }
                    } finally {
                        writer.cancel()
                    }
                }
            }
            is OutgoingContent.ProtocolUpgrade -> throw IllegalArgumentException(
                "Provider HTTP broker does not support protocol upgrades",
            )
            is OutgoingContent.ContentWrapper -> delegate().readBounded(limit)
        }

    private suspend fun ByteReadChannel.readBounded(limit: Int): ByteArray {
        val buffer = Buffer()
        val chunk = ByteArray(minOf(IO_CHUNK_BYTES, limit + 1))
        var total = 0
        while (true) {
            val available = minOf(chunk.size, limit + 1 - total)
            if (available <= 0) {
                throw IllegalArgumentException("Provider HTTP body exceeds the configured limit")
            }
            val read = readAvailable(chunk, 0, available)
            if (read < 0) break
            if (read == 0) continue
            total += read
            require(total <= limit) { "Provider HTTP body exceeds the configured limit" }
            buffer.write(chunk, 0, read)
        }
        return buffer.readByteArray()
    }

    private fun Headers.appendTo(target: MutableMap<String, MutableList<String>>) {
        forEach { name, values ->
            target.getOrPut(name) { mutableListOf() }.addAll(values)
        }
    }

    private companion object {
        const val IO_CHUNK_BYTES = 8 * 1024
    }
}

public class ProviderWorkerBrokerException(
    public val result: ProviderBrokerResult,
) : IllegalStateException(
        (result as? ProviderBrokerResult.Failed)?.message ?: "Provider HTTP broker returned a non-HTTP result",
    )

private const val DEFAULT_MAX_HTTP_BODY_BYTES = 8 * 1024 * 1024
