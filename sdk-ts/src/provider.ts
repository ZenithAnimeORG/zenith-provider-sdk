/**
 * Core provider declaration and lifecycle contracts for Zenith media plugins.
 *
 * @module provider
 */

import {
  CatalogMediaDetails,
  CatalogMediaItem,
  CatalogSearchResult,
  CatalogSection,
  MediaRef,
  PluginManifest,
  ProviderEpisode,
  ProviderMediaStream,
  ProviderStatusInfo,
  ProviderSubtitle,
  ProviderTestResult,
  ProviderTorrentSource,
  ProviderVideoSource,
  SkipInterval,
  UserMediaStatus,
} from "./models.js";
import { ProviderContext } from "./broker.js";

/**
 * Interface implemented by Zenith modular providers.
 *
 * All capability methods are optional. A provider implements only the methods
 * declared in its `manifest.json` capabilities array.
 *
 * Capabilities map to methods:
 * - `"CATALOG"`: {@link getSearch}, {@link getDetails}, {@link getHomeSections}, {@link getPopular}, {@link getLatest}, {@link getRelated}, {@link getEpisodes}
 * - `"MEDIA_SOURCE"`: {@link getSources}, {@link resolveEpisode}
 * - `"TORRENT_SOURCE"`: {@link searchTorrents}, {@link getTorrentSources}
 * - `"SUBTITLES"`: {@link searchSubtitles}, {@link getSubtitleDownloadLink}, {@link getSubtitles}
 * - `"SKIP_TIMINGS"`: {@link getSkipTimes}, {@link getSkipTimings}
 * - `"POSTER_SOURCE"`: {@link getPoster}
 * - `"STREAM_EXTRACTOR"`: {@link canExtractStream}, {@link extractStream}, {@link resolveStream}
 * - `"TRACKER"`: {@link onEpisodeWatched}, {@link syncStatus}, {@link getUserMediaStatus}, {@link updateUserMediaStatus}
 *
 * Lifecycle methods {@link initialize}, {@link dispose}, {@link test}, {@link probeStatus}, {@link executeAction}
 * are available to all providers regardless of declared capabilities.
 */
export interface ProviderDefinition {
  // ==========================================
  // Lifecycle & Diagnostics
  // ==========================================

  /**
   * Initializes the provider when the QuickJS instance is spun up.
   *
   * Use this to prepare in-memory caches, parse settings, or establish session states.
   *
   * @param manifest The parsed plugin manifest.
   * @param context Host execution context.
   *
   * @example
   * ```ts
   * async initialize(manifest, context) {
   *   context.logger.info(`Initializing provider: ${manifest.name} v${manifest.version}`);
   * }
   * ```
   */
  initialize?(manifest: PluginManifest, context: ProviderContext): Promise<void>;

  /**
   * Cleans up provider resources when unloaded or replaced.
   *
   * @example
   * ```ts
   * async dispose() {
   *   this.cache.clear();
   * }
   * ```
   */
  dispose?(): Promise<void>;

  /**
   * Performs a self-test diagnostic check to verify API connectivity and server health.
   *
   * Triggered by user tap on "Test Connection" in Zenith provider settings.
   *
   * @param context Host execution context.
   * @returns Test result with success/failure status and diagnostic latency.
   *
   * @example
   * ```ts
   * async test(context) {
   *   const start = Date.now();
   *   const res = await context.fetch("https://api.example.com/ping");
   *   return res.ok
   *     ? { success: true, message: "Mirror reachable", latencyMs: Date.now() - start }
   *     : { success: false, error: `HTTP ${res.status}` };
   * }
   * ```
   */
  test?(context: ProviderContext): Promise<ProviderTestResult>;

  /**
   * Probes provider operational health and response latency.
   *
   * Lightweight health check called by Zenith's background monitoring to display status dots.
   *
   * @param context Host execution context.
   * @returns Health status information.
   *
   * @example
   * ```ts
   * async probeStatus(context) {
   *   return {
   *     status: "OK",
   *     isOperational: true,
   *     latencyMs: 35
   *   };
   * }
   * ```
   */
  probeStatus?(context: ProviderContext): Promise<ProviderStatusInfo>;

  /**
   * Executes a custom interactive action triggered from Zenith provider settings UI.
   *
   * @param actionId Action identifier matching one declared in `settings.json`.
   * @param context Host execution context.
   * @returns Text message displayed in user-facing confirmation dialog.
   *
   * @example
   * ```ts
   * async executeAction(actionId, context) {
   *   if (actionId === "clear_session") {
   *     await context.setSetting("session_token", "");
   *     return "Session cleared successfully.";
   *   }
   *   throw new Error(`Unknown action: ${actionId}`);
   * }
   * ```
   */
  executeAction?(actionId: string, context: ProviderContext): Promise<string>;

