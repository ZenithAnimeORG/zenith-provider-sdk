package com.pilldev.zenith.provider.model

public fun ProviderId.displayName(): String =
    value
        .split("_", "-")
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercaseChar() } }
