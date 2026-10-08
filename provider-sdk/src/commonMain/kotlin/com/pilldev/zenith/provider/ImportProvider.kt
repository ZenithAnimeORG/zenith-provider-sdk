package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.UserMediaStatus
import kotlinx.serialization.Serializable

public interface ImportProvider : ZenithProvider {
    public val supportedExtensions: List<String>

    public suspend fun parseBackup(data: ByteArray): ProviderResult<ImportBatch>
}

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
