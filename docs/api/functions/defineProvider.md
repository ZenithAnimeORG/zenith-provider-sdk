[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / defineProvider

# Function: defineProvider()

> **defineProvider**(`definition`): [`ProviderExport`](../interfaces/ProviderExport.md)

Defined in: [provider.ts:630](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/provider.ts#L630)

Entrypoint factory for declaring a Zenith media/catalog/tracker provider module.

Attaches the provider definition to the global execution scope and exports it
as the default export of your bundle.

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `definition` | [`ProviderDefinition`](../interfaces/ProviderDefinition.md) | Implementation of [ProviderDefinition](../interfaces/ProviderDefinition.md) methods. |

## Returns

[`ProviderExport`](../interfaces/ProviderExport.md)

Wrapped [ProviderExport](../interfaces/ProviderExport.md) object.

## Example

```ts
import { defineProvider } from "@zenith/sdk/provider";
import { loadHtml } from "@zenith/sdk/dom";

export default defineProvider({
  async getSearch(query, page, pageSize, context) {
    const mirror = await context.getMirror();
    const res = await context.fetch(`${mirror}/search?q=${encodeURIComponent(query)}`);
    const $ = loadHtml(await res.text());

    const items = $(".item").map((_, el) => ({
      mediaRef: { id: $(el).attr("data-id") || "", title: $(el).find(".title").text() },
      title: $(el).find(".title").text()
    })).get();

    return { items, hasNextPage: false };
  }
});
```
