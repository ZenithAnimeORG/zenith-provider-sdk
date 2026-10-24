[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderMediaStream

# Interface: ProviderMediaStream

Defined in: [models.ts:383](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L383)

Direct video stream playable by Zenith's internal MPV media player engine.

## Example

```ts
const stream: ProviderMediaStream = {
  url: "https://cdn.example.com/video/master.m3u8",
  quality: "1080p",
  isHls: true,
  headers: {
    "Referer": "https://embed.example.com/",
    "User-Agent": "Mozilla/5.0"
  }
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="headers"></a> `headers?` | `Record`\<`string`, `string`\> | Custom HTTP headers passed directly to MPV (e.g. `Referer`, `Cookie`, `User-Agent`). | [models.ts:397](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L397) |
| <a id="ishls"></a> `isHls?` | `boolean` | `true` if the stream is an HLS (HTTP Live Streaming) adaptive playlist. | [models.ts:402](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L402) |
| <a id="quality"></a> `quality?` | `string` \| `null` | Human-readable resolution or quality label (e.g. `"1080p"`, `"720p"`, `"4K"`, `"Auto"`). | [models.ts:392](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L392) |
| <a id="url"></a> `url` | `string` | Direct media URL (e.g. HLS `.m3u8`, MP4, or DASH manifest). | [models.ts:387](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L387) |
