package com.pilldev.zenith.provider.matcher

import io.ktor.http.encodeURLParameter
import kotlinx.serialization.Serializable

@Serializable
public object TorrentTitleParser {
    @Serializable
    public enum class TagType {
        RESOLUTION,
        QUALITY,
        AUDIO,
        GROUP,
        CODEC,
        OTHER,
    }

    @Serializable
    public data class Tag(
        val name: String,
        val type: TagType,
    )

    public data class ParsedTitle(
        val cleanTitle: String,
        val tags: List<Tag>,
        val season: Int? = null,
        val episode: Int? = null,
        val episodeEnd: Int? = null,
        val isBatch: Boolean = false,
        val releaseGroup: String? = null,
        val resolution: String? = null,
    ) {
        public val normalizedQuality: String
            get() = resolution?.lowercase() ?: "1080p"
    }

    public fun qualityScore(quality: String): Int {
        val clean = quality.lowercase().trim()
        return when {
            clean.contains("2160") || clean.contains("4k") -> 2160
            clean.contains("1440") || clean.contains("2k") -> 1440
            clean.contains("1080") -> 1080
            clean.contains("900") -> 900
            clean.contains("720") -> 720
            clean.contains("576") -> 576
            clean.contains("480") -> 480
            clean.contains("360") -> 360
            else -> 0
        }
    }

    public val DEFAULT_ANNOUNCE_TRACKERS: List<String> =
        listOf(
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://open.stealth.si:80/announce",
            "udp://tracker.torrent.eu.org:451/announce",
            "udp://explodie.org:6969/announce",
        )

    public fun buildMagnetUri(
        infoHash: String,
        displayName: String? = null,
        trackers: List<String> = DEFAULT_ANNOUNCE_TRACKERS,
    ): String {
        val dnParam = if (!displayName.isNullOrBlank()) "&dn=${displayName.encodeURLParameter()}" else ""
        val trParams = trackers.joinToString("") { "&tr=${it.encodeURLParameter()}" }
        return "magnet:?xt=urn:btih:$infoHash$dnParam$trParams"
    }

    private val BRACKET_REGEX = Regex("""[\[(]([^\])]+)[\])]""")
    private val WHITESPACE_REGEX = Regex("""\s+""")
    private val RESOLUTION_REGEX = Regex("""\b(480p|576p|720p|900p|1080p|1440p|2160p|4k)\b""", RegexOption.IGNORE_CASE)
    private val QUALITY_REGEX = Regex("""\b(BDRip|WEB-DL|Blu-Ray|BluRay|BDMV|BDREMUX|TV|HDRip|DVDRip|WEBRip)\b""", RegexOption.IGNORE_CASE)
    private val AUDIO_REGEX = Regex("""\b(Dual Audio|Dub|Multi-Audio|VO|SUB|FLAC|AAC|AC3|DTS|PCM|OPUS)\b""", RegexOption.IGNORE_CASE)
    private val CODEC_REGEX = Regex("""\b(x264|x265|HEVC|AVC|H\.264|H\.265|H264|H265|10bit|8bit|AV1)\b""", RegexOption.IGNORE_CASE)
    private val BATCH_MARKERS_REGEX = Regex("""(?i)\b(BATCH|COMPLETE|VOL\.?\s*\d+\s*[-~]\s*\d+|COLLECTION|ALL\s*EPISODES)\b""")
    private val EPISODE_RANGE_REGEX = Regex("""(?i)\b(?:EP|E)?\s*(\d{1,3})\s*[-~]\s*(\d{1,3})\b""")

    private val SEASON_PATTERNS = listOf(
        Regex("""(?i)\bs(\d{1,2})\s*[-_. ]?e\d{1,3}\b"""),
        Regex("""(?i)\b(\d{1,2})x\d{1,3}\b"""),
        Regex("""(?i)\b(\d{1,2})(?:st|nd|rd|th)?\s+season\b"""),
        Regex("""(?i)\bseason\s*[-_. ]?(\d{1,2})\b"""),
        Regex("""(?i)第\s*(\d{1,2})\s*季"""),
        Regex("""(?i)\bs(\d{1,2})(?=\b|[^0-9])"""),
    )

    private val EPISODE_PATTERNS = listOf(
        Regex("""(?i)\bs\d{1,2}\s*[-_. ]?e(\d{1,3})(?:v\d+)?\b"""),
        Regex("""(?i)\b(?:EP|E|EPISODE)\s*[-_. ]?0*(\d{1,3})(?:v\d+)?\b"""),
        Regex("""(?i)第\s*0*(\d{1,3})\s*[話话]"""),
        Regex("""(?i)\[(\d{1,3})](?=[^0-9]|$)"""),
        Regex("""(?i)-\s*0*(\d{1,3})(?:v\d+)?\b"""),
        Regex("""(?i)(?:^|[\s\[(._-])0*(\d{1,3})(?:v\d+)?(?=$|[\])\s._-]|\.m(?:kv|p4)\b)"""),
    )

