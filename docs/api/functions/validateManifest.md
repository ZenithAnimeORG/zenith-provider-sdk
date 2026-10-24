[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / validateManifest

# Function: validateManifest()

> **validateManifest**(`manifest`): [`ValidationResult`](../interfaces/ValidationResult.md)

Defined in: [package.ts:138](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/package.ts#L138)

Validates a parsed `manifest.json` against Zenith Manifest V2 rules.

Checks:
- Root object is non-null
- `id` is a valid identifier (1-128 chars `[A-Za-z0-9._-]`)
- `name` and `version` are non-empty strings
- `packageFormat` is `"zpk/v2"` (or legacy `"zpk/v1"`)
- `capabilities` is a non-empty array

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `manifest` | `any` | Raw parsed JSON object. |

## Returns

[`ValidationResult`](../interfaces/ValidationResult.md)

[ValidationResult](../interfaces/ValidationResult.md) with validation status and error list.

## Example

```ts
const result = validateManifest({
  id: "my_provider",
  name: "My Provider",
  version: "1.0.0",
  capabilities: ["CATALOG"]
});
console.log(result.valid); // true
```
