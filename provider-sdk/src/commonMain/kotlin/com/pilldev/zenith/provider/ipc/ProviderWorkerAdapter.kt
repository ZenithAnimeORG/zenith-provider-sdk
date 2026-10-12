package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.CatalogMediaDetails
import com.pilldev.zenith.provider.CatalogMediaItem
import com.pilldev.zenith.provider.CatalogProvider
import com.pilldev.zenith.provider.CatalogSearchResult
import com.pilldev.zenith.provider.CatalogSection
import com.pilldev.zenith.provider.ImportBatch
import com.pilldev.zenith.provider.ImportInstructionStep
import com.pilldev.zenith.provider.ImportProvider
import com.pilldev.zenith.provider.MediaSourceProvider
import com.pilldev.zenith.provider.PosterSourceProvider
import com.pilldev.zenith.provider.SkipTimingsProvider
import com.pilldev.zenith.provider.StreamExtractor
import com.pilldev.zenith.provider.StreamExtractorProvider
import com.pilldev.zenith.provider.SubtitleSourceProvider
import com.pilldev.zenith.provider.TorrentSourceProvider
import com.pilldev.zenith.provider.TrackerProvider
import com.pilldev.zenith.provider.ZenithProvider
import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderEpisode
import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.ProviderStatusInfo
import com.pilldev.zenith.provider.model.ProviderSubtitle
import com.pilldev.zenith.provider.model.ProviderTestResult
import com.pilldev.zenith.provider.model.ProviderTorrentSource
import com.pilldev.zenith.provider.model.ProviderVideoSource
import com.pilldev.zenith.provider.model.SkipInterval
import com.pilldev.zenith.provider.model.UserMediaStatus
import kotlin.coroutines.cancellation.CancellationException

/**
 * Platform IPC session. Implementations must complete broker callbacks before returning from [invoke],
 * transfer backup data through bounded blob frames, and terminate the worker when [dispose] returns.
 */
public interface ProviderWorkerClient {
    /** A Ready frame accepted only after protocol and host-supplied manifest validation. */
    public val ready: ProviderWorkerMessage.Ready

    public suspend fun invoke(operation: ProviderWorkerOperation): ProviderWorkerResult

    public suspend fun importBackup(data: ByteArray): ProviderWorkerResult

    public suspend fun dispose()
}

