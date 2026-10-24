/**
 * Core models and types for the Zenith Provider SDK.
 * Matches Zenith application domain and data models defined in com.pilldev.zenith.provider.
 *
 * @module models
 */

/**
 * Declares the capabilities supported by a Zenith provider plugin.
 *
 * A provider declares its capabilities in its `manifest.json` file. Each capability
 * unlocks corresponding methods in {@link ProviderDefinition} that Zenith will invoke
 * when performing search, playback, or synchronization actions.
 *
 * Capabilities:
 * - `"CATALOG"`: Provides media discovery, search, catalog details, and recommendation carousels.
 * - `"MEDIA_SOURCE"`: Provides direct video sources, translations, and playable media streams (HLS/MP4).
 * - `"TORRENT_SOURCE"`: Provides BitTorrent release search, magnet URIs, and torrent file hashes for playback via jlibtorrent.
 * - `"SUBTITLES"`: Provides external subtitle file searches and download links.
 * - `"SKIP_TIMINGS"`: Provides opening (OP) and ending (ED) skip timecodes for video playback.
 * - `"POSTER_SOURCE"`: Provides high-resolution artwork and poster resolution.
 * - `"STREAM_EXTRACTOR"`: Resolves third-party video host embed URLs into direct playback streams.
 * - `"TRACKER"`: Synchronizes user watch history, episode progress, and media statuses.
 * - `"IMPORT"`: Imports external watchlist bookmarks from CSV, XML, or JSON files.
 * - `"UNKNOWN"`: Placeholder for unrecognized or future capabilities.
 */
export type ProviderCapability =
  | "MEDIA_SOURCE"
  | "POSTER_SOURCE"
  | "SKIP_TIMINGS"
  | "SUBTITLES"
  | "TORRENT_SOURCE"
  | "STREAM_EXTRACTOR"
  | "CATALOG"
  | "TRACKER"
  | "IMPORT"
  | "UNKNOWN";

/**
 * Type of audio/text translation provided by a video source.
 *
 * - `"DUB"`: Full voice dubbing / multi-voice voiceover.
 * - `"VO"`: Voice-over commentary or single-voice reading over original audio.
 * - `"SUB"`: Subtitled release with original audio track.
 * - `"UNKNOWN"`: Unspecified translation format.
 */
export type ProviderTranslationType = "DUB" | "VO" | "SUB" | "UNKNOWN";

/**
 * User watchlist status for media tracking.
 *
 * - `"WATCHING"`: Currently in-progress media.
 * - `"PLANNED"`: Planned to watch in the future.
 * - `"COMPLETED"`: Finished watching all episodes.
 * - `"ON_HOLD"`: Temporarily paused.
 * - `"DROPPED"`: Abandoned before completion.
 */
export type UserMediaStatus =
  | "WATCHING"
  | "PLANNED"
  | "COMPLETED"
  | "ON_HOLD"
  | "DROPPED";

/**
 * Canonical reference identifying a media item across Zenith providers and external tracking services.
 *
 * @example
 * ```ts
 * const mediaRef: MediaRef = {
 *   id: "1535",
 *   title: "Death Note",
 *   externalIds: {
 *     mal: "1535",
 *     shikimori: "1535",
 *     anilist: "1535"
 *   },
 *   alternateTitles: ["Тетрадь смерти", "Desu Nōto"]
 * };
 * ```
 */
export interface MediaRef {
  /**
   * Unique identifier scoped to the provider (e.g. slug, numeric ID, or internal path).
   */
  id: string;

  /**
   * Optional map of recognized external catalog IDs (e.g. `mal`, `shikimori`, `anilist`, `imdb`, `tmdb`, `kinopoisk`).
   * Used by Zenith to link titles across different catalog and video providers.
   */
  externalIds?: Record<string, string>;

  /**
   * Primary title of the media entry.
   */
  title?: string;

  /**
   * Alternate titles or localized names to improve cross-provider matching.
   */
  alternateTitles?: string[];
}

