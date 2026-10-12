package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.CatalogMediaDetails
import com.pilldev.zenith.provider.CatalogMediaItem
import com.pilldev.zenith.provider.CatalogSearchResult
import com.pilldev.zenith.provider.CatalogSection
import com.pilldev.zenith.provider.ImportBatch
import com.pilldev.zenith.provider.ImportInstructionStep
import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderEpisode
import com.pilldev.zenith.provider.model.ProviderMediaStream
import com.pilldev.zenith.provider.model.ProviderStatusInfo
import com.pilldev.zenith.provider.model.ProviderSubtitle
import com.pilldev.zenith.provider.model.ProviderTestResult
import com.pilldev.zenith.provider.model.ProviderTorrentSource
import com.pilldev.zenith.provider.model.ProviderVideoSource
import com.pilldev.zenith.provider.model.SkipInterval
import com.pilldev.zenith.provider.model.UserMediaStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public const val PROVIDER_WORKER_PROTOCOL_VERSION: Int = 1
public const val PROVIDER_WORKER_MAX_FRAME_BYTES: Int = 96 * 1024
public const val PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES: Int = 12 * 1024
public const val PROVIDER_WORKER_MAX_BLOB_BYTES: Long = 32L * 1024 * 1024

@Serializable
public data class ProviderWorkerFrame(
    val protocolVersion: Int,
    val requestId: String,
    val inReplyTo: String? = null,
    val message: ProviderWorkerMessage,
)

@Serializable
public sealed interface ProviderWorkerMessage {
    @Serializable
    public data class Initialize(
        val manifest: PluginManifest,
        val settings: Map<String, String> = emptyMap(),
    ) : ProviderWorkerMessage

    @Serializable
    public data class Ready(
        val manifest: PluginManifest,
        val importExtensions: List<String> = emptyList(),
        val importInstructions: List<ImportInstructionStep> = emptyList(),
        val streamExtractorNames: List<String> = emptyList(),
    ) : ProviderWorkerMessage

    @Serializable
    public data class Invoke(
        val operation: ProviderWorkerOperation,
    ) : ProviderWorkerMessage

    @Serializable
    public data class BrokerRequest(
        val operation: ProviderBrokerOperation,
    ) : ProviderWorkerMessage

    @Serializable
    public data class Complete(
        val result: ProviderWorkerResult,
    ) : ProviderWorkerMessage

    @Serializable
    public data class BrokerComplete(
        val result: ProviderBrokerResult,
    ) : ProviderWorkerMessage

    @Serializable
    public data class Cancel(
        val targetRequestId: String,
    ) : ProviderWorkerMessage

    @Serializable
    public data object Dispose : ProviderWorkerMessage

    @Serializable
    public data object Disposed : ProviderWorkerMessage

    @Serializable
    public data class BlobChunk(
        val transferId: String,
        val sequence: Int,
        val bytes: ByteArray,
        val isLast: Boolean,
    ) : ProviderWorkerMessage

    @Serializable
    public data class Failure(
        val code: ProviderWorkerFailureCode,
        val message: String,
    ) : ProviderWorkerMessage
}

@Serializable
public enum class ProviderWorkerFailureCode {
    PROTOCOL_MISMATCH,
    INVALID_REQUEST,
    PROVIDER_FAILURE,
    BROKER_FAILURE,
    RESOURCE_LIMIT,
    CANCELLED,
    WORKER_CRASHED,
}

@Serializable
public sealed interface ProviderWorkerResult {
    @Serializable
    public data class Success(val value: ProviderWorkerValue) : ProviderWorkerResult

    @Serializable
    public data class Failed(
        val code: ProviderWorkerFailureCode,
        val message: String,
    ) : ProviderWorkerResult
}

@Serializable
public sealed interface ProviderWorkerValue {
    @Serializable
    public data class Test(val result: ProviderTestResult) : ProviderWorkerValue

    @Serializable
    public data class Status(val result: ProviderStatusInfo) : ProviderWorkerValue

    @Serializable
    public data class BooleanResult(val value: Boolean) : ProviderWorkerValue

    @Serializable
    public data class CatalogSearch(val result: CatalogSearchResult) : ProviderWorkerValue