  // ==========================================
  // CATALOG Capability
  // ==========================================

  /**
   * Searches the provider's media catalog by query string.
   *
   * @param query Search keywords entered by user.
   * @param page 1-indexed page number.
   * @param pageSize Requested items per page.
   * @param context Host execution context.
   * @returns List of matching media items and pagination flag.
   *
   * @example
   * ```ts
   * async getSearch(query, page, pageSize, context) {
   *   const res = await context.fetch(`https://api.example.com/search?q=${encodeURIComponent(query)}&page=${page}`);
   *   const json = await res.json();
   *   return {
   *     items: json.results.map(r => ({
   *       mediaRef: { id: r.id, title: r.title },
   *       title: r.title,
   *       posterUrl: r.cover,
   *       score: r.rating
   *     })),
   *     hasNextPage: json.page < json.totalPages
   *   };
   * }
   * ```
   */
  getSearch?(
    query: string,
    page: number,
    pageSize: number,
    context: ProviderContext
  ): Promise<CatalogSearchResult>;

  /**
   * Loads full metadata for a specific title.
   *
   * @param mediaRef Media identifier reference.
   * @param context Host execution context.
   * @returns Full title details (synopsis, genres, score, episodes count).
   *
   * @example
   * ```ts
   * async getDetails(mediaRef, context) {
   *   const res = await context.fetch(`https://api.example.com/anime/${mediaRef.id}`);
   *   const data = await res.json();
   *   return {
   *     mediaRef,
   *     title: data.name,
   *     description: data.synopsis,
   *     posterUrl: data.poster,
   *     score: data.score,
   *     genres: data.genres
   *   };
   * }
   * ```
   */
  getDetails?(mediaRef: MediaRef, context: ProviderContext): Promise<CatalogMediaDetails>;

  /**
   * Loads curated horizontal sections/carousels for the Zenith Home screen.
   *
   * @param context Host execution context.
   * @returns Array of named sections containing preview media cards.
   *
   * @example
   * ```ts
   * async getHomeSections(context) {
   *   return [
   *     { title: "Trending", items: trendingList },
   *     { title: "Top Rated", items: topList }
   *   ];
   * }
   * ```
   */
  getHomeSections?(context: ProviderContext): Promise<CatalogSection[]>;

  /**
   * Loads a paginated list of currently popular media items.
   *
   * @param page 1-indexed page number.
   * @param pageSize Requested items per page.
   * @param context Host execution context.
   * @returns Array of catalog media items.
   */
  getPopular?(page: number, pageSize: number, context: ProviderContext): Promise<CatalogMediaItem[]>;

  /**
   * Loads a paginated list of newly released or updated media items.
   *
   * @param page 1-indexed page number.
   * @param pageSize Requested items per page.
   * @param context Host execution context.
   * @returns Array of catalog media items.
   */
  getLatest?(page: number, pageSize: number, context: ProviderContext): Promise<CatalogMediaItem[]>;

  /**
   * Loads related or recommended titles for a given media item.
   *
   * @param mediaRef Media reference.
   * @param context Host execution context.
   * @returns Array of recommended media items.
   */
  getRelated?(mediaRef: MediaRef, context: ProviderContext): Promise<CatalogMediaItem[]>;

  /**
   * Retrieves the raw list of episodes for a given title directly without grouping into sources.
   *
   * @param mediaRef Media reference.
   * @param context Host execution context.
   * @returns Array of provider episodes.
   */
  getEpisodes?(mediaRef: MediaRef, context: ProviderContext): Promise<ProviderEpisode[]>;

  // ==========================================
  // MEDIA_SOURCE Capability
  // ==========================================

  /**
   * Retrieves available video translation sources (dubbing groups, audio tracks, sub groups) and their episodes.
   *
   * @param mediaRef Media reference.
   * @param context Host execution context.
   * @param episodeNumber Optional episode number filter.
   * @returns Array of video translation sources.
   *
   * @example
   * ```ts
   * async getSources(mediaRef, context) {
   *   return [{
   *     name: "AniLibria",
   *     translationName: "AniLibria",
   *     translationType: "DUB",
   *     episodes: [
   *       { number: 1, url: "https://example.com/ep1" }
   *     ]
   *   }];
   * }
   * ```
   */
  getSources?(
    mediaRef: MediaRef,
    context: ProviderContext,
    episodeNumber?: number
  ): Promise<ProviderVideoSource[]>;

