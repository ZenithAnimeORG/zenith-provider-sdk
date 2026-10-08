package com.pilldev.zenith.provider.model

public sealed class ProviderResult<out T> {
    public data class Success<out T>(
        val data: T
    ) : ProviderResult<T>()

    public data class Failure(
        val message: String,
        val cause: Throwable? = null
    ) : ProviderResult<Nothing>()

    public inline fun <R> map(transform: (T) -> R): ProviderResult<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Failure -> this
        }

    public fun getOrNull(): T? =
        when (this) {
            is Success -> data
            is Failure -> null
        }

    public fun getOrDefault(default: @UnsafeVariance T): T =
        when (this) {
            is Success -> data
            is Failure -> default
        }

    public inline fun getOrElse(onFailure: (Failure) -> @UnsafeVariance T): T =
        when (this) {
            is Success -> data
            is Failure -> onFailure(this)
        }

    public companion object {
        public inline fun <T> of(block: () -> T): ProviderResult<T> =
            try {
                Success(block())
            } catch (t: Throwable) {
                Failure(t.message ?: "Unknown provider error", t)
            }
    }
}
