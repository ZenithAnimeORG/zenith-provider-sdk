package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.UserMediaStatus
import kotlinx.serialization.Serializable

public interface ImportProvider : ZenithProvider {
    public val supportedExtensions: List<String>
    public val instructions: List<ImportInstructionStep> get() = emptyList()

    public suspend fun parseBackup(data: ByteArray): ProviderResult<ImportBatch>
}

@Serializable
public data class ImportInstructionStep(
    val stepNumber: Int = 1,
    val title: String,
    val description: String? = null,
    val note: String? = null,
    val linkUrl: String? = null,
    val linkText: String? = null,
)

@Serializable
public data class ImportBatch(
    val sourceName: String,
    val entries: List<RawImportEntry>,
)

@Serializable
public data class RawImportEntry(
    val title: String,
    val originalTitle: String? = null,
    val releaseYear: Int? = null,
    val watchedEpisodes: Int = 0,
    val totalEpisodes: Int? = null,
    val targetStatus: UserMediaStatus = UserMediaStatus.WATCHING,
    val rating: Int? = null,
)