/**
 * Summary representation of a media entry displayed in search results, carousels, and catalog lists.
 *
 * @example
 * ```ts
 * const item: CatalogMediaItem = {
 *   mediaRef: { id: "frieren-2023", title: "Frieren: Beyond Journey's End" },
 *   title: "Frieren: Beyond Journey's End",
 *   originalTitle: "Sousou no Frieren",
 *   posterUrl: "https://example.com/posters/frieren.jpg",
 *   score: 9.14,
 *   year: 2023,
 *   totalEpisodes: 28
 * };
 * ```
 */
export interface CatalogMediaItem {
  /**
   * Canonical media reference for navigating to detailed views and video playback.
   */
  mediaRef: MediaRef;

  /**
   * Display title rendered in the UI card.
   */
  title: string;

  /**
   * Original Japanese/native title, if available.
   */
  originalTitle?: string | null;

  /**
   * Direct HTTPS URL pointing to the poster image.
   */
  posterUrl?: string | null;

  /**
   * User rating or score (typically on a 0.0 to 10.0 scale).
   */
  score?: number | null;

  /**
   * Release year of the media.
   */
  year?: number | null;

  /**
   * Total number of episodes planned or released.
   */
  totalEpisodes?: number | null;
}

/**
 * Detailed metadata of a media title displayed on the full details screen.
 *
 * @example
 * ```ts
 * const details: CatalogMediaDetails = {
 *   mediaRef: { id: "frieren", title: "Frieren" },
 *   title: "Frieren: Beyond Journey's End",
 *   originalTitle: "Sousou no Frieren",
 *   description: "An elf mage embarks on a journey to understand humanity after defeating the Demon King.",
 *   posterUrl: "https://example.com/posters/frieren.jpg",
 *   bannerUrl: "https://example.com/banners/frieren.jpg",
 *   score: 9.14,
 *   year: 2023,
 *   episodesCount: 28,
 *   genres: ["Adventure", "Drama", "Fantasy"],
 *   studios: ["Madhouse"],
 *   status: "FINISHED"
 * };
 * ```
 */
export interface CatalogMediaDetails {
  /**
   * Canonical media reference.
   */
  mediaRef: MediaRef;

  /**
   * Primary title in user language.
   */
  title: string;

  /**
   * Original / Romaji title.
   */
  originalTitle?: string | null;

  /**
   * Full synopsis / plot description.
   */
  description?: string | null;

  /**
   * High-resolution poster image URL.
   */
  posterUrl?: string | null;

  /**
   * Wide backdrop / hero banner image URL.
   */
  bannerUrl?: string | null;

  /**
   * Community score / rating (e.g. 8.95).
   */
  score?: number | null;

  /**
   * Release year.
   */
  year?: number | null;

  /**
   * Total number of episodes.
   */
  episodesCount?: number | null;

  /**
   * List of genres associated with this title.
   */
  genres?: string[];

  /**
   * Animation studios or production companies.
   */
  studios?: string[];

  /**
   * Broadcast status: `"AIRING"`, `"FINISHED"`, `"UPCOMING"`, or `"CANCELLED"`.
   */
  status?: string | null;
}

/**
 * Paginated response returned by catalog search queries.
 *
 * @example
 * ```ts
 * const result: CatalogSearchResult = {
 *   items: [mediaItem1, mediaItem2],
 *   hasNextPage: true
 * };
 * ```
 */
export interface CatalogSearchResult {
  /**
   * List of media items found on the current page.
   */
  items: CatalogMediaItem[];

  /**
   * Indicates whether subsequent pages of results exist.
   */
  hasNextPage: boolean;
}

/**
 * Curated section / carousel rendered on the Zenith home screen.
 *
 * @example
 * ```ts
 * const section: CatalogSection = {
 *   title: "Popular This Season",
 *   items: [item1, item2, item3]
 * };
 * ```
 */
export interface CatalogSection {
  /**
   * Localized title of the section header (e.g. "Trending Now").
   */
  title: string;

  /**
   * Media items displayed horizontally in this carousel.
   */
  items: CatalogMediaItem[];
}

