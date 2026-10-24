[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / BookmarkImporterDefinition

# Interface: BookmarkImporterDefinition

Defined in: [bookmark-importer.ts:107](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L107)

Interface implemented by bookmark and watchlist importer plugins.

Plugins declare the file formats they handle and provide step-by-step instructions
guiding users on how to export their lists from the third-party service.

## Example

```ts
export default defineBookmarkImporter({
  supportedExtensions: [".csv"],
  async preview(source, context) {
    const rows = await source.readHeaderAndSample(5);
    return { format: "csv", rows };
  },
  async *readBookmarks(source, context) {
    for await (const row of source.readCsvRows()) {
      yield context.bookmark({
        title: row["series_title"] || "",
        watchedEpisodes: parseInt(row["my_watched_episodes"] || "0", 10),
        targetStatus: "WATCHING"
      });
    }
  }
});
```

## Properties

| Property | Modifier | Type | Description | Defined in |
| ------ | ------ | ------ | ------ | ------ |
| <a id="instructions"></a> `instructions?` | `readonly` | [`ImportInstructionStep`](ImportInstructionStep.md)[] | User instructions displayed in Zenith UI explaining how to export data from the source website. | [bookmark-importer.ts:116](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L116) |
| <a id="supportedextensions"></a> `supportedExtensions?` | `readonly` | `string`[] | Array of supported file extensions (e.g. `[".csv", ".json", ".xml"]`). | [bookmark-importer.ts:111](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L111) |

## Methods

### preview()

> **preview**(`source`, `context`): `Promise`\<[`ImportPreviewResult`](ImportPreviewResult.md)\>

Defined in: [bookmark-importer.ts:125](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L125)

Generates a preview summary from the input file before the user confirms import.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `source` | [`BookmarkSource`](BookmarkSource.md) | The input file source. |
| `context` | [`ImportContext`](ImportContext.md) | Importer context. |

#### Returns

`Promise`\<[`ImportPreviewResult`](ImportPreviewResult.md)\>

Detected format and sample rows.

***

### readBookmarks()

> **readBookmarks**(`source`, `context`): `AsyncIterable`\<[`RawImportEntry`](RawImportEntry.md), `any`, `any`\> \| `Promise`\<[`RawImportEntry`](RawImportEntry.md)[]\>

Defined in: [bookmark-importer.ts:137](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L137)

Parses the file and streams or returns all imported bookmark entries.

May be implemented as an async generator (`async *readBookmarks`) to stream large files
without memory spikes, or as an async function returning an array.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `source` | [`BookmarkSource`](BookmarkSource.md) | The input file source. |
| `context` | [`ImportContext`](ImportContext.md) | Importer context. |

#### Returns

`AsyncIterable`\<[`RawImportEntry`](RawImportEntry.md), `any`, `any`\> \| `Promise`\<[`RawImportEntry`](RawImportEntry.md)[]\>

Async iterable or array of [RawImportEntry](RawImportEntry.md).
