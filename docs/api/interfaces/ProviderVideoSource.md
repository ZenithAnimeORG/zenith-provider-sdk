[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderVideoSource

# Interface: ProviderVideoSource

Defined in: [models.ts:340](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L340)

Video release or translation source containing a list of episodes.

Represents an audio track, dubbing studio, or release group (e.g. "AniLibria", "Crunchyroll", "Subtitles").

## Example

```ts
const source: ProviderVideoSource = {
  name: "AniLibria (Dub)",
  translationName: "AniLibria",
  translationType: "DUB",
  episodes: [
    { number: 1, url: "https://example.com/ep1" },
    { number: 2, url: "https://example.com/ep2" }
  ]
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="episodes"></a> `episodes` | [`ProviderEpisode`](ProviderEpisode.md)[] | List of available episodes in this source. | [models.ts:359](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L359) |
| <a id="name"></a> `name` | `string` | Display name of the source (e.g. "AniLibria", "Subtitles - Official"). | [models.ts:344](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L344) |
| <a id="qualities"></a> `qualities?` | `Record`\<`string`, `string`\> \| `null` | Optional dictionary of available quality levels. | [models.ts:364](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L364) |
| <a id="translationname"></a> `translationName?` | `string` | Specific dubbing team or studio name. | [models.ts:349](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L349) |
| <a id="translationtype"></a> `translationType?` | [`ProviderTranslationType`](../type-aliases/ProviderTranslationType.md) | Type of translation (`"DUB"`, `"VO"`, or `"SUB"`). | [models.ts:354](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L354) |