/**
 * Individual episode descriptor within a video or audio source.
 *
 * @example
 * ```ts
 * const episode: ProviderEpisode = {
 *   number: 1,
 *   url: "https://example.com/watch/frieren-ep1",
 *   quality: { "1080p": "https://cdn.example.com/1080.m3u8" },
 *   providerId: "my_provider"
 * };
 * ```
 */
export interface ProviderEpisode {
  /**
   * Episode sequence number (1-indexed; may be fractional for specials like 1.5).
   */
  number: number;

  /**
   * Provider-specific URL, stream reference, or payload needed to resolve playback.
   */
  url: string;

  /**
   * Optional mapping of pre-resolved quality strings to stream URLs.
   */
  quality?: Record<string, string> | null;

  /**
   * Optional provider identifier that originated this episode.
   */
  providerId?: string | null;
}

/**
 * Video release or translation source containing a list of episodes.
 *
 * Represents an audio track, dubbing studio, or release group (e.g. "AniLibria", "Crunchyroll", "Subtitles").
 *
 * @example
 * ```ts
 * const source: ProviderVideoSource = {
 *   name: "AniLibria (Dub)",
 *   translationName: "AniLibria",
 *   translationType: "DUB",
 *   episodes: [
 *     { number: 1, url: "https://example.com/ep1" },
 *     { number: 2, url: "https://example.com/ep2" }
 *   ]
 * };
 * ```
 */
export interface ProviderVideoSource {
  /**
   * Display name of the source (e.g. "AniLibria", "Subtitles - Official").
   */
  name: string;

  /**
   * Specific dubbing team or studio name.
   */
  translationName?: string;

  /**
   * Type of translation (`"DUB"`, `"VO"`, or `"SUB"`).
   */
  translationType?: ProviderTranslationType;

  /**
   * List of available episodes in this source.
   */
  episodes: ProviderEpisode[];

  /**
   * Optional dictionary of available quality levels.
   */
  qualities?: Record<string, string> | null;
}

/**
 * Direct video stream playable by Zenith's internal MPV media player engine.
 *
 * @example
 * ```ts
 * const stream: ProviderMediaStream = {
 *   url: "https://cdn.example.com/video/master.m3u8",
 *   quality: "1080p",
 *   isHls: true,
 *   headers: {
 *     "Referer": "https://embed.example.com/",
 *     "User-Agent": "Mozilla/5.0"
 *   }
 * };
 * ```
 */
export interface ProviderMediaStream {
  /**
   * Direct media URL (e.g. HLS `.m3u8`, MP4, or DASH manifest).
   */
  url: string;

  /**
   * Human-readable resolution or quality label (e.g. `"1080p"`, `"720p"`, `"4K"`, `"Auto"`).
   */
  quality?: string | null;

  /**
   * Custom HTTP headers passed directly to MPV (e.g. `Referer`, `Cookie`, `User-Agent`).
   */
  headers?: Record<string, string>;

  /**
   * `true` if the stream is an HLS (HTTP Live Streaming) adaptive playlist.
   */
  isHls?: boolean;
}

/**
 * BitTorrent release metadata for direct peer-to-peer torrent streaming via `jlibtorrent`.
 *
 * @example
 * ```ts
 * const torrent: ProviderTorrentSource = {
 *   title: "[Erai-raws] Sousou no Frieren - 01 [1080p][HEVC]",
 *   infoHash: "4b98c36671c6bc1b8a5b28d7a31b4021be07ef02",
 *   magnetUri: "magnet:?xt=urn:btih:4b98c366...",
 *   size: 1450000000,
 *   seeders: 124,
 *   leechers: 12,
 *   quality: "1080p",
 *   trackerName: "Nyaa"
 * };
 * ```
 */
export interface ProviderTorrentSource {
  /**
   * Release title or filename.
   */
  title: string;

  /**
   * SHA-1 hex-encoded info-hash (40 characters).
   */
  infoHash?: string | null;

  /**
   * Full `magnet:` URI including tracker parameters.
   */
  magnetUri?: string | null;

  /**
   * Total size of the torrent release in bytes.
   */
  size?: number;

