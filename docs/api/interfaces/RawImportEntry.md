[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / RawImportEntry

# Interface: RawImportEntry

Defined in: [models.ts:667](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L667)

Raw media entry extracted during watchlist or bookmark file import.

## Example

```ts
const entry: RawImportEntry = {
  title: "Death Note",
  originalTitle: "Desu Nōto",
  releaseYear: 2006,
  watchedEpisodes: 37,
  totalEpisodes: 37,
  targetStatus: "COMPLETED",
  rating: 9
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="originaltitle"></a> `originalTitle?` | `string` \| `null` | Original Japanese / native title. | [models.ts:676](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L676) |
| <a id="rating"></a> `rating?` | `number` \| `null` | User rating score (1-10). | [models.ts:701](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L701) |
| <a id="releaseyear"></a> `releaseYear?` | `number` \| `null` | Year of release. | [models.ts:681](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L681) |
| <a id="targetstatus"></a> `targetStatus?` | [`UserMediaStatus`](../type-aliases/UserMediaStatus.md) | Watch status to assign in Zenith bookmarks (`"WATCHING"`, `"COMPLETED"`, etc.). | [models.ts:696](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L696) |
| <a id="title"></a> `title` | `string` | Primary title of the media. | [models.ts:671](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L671) |
| <a id="totalepisodes"></a> `totalEpisodes?` | `number` \| `null` | Total episodes in the series. | [models.ts:691](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L691) |
| <a id="watchedepisodes"></a> `watchedEpisodes?` | `number` | Number of episodes the user has watched. | [models.ts:686](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L686) |
