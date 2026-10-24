import { ProviderContext } from "./broker.js";
import { BookmarkSource, ImportContext } from "./bookmark-importer.js";
import { RawImportEntry } from "./models.js";

declare global {
  var __zenith_dispatch: ((requestJson: string) => Promise<string>) | undefined;
}

export interface DispatchResponse {
  type: "Success" | "Failed";
  value?: any;
  code?: string;
  message?: string;
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
    const headers = headerLine.split(",").map((h) => h.trim().replace(/^"|"$/g, ""));
    for (let i = 1; i < this.lines.length; i++) {
      const line = this.lines[i];
      if (!line || !line.trim()) continue;
      const values = line.split(",").map((v) => v.trim().replace(/^"|"$/g, ""));
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
    const opType = (op.type ?? op.operation ?? "").replace(/^.*[.$]/, "");
    const provider = globalThis.__zenith_provider;
    const importer = globalThis.__zenith_bookmark_importer;
    const context = new ProviderContext(op.providerId ?? "quickjs-provider", op.settings ?? {});

    let resultValue: any = null;

    switch (opType) {
      case "Test": {
        if (!provider?.test) {
          resultValue = { type: "Test", result: { success: true, message: "Provider ready" } };
        } else {
          const res = await provider.test(context);
          resultValue = { type: "Test", result: res };
        }
        break;
      }

      case "ProbeStatus": {
        if (!provider?.probeStatus) {
          resultValue = { type: "Status", result: { isOperational: true, message: "OK" } };
        } else {
          const res = await provider.probeStatus(context);
          resultValue = { type: "Status", result: res };
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
        resultValue = { type: "CatalogSearch", result: searchResult };
        break;
      }

      case "CatalogDetails": {
        if (!provider?.getDetails) {
          throw new Error("Catalog details capability not implemented by provider");
        }
        const details = await provider.getDetails(op.mediaRef, context);
        resultValue = { type: "CatalogDetails", result: details };
        break;
      }

      case "CatalogHomeSections": {
        if (!provider?.getHomeSections) {
          resultValue = { type: "CatalogSections", result: [] };
        } else {
          const sections = await provider.getHomeSections(context);
          resultValue = { type: "CatalogSections", result: sections };
        }
        break;
      }

      case "CatalogPopular": {
        if (!provider?.getPopular) {
          resultValue = { type: "CatalogItems", result: [] };
        } else {
          const items = await provider.getPopular(op.page ?? 1, op.pageSize ?? 20, context);
          resultValue = { type: "CatalogItems", result: items };
        }
        break;
      }

      case "CatalogLatest": {
        if (!provider?.getLatest) {
          resultValue = { type: "CatalogItems", result: [] };
        } else {
          const items = await provider.getLatest(op.page ?? 1, op.pageSize ?? 20, context);
          resultValue = { type: "CatalogItems", result: items };
        }
        break;
      }

      case "MediaSources": {
        if (!provider?.getSources) {
          resultValue = { type: "VideoSources", result: [] };
        } else {
          const sources = await provider.getSources(op.mediaRef, op.episodeNumber ?? 1, context);
          resultValue = { type: "VideoSources", result: sources };
        }
        break;
      }

      case "TorrentSources": {
        if (!provider?.getTorrentSources) {
          resultValue = { type: "TorrentSources", result: [] };
        } else {
          const torrents = await provider.getTorrentSources(op.mediaRef, op.episodeNumber ?? 1, context);
          resultValue = { type: "TorrentSources", result: torrents };
        }
        break;
      }

      case "Subtitles": {
        if (!provider?.getSubtitles) {
          resultValue = { type: "Subtitles", result: [] };
        } else {
          const subs = await provider.getSubtitles(op.mediaRef, op.episodeNumber ?? 1, context);
          resultValue = { type: "Subtitles", result: subs };
        }
        break;
      }

      case "SkipTimings": {
        if (!provider?.getSkipTimings) {
          resultValue = { type: "SkipTimes", result: [] };
        } else {
          const timings = await provider.getSkipTimings(op.mediaRef, op.episodeNumber ?? 1, context);
          resultValue = { type: "SkipTimes", result: timings };
        }
        break;
      }

      case "Poster": {
        if (!provider?.getPoster) {
          resultValue = { type: "Poster", url: null };
        } else {
          const posterUrl = await provider.getPoster(op.mediaRef, context);
          resultValue = { type: "Poster", url: posterUrl };
        }
        break;
      }

      case "ResolveStream": {
        if (!provider?.resolveStream) {
          resultValue = { type: "MediaStreams", result: [] };
        } else {
          const streams = await provider.resolveStream(op.extractorName, op.url, context);
          resultValue = { type: "MediaStreams", result: streams };
        }
        break;
      }

      case "UserStatus": {
        if (!provider?.getUserMediaStatus) {
          throw new Error("Tracker getUserMediaStatus not implemented");
        }
        const status = await provider.getUserMediaStatus(op.mediaRef, context);
        resultValue = { type: "UserStatus", result: status };
        break;
      }

      case "UpdateUserStatus": {
        if (!provider?.updateUserMediaStatus) {
          throw new Error("Tracker updateUserMediaStatus not implemented");
        }
        await provider.updateUserMediaStatus(op.mediaRef, op.status, op.progress ?? 0, context);
        resultValue = { type: "UnitResult" };
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
        resultValue = { type: "ImportPreviewResult", result: previewResult };
        break;
      }

      case "ImportReadBatch": {
        if (!importer?.readBookmarks) {
          throw new Error("Bookmark importer readBookmarks not implemented");
        }
        const source = new MemoryBookmarkSource(op.lines ?? []);
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
          type: "ImportData",
          result: { sourceName: op.providerId ?? "importer", entries },
        };
        break;
      }

      default:
        throw new Error(`Unsupported operation type: ${opType}`);
    }

    const response: DispatchResponse = {
      type: "Success",
      value: resultValue,
    };
    return JSON.stringify(response);
  } catch (err) {
    const response: DispatchResponse = {
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
