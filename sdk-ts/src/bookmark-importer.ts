/**
 * Watchlist and bookmark importer module contracts for migrating user lists from external services.
 *
 * Importers parse exported CSV, JSON, or XML files and feed standardized entries into Zenith bookmarks.
 *
 * @module bookmark-importer
 */

import {
  ImportInstructionStep,
  ImportPreviewResult,
  RawImportEntry,
} from "./models.js";
import { ProviderLogger } from "./broker.js";

/**
 * File source interface provided to importers for streaming and reading user uploaded files.
 */
export interface BookmarkSource {
  /**
   * Reads the initial lines of the file for schema detection and preview display.
   *
   * @param maxLines Maximum number of lines to sample.
   * @returns Array of raw string lines.
   */
  readHeaderAndSample(maxLines: number): Promise<string[]>;

  /**
   * Asynchronously iterates over parsed CSV rows as header-keyed records.
   *
   * Handles quoted commas, multiline values, and BOM headers automatically.
   *
   * @returns Async iterable of column-keyed row objects.
   *
   * @example
   * ```ts
   * for await (const row of source.readCsvRows()) {
   *   console.log(row["series_title"], row["my_status"]);
   * }
   * ```
   */
  readCsvRows(): AsyncIterable<Record<string, string>>;

  /**
   * Asynchronously iterates over individual raw text lines from the input file.
   *
   * @returns Async iterable of string lines.
   */
  readLines(): AsyncIterable<string>;

  /**
   * Reads the entire input file into memory as a single UTF-8 string.
   *
   * Useful for small JSON or XML documents.
   *
   * @returns Entire file content string.
   */
  readText(): Promise<string>;
}

/**
 * Execution context supplied to bookmark importers.
 */
export interface ImportContext {
  /**
   * Normalizes and validates an imported bookmark entry.
   *
   * Pass each extracted item through this method before yielding.
   *
   * @param entry Raw imported item.
   * @returns Validated entry.
   */
  bookmark(entry: RawImportEntry): RawImportEntry;

  /**
   * Structured logger for recording import warnings or unrecognized status codes.
   */
  readonly logger: ProviderLogger;
}

/**
 * Interface implemented by bookmark and watchlist importer plugins.
 *
 * Plugins declare the file formats they handle and provide step-by-step instructions
 * guiding users on how to export their lists from the third-party service.
 *
 * @example
 * ```ts
 * export default defineBookmarkImporter({
 *   supportedExtensions: [".csv"],
 *   async preview(source, context) {
 *     const rows = await source.readHeaderAndSample(5);
 *     return { format: "csv", rows };
 *   },
 *   async *readBookmarks(source, context) {
 *     for await (const row of source.readCsvRows()) {
 *       yield context.bookmark({
 *         title: row["series_title"] || "",
 *         watchedEpisodes: parseInt(row["my_watched_episodes"] || "0", 10),
 *         targetStatus: "WATCHING"
 *       });
 *     }
 *   }
 * });
 * ```
 */
export interface BookmarkImporterDefinition {
  /**
   * Array of supported file extensions (e.g. `[".csv", ".json", ".xml"]`).
   */
  readonly supportedExtensions?: string[];

  /**
   * User instructions displayed in Zenith UI explaining how to export data from the source website.
   */
  readonly instructions?: ImportInstructionStep[];

  /**
   * Generates a preview summary from the input file before the user confirms import.
   *
   * @param source The input file source.
   * @param context Importer context.
   * @returns Detected format and sample rows.
   */
  preview(source: BookmarkSource, context: ImportContext): Promise<ImportPreviewResult>;

  /**
   * Parses the file and streams or returns all imported bookmark entries.
   *
   * May be implemented as an async generator (`async *readBookmarks`) to stream large files
   * without memory spikes, or as an async function returning an array.
   *
   * @param source The input file source.
   * @param context Importer context.
   * @returns Async iterable or array of {@link RawImportEntry}.
   */
  readBookmarks(
    source: BookmarkSource,
    context: ImportContext
  ): AsyncIterable<RawImportEntry> | Promise<RawImportEntry[]>;
}

/**
 * Export wrapper produced by {@link defineBookmarkImporter}.
 */
export interface BookmarkImporterExport {
  /**
   * Export discriminator indicating this module is a bookmark importer.
   */
  readonly type: "bookmark-importer";

  /**
   * The wrapped bookmark importer implementation.
   */
  readonly definition: BookmarkImporterDefinition;
}

declare global {
  var __zenith_bookmark_importer: BookmarkImporterDefinition | undefined;
}

/**
 * Entrypoint factory for declaring a Zenith bookmark/watchlist importer module.
 *
 * Attaches the importer definition to the global execution scope and exports it
 * as the default export of your bundle.
 *
 * @param definition Implementation of {@link BookmarkImporterDefinition}.
 * @returns Wrapped {@link BookmarkImporterExport} object.
 *
 * @example
 * ```ts
 * import { defineBookmarkImporter } from "@zenith/sdk/bookmark-importer";
 *
 * export default defineBookmarkImporter({
 *   supportedExtensions: [".csv"],
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
