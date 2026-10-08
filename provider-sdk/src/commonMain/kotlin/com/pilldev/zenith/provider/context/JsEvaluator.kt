package com.pilldev.zenith.provider.context

public interface JsEvaluator {
    public suspend fun evaluate(script: String): String

    public suspend fun loadContext(scriptContent: String)
}
