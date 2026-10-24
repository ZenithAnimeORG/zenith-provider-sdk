[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ImportContext

# Interface: ImportContext

Defined in: [bookmark-importer.ts:64](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L64)

Execution context supplied to bookmark importers.

## Properties

| Property | Modifier | Type | Description | Defined in |
| ------ | ------ | ------ | ------ | ------ |
| <a id="logger"></a> `logger` | `readonly` | [`ProviderLogger`](ProviderLogger.md) | Structured logger for recording import warnings or unrecognized status codes. | [bookmark-importer.ts:78](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L78) |

## Methods

### bookmark()

> **bookmark**(`entry`): [`RawImportEntry`](RawImportEntry.md)

Defined in: [bookmark-importer.ts:73](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/bookmark-importer.ts#L73)

Normalizes and validates an imported bookmark entry.

Pass each extracted item through this method before yielding.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `entry` | [`RawImportEntry`](RawImportEntry.md) | Raw imported item. |

#### Returns

[`RawImportEntry`](RawImportEntry.md)

Validated entry.
