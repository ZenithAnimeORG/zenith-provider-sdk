package com.pilldev.zenith.provider.model

import kotlinx.serialization.Serializable

@Serializable
public sealed interface ProviderTestResult {
    @Serializable
    public data class Success(
        val message: String,
        val latencyMs: Long = 0L,
    ) : ProviderTestResult

    @Serializable
    public data class Failure(
        val error: String,
        val message: String? = null,
    ) : ProviderTestResult
}

public suspend inline fun measureTest(
    crossinline block: suspend () -> String,
): ProviderTestResult {
    val mark = kotlin.time.TimeSource.Monotonic
        .markNow()
    return try {
        val message = block()
        ProviderTestResult.Success(message, mark.elapsedNow().inWholeMilliseconds)
    } catch (e: Throwable) {
        ProviderTestResult.Failure(e.message ?: "Test failed", e.stackTraceToString())
    }
}