    @Serializable
    public data class CatalogDetails(val result: CatalogMediaDetails) : ProviderWorkerValue

    @Serializable
    public data class CatalogSections(val result: List<CatalogSection>) : ProviderWorkerValue

    @Serializable
    public data class CatalogItems(val result: List<CatalogMediaItem>) : ProviderWorkerValue

    @Serializable
    public data class VideoSources(val result: List<ProviderVideoSource>) : ProviderWorkerValue

    @Serializable
    public data class MediaStreams(val result: List<ProviderMediaStream>) : ProviderWorkerValue

    @Serializable
    public data class TorrentSources(val result: List<ProviderTorrentSource>) : ProviderWorkerValue

    @Serializable
    public data class Poster(val url: String?) : ProviderWorkerValue

    @Serializable
    public data class SkipTimes(val result: List<SkipInterval>) : ProviderWorkerValue

    @Serializable
    public data class Subtitles(val result: List<ProviderSubtitle>) : ProviderWorkerValue

    @Serializable
    public data class DownloadLink(val url: String?) : ProviderWorkerValue

    @Serializable
    public data class ImportData(val result: ImportBatch) : ProviderWorkerValue

    @Serializable
    public data class ActionResult(val result: String) : ProviderWorkerValue

    @Serializable
    public data object UnitResult : ProviderWorkerValue
}

@Serializable
public sealed interface ProviderWorkerOperation {
    @Serializable
    public data class Test(val settings: Map<String, String> = emptyMap()) : ProviderWorkerOperation

    @Serializable
    public data object ProbeStatus : ProviderWorkerOperation

    @Serializable
    public data class CatalogSearch(
        val query: String,
        val page: Int,
        val pageSize: Int,
    ) : ProviderWorkerOperation

    @Serializable
    public data class CatalogDetails(val mediaRef: MediaRef) : ProviderWorkerOperation

    @Serializable
    public data object CatalogHomeSections : ProviderWorkerOperation

    @Serializable
    public data class CatalogRelated(val mediaRef: MediaRef) : ProviderWorkerOperation

    @Serializable
    public data class MediaSources(val mediaRef: MediaRef) : ProviderWorkerOperation

    @Serializable
    public data class ResolveEpisode(val episode: ProviderEpisode) : ProviderWorkerOperation

    @Serializable
    public data class TorrentSearch(val mediaRef: MediaRef) : ProviderWorkerOperation

    @Serializable
    public data class Poster(val mediaRef: MediaRef) : ProviderWorkerOperation

    @Serializable
    public data class SkipTimes(
        val mediaRef: MediaRef,
        val episodeNumber: Int,
        val episodeLength: Double?,
        val translationName: String?,
    ) : ProviderWorkerOperation

    @Serializable
    public data class SubtitleSearch(
        val query: String,
        val languages: String,
        val episodeNumber: Int?,
        val seasonNumber: Int?,
    ) : ProviderWorkerOperation

    @Serializable
    public data class SubtitleDownloadLink(val fileId: Long) : ProviderWorkerOperation

    @Serializable
    public data class EpisodeWatched(
        val mediaRef: MediaRef,
        val episode: Int,
    ) : ProviderWorkerOperation

    @Serializable
    public data class SyncStatus(
        val mediaRef: MediaRef,
        val status: UserMediaStatus,
    ) : ProviderWorkerOperation

    @Serializable
    public data class ImportBackup(val input: ProviderWorkerBlob) : ProviderWorkerOperation

    @Serializable
    public data class ExecuteAction(val actionId: String) : ProviderWorkerOperation

    @Serializable
    public data class StreamCanExtract(
        val extractorName: String,
        val url: String,
    ) : ProviderWorkerOperation

    @Serializable
    public data class StreamExtract(
        val extractorName: String,
        val url: String,
        val referer: String?,
    ) : ProviderWorkerOperation
}

@Serializable
public sealed interface ProviderBrokerOperation {
    @Serializable
    public data class HttpRequest(
        val url: String,
        val method: String,
        val headers: Map<String, List<String>> = emptyMap(),
        val body: ProviderWorkerBlob? = null,
    ) : ProviderBrokerOperation

    @Serializable
    public data class GetSetting(val key: String) : ProviderBrokerOperation

