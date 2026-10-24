package com.pilldev.zenith.provider.model

public object ZpkPackageEnvelope {
    public const val MAX_PACKAGE_SIZE_BYTES: Long = 16L * 1024 * 1024 // 16 MiB
    public const val MAX_TOTAL_UNCOMPRESSED_BYTES: Long = 32L * 1024 * 1024 // 32 MiB
    public const val MAX_MANIFEST_BYTES: Long = 256L * 1024 // 256 KiB
    public const val MAX_ENTRYPOINT_BYTES: Long = 5L * 1024 * 1024 // 5 MiB
    public const val MAX_SETTINGS_BYTES: Long = 128L * 1024 // 128 KiB
    public const val MAX_ICON_BYTES: Long = 256L * 1024 // 256 KiB
    public const val MAX_RESOURCE_BYTES: Long = 2L * 1024 * 1024 // 2 MiB
    public const val MAX_ENTRY_COUNT: Int = 100

    public sealed interface ValidationResult {
        public val isValid: Boolean

        public data object Valid : ValidationResult {
            override val isValid: Boolean = true
        }

        public data class Invalid(
            val errors: List<String>,
        ) : ValidationResult {
            override val isValid: Boolean = false
        }
    }

    public fun isSafeEntryName(name: String): Boolean {
        if (name.isBlank()) return false
        if (name.startsWith("/") || name.startsWith("\\")) return false
        if (name.contains("..")) return false
        return name.matches(Regex("^[a-zA-Z0-9_\\-./]+$"))
    }

    public fun validateEnvelope(
        manifest: PluginManifest,
        entries: Map<String, Long>,
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (entries.size > MAX_ENTRY_COUNT) {
            errors.add("Package entry count ${entries.size} exceeds maximum limit of $MAX_ENTRY_COUNT")
        }

        var totalBytes = 0L

        for ((name, size) in entries) {
            if (!isSafeEntryName(name)) {
                errors.add("Unsafe entry path in package: $name")
            }

            totalBytes += size

            val lower = name.lowercase()
            if (manifest.packageFormat == PACKAGE_FORMAT_V2) {
                if (lower.endsWith(".jar") ||
                    lower.endsWith(".dex") ||
                    lower.endsWith(".class") ||
                    lower.endsWith(".so") ||
                    lower.endsWith(".dll") ||
                    lower.endsWith(".dylib") ||
                    lower.endsWith(".exe") ||
                    lower.endsWith(".apk")
                ) {
                    errors.add("Forbidden executable binary in portable package: $name")
                }
            }

            if (name == "manifest.json" && size > MAX_MANIFEST_BYTES) {
                errors.add("manifest.json exceeds size limit of $MAX_MANIFEST_BYTES bytes")
            }

            val entrypoint = manifest.entrypoint.ifBlank { "index.js" }
            if (name == entrypoint && size > MAX_ENTRYPOINT_BYTES) {
                errors.add("Entrypoint '$entrypoint' exceeds size limit of $MAX_ENTRYPOINT_BYTES bytes")
            }

            if (name == (manifest.settings ?: "settings.json") && size > MAX_SETTINGS_BYTES) {
                errors.add("Settings schema exceeds size limit of $MAX_SETTINGS_BYTES bytes")
            }

            if (name == (manifest.icon ?: "icon.png") && size > MAX_ICON_BYTES) {
                errors.add("Icon exceeds size limit of $MAX_ICON_BYTES bytes")
            }
        }

        if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
            errors.add("Total uncompressed size $totalBytes exceeds limit of $MAX_TOTAL_UNCOMPRESSED_BYTES bytes")
        }

        if (!entries.containsKey("manifest.json")) {
            errors.add("Package envelope is missing required entry: manifest.json")
        }

        if (manifest.packageFormat == PACKAGE_FORMAT_V2) {
            val entrypoint = manifest.entrypoint.ifBlank { "index.js" }
            if (!entries.containsKey(entrypoint)) {
                errors.add("Package envelope is missing declared entrypoint: $entrypoint")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
