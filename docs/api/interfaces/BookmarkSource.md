[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / BookmarkSource

# Interface: BookmarkSource

Defined in: [bookmark-importer.ts:19](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L19)

File source interface provided to importers for streaming and reading user uploaded files.

## Methods

### readCsvRows()

> **readCsvRows**(): `AsyncIterable`\<`Record`\<`string`, `string`\>\>

Defined in: [bookmark-importer.ts:42](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L42)

Asynchronously iterates over parsed CSV rows as header-keyed records.

Handles quoted commas, multiline values, and BOM headers automatically.

#### Returns

`AsyncIterable`\<`Record`\<`string`, `string`\>\>

Async iterable of column-keyed row objects.

#### Example

```ts
for await (const row of source.readCsvRows()) {
  console.log(row["series_title"], row["my_status"]);
}
```

***

### readHeaderAndSample()

> **readHeaderAndSample**(`maxLines`): `Promise`\<`string`[]\>

Defined in: [bookmark-importer.ts:26](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L26)

Reads the initial lines of the file for schema detection and preview display.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `maxLines` | `number` | Maximum number of lines to sample. |

#### Returns

`Promise`\<`string`[]\>

Array of raw string lines.

***

### readLines()

> **readLines**(): `AsyncIterable`\<`string`\>

Defined in: [bookmark-importer.ts:49](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L49)

Asynchronously iterates over individual raw text lines from the input file.

#### Returns

`AsyncIterable`\<`string`\>

Async iterable of string lines.

***

### readText()

> **readText**(): `Promise`\<`string`\>

Defined in: [bookmark-importer.ts:58](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L58)

Reads the entire input file into memory as a single UTF-8 string.

Useful for small JSON or XML documents.

#### Returns

`Promise`\<`string`\>

Entire file content string.
