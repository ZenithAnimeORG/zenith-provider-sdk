package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.CatalogProvider
import com.pilldev.zenith.provider.ImportProvider
import com.pilldev.zenith.provider.MediaSourceProvider
import com.pilldev.zenith.provider.PosterSourceProvider
import com.pilldev.zenith.provider.SkipTimingsProvider
import com.pilldev.zenith.provider.StreamExtractorProvider
import com.pilldev.zenith.provider.SubtitleSourceProvider
import com.pilldev.zenith.provider.TorrentSourceProvider
import com.pilldev.zenith.provider.TrackerProvider
import com.pilldev.zenith.provider.ZenithProvider
import com.pilldev.zenith.provider.ZenithProviderFactory
import com.pilldev.zenith.provider.context.ProviderContext
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderResult
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Dispatches SDK operations inside the worker process. Platform launchers are responsible for
 * loading [factory] only after native isolation is established and for providing a broker-backed
 * [ProviderContext].
 */
public class ProviderWorkerDispatcher(
    private val factory: ZenithProviderFactory,
    private val context: ProviderContext,
    private val readBlob: suspend (ProviderWorkerBlob) -> ByteArray = {
        throw IllegalStateException("Blob transport is unavailable")
    },
) {
    private val logger = Logger.withTag("ProviderWorkerDispatcher")
    private val mutex = Mutex()
    private var provider: ZenithProvider? = null
    private var installedManifest: PluginManifest? = null
    private var responseSequence = 0L

    public suspend fun handle(frame: ProviderWorkerFrame): ProviderWorkerFrame {
        if (frame.protocolVersion != PROVIDER_WORKER_PROTOCOL_VERSION) {
            return protocolFailure(ProviderWorkerFailureCode.PROTOCOL_MISMATCH, "Unsupported protocol version")
        }
        try {
            ProviderWorkerProtocol.encode(frame)
        } catch (error: IllegalArgumentException) {
            return protocolFailure(ProviderWorkerFailureCode.INVALID_REQUEST, "Invalid worker request frame")
        }
        return mutex.withLock {
            when (val message = frame.message) {
                is ProviderWorkerMessage.Initialize -> initialize(frame, message)
                is ProviderWorkerMessage.Invoke -> invoke(frame, message.operation)
                ProviderWorkerMessage.Dispose -> dispose(frame)
                else -> failureFrame(frame, ProviderWorkerFailureCode.INVALID_REQUEST, "Unexpected worker request")
            }
        }
    }

    private suspend fun initialize(
        frame: ProviderWorkerFrame,
        message: ProviderWorkerMessage.Initialize,
    ): ProviderWorkerFrame {
        if (provider != null) {
            return failureFrame(frame, ProviderWorkerFailureCode.INVALID_REQUEST, "Worker is already initialized")
        }
        var created: ZenithProvider? = null
        return try {
            message.settings.forEach { (key, value) -> context.setSetting(key, value) }
            created = factory.create(message.manifest)
            require(created.manifest == message.manifest) { "Provider factory returned a different manifest" }
            created.initialize(context)
            provider = created
            installedManifest = message.manifest
            ProviderWorkerFrame(
                protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                requestId = nextRequestId(),
                inReplyTo = frame.requestId,
                message = ProviderWorkerMessage.Ready(
                    manifest = created.manifest,
                    importExtensions = (created as? ImportProvider)?.supportedExtensions.orEmpty(),
                    importInstructions = (created as? ImportProvider)?.instructions.orEmpty(),
                    streamExtractorNames = (created as? StreamExtractorProvider)
                        ?.streamExtractors()
                        ?.map { it.name }
                        .orEmpty(),
                ),
            )
        } catch (cancellation: CancellationException) {
            disposeAfterFailedInitialization(created, cancellation)
            throw cancellation
        } catch (error: Exception) {
            logger.e(error) { "Provider initialization failed for ${message.manifest.id.value}" }
            disposeAfterFailedInitialization(created, error)
            failureFrame(
                frame,
                ProviderWorkerFailureCode.PROVIDER_FAILURE,
                error.message ?: "Provider initialization failed",
            )
        }
    }

    private suspend fun disposeAfterFailedInitialization(
        provider: ZenithProvider?,
        cause: Throwable,
    ) {
        if (provider == null) return
        try {
            provider.dispose()
        } catch (cleanupError: CancellationException) {
            throw cleanupError
        } catch (cleanupError: Exception) {
            logger.e(cleanupError) { "Failed to dispose provider ${provider.manifest.id.value} after initialization failure" }
            cause.addSuppressed(cleanupError)
        }
    }

    private suspend fun invoke(
        frame: ProviderWorkerFrame,
        operation: ProviderWorkerOperation,
    ): ProviderWorkerFrame {
        val activeProvider = provider
            ?: return failureFrame(frame, ProviderWorkerFailureCode.INVALID_REQUEST, "Worker is not initialized")
        return try {
            val capability = capabilityFor(operation)
            if (capability != null && capability !in (installedManifest?.capabilities.orEmpty())) {
                return complete(
                    frame,
                    ProviderWorkerResult.Failed(
                        ProviderWorkerFailureCode.INVALID_REQUEST,
                        "Provider does not declare the $capability capability",
                    ),
                )
            }
            complete(frame, dispatch(activeProvider, operation))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            logger.e(error) { "Provider operation ${operation::class.simpleName} failed for ${installedManifest?.id?.value}" }
            complete(
                frame,
                ProviderWorkerResult.Failed(
                    ProviderWorkerFailureCode.PROVIDER_FAILURE,
                    error.message ?: "Provider operation failed",
                ),
            )
        }
    }

    private suspend fun dispatch(
        activeProvider: ZenithProvider,
        operation: ProviderWorkerOperation,
    ): ProviderWorkerResult =
        when (operation) {
            is ProviderWorkerOperation.Test ->
                success(ProviderWorkerValue.Test(activeProvider.test(context)))
            ProviderWorkerOperation.ProbeStatus ->
                mapResult(activeProvider.probeStatus(context)) { ProviderWorkerValue.Status(it) }
            is ProviderWorkerOperation.ExecuteAction ->
                mapResult(activeProvider.executeAction(operation.actionId, context)) { ProviderWorkerValue.ActionResult(it) }
            is ProviderWorkerOperation.CatalogSearch -> withCapability<CatalogProvider>(activeProvider) {
                mapResult(search(operation.query, operation.page, operation.pageSize)) { ProviderWorkerValue.CatalogSearch(it) }
            }
            is ProviderWorkerOperation.CatalogDetails -> withCapability<CatalogProvider>(activeProvider) {
                mapResult(getDetails(operation.mediaRef)) { ProviderWorkerValue.CatalogDetails(it) }
            }
            ProviderWorkerOperation.CatalogHomeSections -> withCapability<CatalogProvider>(activeProvider) {
                mapResult(getHomeSections()) { ProviderWorkerValue.CatalogSections(it) }
            }
            is ProviderWorkerOperation.CatalogRelated -> withCapability<CatalogProvider>(activeProvider) {
                mapResult(getRelated(operation.mediaRef)) { ProviderWorkerValue.CatalogItems(it) }
            }
            is ProviderWorkerOperation.MediaSources -> withCapability<MediaSourceProvider>(activeProvider) {
                mapResult(getSources(operation.mediaRef)) { ProviderWorkerValue.VideoSources(it) }
            }
            is ProviderWorkerOperation.ResolveEpisode -> withCapability<MediaSourceProvider>(activeProvider) {
                mapResult(resolveStream(operation.episode)) { ProviderWorkerValue.MediaStreams(it) }
            }
            is ProviderWorkerOperation.TorrentSearch -> withCapability<TorrentSourceProvider>(activeProvider) {
                mapResult(search(operation.mediaRef)) { ProviderWorkerValue.TorrentSources(it) }
            }
            is ProviderWorkerOperation.Poster -> withCapability<PosterSourceProvider>(activeProvider) {
                mapResult(getPoster(operation.mediaRef)) { ProviderWorkerValue.Poster(it) }
            }
            is ProviderWorkerOperation.SkipTimes -> withCapability<SkipTimingsProvider>(activeProvider) {
                mapResult(
                    getSkipTimes(
                        operation.mediaRef,
                        operation.episodeNumber,
                        operation.episodeLength,
                        operation.translationName,
                    ),
                ) { ProviderWorkerValue.SkipTimes(it) }
            }
            is ProviderWorkerOperation.SubtitleSearch -> withCapability<SubtitleSourceProvider>(activeProvider) {
                mapResult(
                    searchSubtitles(operation.query, operation.languages, operation.episodeNumber, operation.seasonNumber),
                ) { ProviderWorkerValue.Subtitles(it) }
            }
            is ProviderWorkerOperation.SubtitleDownloadLink -> withCapability<SubtitleSourceProvider>(activeProvider) {
                mapResult(getDownloadLink(operation.fileId)) { ProviderWorkerValue.DownloadLink(it) }
            }
            is ProviderWorkerOperation.EpisodeWatched -> withCapability<TrackerProvider>(activeProvider) {
                mapResult(onEpisodeWatched(operation.mediaRef, operation.episode)) { ProviderWorkerValue.UnitResult }
            }
            is ProviderWorkerOperation.SyncStatus -> withCapability<TrackerProvider>(activeProvider) {
                mapResult(syncStatus(operation.mediaRef, operation.status)) { ProviderWorkerValue.UnitResult }
            }
            is ProviderWorkerOperation.ImportBackup -> withCapability<ImportProvider>(activeProvider) {
                mapResult(parseBackup(readBlob(operation.input))) { ProviderWorkerValue.ImportData(it) }
            }
            is ProviderWorkerOperation.StreamCanExtract -> withStreamExtractor(activeProvider, operation.extractorName) {
                mapResult(canExtract(operation.url)) { ProviderWorkerValue.BooleanResult(it) }
            }
            is ProviderWorkerOperation.StreamExtract -> withStreamExtractor(activeProvider, operation.extractorName) {
                mapResult(extract(operation.url, operation.referer, context)) { ProviderWorkerValue.MediaStreams(it) }
            }
        }

    private suspend inline fun <reified T> withCapability(
        activeProvider: ZenithProvider,
        crossinline block: suspend T.() -> ProviderWorkerResult,
    ): ProviderWorkerResult {
        val target = activeProvider as? T
            ?: return ProviderWorkerResult.Failed(
                ProviderWorkerFailureCode.INVALID_REQUEST,
                "Provider does not implement the requested SDK contract",
            )
        return target.block()
    }

    private suspend fun withStreamExtractor(
        activeProvider: ZenithProvider,
        extractorName: String,
        block: suspend com.pilldev.zenith.provider.StreamExtractor.() -> ProviderWorkerResult,
    ): ProviderWorkerResult {
        val extractor = (activeProvider as? StreamExtractorProvider)
            ?.streamExtractors()
            ?.firstOrNull { it.name == extractorName }
            ?: return ProviderWorkerResult.Failed(
                ProviderWorkerFailureCode.INVALID_REQUEST,
                "Unknown stream extractor",
            )
        return extractor.block()
    }

    private suspend fun dispose(frame: ProviderWorkerFrame): ProviderWorkerFrame {
        val activeProvider = provider
        provider = null
        installedManifest = null
        return try {
            activeProvider?.dispose()
            ProviderWorkerFrame(
                protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                requestId = nextRequestId(),
                inReplyTo = frame.requestId,
                message = ProviderWorkerMessage.Disposed,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            failureFrame(
                frame,
                ProviderWorkerFailureCode.PROVIDER_FAILURE,
                error.message ?: "Provider disposal failed",
            )
        }
    }

    private fun capabilityFor(operation: ProviderWorkerOperation): ProviderCapability? =
        when (operation) {
            is ProviderWorkerOperation.CatalogSearch,
            is ProviderWorkerOperation.CatalogDetails,
            ProviderWorkerOperation.CatalogHomeSections,
            is ProviderWorkerOperation.CatalogRelated,
            -> ProviderCapability.CATALOG
            is ProviderWorkerOperation.MediaSources,
            is ProviderWorkerOperation.ResolveEpisode,
            -> ProviderCapability.MEDIA_SOURCE
            is ProviderWorkerOperation.TorrentSearch -> ProviderCapability.TORRENT_SOURCE
            is ProviderWorkerOperation.Poster -> ProviderCapability.POSTER_SOURCE
            is ProviderWorkerOperation.SkipTimes -> ProviderCapability.SKIP_TIMINGS
            is ProviderWorkerOperation.SubtitleSearch,
            is ProviderWorkerOperation.SubtitleDownloadLink,
            -> ProviderCapability.SUBTITLES
            is ProviderWorkerOperation.EpisodeWatched,
            is ProviderWorkerOperation.SyncStatus,
            -> ProviderCapability.TRACKER
            is ProviderWorkerOperation.ImportBackup -> ProviderCapability.IMPORT
            is ProviderWorkerOperation.StreamCanExtract,
            is ProviderWorkerOperation.StreamExtract,
            -> ProviderCapability.STREAM_EXTRACTOR
            is ProviderWorkerOperation.Test,
            ProviderWorkerOperation.ProbeStatus,
            is ProviderWorkerOperation.ExecuteAction,
            -> null
        }

    private fun complete(
        frame: ProviderWorkerFrame,
        result: ProviderWorkerResult,
    ): ProviderWorkerFrame =
        ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = nextRequestId(),
            inReplyTo = frame.requestId,
            message = ProviderWorkerMessage.Complete(result),
        )

    private fun failureFrame(
        frame: ProviderWorkerFrame,
        code: ProviderWorkerFailureCode,
        message: String,
    ): ProviderWorkerFrame =
        ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = nextRequestId(),
            inReplyTo = frame.requestId,
            message = ProviderWorkerMessage.Failure(code, message),
        )

    private fun protocolFailure(
        code: ProviderWorkerFailureCode,
        message: String,
    ): ProviderWorkerFrame =
        ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = "protocol_error",
            message = ProviderWorkerMessage.Failure(code, message),
        )

    private fun nextRequestId(): String {
        responseSequence += 1
        return "worker_$responseSequence"
    }

    private fun success(value: ProviderWorkerValue): ProviderWorkerResult =
        ProviderWorkerResult.Success(value)

    private inline fun <T> mapResult(
        result: ProviderResult<T>,
        value: (T) -> ProviderWorkerValue,
    ): ProviderWorkerResult =
        when (result) {
            is ProviderResult.Success -> ProviderWorkerResult.Success(value(result.data))
            is ProviderResult.Failure -> {
                logger.w { "Provider operation returned failure: ${result.message}" }
                ProviderWorkerResult.Failed(ProviderWorkerFailureCode.PROVIDER_FAILURE, result.message)
            }
        }
}
