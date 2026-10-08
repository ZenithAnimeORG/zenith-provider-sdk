package com.pilldev.zenith.provider

import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.ProviderResult
import kotlinx.serialization.Serializable

public interface CatalogProvider : ZenithProvider {
    public suspend fun search(
        query: String,
        page: Int = 1,
        pageSize: Int = 20,
    ): ProviderResult<CatalogSearchResult>

    public suspend fun getDetails(
        mediaRef: MediaRef,
    ): ProviderResult<CatalogMediaDetails>

    public suspend fun getHomeSections(): ProviderResult<List<CatalogSection>>

    public suspend fun getRelated(
        mediaRef: MediaRef,
    ): ProviderResult<List<CatalogMediaItem>> = ProviderResult.Success(emptyList())
}

@Serializable
public data class CatalogSearchResult(
    val items: List<CatalogMediaItem> = emptyList(),
    val hasNextPage: Boolean = false,
)

@Serializable
public data class CatalogMediaItem(
    val mediaRef: MediaRef,
    val title: String,
    val originalTitle: String? = null,
    val posterUrl: String? = null,
    val score: Double? = null,
    val year: Int? = null,
    val totalEpisodes: Int? = null,
)

@Serializable
public data class CatalogMediaDetails(
    val mediaRef: MediaRef,
    val title: String,
    val originalTitle: String? = null,
    val description: String? = null,
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val score: Double? = null,
    val year: Int? = null,
    val episodesCount: Int? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val status: String? = null,
)

@Serializable
public data class CatalogSection(
    val title: String,
    val items: List<CatalogMediaItem> = emptyList(),
)