  /**
   * Number of active seeders currently sharing the release.
   */
  seeders?: number;

  /**
   * Number of active leechers currently downloading the release.
   */
  leechers?: number;

  /**
   * Video quality string (e.g. `"1080p"`, `"720p"`).
   */
  quality?: string;

  /**
   * Name of the tracker (e.g. `"Nyaa"`, `"RuTracker"`).
   */
  trackerName?: string;

  /**
   * Release group or translation team.
   */
  translationName?: string;

  /**
   * Translation type (`"DUB"`, `"VO"`, or `"SUB"`).
   */
  translationType?: ProviderTranslationType;
}

/**
 * External subtitle track provided by subtitle services (e.g. OpenSubtitles).
 *
 * @example
 * ```ts
 * const sub: ProviderSubtitle = {
 *   language: "en",
 *   releaseName: "Frieren.E01.1080p.CR.WEB-DL.AAC2.0.H.264-VARYG",
 *   directUrl: "https://example.com/subs/frieren-01-en.ass",
 *   isHearingImpaired: false
 * };
 * ```
 */
export interface ProviderSubtitle {
  /**
   * Unique identifier of the subtitle file in the remote service.
   */
  fileId?: number;

  /**
   * Subtitle language code (e.g. `"en"`, `"ru"`, `"es"`).
   */
  language: string;

  /**
   * Video release name the subtitle was timed and tested against.
   */
  releaseName: string;

  /**
   * Whether the subtitles contain closed captions for the hearing impaired (SDH).
   */
  isHearingImpaired?: boolean;

  /**
   * Whether the subtitle was generated by machine / AI translation.
   */
  isAiTranslated?: boolean;

  /**
   * Number of times this subtitle has been downloaded.
   */
  downloadCount?: number;

  /**
   * Direct HTTPS download URL for the subtitle file (`.ass`, `.srt`, `.vtt`).
   */
  directUrl?: string | null;

  /**
   * Name of the subtitle provider (e.g. `"OpenSubtitles"`).
   */
  provider?: string;
}

/**
 * Timecode interval marking intro/outro segments to skip during playback.
 *
 * Used by skip timing integrations (e.g. AniSkip, AnimeSkip).
 *
 * @example
 * ```ts
 * const opInterval: SkipInterval = {
 *   startTime: 92.5,
 *   endTime: 182.0,
 *   skipType: "op",
 *   episodeLength: 1420
 * };
 * ```
 */
export interface SkipInterval {
  /**
   * Timestamp in seconds where the skippable interval begins.
   */
  startTime: number;

  /**
   * Timestamp in seconds where the skippable interval ends.
   */
  endTime: number;

  /**
   * Interval category: `"op"` (opening theme), `"ed"` (ending theme), `"mixed-op"`, or `"recap"`.
   */
  skipType: string;

  /**
   * Total length of the episode in seconds, if known.
   */
  episodeLength?: number | null;
}

/**
 * Diagnostic status and latency probe info for a provider.
 *
 * Rendered in Zenith provider settings and status badges.
 */
export interface ProviderStatusInfo {
  /**
   * Operational status string (e.g. `"OK"`, `"WARNING"`, `"ERROR"`).
   */
  status?: string;

  /**
   * Key-value diagnostic details (e.g. active mirror URL, cache hits).
   */
  details?: Record<string, string>;

  /**
   * Round-trip network ping latency in milliseconds.
   */
  latencyMs?: number | null;

  /**
   * Whether the provider is considered healthy and ready to handle queries.
   */
  isOperational?: boolean;

  /**
   * Human-readable diagnostic status message.
   */
  message?: string;
}

/**
 * Result returned by the provider's self-test diagnostic check (`test()`).
 *
 * @example
 * ```ts
 * const success: ProviderTestResult = {
 *   success: true,
 *   message: "Successfully connected to mirror: https://api.example.com",
 *   latencyMs: 42
 * };
 * ```
 */
