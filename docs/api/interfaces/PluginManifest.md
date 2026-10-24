[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / PluginManifest

# Interface: PluginManifest

Defined in: [models.ts:845](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L845)

Zenith Provider Manifest V2 (`manifest.json`) metadata specification.

Every `.zpk` package must contain a root `manifest.json` conforming to this schema.

## Example

```json
{
  "id": "anime_sample",
  "name": "Sample Anime Provider",
  "version": "1.0.0",
  "sdkVersion": "0.1.0",
  "packageFormat": "zpk/v2",
  "moduleApiVersion": "1.0.0",
  "entrypoint": "index.js",
  "author": "Zenith Author",
  "description": "Fast anime catalog and stream provider",
  "capabilities": ["CATALOG", "MEDIA_SOURCE"],
  "hosts": ["api.example.com", "*.cdn.example.com"]
}
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="author"></a> `author?` | `string` | Name or handle of the author/maintainer. | [models.ts:889](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L889) |
| <a id="capabilities"></a> `capabilities` | [`ProviderCapability`](../type-aliases/ProviderCapability.md)[] | List of capabilities implemented by this provider plugin. | [models.ts:904](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L904) |
| <a id="cons"></a> `cons?` | `string`[] | Known limitations/cons shown in the Provider Store details card. | [models.ts:935](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L935) |
| <a id="description"></a> `description?` | `string` | Short description of provider content and features. | [models.ts:884](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L884) |
| <a id="entry"></a> `entry?` | [`PluginArtifactEntry`](PluginArtifactEntry.md) | Optional platform entrypoint mappings. | [models.ts:915](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L915) |
| <a id="entryclass"></a> `entryClass?` | `string` | Legacy class entrypoint name. | [models.ts:920](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L920) |
| <a id="entrypoint"></a> `entrypoint?` | `string` | Relative filename of the bundled JavaScript entrypoint inside the archive (defaults to `"index.js"`). | [models.ts:879](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L879) |
| <a id="homepage"></a> `homepage?` | `string` | URL to source repository or documentation website. | [models.ts:894](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L894) |
| <a id="hosts"></a> `hosts?` | `string`[] | Host allowlist for network requests. Zenith Host Broker blocks requests to domains not matching this list. Supports wildcards (e.g. `"*.example.com"`). | [models.ts:910](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L910) |
| <a id="icon"></a> `icon?` | `string` | Filename of the square 512x512 PNG icon inside the archive (e.g. `"icon.png"`). | [models.ts:899](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L899) |
| <a id="id"></a> `id` | `string` | Unique alphanumeric identifier (1-128 chars, may include `.` `_` `-`). | [models.ts:849](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L849) |
| <a id="localizeddescriptions"></a> `localizedDescriptions?` | `Record`\<`string`, `string`\> | Map of ISO language codes to localized descriptions. | [models.ts:955](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L955) |
| <a id="localizednames"></a> `localizedNames?` | `Record`\<`string`, `string`\> | Map of ISO language codes to localized provider names (e.g. `{"ru": "Название", "en": "Name"}`). | [models.ts:950](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L950) |
| <a id="mirrors"></a> `mirrors?` | `object`[] | Predefined mirror servers. | [models.ts:945](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L945) |
| <a id="moduleapiversion"></a> `moduleApiVersion?` | `string` | Runtime module API version (e.g. `"1.0.0"`). | [models.ts:874](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L874) |
| <a id="name"></a> `name` | `string` | Human-readable display name. | [models.ts:854](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L854) |
| <a id="packageformat"></a> `packageFormat?` | `string` | Package envelope format version: must be `"zpk/v2"` for portable TypeScript providers. | [models.ts:869](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L869) |
| <a id="pros"></a> `pros?` | `string`[] | Highlighted advantages/pros shown in the Provider Store details card. | [models.ts:930](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L930) |
| <a id="sdkversion"></a> `sdkVersion?` | `string` | Zenith SDK version the provider was built against (e.g. `"0.1.0"`). | [models.ts:864](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L864) |
| <a id="settings"></a> `settings?` | `string` | Relative filename of the declarative `settings.json` file inside the archive. | [models.ts:925](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L925) |
| <a id="settingsschema"></a> `settingsSchema?` | [`SettingItemSpec`](SettingItemSpec.md)[] | Inlined settings schema array. | [models.ts:940](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L940) |
| <a id="version"></a> `version` | `string` | Semantic version string (e.g. `"1.0.0"`). | [models.ts:859](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L859) |
