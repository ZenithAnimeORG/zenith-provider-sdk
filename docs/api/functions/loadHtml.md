[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / loadHtml

# Function: loadHtml()

> **loadHtml**(`html`, `options?`): `CheerioAPI`

Defined in: [dom.ts:59](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/main/sdk-ts/src/dom.ts#L59)

Loads an HTML document string into a Cheerio DOM querying instance.

Runs entirely inside the QuickJS virtual machine without any Node.js native dependencies.
Supports standard jQuery-like selector syntax: `$('div.item').map(...)`.

## Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `html` | `string` \| `null` \| `undefined` | The raw HTML string to parse. Handles `null` or `undefined` safely by loading an empty document. |
| `options?` | `any` | Optional Cheerio parser options (such as XML mode or decode entities). |

## Returns

`CheerioAPI`

Cheerio API instance rooted at the parsed HTML.

## Examples

```ts
import { loadHtml } from "@zenith/sdk/dom";

const html = `
  <div class="anime-card" data-id="123" data-score="8.5">
    <h2 class="title">Frieren</h2>
    <img class="poster" src="https://example.com/cover.jpg" />
  </div>
`;

const $ = loadHtml(html);
const title = $(".title").text().trim();
const id = $(".anime-card").attr("data-id");
const poster = $("img.poster").attr("src");
```

**Iterating over collections with map and get**

```ts
const items = $(".anime-card").map((_, el) => {
  const $el = $(el);
  return {
    id: $el.attr("data-id") || "",
    title: $el.find(".title").text().trim(),
    score: parseFloat($el.attr("data-score") || "0")
  };
}).get();
```