    private val EXTENSIONS = listOf(".mkv", ".mp4", ".avi", ".mov", ".torrent")

    public fun parse(title: String): ParsedTitle {
        val rawTags = mutableListOf<String>()

        // Extract content within [] and ()
        val matches = BRACKET_REGEX.findAll(title).toList()
        matches.forEach { match ->
            val tag = match.groupValues[1].trim()
            if (tag.isNotEmpty()) {
                rawTags.add(tag)
            }
        }

        // Clean title: remove the matched brackets and their contents
        var clean = title
        matches.forEach { match ->
            clean = clean.replace(match.value, "")
        }

        // Detect resolution
        val resolutionMatch = RESOLUTION_REGEX.find(title)
        val resolution = resolutionMatch?.value?.uppercase()

        // Detect batch
        val isBatch = BATCH_MARKERS_REGEX.containsMatchIn(title)

        // Detect season
        val season = SEASON_PATTERNS.firstNotNullOfOrNull { pattern ->
            pattern
                .find(title)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
        }

        // Detect episode or episode range
        var episode: Int? = null
        var episodeEnd: Int? = null

        val rangeMatch = EPISODE_RANGE_REGEX.find(title)
        if (rangeMatch != null) {
            episode = rangeMatch.groupValues.getOrNull(1)?.toIntOrNull()
            episodeEnd = rangeMatch.groupValues.getOrNull(2)?.toIntOrNull()
        } else {
            episode = EPISODE_PATTERNS.firstNotNullOfOrNull { pattern ->
                pattern
                    .find(title)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
            }
        }

        // Release group: usually the first bracket or well-known pattern
        val firstBracketTag = matches
            .firstOrNull { it.value.startsWith("[") }
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        val releaseGroup = firstBracketTag?.takeIf { it.length < 30 && !it.contains(RESOLUTION_REGEX) }

        EXTENSIONS.forEach { ext ->
            if (clean.lowercase().endsWith(ext)) {
                clean = clean.substring(0, clean.length - ext.length).trim()
            }
        }

        // Remove extra dots, underscores and spaces for cleanTitle
        clean = clean
            .replace(".", " ")
            .replace("_", " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()

        if (clean.isBlank()) {
            clean = title.substringBeforeLast(".")
        }

        // Categorize tags
        val structuredTags = rawTags.distinct().map { name ->
            val type = when {
                name.contains(RESOLUTION_REGEX) -> TagType.RESOLUTION
                name.contains(QUALITY_REGEX) -> TagType.QUALITY
                name.contains(AUDIO_REGEX) -> TagType.AUDIO
                name.contains(CODEC_REGEX) -> TagType.CODEC
                name == releaseGroup -> TagType.GROUP
                rawTags.indexOf(name) == 0 && name.length < 20 -> TagType.GROUP
                name.equals("SubsPlease", ignoreCase = true) || name.equals("Erai-raws", ignoreCase = true) -> TagType.GROUP
                else -> TagType.OTHER
            }
            Tag(name, type)
        }

        return ParsedTitle(
            cleanTitle = clean,
            tags = structuredTags,
            season = season,
            episode = episode,
            episodeEnd = episodeEnd,
            isBatch = isBatch,
            releaseGroup = releaseGroup,
            resolution = resolution,
        )
    }

    private val BYTE_SIZE_REGEX =
        Regex("""(?i)^\s*(\d+(?:[.,]\d+)?)\s*([KMGT]?I?B?)\s*$""")

    public fun parseByteSize(sizeString: String): Long {
        if (sizeString.isBlank()) return 0L
        val clean = sizeString.replace("\u00A0", " ").trim()
        val match = BYTE_SIZE_REGEX.find(clean) ?: return 0L
        val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return 0L
        val unit = match.groupValues[2].uppercase()

        return when {
            unit.startsWith("T") -> (value * 1024.0 * 1024.0 * 1024.0 * 1024.0).toLong()
            unit.startsWith("G") -> (value * 1024.0 * 1024.0 * 1024.0).toLong()
            unit.startsWith("M") -> (value * 1024.0 * 1024.0).toLong()
            unit.startsWith("K") -> (value * 1024.0).toLong()
            else -> value.toLong()
        }
    }
}
