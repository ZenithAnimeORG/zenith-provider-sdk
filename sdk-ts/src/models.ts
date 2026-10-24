/**
 * Core models and types for the Zenith Provider SDK.
 * Matches Kotlin SDK data models defined in com.pilldev.zenith.provider.
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

export type ProviderTranslationType = "DUB" | "VO" | "SUB" | "UNKNOWN";

export type UserMediaStatus =
  | "WATCHING"
  | "PLANNED"
  | "COMPLETED"
  | "ON_HOLD"
  | "DROPPED";

export interface MediaRef {
  id: string;
  externalIds?: Record<string, string>;
  title?: string;
  alternateTitles?: string[];
}

export interface CatalogMediaItem {
  mediaRef: MediaRef;
  title: string;
  originalTitle?: string | null;
  posterUrl?: string | null;
  score?: number | null;
  year?: number | null;
  totalEpisodes?: number | null;
}

export interface CatalogMediaDetails {
  mediaRef: MediaRef;
  title: string;
  originalTitle?: string | null;
  description?: string | null;
  posterUrl?: string | null;
  bannerUrl?: string | null;
  score?: number | null;
  year?: number | null;
  episodesCount?: number | null;
  genres?: string[];
  studios?: string[];
  status?: string | null;
}

export interface CatalogSearchResult {
  items: CatalogMediaItem[];
  hasNextPage: boolean;
}

export interface CatalogSection {
  title: string;
  items: CatalogMediaItem[];
}

export interface ProviderEpisode {
  number: number;
  url: string;
  quality?: Record<string, string> | null;
  providerId?: string | null;
}

export interface ProviderVideoSource {
  name: string;
  translationName?: string;
  translationType?: ProviderTranslationType;
  episodes: ProviderEpisode[];
  qualities?: Record<string, string> | null;
}

export interface ProviderMediaStream {
  url: string;
  quality?: string | null;
  headers?: Record<string, string>;
  isHls?: boolean;
}

export interface ProviderTorrentSource {
  title: string;
  infoHash?: string | null;
  magnetUri?: string | null;
  size?: number;
  seeders?: number;
  leechers?: number;
  quality?: string;
  trackerName?: string;
  translationName?: string;
  translationType?: ProviderTranslationType;
}

export interface ProviderSubtitle {
  fileId?: number;
  language: string;
  releaseName: string;
  isHearingImpaired?: boolean;
  isAiTranslated?: boolean;
  downloadCount?: number;
  directUrl?: string | null;
  provider?: string;
}

export interface SkipInterval {
  startTime: number;
  endTime: number;
  skipType: string;
  episodeLength?: number | null;
}

export interface ProviderStatusInfo {
  status?: string;
  details?: Record<string, string>;
  latencyMs?: number | null;
  isOperational?: boolean;
  message?: string;
}

export type ProviderTestResult =
  | { success: true; message: string; latencyMs?: number; type?: "Success" }
  | { success: false; error: string; message?: string; latencyMs?: number; type?: "Failure" }
  | { type: "Success"; message: string; latencyMs?: number; success?: boolean }
  | { type: "Failure"; error: string; message?: string; latencyMs?: number; success?: boolean };

export interface ImportInstructionStep {
  stepNumber: number;
  title: string;
  description?: string | null;
  note?: string | null;
  linkUrl?: string | null;
  linkText?: string | null;
}

export interface RawImportEntry {
  title: string;
  originalTitle?: string | null;
  releaseYear?: number | null;
  watchedEpisodes?: number;
  totalEpisodes?: number | null;
  targetStatus?: UserMediaStatus;
  rating?: number | null;
}

export interface ImportBatch {
  sourceName: string;
  entries: RawImportEntry[];
}

export interface ImportPreviewResult {
  format: string;
  rows: string[];
  totalDetectedCount?: number;
}

export interface PluginArtifactEntry {
  jvm?: string;
  android?: string;
  js?: string;
}

export interface SettingItemSpec {
  key: string;
  title: string;
  description: string;
  type: "TOGGLE" | "TEXT" | "CHOICE" | "MIRROR_LIST";
  defaultValue?: any;
  options?: Array<{ value: string; label: string }>;
  isSecret?: boolean;
  required?: boolean;
}

export interface PluginManifest {
  id: string;
  name: string;
  version: string;
  sdkVersion?: string;
  packageFormat?: string;
  moduleApiVersion?: string;
  entrypoint?: string;
  description?: string;
  author?: string;
  homepage?: string;
  icon?: string;
  capabilities: ProviderCapability[];
  hosts?: string[];
  entry?: PluginArtifactEntry;
  entryClass?: string;
  settings?: string;
  pros?: string[];
  cons?: string[];
  settingsSchema?: SettingItemSpec[];
  mirrors?: Array<{ url: string; label?: string }>;
  localizedNames?: Record<string, string>;
  localizedDescriptions?: Record<string, string>;
}