export type ProviderTestResult =
  | { success: true; message: string; latencyMs?: number; type?: "Success" }
  | { success: false; error: string; message?: string; latencyMs?: number; type?: "Failure" }
  | { type: "Success"; message: string; latencyMs?: number; success?: boolean }
  | { type: "Failure"; error: string; message?: string; latencyMs?: number; success?: boolean };

/**
 * Step-by-step export instructions rendered in Zenith UI when importing external bookmarks.
 */
export interface ImportInstructionStep {
  /**
   * Step index (1-based).
   */
  stepNumber: number;

  /**
   * Heading of the instruction step.
   */
  title: string;

  /**
   * Detailed explanation of what the user needs to do on the external website.
   */
  description?: string | null;

  /**
   * Additional cautionary note or format advice.
   */
  note?: string | null;

  /**
   * Direct URL to the export page on the external service.
   */
  linkUrl?: string | null;

  /**
   * Clickable anchor text for the link.
   */
  linkText?: string | null;
}

/**
 * Raw media entry extracted during watchlist or bookmark file import.
 *
 * @example
 * ```ts
 * const entry: RawImportEntry = {
 *   title: "Death Note",
 *   originalTitle: "Desu Nōto",
 *   releaseYear: 2006,
 *   watchedEpisodes: 37,
 *   totalEpisodes: 37,
 *   targetStatus: "COMPLETED",
 *   rating: 9
 * };
 * ```
 */
export interface RawImportEntry {
  /**
   * Primary title of the media.
   */
  title: string;

  /**
   * Original Japanese / native title.
   */
  originalTitle?: string | null;

  /**
   * Year of release.
   */
  releaseYear?: number | null;

  /**
   * Number of episodes the user has watched.
   */
  watchedEpisodes?: number;

  /**
   * Total episodes in the series.
   */
  totalEpisodes?: number | null;

  /**
   * Watch status to assign in Zenith bookmarks (`"WATCHING"`, `"COMPLETED"`, etc.).
   */
  targetStatus?: UserMediaStatus;

  /**
   * User rating score (1-10).
   */
  rating?: number | null;
}

/**
 * Batch of bookmark entries imported from a named source.
 */
export interface ImportBatch {
  /**
   * Source service name (e.g. `"MyAnimeList"`, `"Shikimori"`, `"AniList"`).
   */
  sourceName: string;

  /**
   * Extracted bookmark items.
   */
  entries: RawImportEntry[];
}

/**
 * Preview summary displayed to the user prior to committing a bookmark file import.
 */
export interface ImportPreviewResult {
  /**
   * Detected file format (e.g. `"csv"`, `"json"`, `"xml"`).
   */
  format: string;

  /**
   * Sample text lines or titles extracted from the file for user confirmation.
   */
  rows: string[];

  /**
   * Total number of valid bookmark entries detected in the source.
   */
  totalDetectedCount?: number;
}

/**
 * Artifact entry paths inside the `.zpk` envelope.
 */
export interface PluginArtifactEntry {
  /**
   * Legacy JVM jar path.
   */
  jvm?: string;

  /**
   * Legacy Android dex path.
   */
  android?: string;

  /**
   * JavaScript bundle entrypoint relative to package root (e.g. `"index.js"`).
   */
  js?: string;
}

/**
 * Declarative UI setting item schema rendered natively by Zenith Material 3 Settings UI.
 *
 * @example
 * ```json
 * {
 *   "key": "mirror",
 *   "title": "API Mirror",
 *   "description": "Select the active mirror server",
 *   "type": "CHOICE",
 *   "defaultValue": "https://api.example.com",
 *   "options": [
 *     { "value": "https://api.example.com", "label": "Main Server" },
 *     { "value": "https://backup.example.com", "label": "Backup Server" }
 *   ]
 * }
 * ```
 */
export interface SettingItemSpec {
  /**
   * Unique preference key accessed via `context.getSetting(key)`.
   */
  key: string;

  /**
   * Human-readable label displayed in settings UI.
   */
  title: string;

  /**
   * Subtitle explaining what this setting controls.
   */
  description: string;

