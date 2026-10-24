[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / MediaRef

# Interface: MediaRef

Defined in: [models.ts:82](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L82)

Canonical reference identifying a media item across Zenith providers and external tracking services.

## Example

```ts
const mediaRef: MediaRef = {
  id: "1535",
  title: "Death Note",
  externalIds: {
    mal: "1535",
    shikimori: "1535",
    anilist: "1535"
  },
  alternateTitles: ["Тетрадь смерти", "Desu Nōto"]
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="alternatetitles"></a> `alternateTitles?` | `string`[] | Alternate titles or localized names to improve cross-provider matching. | [models.ts:102](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L102) |
| <a id="externalids"></a> `externalIds?` | `Record`\<`string`, `string`\> | Optional map of recognized external catalog IDs (e.g. `mal`, `shikimori`, `anilist`, `imdb`, `tmdb`, `kinopoisk`). Used by Zenith to link titles across different catalog and video providers. | [models.ts:92](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L92) |
| <a id="id"></a> `id` | `string` | Unique identifier scoped to the provider (e.g. slug, numeric ID, or internal path). | [models.ts:86](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L86) |
| <a id="title"></a> `title?` | `string` | Primary title of the media entry. | [models.ts:97](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L97) |
