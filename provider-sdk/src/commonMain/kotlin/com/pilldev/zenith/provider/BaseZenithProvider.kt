package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.context.ProviderContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public abstract class BaseZenithProvider : ZenithProvider {
    private val initializationMutex = Mutex()
    private val initializedContext = MutableStateFlow<ProviderContext?>(null)

    final override suspend fun initialize(context: ProviderContext) {
        initializationMutex.withLock {
            val current = initializedContext.value
            check(current == null || current === context) { "Provider is already initialized with a different context" }
            if (current == null) {
                initializedContext.value = context
            }
        }
    }

    final override suspend fun dispose() {
        initializationMutex.withLock {
            if (initializedContext.value != null) {
                initializedContext.value = null
                onDispose()
            }
        }
    }

    protected open suspend fun onDispose() {}

    protected val providerContext: ProviderContext
        get() = checkNotNull(initializedContext.value) { "Provider has not been initialized" }

    protected fun getSetting(key: String): String? = providerContext.getSetting(key)

    protected fun setSetting(
        key: String,
        value: String?,
    ) {
        providerContext.setSetting(key, value)
    }

    protected suspend fun effectiveMirror(): String = providerContext.getEffectiveMirror()
}