  /**
   * Setting input control type:
   * - `"TOGGLE"`: Material 3 Switch toggle (boolean).
   * - `"TEXT"`: Text input dialog / field.
   * - `"CHOICE"`: Single-choice selection dialog from `options`.
   * - `"MIRROR_LIST"`: Interactive server mirror selector.
   */
  type: "TOGGLE" | "TEXT" | "CHOICE" | "MIRROR_LIST";

  /**
   * Default fallback value if user has not customized this setting.
   */
  defaultValue?: any;

  /**
   * Selectable options for `"CHOICE"` or `"MIRROR_LIST"` setting types.
   */
  options?: Array<{ value: string; label: string }>;

  /**
   * If `true`, the text value is masked as a password / API token in the UI.
   */
  isSecret?: boolean;

  /**
   * If `true`, the user cannot leave this setting empty.
   */
  required?: boolean;
}

/**
 * Zenith Provider Manifest V2 (`manifest.json`) metadata specification.
 *
 * Every `.zpk` package must contain a root `manifest.json` conforming to this schema.
 *
 * @example
 * ```json
 * {
 *   "id": "anime_sample",
 *   "name": "Sample Anime Provider",
 *   "version": "1.0.0",
 *   "sdkVersion": "0.1.0",
 *   "packageFormat": "zpk/v2",
 *   "moduleApiVersion": "1.0.0",
 *   "entrypoint": "index.js",
 *   "author": "Zenith Author",
 *   "description": "Fast anime catalog and stream provider",
 *   "capabilities": ["CATALOG", "MEDIA_SOURCE"],
 *   "hosts": ["api.example.com", "*.cdn.example.com"]
 * }
 * ```
 */
export interface PluginManifest {
  /**
   * Unique alphanumeric identifier (1-128 chars, may include `.` `_` `-`).
   */
  id: string;

  /**
   * Human-readable display name.
   */
  name: string;

  /**
   * Semantic version string (e.g. `"1.0.0"`).
   */
  version: string;

  /**
   * Zenith SDK version the provider was built against (e.g. `"0.1.0"`).
   */
  sdkVersion?: string;

  /**
   * Package envelope format version: must be `"zpk/v2"` for portable TypeScript providers.
   */
  packageFormat?: string;

  /**
   * Runtime module API version (e.g. `"1.0.0"`).
   */
  moduleApiVersion?: string;

  /**
   * Relative filename of the bundled JavaScript entrypoint inside the archive (defaults to `"index.js"`).
   */
  entrypoint?: string;

  /**
   * Short description of provider content and features.
   */
  description?: string;

  /**
   * Name or handle of the author/maintainer.
   */
  author?: string;

  /**
   * URL to source repository or documentation website.
   */
  homepage?: string;

  /**
   * Filename of the square 512x512 PNG icon inside the archive (e.g. `"icon.png"`).
   */
  icon?: string;

  /**
   * List of capabilities implemented by this provider plugin.
   */
  capabilities: ProviderCapability[];

  /**
   * Host allowlist for network requests. Zenith Host Broker blocks requests to domains not matching this list.
   * Supports wildcards (e.g. `"*.example.com"`).
   */
  hosts?: string[];

  /**
   * Optional platform entrypoint mappings.
   */
  entry?: PluginArtifactEntry;

  /**
   * Legacy class entrypoint name.
   */
  entryClass?: string;

  /**
   * Relative filename of the declarative `settings.json` file inside the archive.
   */
  settings?: string;

  /**
   * Highlighted advantages/pros shown in the Provider Store details card.
   */
  pros?: string[];

  /**
   * Known limitations/cons shown in the Provider Store details card.
   */
  cons?: string[];

  /**
   * Inlined settings schema array.
   */
  settingsSchema?: SettingItemSpec[];

  /**
   * Predefined mirror servers.
   */
  mirrors?: Array<{ url: string; label?: string }>;

  /**
   * Map of ISO language codes to localized provider names (e.g. `{"ru": "Название", "en": "Name"}`).
   */
  localizedNames?: Record<string, string>;

  /**
   * Map of ISO language codes to localized descriptions.
   */
  localizedDescriptions?: Record<string, string>;
}