public class ProviderWorkerAdapter(
    private val client: ProviderWorkerClient,
    override val manifest: PluginManifest,
) : ZenithProvider,
    CatalogProvider,
    ImportProvider,
    MediaSourceProvider,
    PosterSourceProvider,
    SkipTimingsProvider,
    SubtitleSourceProvider,
    TorrentSourceProvider,
    TrackerProvider,
    StreamExtractorProvider {
    init {
        require(client.ready.manifest == manifest) {
            "Provider worker returned a manifest different from the installed package"
        }
    }

    override val supportedExtensions: List<String>
        get() = client.ready.importExtensions

    override val instructions: List<ImportInstructionStep>
        get() = client.ready.importInstructions

    override suspend fun initialize(context: ProviderContext) = Unit

    override suspend fun dispose() = client.dispose()

    override suspend fun test(context: ProviderContext): ProviderTestResult =
        when (
            val result = request(
                operation = ProviderWorkerOperation.Test(),
                capability = null,
                decode = { (it as? ProviderWorkerValue.Test)?.let { value -> ProviderResult.Success(value.result) } },
            )
        ) {
            is ProviderResult.Success -> result.data
            is ProviderResult.Failure -> ProviderTestResult.Failure(result.message)
        }

    override suspend fun probeStatus(context: ProviderContext): ProviderResult<ProviderStatusInfo> =
        request(
            operation = ProviderWorkerOperation.ProbeStatus,
            capability = null,
            decode = { (it as? ProviderWorkerValue.Status)?.let { value -> ProviderResult.Success(value.result) } },
        )

    override suspend fun executeAction(
        actionId: String,
        context: ProviderContext,
    ): ProviderResult<String> =
        request(
            operation = ProviderWorkerOperation.ExecuteAction(actionId),
            capability = null,
            decode = { (it as? ProviderWorkerValue.ActionResult)?.let { value -> ProviderResult.Success(value.result) } },
        )

    override suspend fun search(
        query: String,
        page: Int,
        pageSize: Int,
    ): ProviderResult<CatalogSearchResult> =
        request(
            ProviderWorkerOperation.CatalogSearch(query, page, pageSize),
            ProviderCapability.CATALOG,
        ) { (it as? ProviderWorkerValue.CatalogSearch)?.let { value -> ProviderResult.Success(value.result) } }

    override suspend fun getDetails(mediaRef: MediaRef): ProviderResult<CatalogMediaDetails> =
        request(ProviderWorkerOperation.CatalogDetails(mediaRef), ProviderCapability.CATALOG) {
            (it as? ProviderWorkerValue.CatalogDetails)?.let { value -> ProviderResult.Success(value.result) }
        }

    override suspend fun getHomeSections(): ProviderResult<List<CatalogSection>> =
        request(ProviderWorkerOperation.CatalogHomeSections, ProviderCapability.CATALOG) {
            (it as? ProviderWorkerValue.CatalogSections)?.let { value -> ProviderResult.Success(value.result) }
        }

    override suspend fun getRelated(mediaRef: MediaRef): ProviderResult<List<CatalogMediaItem>> =
        request(ProviderWorkerOperation.CatalogRelated(mediaRef), ProviderCapability.CATALOG) {
            (it as? ProviderWorkerValue.CatalogItems)?.let { value -> ProviderResult.Success(value.result) }
        }

    override suspend fun parseBackup(data: ByteArray): ProviderResult<ImportBatch> {
        if (ProviderCapability.IMPORT !in manifest.capabilities) {
            return unsupportedCapability(ProviderCapability.IMPORT)
        }
        return when (val result = importWorkerBackup(client, data)) {
            is ProviderWorkerResult.Success ->
                (result.value as? ProviderWorkerValue.ImportData)?.let { ProviderResult.Success(it.result) }
                    ?: invalidResponse(ProviderCapability.IMPORT)
            is ProviderWorkerResult.Failed -> workerFailure(result)
        }
    }

    override suspend fun getSources(mediaRef: MediaRef): ProviderResult<List<ProviderVideoSource>> =
        request(ProviderWorkerOperation.MediaSources(mediaRef), ProviderCapability.MEDIA_SOURCE) {
            (it as? ProviderWorkerValue.VideoSources)?.let { value -> ProviderResult.Success(value.result) }
        }

    override suspend fun resolveStream(episode: ProviderEpisode): ProviderResult<List<ProviderMediaStream>> =
        request(ProviderWorkerOperation.ResolveEpisode(episode), ProviderCapability.MEDIA_SOURCE) {
            (it as? ProviderWorkerValue.MediaStreams)?.let { value -> ProviderResult.Success(value.result) }
        }

    override suspend fun getPoster(mediaRef: MediaRef): ProviderResult<String?> =
        request(ProviderWorkerOperation.Poster(mediaRef), ProviderCapability.POSTER_SOURCE) {
            (it as? ProviderWorkerValue.Poster)?.let { value -> ProviderResult.Success(value.url) }
        }

    override suspend fun getSkipTimes(
        mediaRef: MediaRef,
        episodeNumber: Int,
        episodeLength: Double?,
        translationName: String?,
    ): ProviderResult<List<SkipInterval>> =
        request(
            ProviderWorkerOperation.SkipTimes(mediaRef, episodeNumber, episodeLength, translationName),
            ProviderCapability.SKIP_TIMINGS,
        ) { (it as? ProviderWorkerValue.SkipTimes)?.let { value -> ProviderResult.Success(value.result) } }

    override suspend fun searchSubtitles(
        query: String,
        languages: String,
        episodeNumber: Int?,
        seasonNumber: Int?,
    ): ProviderResult<List<ProviderSubtitle>> =
        request(
            ProviderWorkerOperation.SubtitleSearch(query, languages, episodeNumber, seasonNumber),
            ProviderCapability.SUBTITLES,
        ) { (it as? ProviderWorkerValue.Subtitles)?.let { value -> ProviderResult.Success(value.result) } }

    override suspend fun getDownloadLink(fileId: Long): ProviderResult<String?> =
        request(ProviderWorkerOperation.SubtitleDownloadLink(fileId), ProviderCapability.SUBTITLES) {
            (it as? ProviderWorkerValue.DownloadLink)?.let { value -> ProviderResult.Success(value.url) }
        }

    override suspend fun search(mediaRef: MediaRef): ProviderResult<List<ProviderTorrentSource>> =
        request(ProviderWorkerOperation.TorrentSearch(mediaRef), ProviderCapability.TORRENT_SOURCE) {
            (it as? ProviderWorkerValue.TorrentSources)?.let { value -> ProviderResult.Success(value.result) }
        }

    override suspend fun onEpisodeWatched(mediaRef: MediaRef, episode: Int): ProviderResult<Unit> =
        request(ProviderWorkerOperation.EpisodeWatched(mediaRef, episode), ProviderCapability.TRACKER) {
            (it as? ProviderWorkerValue.UnitResult)?.let { ProviderResult.Success(Unit) }
        }

    override suspend fun syncStatus(mediaRef: MediaRef, status: UserMediaStatus): ProviderResult<Unit> =
        request(ProviderWorkerOperation.SyncStatus(mediaRef, status), ProviderCapability.TRACKER) {
            (it as? ProviderWorkerValue.UnitResult)?.let { ProviderResult.Success(Unit) }
        }

    override fun streamExtractors(): List<StreamExtractor> =
        if (ProviderCapability.STREAM_EXTRACTOR in manifest.capabilities) {
            client.ready.streamExtractorNames.map { name -> ProviderWorkerStreamExtractor(client, name) }
        } else {
            emptyList()
        }

    private suspend fun <T> request(
        operation: ProviderWorkerOperation,
        capability: ProviderCapability?,
        decode: (ProviderWorkerValue) -> ProviderResult<T>?,
    ): ProviderResult<T> {
        if (capability != null && capability !in manifest.capabilities) {
            return unsupportedCapability(capability)
        }
        return when (val result = invokeWorker(client, operation)) {
            is ProviderWorkerResult.Success -> decode(result.value) ?: invalidResponse(capability)
            is ProviderWorkerResult.Failed -> workerFailure(result)
        }
    }

    private fun <T> unsupportedCapability(capability: ProviderCapability): ProviderResult<T> =
        ProviderResult.Failure("Provider does not declare the $capability capability")

    private fun <T> invalidResponse(capability: ProviderCapability?): ProviderResult<T> =
        ProviderResult.Failure("Worker returned an invalid response for ${capability ?: "provider"}")

    private fun <T> workerFailure(result: ProviderWorkerResult.Failed): ProviderResult<T> =
        ProviderResult.Failure("Worker ${result.code}: ${result.message}")
}