    @Serializable
    public data class SetSetting(
        val key: String,
        val value: String?,
    ) : ProviderBrokerOperation

    @Serializable
    public data object GetEffectiveMirror : ProviderBrokerOperation

    @Serializable
    public data class ResolveStream(
        val url: String,
        val referer: String?,
    ) : ProviderBrokerOperation
}

@Serializable
public sealed interface ProviderBrokerResult {
    @Serializable
    public data class HttpResponse(
        val statusCode: Int,
        val headers: Map<String, List<String>> = emptyMap(),
        val body: ProviderWorkerBlob? = null,
    ) : ProviderBrokerResult

    @Serializable
    public data class Setting(val value: String?) : ProviderBrokerResult

    @Serializable
    public data class Mirror(val url: String) : ProviderBrokerResult

    @Serializable
    public data class Streams(val values: List<ProviderMediaStream>) : ProviderBrokerResult

    @Serializable
    public data object UnitResult : ProviderBrokerResult

    @Serializable
    public data class Failed(
        val code: ProviderWorkerFailureCode,
        val message: String,
    ) : ProviderBrokerResult
}

@Serializable
public data class ProviderWorkerBlob(
    val transferId: String,
    val sizeBytes: Long,
    val sha256: String,
)

public object ProviderWorkerProtocol {
    private val json = Json {
        classDiscriminator = "kind"
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    public fun encode(frame: ProviderWorkerFrame): String {
        validate(frame)
        val encoded = json.encodeToString(ProviderWorkerFrame.serializer(), frame)
        require(encoded.encodeToByteArray().size <= PROVIDER_WORKER_MAX_FRAME_BYTES) {
            "Provider worker frame exceeds the size limit"
        }
        return encoded
    }

    public fun decode(frame: String): ProviderWorkerFrame {
        require(frame.encodeToByteArray().size <= PROVIDER_WORKER_MAX_FRAME_BYTES) {
            "Provider worker frame exceeds the size limit"
        }
        val decoded = try {
            json.decodeFromString(ProviderWorkerFrame.serializer(), frame)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Malformed provider worker frame", e)
        }
        validate(decoded)
        return decoded
    }

    private fun validate(frame: ProviderWorkerFrame) {
        require(frame.protocolVersion == PROVIDER_WORKER_PROTOCOL_VERSION) {
            "Unsupported provider worker protocol version: ${frame.protocolVersion}"
        }
        require(frame.requestId.matches(REQUEST_ID_PATTERN)) {
            "Invalid provider worker request ID"
        }
        require(frame.inReplyTo == null || frame.inReplyTo.matches(REQUEST_ID_PATTERN)) {
            "Invalid provider worker correlation ID"
        }
        if (frame.message is ProviderWorkerMessage.BlobChunk) {
            require(frame.message.sequence >= 0) { "Invalid provider worker blob sequence" }
            require(frame.message.bytes.size <= PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES) {
                "Provider worker blob chunk exceeds the size limit"
            }
            require(frame.message.transferId.matches(REQUEST_ID_PATTERN)) {
                "Invalid provider worker blob transfer ID"
            }
        }
        val referencedBlob = when (val message = frame.message) {
            is ProviderWorkerMessage.Invoke -> (message.operation as? ProviderWorkerOperation.ImportBackup)?.input
            is ProviderWorkerMessage.BrokerRequest ->
                (message.operation as? ProviderBrokerOperation.HttpRequest)?.body
            is ProviderWorkerMessage.BrokerComplete ->
                (message.result as? ProviderBrokerResult.HttpResponse)?.body
            else -> null
        }
        if (referencedBlob != null) validate(referencedBlob)
    }

    private fun validate(blob: ProviderWorkerBlob) {
        require(blob.transferId.matches(REQUEST_ID_PATTERN)) {
            "Invalid provider worker blob transfer ID"
        }
        require(blob.sizeBytes in 0..PROVIDER_WORKER_MAX_BLOB_BYTES) {
            "Provider worker blob exceeds the size limit"
        }
        require(blob.sha256.matches(SHA256_PATTERN)) {
            "Invalid provider worker blob checksum"
        }
    }

    private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9_-]{1,64}")
    private val SHA256_PATTERN = Regex("[A-Fa-f0-9]{64}")
}
