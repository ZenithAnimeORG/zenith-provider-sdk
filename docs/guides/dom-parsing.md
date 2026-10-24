# Cheerio DOM Parsing

Most third-party anime and media sites do not offer clean public JSON APIs. Scrapers must parse HTML pages directly to extract titles, poster images, episode links, and video player embeds.

Zenith provides **`@zenith/sdk/dom`**, an in-bundle HTML parser powered by **Cheerio Slim** (`htmlparser2`).

---

## Why `@zenith/sdk/dom`?

Traditional plugin architectures rely on the host application to run HTML queries (e.g. sending selector queries across an IPC bridge to Kotlin Jsoup) or spin up heavy invisible WebViews.

In Zenith:
- **100% Pure JavaScript**: Cheerio Slim runs directly inside the **QuickJS** heap with zero Node.js native bindings or Buffer dependencies.
- **Zero Host Round-Trips**: All traversal, filtering, and text manipulation executes in microseconds without leaving the JavaScript virtual machine.
- **jQuery-Style Familiarity**: Use the standard `$` syntax familiar to millions of web developers.

---

## Basic Usage

Import `loadHtml` from `@zenith/sdk/dom`:

```ts
import { loadHtml } from "@zenith/sdk/dom";

const response = await context.fetch("https://example.com/catalog");
const html = await response.text();

// Load the HTML string into a Cheerio query instance
const $ = loadHtml(html);

// Query elements using CSS selectors
const pageTitle = $("h1.page-title").text().trim();
const mainCover = $("img.hero-cover").attr("src");
```

---

## Common Scraping Patterns

### 1. Extracting Lists of Cards (`.map().get()`)

When parsing catalog items or search results, use `.map()` combined with `.get()` to return a plain JavaScript array:

```ts
const items = $(".anime-grid .card").map((index, element) => {
  const $card = $(element);

  const id = $card.attr("data-anime-id") || "";
  const title = $card.find(".card-title").text().trim();
  const posterUrl = $card.find("img").attr("data-src") || $card.find("img").attr("src");
  const rawScore = $card.find(".badge-score").text().trim();
  const score = parseFloat(rawScore) || null;

  return {
    mediaRef: { id, title },
    title,
    posterUrl,
    score
  };
}).get();
```

::: tip Why `.get()`?
Cheerio's `.map()` returns a wrapped Cheerio collection. Calling `.get()` unwraps it into a standard JavaScript array (`T[]`) expected by Zenith SDK models.
:::

---

### 2. Resolving Relative URLs

Web pages often use relative paths for images (`/images/cover.jpg`) or episode links (`/watch/123`). Always resolve them against the base URL or mirror:

```ts
function resolveUrl(base: string, path: string | undefined): string | null {
  if (!path) return null;
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  if (path.startsWith("//")) return `https:${path}`;
  const trimmedBase = base.replace(/\/+$/, "");
  const trimmedPath = path.replace(/^\/+/, "");
  return `${trimmedBase}/${trimmedPath}`;
}

const mirror = await context.getMirror();
const posterPath = $("img.poster").attr("src");
const fullPosterUrl = resolveUrl(mirror, posterPath);
```

---

### 3. Extracting Embedded JSON (`__NEXT_DATA__` / Script Tags)

Modern websites built with Next.js, Nuxt, or React often embed server-rendered state directly inside `<script>` tags:

```ts
// Extract Next.js page state
const nextDataScript = $("#__NEXT_DATA__").text();
if (nextDataScript) {
  try {
    const data = JSON.parse(nextDataScript);
    const animeData = data.props?.pageProps?.anime;
    // Work directly with structured JSON!
  } catch (err) {
    context.logger.warn("Failed to parse __NEXT_DATA__ JSON", err);
  }
}
```

Or extracting config objects with regex from script contents:

```ts
let playerUrl = "";
$("script").each((_, script) => {
  const code = $(script).html() || "";
  const match = code.match(/var\s+videoSrc\s*=\s*["']([^"']+)["']/);
  if (match && match[1]) {
    playerUrl = match[1];
  }
});
```

---

### 4. Parsing HTML Tables

When parsing subtitle or torrent trackers that display data in `<table>` elements:

```ts
const rows: TorrentRow[] = [];

$("table.tracker-list tbody tr").each((_, tr) => {
  const $tr = $(tr);
  const title = $tr.find("td.title a").text().trim();
  const magnet = $tr.find("a[href^='magnet:']").attr("href");
  const size = $tr.find("td.size").text().trim();
  const seeders = parseInt($tr.find("td.seeders").text().trim(), 10) || 0;

  if (magnet) {
    rows.push({ title, magnet, size, seeders });
  }
});
```

---

## Memory & Performance Best Practices

1. **Do Not Re-parse the Same HTML**:
   Invoke `loadHtml(html)` once and reuse the resulting `$` instance throughout your capability method.
2. **Never Retain DOM in Global Variables**:
   QuickJS runtimes enforce a **64 MiB** memory ceiling. Storing large Cheerio DOM trees in module-level global variables will cause memory exhaustion. Parse, extract plain data models, and let the DOM be garbage collected.
3. **Safe Null Handling**:
   `loadHtml(null)` or `loadHtml(undefined)` returns a safe empty Cheerio instance rather than throwing an exception.