  /**
   * Resolves direct playable video streams (HLS m3u8 or MP4) for an episode.
   *
   * @param episode The episode selected for playback.
   * @param context Host execution context.
   * @returns Array of playable streams with quality tags and headers.
   *
   * @example
   * ```ts
   * async resolveEpisode(episode, context) {
   *   return [{
   *     url: "https://cdn.example.com/master.m3u8",
   *     quality: "1080p",
   *     isHls: true,
   *     headers: { Referer: "https://example.com" }
   *   }];
   * }
   * ```
   */
  resolveEpisode?(
    episode: ProviderEpisode,
    context: ProviderContext
  ): Promise<ProviderMediaStream[]>;

  // ==========================================
  // TORRENT_SOURCE Capability
  // ==========================================

  /**
   * Searches for BitTorrent releases matching a title.
   *
   * @param mediaRef Media reference.
   * @param context Host execution context.
   * @returns Array of torrent releases for streaming via `jlibtorrent`.
   *
   * @example
   * ```ts
   * async searchTorrents(mediaRef, context) {
   *   return [{
   *     title: "[SubsPlease] Sousou no Frieren - 01 (1080p) [98D8D5D5].mkv",
   *     magnetUri: "magnet:?xt=urn:btih:...",
   *     size: 1400000000,
   *     seeders: 45,
   *     leechers: 3,
   *     quality: "1080p"
   *   }];
   * }
   * ```
   */
  searchTorrents?(
    mediaRef: MediaRef,
    context: ProviderContext
  ): Promise<ProviderTorrentSource[]>;

  /**
   * Retrieves torrent releases filtered for a specific episode number.
   *
   * @param mediaRef Media reference.
   * @param episodeNumber Episode number.
   * @param context Host execution context.
   * @returns Array of matching torrent releases.
   */
  getTorrentSources?(
    mediaRef: MediaRef,
    episodeNumber: number,
    context: ProviderContext
  ): Promise<ProviderTorrentSource[]>;

  // ==========================================
  // SUBTITLES Capability
  // ==========================================

  /**
   * Searches for subtitle tracks matching a title or episode.
   *
   * @param query Search text or release name.
   * @param languages Comma-separated ISO language codes (e.g. `"en,ru"`).
   * @param episodeNumber Episode number, if applicable.
   * @param seasonNumber Season number, if applicable.
   * @param context Host execution context.
   * @returns Array of found subtitle tracks.
   *
   * @example
   * ```ts
   * async searchSubtitles(query, languages, episodeNumber, seasonNumber, context) {
   *   return [{
   *     language: "en",
   *     releaseName: "SubsPlease Frieren 01 1080p",
   *     directUrl: "https://example.com/subs/01.ass"
   *   }];
   * }
   * ```
   */
  searchSubtitles?(
    query: string,
    languages: string,
    episodeNumber: number | null,
    seasonNumber: number | null,
    context: ProviderContext
  ): Promise<ProviderSubtitle[]>;

  /**
   * Resolves the direct download URL for a subtitle file ID.
   *
   * @param fileId Numeric subtitle file ID.
   * @param context Host execution context.
   * @returns Download URL or `null` if expired/unavailable.
   */
  getSubtitleDownloadLink?(
    fileId: number,
    context: ProviderContext
  ): Promise<string | null>;

  /**
   * Retrieves subtitles for a specific media item and episode.
   *
   * @param mediaRef Media reference.
   * @param episodeNumber Episode number.
   * @param context Host execution context.
   * @returns Array of subtitle tracks.
   */
  getSubtitles?(
    mediaRef: MediaRef,
    episodeNumber: number,
    context: ProviderContext
  ): Promise<ProviderSubtitle[]>;

  // ==========================================
  // SKIP_TIMINGS Capability
  // ==========================================

  /**
   * Retrieves intro/outro skip intervals for an episode.
   *
   * @param mediaRef Media reference.
   * @param episodeNumber Episode number.
   * @param episodeLength Total episode duration in seconds, if known.
   * @param translationName Translation name or release group.
   * @param context Host execution context.
   * @returns Array of skip intervals (OP/ED).
   *
   * @example
   * ```ts
   * async getSkipTimes(mediaRef, episodeNumber, episodeLength, translationName, context) {
   *   return [{
   *     startTime: 90,
   *     endTime: 180,
   *     skipType: "op"
   *   }];
   * }
   * ```
   */
  getSkipTimes?(
    mediaRef: MediaRef,
    episodeNumber: number,
    episodeLength: number | null,
    translationName: string | null,
    context: ProviderContext
  ): Promise<SkipInterval[]>;

  /**
   * Simplified overload for skip intervals.
   *
   * @param mediaRef Media reference.
   * @param episodeNumber Episode number.
   * @param context Host execution context.
   * @returns Array of skip intervals.
   */
  getSkipTimings?(
    mediaRef: MediaRef,
    episodeNumber: number,
    context: ProviderContext
  ): Promise<SkipInterval[]>;

