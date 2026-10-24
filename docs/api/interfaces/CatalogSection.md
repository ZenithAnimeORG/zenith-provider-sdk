[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / CatalogSection

# Interface: CatalogSection

Defined in: [models.ts:275](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L275)

Curated section / carousel rendered on the Zenith home screen.

## Example

```ts
const section: CatalogSection = {
  title: "Popular This Season",
  items: [item1, item2, item3]
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="items"></a> `items` | [`CatalogMediaItem`](CatalogMediaItem.md)[] | Media items displayed horizontally in this carousel. | [models.ts:284](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L284) |
| <a id="title"></a> `title` | `string` | Localized title of the section header (e.g. "Trending Now"). | [models.ts:279](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L279) |
