[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / CatalogMediaDetails

# Interface: CatalogMediaDetails

Defined in: [models.ts:179](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L179)

Detailed metadata of a media title displayed on the full details screen.

## Example

```ts
const details: CatalogMediaDetails = {
  mediaRef: { id: "frieren", title: "Frieren" },
  title: "Frieren: Beyond Journey's End",
  originalTitle: "Sousou no Frieren",
  description: "An elf mage embarks on a journey to understand humanity after defeating the Demon King.",
  posterUrl: "https://example.com/posters/frieren.jpg",
  bannerUrl: "https://example.com/banners/frieren.jpg",
  score: 9.14,
  year: 2023,
  episodesCount: 28,
  genres: ["Adventure", "Drama", "Fantasy"],
  studios: ["Madhouse"],
  status: "FINISHED"
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="bannerurl"></a> `bannerUrl?` | `string` \| `null` | Wide backdrop / hero banner image URL. | [models.ts:208](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L208) |
| <a id="description"></a> `description?` | `string` \| `null` | Full synopsis / plot description. | [models.ts:198](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L198) |
| <a id="episodescount"></a> `episodesCount?` | `number` \| `null` | Total number of episodes. | [models.ts:223](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L223) |
| <a id="genres"></a> `genres?` | `string`[] | List of genres associated with this title. | [models.ts:228](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L228) |
| <a id="mediaref"></a> `mediaRef` | [`MediaRef`](MediaRef.md) | Canonical media reference. | [models.ts:183](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L183) |
| <a id="originaltitle"></a> `originalTitle?` | `string` \| `null` | Original / Romaji title. | [models.ts:193](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L193) |
| <a id="posterurl"></a> `posterUrl?` | `string` \| `null` | High-resolution poster image URL. | [models.ts:203](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L203) |
| <a id="score"></a> `score?` | `number` \| `null` | Community score / rating (e.g. 8.95). | [models.ts:213](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L213) |
| <a id="status"></a> `status?` | `string` \| `null` | Broadcast status: `"AIRING"`, `"FINISHED"`, `"UPCOMING"`, or `"CANCELLED"`. | [models.ts:238](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L238) |
| <a id="studios"></a> `studios?` | `string`[] | Animation studios or production companies. | [models.ts:233](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L233) |
| <a id="title"></a> `title` | `string` | Primary title in user language. | [models.ts:188](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L188) |
| <a id="year"></a> `year?` | `number` \| `null` | Release year. | [models.ts:218](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L218) |
