[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / defineBookmarkImporter

# Function: defineBookmarkImporter()

> **defineBookmarkImporter**(`definition`): [`BookmarkImporterExport`](../interfaces/BookmarkImporterExport.md)

Defined in: [bookmark-importer.ts:192](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L192)

Entrypoint factory for declaring a Zenith bookmark/watchlist importer module.

Attaches the importer definition to the global execution scope and exports it
as the default export of your bundle.

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `definition` | [`BookmarkImporterDefinition`](../interfaces/BookmarkImporterDefinition.md) | Implementation of [BookmarkImporterDefinition](../interfaces/BookmarkImporterDefinition.md). |

## Returns

[`BookmarkImporterExport`](../interfaces/BookmarkImporterExport.md)

Wrapped [BookmarkImporterExport](../interfaces/BookmarkImporterExport.md) object.

## Example

```ts
import { defineBookmarkImporter } from "@zenith/sdk/bookmark-importer";

export default defineBookmarkImporter({
  supportedExtensions: [".csv"],
  async preview(source, context) {
    return { format: "csv", rows: await source.readHeaderAndSample(10) };
  },
  async *readBookmarks(source, context) {
    for await (const row of source.readCsvRows()) {
      yield context.bookmark({
        title: row.title ?? "",
        targetStatus: "WATCHING",
        watchedEpisodes: Number(row.episodes ?? 0)
      });
    }
  }
});
```
