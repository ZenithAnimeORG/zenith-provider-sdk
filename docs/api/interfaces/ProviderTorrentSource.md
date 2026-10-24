[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderTorrentSource

# Interface: ProviderTorrentSource

Defined in: [models.ts:422](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L422)

BitTorrent release metadata for direct peer-to-peer torrent streaming via `jlibtorrent`.

## Example

```ts
const torrent: ProviderTorrentSource = {
  title: "[Erai-raws] Sousou no Frieren - 01 [1080p][HEVC]",
  infoHash: "4b98c36671c6bc1b8a5b28d7a31b4021be07ef02",
  magnetUri: "magnet:?xt=urn:btih:4b98c366...",
  size: 1450000000,
  seeders: 124,
  leechers: 12,
  quality: "1080p",
  trackerName: "Nyaa"
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="infohash"></a> `infoHash?` | `string` \| `null` | SHA-1 hex-encoded info-hash (40 characters). | [models.ts:431](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L431) |
| <a id="leechers"></a> `leechers?` | `number` | Number of active leechers currently downloading the release. | [models.ts:451](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L451) |
| <a id="magneturi"></a> `magnetUri?` | `string` \| `null` | Full `magnet:` URI including tracker parameters. | [models.ts:436](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L436) |
| <a id="quality"></a> `quality?` | `string` | Video quality string (e.g. `"1080p"`, `"720p"`). | [models.ts:456](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L456) |
| <a id="seeders"></a> `seeders?` | `number` | Number of active seeders currently sharing the release. | [models.ts:446](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L446) |
| <a id="size"></a> `size?` | `number` | Total size of the torrent release in bytes. | [models.ts:441](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L441) |
| <a id="title"></a> `title` | `string` | Release title or filename. | [models.ts:426](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L426) |
| <a id="trackername"></a> `trackerName?` | `string` | Name of the tracker (e.g. `"Nyaa"`, `"RuTracker"`). | [models.ts:461](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L461) |
| <a id="translationname"></a> `translationName?` | `string` | Release group or translation team. | [models.ts:466](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L466) |
| <a id="translationtype"></a> `translationType?` | [`ProviderTranslationType`](../type-aliases/ProviderTranslationType.md) | Translation type (`"DUB"`, `"VO"`, or `"SUB"`). | [models.ts:471](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L471) |
