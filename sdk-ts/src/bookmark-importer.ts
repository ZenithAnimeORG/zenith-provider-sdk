import {
  ImportInstructionStep,
  ImportPreviewResult,
  RawImportEntry,
} from "./models.js";
import { ProviderLogger } from "./broker.js";

export interface BookmarkSource {
  readHeaderAndSample(maxLines: number): Promise<string[]>;
  readCsvRows(): AsyncIterable<Record<string, string>>;
  readLines(): AsyncIterable<string>;
  readText(): Promise<string>;
}

export interface ImportContext {
  bookmark(entry: RawImportEntry): RawImportEntry;
  readonly logger: ProviderLogger;
}

export interface BookmarkImporterDefinition {
  readonly supportedExtensions?: string[];
  readonly instructions?: ImportInstructionStep[];

  preview(source: BookmarkSource, context: ImportContext): Promise<ImportPreviewResult>;

  readBookmarks(
    source: BookmarkSource,
    context: ImportContext
  ): AsyncIterable<RawImportEntry> | Promise<RawImportEntry[]>;
}

export interface BookmarkImporterExport {
  readonly type: "bookmark-importer";
  readonly definition: BookmarkImporterDefinition;
}

declare global {
  var __zenith_bookmark_importer: BookmarkImporterDefinition | undefined;
}

/**
 * Entrypoint factory for declaring a Zenith bookmark/watchlist importer module.
 *
 * Example:
 * ```ts
 * export default defineBookmarkImporter({
 *   async preview(source, context) {
 *     return { format: "csv", rows: await source.readHeaderAndSample(10) };
 *   },
 *   async *readBookmarks(source, context) {
 *     for await (const row of source.readCsvRows()) {
 *       yield context.bookmark({
 *         title: row.title ?? "",
 *         targetStatus: "WATCHING",
 *         watchedEpisodes: Number(row.episodes ?? 0)
 *       });
 *     }
 *   }
 * });
 * ```
 */
export function defineBookmarkImporter(
  definition: BookmarkImporterDefinition
): BookmarkImporterExport {
  if (typeof globalThis !== "undefined") {
    globalThis.__zenith_bookmark_importer = definition;
  }
  return {
    type: "bookmark-importer",
    definition,
  };
}
