import { ProviderContext } from "./broker.js";
import { BookmarkSource, ImportContext } from "./bookmark-importer.js";
import { RawImportEntry } from "./models.js";

declare global {
  var __zenith_dispatch: ((requestJson: string) => Promise<string>) | undefined;
}

export interface DispatchResponse {
  kind: "Success" | "Failed";
  type: "Success" | "Failed";
  value?: any;
  code?: string;
  message?: string;
}

function parseCsvLine(line: string): string[] {
  const result: string[] = [];
  let current = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (char === "," && !inQuotes) {
      result.push(current.trim());
      current = "";
    } else {
      current += char;
    }
  }
  result.push(current.trim());
  return result;
}

class MemoryBookmarkSource implements BookmarkSource {
  constructor(private readonly lines: string[]) {}

  async readHeaderAndSample(maxLines: number): Promise<string[]> {
    return this.lines.slice(0, maxLines);
  }

  async *readCsvRows(): AsyncIterable<Record<string, string>> {
    if (this.lines.length === 0) return;
    const headerLine = this.lines[0];
    if (!headerLine) return;
    const headers = parseCsvLine(headerLine).map((h) => h.replace(/^"|"$/g, ""));
    for (let i = 1; i < this.lines.length; i++) {
      const line = this.lines[i];
      if (!line || !line.trim()) continue;
      const values = parseCsvLine(line).map((v) => v.replace(/^"|"$/g, ""));
      const row: Record<string, string> = {};
      for (let j = 0; j < headers.length; j++) {
        const h = headers[j];
        if (h) {
          row[h] = values[j] ?? "";
        }
      }
      yield row;
    }
  }

  async *readLines(): AsyncIterable<string> {
    for (const line of this.lines) {
      yield line;
    }
  }

  async readText(): Promise<string> {
    return this.lines.join("\n");
  }
}

/**
 * Handles incoming dispatched operation JSON from the Zenith Kotlin host runtime.
 */
