[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / isSafeEntryName

# Function: isSafeEntryName()

> **isSafeEntryName**(`name`): `boolean`

Defined in: [package.ts:103](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/package.ts#L103)

Checks whether an entry filename is safe and free of directory traversal vulnerabilities.

Rejects absolute paths (`/foo`), path parent traversals (`..`), backslashes, double slashes,
and characters outside alphanumeric and `_-./`.

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `name` | `string` | File path within the ZIP archive. |

## Returns

`boolean`

`true` if the path is safe to extract or inspect, otherwise `false`.

## Example

```ts
isSafeEntryName("manifest.json");       // true
isSafeEntryName("icons/icon.png");       // true
isSafeEntryName("../secret.txt");        // false (traversal)
isSafeEntryName("/etc/passwd");          // false (absolute path)
```
