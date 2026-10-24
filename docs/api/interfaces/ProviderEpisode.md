[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderEpisode

# Interface: ProviderEpisode

Defined in: [models.ts:300](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L300)

Individual episode descriptor within a video or audio source.

## Example

```ts
const episode: ProviderEpisode = {
  number: 1,
  url: "https://example.com/watch/frieren-ep1",
  quality: { "1080p": "https://cdn.example.com/1080.m3u8" },
  providerId: "my_provider"
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="number"></a> `number` | `number` | Episode sequence number (1-indexed; may be fractional for specials like 1.5). | [models.ts:304](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L304) |
| <a id="providerid"></a> `providerId?` | `string` \| `null` | Optional provider identifier that originated this episode. | [models.ts:319](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L319) |
| <a id="quality"></a> `quality?` | `Record`\<`string`, `string`\> \| `null` | Optional mapping of pre-resolved quality strings to stream URLs. | [models.ts:314](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L314) |
| <a id="url"></a> `url` | `string` | Provider-specific URL, stream reference, or payload needed to resolve playback. | [models.ts:309](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L309) |
