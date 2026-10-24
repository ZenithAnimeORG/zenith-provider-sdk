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
 * All capability methods are optional; declare supported capabilities in manifest.json.
 */
export interface ProviderDefinition {
  initialize?(manifest: PluginManifest, context: ProviderContext): Promise<void>;
  dispose?(): Promise<void>;
  test?(context: ProviderContext): Promise<ProviderTestResult>;
  probeStatus?(context: ProviderContext): Promise<ProviderStatusInfo>;
  executeAction?(actionId: string, context: ProviderContext): Promise<string>;

  // CATALOG Capability
  getSearch?(
    query: string,
    page: number,
    pageSize: number,
    context: ProviderContext
  ): Promise<CatalogSearchResult>;
  getDetails?(mediaRef: MediaRef, context: ProviderContext): Promise<CatalogMediaDetails>;
  getHomeSections?(context: ProviderContext): Promise<CatalogSection[]>;
  getPopular?(page: number, pageSize: number, context: ProviderContext): Promise<CatalogMediaItem[]>;
  getLatest?(page: number, pageSize: number, context: ProviderContext): Promise<CatalogMediaItem[]>;
  getRelated?(mediaRef: MediaRef, context: ProviderContext): Promise<CatalogMediaItem[]>;
  getEpisodes?(mediaRef: MediaRef, context: ProviderContext): Promise<ProviderEpisode[]>;

  // MEDIA_SOURCE Capability
  getSources?(
    mediaRef: MediaRef,
    context: ProviderContext,
    episodeNumber?: number
  ): Promise<ProviderVideoSource[]>;
  resolveEpisode?(
    episode: ProviderEpisode,
    context: ProviderContext
  ): Promise<ProviderMediaStream[]>;

  // TORRENT_SOURCE Capability
  searchTorrents?(
    mediaRef: MediaRef,
    context: ProviderContext
  ): Promise<ProviderTorrentSource[]>;
  getTorrentSources?(
    mediaRef: MediaRef,
    episodeNumber: number,
    context: ProviderContext
  ): Promise<ProviderTorrentSource[]>;

  // SUBTITLES Capability
  searchSubtitles?(
    query: string,
    languages: string,
    episodeNumber: number | null,
    seasonNumber: number | null,
    context: ProviderContext
  ): Promise<ProviderSubtitle[]>;
  getSubtitleDownloadLink?(
    fileId: number,
    context: ProviderContext
  ): Promise<string | null>;
  getSubtitles?(
    mediaRef: MediaRef,
    episodeNumber: number,
    context: ProviderContext
  ): Promise<ProviderSubtitle[]>;

  // SKIP_TIMINGS Capability
  getSkipTimes?(
    mediaRef: MediaRef,
    episodeNumber: number,
    episodeLength: number | null,
    translationName: string | null,
    context: ProviderContext
  ): Promise<SkipInterval[]>;
  getSkipTimings?(
    mediaRef: MediaRef,
    episodeNumber: number,
    context: ProviderContext
  ): Promise<SkipInterval[]>;

  // POSTER_SOURCE Capability
  getPoster?(mediaRef: MediaRef, context: ProviderContext): Promise<string | null>;

  // STREAM_EXTRACTOR Capability
  canExtractStream?(
    extractorName: string,
    url: string,
    context: ProviderContext
  ): Promise<boolean>;
  extractStream?(
    extractorName: string,
    url: string,
    referer: string | null,
    context: ProviderContext
  ): Promise<ProviderMediaStream[]>;
  resolveStream?(
    extractorName: string,
    url: string,
    context: ProviderContext
  ): Promise<ProviderMediaStream[]>;

  // TRACKER Capability
  onEpisodeWatched?(
    mediaRef: MediaRef,
    episode: number,
    context: ProviderContext
  ): Promise<void>;
  syncStatus?(
    mediaRef: MediaRef,
    status: UserMediaStatus,
    context: ProviderContext
  ): Promise<void>;
  getUserMediaStatus?(mediaRef: MediaRef, context: ProviderContext): Promise<UserMediaStatus>;
  updateUserMediaStatus?(
    mediaRef: MediaRef,
    status: string,
    progress: number,
    context: ProviderContext
  ): Promise<void>;
}

export interface ProviderExport {
  readonly type: "provider";
  readonly definition: ProviderDefinition;
}

declare global {
  var __zenith_provider: ProviderDefinition | undefined;
}

/**
 * Entrypoint factory for declaring a Zenith media/catalog/tracker provider module.
 *
 * Example:
 * ```ts
 * export default defineProvider({
 *   async getSearch(query, page, pageSize, context) {
 *     return { items: [], hasNextPage: false };
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
