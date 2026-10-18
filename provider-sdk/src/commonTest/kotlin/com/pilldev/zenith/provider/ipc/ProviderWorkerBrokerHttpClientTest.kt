package com.pilldev.zenith.provider.ipc

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProviderWorkerBrokerHttpClientTest {
    @Test
    fun brokerHttpClientForwardsBoundedRequestAndMapsHttpResponse() = kotlinx.coroutines.test.runTest {
        val responseBytes = "broker response".encodeToByteArray()
        val client = createProviderWorkerBrokerHttpClient(
            exchange = { operation, requestBody ->
                assertEquals("https://provider.example/api", operation.url)
                assertEquals("POST", operation.method)
                assertEquals(listOf("client-test"), operation.headers["X-Client-Test"])
                assertEquals(listOf(ContentType.Application.Json.toString()), operation.headers[HttpHeaders.ContentType])
                assertEquals("payload".encodeToByteArray().toList(), requestBody?.toList())
                ProviderWorkerBrokerResponse(
                    ProviderBrokerResult.HttpResponse(
                        statusCode = 201,
                        headers = mapOf(HttpHeaders.ContentType to listOf("text/plain")),
                        body = ProviderWorkerBlob("broker_body", responseBytes.size.toLong(), "a".repeat(64)),
                    ),
                    responseBody = responseBytes,
                )
            },
            maxRequestBytes = 16,
            maxResponseBytes = 32,
        )

        val response = client.post("https://provider.example/api") {
            header("X-Client-Test", "client-test")
            contentType(ContentType.Application.Json)
            setBody("payload")
        }

        assertEquals(201, response.status.value)
        assertEquals("broker response", response.bodyAsText())
        client.close()
    }

    @Test
    fun brokerHttpClientRejectsRequestBodiesAboveConfiguredLimit() = kotlinx.coroutines.test.runTest {
        var brokerCalled = false
        val client = createProviderWorkerBrokerHttpClient(
            exchange = { _, _ ->
                brokerCalled = true
                ProviderWorkerBrokerResponse(ProviderBrokerResult.HttpResponse(statusCode = 200))
            },
            maxRequestBytes = 4,
        )

        assertFailsWith<IllegalArgumentException> {
            client.post("https://provider.example/api") { setBody("oversized") }
        }

        assertEquals(false, brokerCalled)
        client.close()
    }

    @Test
    fun brokerHttpClientRejectsBrokerFailureAsAnExplicitException() = kotlinx.coroutines.test.runTest {
        val client = createProviderWorkerBrokerHttpClient(
            exchange = { _, _ ->
                ProviderWorkerBrokerResponse(
                    ProviderBrokerResult.Failed(
                        ProviderWorkerFailureCode.BROKER_FAILURE,
                        "request denied",
                    ),
                )
            },
        )

        val failure = assertFailsWith<ProviderWorkerBrokerException> {
            client.post("https://provider.example/api")
        }

        assertEquals("request denied", failure.message)
        client.close()
    }

    @Test
    fun brokerHttpClientPreservesWorkerRequestContextInExchange() = kotlinx.coroutines.test.runTest {
        var contextInExchange: kotlin.coroutines.CoroutineContext? = null
        val client = createProviderWorkerBrokerHttpClient(
            exchange = { _, _ ->
                contextInExchange = kotlinx.coroutines.currentCoroutineContext()
                ProviderWorkerBrokerResponse(
                    ProviderBrokerResult.HttpResponse(200),
                )
            },
        )
        kotlinx.coroutines.withContext(WorkerRequestContext("test_req_123")) {
            client.post("https://provider.example/api")
        }
        assertEquals("test_req_123", contextInExchange?.get(WorkerRequestContext)?.requestId)
        client.close()
    }
}