export async function handleZenithDispatch(requestJson: string): Promise<string> {
  try {
    const op = JSON.parse(requestJson);
    const opType = (op.kind ?? op.type ?? op.operation ?? "").replace(/^.*[.$]/, "");
    const provider = globalThis.__zenith_provider;
    const importer = globalThis.__zenith_bookmark_importer;
    const context = new ProviderContext(op.providerId ?? "quickjs-provider", op.settings ?? {});

    let resultValue: any = null;

    switch (opType) {
      case "Test": {
        if (!provider?.test) {
          resultValue = {
            kind: "Test",
            type: "Test",
            result: {
              kind: "Success",
              type: "Success",
              success: true,
              message: "Provider ready",
              latencyMs: 0,
            },
          };
        } else {
          const res = await provider.test(context);
          const isSuccess = ("success" in res) ? res.success : res.type === "Success";
          const message = ("message" in res && res.message) ? res.message : ("error" in res ? res.error : "OK");
          const latencyMs = res.latencyMs ?? 0;
          resultValue = {
            kind: "Test",
            type: "Test",
            result: {
              kind: isSuccess ? "Success" : "Failure",
              type: isSuccess ? "Success" : "Failure",
              success: isSuccess,
              message,
              error: isSuccess ? undefined : message,
              latencyMs,
            },
          };
        }
        break;
      }

      case "ProbeStatus": {
        if (!provider?.probeStatus) {
          resultValue = {
            kind: "Status",
            type: "Status",
            result: {
              status: "OK",
              isOperational: true,
              details: {},
              latencyMs: null,
            },
          };
        } else {
          const res = await provider.probeStatus(context);
          resultValue = {
            kind: "Status",
            type: "Status",
            result: {
              status: res.status ?? (res.isOperational ? "OK" : "DEGRADED"),
              isOperational: res.isOperational ?? (res.status === "OK"),
              details: res.details ?? {},
              latencyMs: res.latencyMs ?? null,
            },
          };
        }
        break;
      }

      case "ExecuteAction": {
        if (!provider?.executeAction) {
          resultValue = {
            kind: "ActionResult",
            type: "ActionResult",
            result: `Action ${op.actionId ?? ""} is not supported`,
          };
        } else {
          const res = await provider.executeAction(op.actionId ?? "", context);
          resultValue = {
            kind: "ActionResult",
            type: "ActionResult",
            result: res,
          };
        }
        break;
      }

      case "CatalogSearch": {
        if (!provider?.getSearch) {
          throw new Error("Catalog search capability not implemented by provider");
        }
        const searchResult = await provider.getSearch(
          op.query ?? "",
          op.page ?? 1,
          op.pageSize ?? 20,
          context
        );
        resultValue = {
          kind: "CatalogSearch",
          type: "CatalogSearch",
          result: searchResult,
        };
        break;
      }

      case "CatalogDetails": {
        if (!provider?.getDetails) {
          throw new Error("Catalog details capability not implemented by provider");
        }
        const details = await provider.getDetails(op.mediaRef, context);
        resultValue = {
          kind: "CatalogDetails",
          type: "CatalogDetails",
          result: details,
        };
        break;
      }

      case "CatalogHomeSections": {
        if (!provider?.getHomeSections) {
          resultValue = { kind: "CatalogSections", type: "CatalogSections", result: [] };
        } else {
          const sections = await provider.getHomeSections(context);
          resultValue = {
            kind: "CatalogSections",
            type: "CatalogSections",
            result: sections,
          };
        }
        break;
      }

      case "CatalogRelated": {
        if (!provider?.getRelated) {
          resultValue = { kind: "CatalogItems", type: "CatalogItems", result: [] };
        } else {
          const items = await provider.getRelated(op.mediaRef, context);
          resultValue = {
            kind: "CatalogItems",
            type: "CatalogItems",
            result: items,
          };
        }
        break;
      }

      case "CatalogPopular": {
        if (!provider?.getPopular) {
          resultValue = { kind: "CatalogItems", type: "CatalogItems", result: [] };
        } else {
          const items = await provider.getPopular(op.page ?? 1, op.pageSize ?? 20, context);
          resultValue = {
            kind: "CatalogItems",
            type: "CatalogItems",
            result: items,
          };
        }
        break;
      }

      case "CatalogLatest": {
        if (!provider?.getLatest) {
          resultValue = { kind: "CatalogItems", type: "CatalogItems", result: [] };
        } else {
          const items = await provider.getLatest(op.page ?? 1, op.pageSize ?? 20, context);
          resultValue = {
            kind: "CatalogItems",
            type: "CatalogItems",
            result: items,
          };
        }
        break;
      }

      case "MediaSources": {
        if (!provider?.getSources) {
          resultValue = { kind: "VideoSources", type: "VideoSources", result: [] };
        } else {
          const sources = await provider.getSources(op.mediaRef, context, op.episodeNumber);
          resultValue = {
            kind: "VideoSources",
            type: "VideoSources",
            result: sources,
          };
        }
        break;
      }

      case "ResolveEpisode": {
        if (provider?.resolveEpisode) {
          const streams = await provider.resolveEpisode(op.episode, context);
          resultValue = {
            kind: "MediaStreams",
            type: "MediaStreams",
            result: streams,
          };
        } else if (provider?.resolveStream) {
          const streams = await provider.resolveStream(
            op.episode?.providerId ?? "default",
            op.episode?.url ?? "",
            context
          );
          resultValue = {
            kind: "MediaStreams",
            type: "MediaStreams",
            result: streams,
          };
        } else {
          const url = op.episode?.url ?? "";
          const isHls = url.includes(".m3u8");
          const defaultStream = url
            ? [{ url, quality: "Auto", isHls }]
            : [];
          resultValue = {
            kind: "MediaStreams",
            type: "MediaStreams",
            result: defaultStream,
          };
        }
        break;
      }

      case "TorrentSearch":
      case "TorrentSources": {
        const fn = provider?.searchTorrents ?? provider?.getTorrentSources;
        if (!fn) {
          resultValue = { kind: "TorrentSources", type: "TorrentSources", result: [] };
        } else {
          const torrents = await (provider?.searchTorrents
            ? provider.searchTorrents(op.mediaRef, context)
            : provider!.getTorrentSources!(op.mediaRef, op.episodeNumber ?? 1, context));
          resultValue = {
            kind: "TorrentSources",
            type: "TorrentSources",
            result: torrents,
          };
        }
        break;
      }

      case "SubtitleSearch":
      case "Subtitles": {
        if (provider?.searchSubtitles) {
          const subs = await provider.searchSubtitles(
            op.query ?? op.mediaRef?.title ?? "",
            op.languages ?? "ru,en",
            op.episodeNumber ?? null,
            op.seasonNumber ?? null,
            context
          );
          resultValue = {
            kind: "Subtitles",
            type: "Subtitles",
            result: subs,
          };
        } else if (provider?.getSubtitles) {
          const subs = await provider.getSubtitles(op.mediaRef, op.episodeNumber ?? 1, context);
          resultValue = {
            kind: "Subtitles",
            type: "Subtitles",
            result: subs,
          };
        } else {
          resultValue = { kind: "Subtitles", type: "Subtitles", result: [] };
        }
        break;
      }

      case "SubtitleDownloadLink": {
        if (!provider?.getSubtitleDownloadLink) {
          resultValue = { kind: "DownloadLink", type: "DownloadLink", url: null };
        } else {
          const link = await provider.getSubtitleDownloadLink(op.fileId ?? 0, context);
          resultValue = {
            kind: "DownloadLink",
            type: "DownloadLink",
            url: link,
          };
        }
        break;
      }

      case "SkipTimes":
      case "SkipTimings": {
        if (provider?.getSkipTimes) {
          const timings = await provider.getSkipTimes(
            op.mediaRef,
            op.episodeNumber ?? 1,
            op.episodeLength ?? null,
            op.translationName ?? null,
            context
          );
          resultValue = {
            kind: "SkipTimes",
            type: "SkipTimes",
            result: timings,
          };
        } else if (provider?.getSkipTimings) {
          const timings = await provider.getSkipTimings(op.mediaRef, op.episodeNumber ?? 1, context);
          resultValue = {
            kind: "SkipTimes",
            type: "SkipTimes",
            result: timings,
          };
        } else {
          resultValue = { kind: "SkipTimes", type: "SkipTimes", result: [] };
        }
        break;
      }

      case "Poster": {
        if (!provider?.getPoster) {
          resultValue = { kind: "Poster", type: "Poster", url: null };
        } else {
          const posterUrl = await provider.getPoster(op.mediaRef, context);
          resultValue = {
            kind: "Poster",
            type: "Poster",
            url: posterUrl,
          };
        }
        break;
      }

      case "StreamCanExtract": {
        if (!provider?.canExtractStream) {
          resultValue = { kind: "BooleanResult", type: "BooleanResult", value: false };
        } else {
          const canExtract = await provider.canExtractStream(op.extractorName, op.url, context);
          resultValue = {
            kind: "BooleanResult",
            type: "BooleanResult",
            value: canExtract,
          };
        }
        break;
      }

      case "StreamExtract":
      case "ResolveStream": {
        if (provider?.extractStream) {
          const streams = await provider.extractStream(
            op.extractorName,
            op.url,
            op.referer ?? null,
            context
          );
          resultValue = {
            kind: "MediaStreams",
            type: "MediaStreams",
            result: streams,
          };
        } else if (provider?.resolveStream) {
          const streams = await provider.resolveStream(op.extractorName, op.url, context);
          resultValue = {
            kind: "MediaStreams",
            type: "MediaStreams",
            result: streams,
          };
        } else {
          resultValue = { kind: "MediaStreams", type: "MediaStreams", result: [] };
        }
        break;
      }

      case "EpisodeWatched": {
        if (provider?.onEpisodeWatched) {
          await provider.onEpisodeWatched(op.mediaRef, op.episode ?? 1, context);
        }
        resultValue = { kind: "UnitResult", type: "UnitResult" };
        break;
      }

      case "SyncStatus": {
        if (provider?.syncStatus) {
          await provider.syncStatus(op.mediaRef, op.status, context);
        }
        resultValue = { kind: "UnitResult", type: "UnitResult" };
        break;
      }

      // Legacy tracker operation aliases
      case "UserStatus": {
        if (!provider?.getUserMediaStatus) {
          throw new Error("Tracker getUserMediaStatus not implemented");
        }
        const status = await provider.getUserMediaStatus(op.mediaRef, context);
        resultValue = { kind: "UserStatus", type: "UserStatus", result: status };
        break;
      }

      case "UpdateUserStatus": {
        if (!provider?.updateUserMediaStatus) {
          throw new Error("Tracker updateUserMediaStatus not implemented");
        }
        await provider.updateUserMediaStatus(op.mediaRef, op.status, op.progress ?? 0, context);
        resultValue = { kind: "UnitResult", type: "UnitResult" };
        break;
      }

      case "ImportPreview": {
        if (!importer?.preview) {
          throw new Error("Bookmark importer preview not implemented");
        }
        const source = new MemoryBookmarkSource(op.sampleLines ?? []);
        const importCtx: ImportContext = {
          bookmark: (c) => c,
          logger: context.logger,
        };
        const previewResult = await importer.preview(source, importCtx);
        resultValue = {
          kind: "ImportPreview",
          type: "ImportPreviewResult",
          result: previewResult,
        };
        break;
      }

      case "ImportBackup":
      case "ImportReadBatch": {
        if (!importer?.readBookmarks) {
          throw new Error("Bookmark importer readBookmarks not implemented");
        }
        const source = new MemoryBookmarkSource(op.lines ?? op.sampleLines ?? []);
        const importCtx: ImportContext = {
          bookmark: (c) => c,
          logger: context.logger,
        };
        const raw = importer.readBookmarks(source, importCtx);
        const entries: RawImportEntry[] = [];
        if (Symbol.asyncIterator in Object(raw)) {
          for await (const b of raw as AsyncIterable<RawImportEntry>) {
            entries.push(b);
          }
        } else {
          entries.push(...(await (raw as Promise<RawImportEntry[]>)));
        }
        resultValue = {
          kind: "ImportData",
          type: "ImportData",
          result: { sourceName: op.providerId ?? "importer", entries },
        };
        break;
      }

      default:
        throw new Error(`Unsupported operation type: ${opType}`);
    }

    const response: DispatchResponse = {
      kind: "Success",
      type: "Success",
      value: resultValue,
    };
    return JSON.stringify(response);
  } catch (err) {
    const response: DispatchResponse = {
      kind: "Failed",
      type: "Failed",
      code: "PROVIDER_FAILURE",
      message: err instanceof Error ? err.message : String(err),
    };
    return JSON.stringify(response);
  }
}

// Automatically wire global dispatcher for host execution
if (typeof globalThis !== "undefined") {
  globalThis.__zenith_dispatch = handleZenithDispatch;
}