private class ProviderWorkerStreamExtractor(
    private val client: ProviderWorkerClient,
    override val name: String,
) : StreamExtractor {
    override suspend fun canExtract(url: String): ProviderResult<Boolean> =
        request(ProviderWorkerOperation.StreamCanExtract(name, url)) {
            (it as? ProviderWorkerValue.BooleanResult)?.let { value -> ProviderResult.Success(value.value) }
        }

    override suspend fun extract(
        url: String,
        referer: String?,
        context: ProviderContext?,
    ): ProviderResult<List<ProviderMediaStream>> =
        request(ProviderWorkerOperation.StreamExtract(name, url, referer)) {
            (it as? ProviderWorkerValue.MediaStreams)?.let { value -> ProviderResult.Success(value.result) }
        }

    private suspend fun <T> request(
        operation: ProviderWorkerOperation,
        decode: (ProviderWorkerValue) -> ProviderResult<T>?,
    ): ProviderResult<T> =
        when (val result = invokeWorker(client, operation)) {
            is ProviderWorkerResult.Success -> decode(result.value)
                ?: ProviderResult.Failure("Worker returned an invalid stream-extractor response")
            is ProviderWorkerResult.Failed -> ProviderResult.Failure("Worker ${result.code}: ${result.message}")
        }
}

private suspend fun invokeWorker(
    client: ProviderWorkerClient,
    operation: ProviderWorkerOperation,
): ProviderWorkerResult =
    try {
        client.invoke(operation)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        ProviderWorkerResult.Failed(
            ProviderWorkerFailureCode.WORKER_CRASHED,
            error.message ?: "Provider worker invocation failed",
        )
    }

private suspend fun importWorkerBackup(
    client: ProviderWorkerClient,
    data: ByteArray,
): ProviderWorkerResult =
    try {
        client.importBackup(data)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        ProviderWorkerResult.Failed(
            ProviderWorkerFailureCode.WORKER_CRASHED,
            error.message ?: "Provider worker import failed",
        )
    }
