[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / CatalogMediaItem

# Interface: CatalogMediaItem

Defined in: [models.ts:121](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L121)

Summary representation of a media entry displayed in search results, carousels, and catalog lists.

## Example

```ts
const item: CatalogMediaItem = {
  mediaRef: { id: "frieren-2023", title: "Frieren: Beyond Journey's End" },
  title: "Frieren: Beyond Journey's End",
  originalTitle: "Sousou no Frieren",
  posterUrl: "https://example.com/posters/frieren.jpg",
  score: 9.14,
  year: 2023,
  totalEpisodes: 28
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="mediaref"></a> `mediaRef` | [`MediaRef`](MediaRef.md) | Canonical media reference for navigating to detailed views and video playback. | [models.ts:125](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L125) |
| <a id="originaltitle"></a> `originalTitle?` | `string` \| `null` | Original Japanese/native title, if available. | [models.ts:135](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L135) |
| <a id="posterurl"></a> `posterUrl?` | `string` \| `null` | Direct HTTPS URL pointing to the poster image. | [models.ts:140](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L140) |
| <a id="score"></a> `score?` | `number` \| `null` | User rating or score (typically on a 0.0 to 10.0 scale). | [models.ts:145](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L145) |
| <a id="title"></a> `title` | `string` | Display title rendered in the UI card. | [models.ts:130](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L130) |
| <a id="totalepisodes"></a> `totalEpisodes?` | `number` \| `null` | Total number of episodes planned or released. | [models.ts:155](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L155) |
| <a id="year"></a> `year?` | `number` \| `null` | Release year of the media. | [models.ts:150](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L150) |
