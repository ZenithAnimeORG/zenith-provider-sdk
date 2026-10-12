package com.pilldev.zenith.provider.testkit

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

public object MockProviderHttpClient {
    public fun withCanonicalFixtures(): HttpClient =
        create { request ->
            val acceptsHtml = request.headers[HttpHeaders.Accept]?.contains("text/html", ignoreCase = true) == true
            if (acceptsHtml) {
                respond(
                    content = "<html><body><h1>Zenith provider contract fixture</h1></body></html>",
                    headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
                )
            } else {
                respond(
                    content = """{"status":"ok","data":[]}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }

    public fun create(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = HttpClient(MockEngine(handler))

    public fun withStaticResponses(
        responses: Map<String, String>,
        defaultStatusCode: HttpStatusCode = HttpStatusCode.OK,
        defaultHeaders: Headers = headersOf("Content-Type", "application/json"),
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                val url = request.url.toString()
                val matchedKey = responses.keys.firstOrNull { url.contains(it) }
                if (matchedKey != null) {
                    respond(
                        content = responses.getValue(matchedKey),
                        status = defaultStatusCode,
                        headers = defaultHeaders,
                    )
                } else {
                    respond(
                        content = "Not Found: $url",
                        status = HttpStatusCode.NotFound,
                    )
                }
            },
        )
}
