package com.pilldev.zenith.provider.net

public object UrlNormalizer {
    public fun resolve(
        rawUrl: String,
        baseUrl: String? = null,
        defaultScheme: String = "https",
    ): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""

        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "$defaultScheme:$trimmed"
            baseUrl != null && trimmed.startsWith("/") -> {
                val base = baseUrl.trimEnd('/')
                val root =
                    if (base.contains("://")) {
                        val scheme = base.substringBefore("://")
                        val rest = base.substringAfter("://")
                        val host = rest.substringBefore('/')
                        "$scheme://$host"
                    } else {
                        base
                    }
                "$root$trimmed"
            }
            baseUrl != null -> {
                val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                "$base$trimmed"
            }
            else -> trimmed
        }
    }
}
