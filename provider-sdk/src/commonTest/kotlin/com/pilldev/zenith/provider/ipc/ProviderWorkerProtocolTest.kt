package com.pilldev.zenith.provider.ipc

import com.pilldev.zenith.provider.CatalogMediaItem
import com.pilldev.zenith.provider.CatalogSearchResult
import com.pilldev.zenith.provider.model.MediaId
import com.pilldev.zenith.provider.model.MediaRef
import com.pilldev.zenith.provider.model.PluginManifest
import com.pilldev.zenith.provider.model.ProviderCapability
import com.pilldev.zenith.provider.model.ProviderId
import com.pilldev.zenith.provider.model.ProviderResult
import com.pilldev.zenith.provider.model.ProviderStatusInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProviderWorkerProtocolTest {
    private val wireJson = Json {
        classDiscriminator = "kind"
        encodeDefaults = true
    }

    @Test
    fun providerOperationFrameRoundTripsWithCorrelationId() {
        val frame = ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = "host_call_1",
            message = ProviderWorkerMessage.Invoke(
                ProviderWorkerOperation.CatalogDetails(
                    MediaRef(MediaId("media-1"), title = "Example"),
                ),
            ),
        )

        assertEquals(frame, ProviderWorkerProtocol.decode(ProviderWorkerProtocol.encode(frame)))
    }

    @Test
    fun statusAndStreamExtractorResultsRoundTrip() {
        val frames = listOf(
            ProviderWorkerFrame(
                protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                requestId = "status_result_1",
                message = ProviderWorkerMessage.Complete(
                    ProviderWorkerResult.Success(
                        ProviderWorkerValue.Status(
                            ProviderStatusInfo(
                                status = "OK",
                                details = mapOf("account" to "connected"),
                                latencyMs = 12,
                            ),
                        ),
                    ),
                ),
            ),
            ProviderWorkerFrame(
                protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                requestId = "extractor_result_1",
                message = ProviderWorkerMessage.Complete(
                    ProviderWorkerResult.Success(ProviderWorkerValue.BooleanResult(value = true)),
                ),
            ),
            ProviderWorkerFrame(
                protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
                requestId = "probe_status_1",
                message = ProviderWorkerMessage.Invoke(ProviderWorkerOperation.ProbeStatus),
            ),
        )

        frames.forEach { frame ->
            assertEquals(frame, ProviderWorkerProtocol.decode(ProviderWorkerProtocol.encode(frame)))
        }
    }

    @Test
    fun remoteAdapterMapsResultsAndBlocksUndeclaredCapabilities() = kotlinx.coroutines.test.runTest {
        val mediaRef = MediaRef(MediaId("remote-media"))
        val client = FakeWorkerClient(
            ready = ProviderWorkerMessage.Ready(
                manifest = PluginManifest(
                    id = ProviderId("remote"),
                    name = "Remote",
                    version = "1",
                    capabilities = setOf(ProviderCapability.CATALOG, ProviderCapability.STREAM_EXTRACTOR),
                ),
                streamExtractorNames = listOf("remote-extractor"),
            ),
            response = { operation ->
                when (operation) {
                    is ProviderWorkerOperation.CatalogSearch ->
                        ProviderWorkerResult.Success(
                            ProviderWorkerValue.CatalogSearch(
                                CatalogSearchResult(items = listOf(CatalogMediaItem(mediaRef, "Example"))),
                            ),
                        )
                    is ProviderWorkerOperation.StreamCanExtract ->
                        ProviderWorkerResult.Success(ProviderWorkerValue.BooleanResult(true))
                    else -> ProviderWorkerResult.Failed(
                        ProviderWorkerFailureCode.INVALID_REQUEST,
                        "Unexpected operation",
                    )
                }
            },
        )
        val adapter = ProviderWorkerAdapter(client, client.ready.manifest)

        val searchResult = adapter.search("example")
        val posterResult = adapter.getPoster(mediaRef)
        val streamMatch = adapter.streamExtractors().single().canExtract("https://media.example/watch")

        assertEquals("Example", (searchResult as ProviderResult.Success).data.items.single().title)
        assertEquals(
            "Provider does not declare the POSTER_SOURCE capability",
            (posterResult as ProviderResult.Failure).message,
        )
        assertEquals(true, (streamMatch as ProviderResult.Success).data)
        assertEquals(2, client.operations.size)
        assertFailsWith<IllegalArgumentException> {
            ProviderWorkerAdapter(
                client,
                PluginManifest(id = ProviderId("different"), name = "Different", version = "1"),
            )
        }
    }

    @Test
    fun brokerHttpRequestAndBlobMetadataRoundTripWithoutHostPaths() {
        val frame = ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = "broker_call_1",
            inReplyTo = "host_call_1",
            message = ProviderWorkerMessage.BrokerRequest(
                ProviderBrokerOperation.HttpRequest(
                    url = "https://provider.example/api",
                    method = "POST",
                    headers = mapOf("Content-Type" to listOf("application/json")),
                    body = ProviderWorkerBlob(
                        transferId = "transfer_1",
                        sizeBytes = 12,
                        sha256 = "a".repeat(64),
                    ),
                ),
            ),
        )

        assertEquals(frame, ProviderWorkerProtocol.decode(ProviderWorkerProtocol.encode(frame)))
    }

    @Test
    fun blobChunksAreBoundedAndRoundTripExactly() {
        val bytes = ByteArray(PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES) { (it % 127).toByte() }
        val frame = ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = "chunk_1",
            message = ProviderWorkerMessage.BlobChunk("transfer_1", 0, bytes, isLast = true),
        )

        val decoded = ProviderWorkerProtocol.decode(ProviderWorkerProtocol.encode(frame))

        assertEquals(frame.requestId, decoded.requestId)
        assertContentEquals(bytes, (decoded.message as ProviderWorkerMessage.BlobChunk).bytes)
    }

    @Test
    fun decoderRejectsUnsupportedProtocolVersion() {
        val unsupported = ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION + 1,
            requestId = "version_1",
            message = ProviderWorkerMessage.Dispose,
        )
        val encoded = wireJson.encodeToString(ProviderWorkerFrame.serializer(), unsupported)

        assertFailsWith<IllegalArgumentException> { ProviderWorkerProtocol.decode(encoded) }
    }

    @Test
    fun decoderRejectsMalformedCorrelationIdsAndOversizedChunks() {
        val malformedId = ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = "../escape",
            message = ProviderWorkerMessage.Dispose,
        )
        val oversizedChunk = ProviderWorkerFrame(
            protocolVersion = PROVIDER_WORKER_PROTOCOL_VERSION,
            requestId = "chunk_2",
            message = ProviderWorkerMessage.BlobChunk(
                transferId = "transfer_2",
                sequence = 0,
                bytes = ByteArray(PROVIDER_WORKER_MAX_BLOB_CHUNK_BYTES + 1),
                isLast = true,
            ),
        )

        assertFailsWith<IllegalArgumentException> { ProviderWorkerProtocol.encode(malformedId) }
        assertFailsWith<IllegalArgumentException> { ProviderWorkerProtocol.encode(oversizedChunk) }
    }

    private class FakeWorkerClient(
        override val ready: ProviderWorkerMessage.Ready,
        private val response: (ProviderWorkerOperation) -> ProviderWorkerResult,
    ) : ProviderWorkerClient {
        val operations = mutableListOf<ProviderWorkerOperation>()

        override suspend fun invoke(operation: ProviderWorkerOperation): ProviderWorkerResult {
            operations += operation
            return response(operation)
        }

        override suspend fun importBackup(data: ByteArray): ProviderWorkerResult =
            ProviderWorkerResult.Failed(ProviderWorkerFailureCode.INVALID_REQUEST, "Unexpected import")

        override suspend fun dispose() = Unit
    }
}
