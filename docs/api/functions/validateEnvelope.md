[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / validateEnvelope

# Function: validateEnvelope()

> **validateEnvelope**(`entries`, `manifest`): [`ValidationResult`](../interfaces/ValidationResult.md)

Defined in: [package.ts:200](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/package.ts#L200)

Validates that a `.zpk` package envelope satisfies physical archive constraints.

Enforces:
- Entry count <= 100 entries ([MAX\_ENTRY\_COUNT](../variables/MAX_ENTRY_COUNT.md))
- Total uncompressed size <= 32 MiB ([MAX\_TOTAL\_UNCOMPRESSED\_BYTES](../variables/MAX_TOTAL_UNCOMPRESSED_BYTES.md))
- Entrypoint size <= 5 MiB ([MAX\_ENTRYPOINT\_BYTES](../variables/MAX_ENTRYPOINT_BYTES.md))
- `manifest.json` size <= 256 KiB ([MAX\_MANIFEST\_BYTES](../variables/MAX_MANIFEST_BYTES.md))
- Path traversal security via [isSafeEntryName](isSafeEntryName.md)
- No banned native/compiled binaries (`.jar`, `.dex`, `.class`, `.so`, `.dll`, `.dylib`, `.exe`, `.apk`) in `zpk/v2`
- Mandatory presence of `manifest.json` and declared entrypoint (e.g. `index.js`)

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `entries` | `object`[] | List of file names and uncompressed sizes in the archive. |
| `manifest` | [`PluginManifest`](../interfaces/PluginManifest.md) | The parsed plugin manifest. |

## Returns

[`ValidationResult`](../interfaces/ValidationResult.md)

[ValidationResult](../interfaces/ValidationResult.md) with validation status and error list.

## Example

```ts
const result = validateEnvelope([
  { name: "manifest.json", size: 1024 },
  { name: "index.js", size: 120000 },
  { name: "icon.png", size: 45000 }
], manifest);

if (!result.valid) {
  console.error("Envelope validation failed:", result.errors);
}
```
