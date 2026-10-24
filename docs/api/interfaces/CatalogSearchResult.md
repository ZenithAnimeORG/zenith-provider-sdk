[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / CatalogSearchResult

# Interface: CatalogSearchResult

Defined in: [models.ts:252](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L252)

Paginated response returned by catalog search queries.

## Example

```ts
const result: CatalogSearchResult = {
  items: [mediaItem1, mediaItem2],
  hasNextPage: true
};
```

## Properties

| Property | Type | Description | Defined in |
| ------ | ------ | ------ | ------ |
| <a id="hasnextpage"></a> `hasNextPage` | `boolean` | Indicates whether subsequent pages of results exist. | [models.ts:261](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L261) |
| <a id="items"></a> `items` | [`CatalogMediaItem`](CatalogMediaItem.md)[] | List of media items found on the current page. | [models.ts:256](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/models.ts#L256) |