  // ==========================================
  // POSTER_SOURCE Capability
  // ==========================================

  /**
   * Resolves a high-resolution poster artwork URL for a media item.
   *
   * @param mediaRef Media reference.
   * @param context Host execution context.
   * @returns Poster image URL or `null`.
   */
  getPoster?(mediaRef: MediaRef, context: ProviderContext): Promise<string | null>;

  // ==========================================
  // STREAM_EXTRACTOR Capability
  // ==========================================

  /**
   * Checks whether this provider can extract streams from a video hosting embed URL.
   *
   * @param extractorName Extractor identifier.
   * @param url Embed URL.
   * @param context Host execution context.
   * @returns `true` if extractor supports the given URL.
   */
  canExtractStream?(
    extractorName: string,
    url: string,
    context: ProviderContext
  ): Promise<boolean>;

  /**
   * Extracts playable video streams from a video hosting embed URL.
   *
   * @param extractorName Extractor identifier.
   * @param url Embed URL.
   * @param referer HTTP Referer header to include.
   * @param context Host execution context.
   * @returns Array of extracted media streams.
   */
  extractStream?(
    extractorName: string,
    url: string,
    referer: string | null,
    context: ProviderContext
  ): Promise<ProviderMediaStream[]>;

  /**
   * Resolves streams using a registered extractor.
   *
   * @param extractorName Extractor identifier.
   * @param url Embed URL.
   * @param context Host execution context.
   * @returns Array of extracted media streams.
   */
  resolveStream?(
    extractorName: string,
    url: string,
    context: ProviderContext
  ): Promise<ProviderMediaStream[]>;

  // ==========================================
  // TRACKER Capability
  // ==========================================

  /**
   * Invoked when an episode has been watched in Zenith.
   *
   * @param mediaRef Media reference.
   * @param episode Watched episode number.
   * @param context Host execution context.
   */
  onEpisodeWatched?(
    mediaRef: MediaRef,
    episode: number,
    context: ProviderContext
  ): Promise<void>;

  /**
   * Synchronizes user watch status to the tracking service.
   *
   * @param mediaRef Media reference.
   * @param status Watchlist status (`"WATCHING"`, `"COMPLETED"`, etc.).
   * @param context Host execution context.
   */
  syncStatus?(
    mediaRef: MediaRef,
    status: UserMediaStatus,
    context: ProviderContext
  ): Promise<void>;

  /**
   * Retrieves the current user tracking status for a media item.
   *
   * @param mediaRef Media reference.
   * @param context Host execution context.
   * @returns User media status.
   */
  getUserMediaStatus?(mediaRef: MediaRef, context: ProviderContext): Promise<UserMediaStatus>;

  /**
   * Updates user watch status and episode progress.
   *
   * @param mediaRef Media reference.
   * @param status Watchlist status string.
   * @param progress Watched episode count.
   * @param context Host execution context.
   */
  updateUserMediaStatus?(
    mediaRef: MediaRef,
    status: string,
    progress: number,
    context: ProviderContext
  ): Promise<void>;
}

/**
 * Return type wrapper produced by {@link defineProvider}.
 */
export interface ProviderExport {
  /**
   * Export discriminator indicating this module is a media provider plugin.
   */
  readonly type: "provider";

  /**
   * The wrapped provider implementation.
   */
  readonly definition: ProviderDefinition;
}

declare global {
  var __zenith_provider: ProviderDefinition | undefined;
}

/**
 * Entrypoint factory for declaring a Zenith media/catalog/tracker provider module.
 *
 * Attaches the provider definition to the global execution scope and exports it
 * as the default export of your bundle.
 *
 * @param definition Implementation of {@link ProviderDefinition} methods.
 * @returns Wrapped {@link ProviderExport} object.
 *
 * @example
 * ```ts
 * import { defineProvider } from "@zenith/sdk/provider";
 * import { loadHtml } from "@zenith/sdk/dom";
 *
 * export default defineProvider({
 *   async getSearch(query, page, pageSize, context) {
 *     const mirror = await context.getMirror();
 *     const res = await context.fetch(`${mirror}/search?q=${encodeURIComponent(query)}`);
 *     const $ = loadHtml(await res.text());
 *
 *     const items = $(".item").map((_, el) => ({
 *       mediaRef: { id: $(el).attr("data-id") || "", title: $(el).find(".title").text() },
 *       title: $(el).find(".title").text()
 *     })).get();
 *
 *     return { items, hasNextPage: false };
 *   }
 * });
 * ```
 */
export function defineProvider(definition: ProviderDefinition): ProviderExport {
  if (typeof globalThis !== "undefined") {
    globalThis.__zenith_provider = definition;
  }
  return {
    type: "provider",
    definition,
  };
}
