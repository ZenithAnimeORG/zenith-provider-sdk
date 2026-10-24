# Testing & Fixtures

External media websites frequently update their frontend markup, change CSS class names, or block cloud CI runners with Cloudflare challenges.

To ensure your provider remains robust, dependable, and error-free, the Zenith Provider SDK emphasizes **deterministic unit testing using saved HTML fixtures** with **Vitest**.

---

## Why Offline Fixtures?

- **Zero Network Flakiness**: Tests run entirely offline in milliseconds without relying on live site uptime or risking IP bans.
- **Continuous Integration Ready**: Run `npm test` on GitHub Actions reliably on every pull request.
- **Edge-Case Simulation**: Test rare cases (e.g. titles without posters, unreleased seasons, specials, empty search queries) that are hard to trigger live.

---

## Capturing & Organizing Fixtures

Save raw HTML or JSON responses from your target website directly into the `test/fixtures/` directory:

```
test/
├── fixtures/
│   ├── search_frieren.html     # Real HTML response for "frieren"
│   ├── search_empty.html       # Response for a query with no results
│   ├── details_series.html     # Details page for a multi-season series
│   └── episodes.json           # JSON response containing stream URLs
└── provider.test.ts            # Vitest test suite
```

### Saving Fixtures with cURL

You can quickly capture fixtures using `curl`:

```bash
curl -s "https://example.com/search?q=frieren" > test/fixtures/search_frieren.html
curl -s "https://example.com/anime/frieren" > test/fixtures/details_series.html
```

---

## Writing Tests with Vitest

In your `test/provider.test.ts`, use Vitest to mock `context.fetch()` and test your provider methods against the captured fixtures.

### Complete Example

```ts
import { describe, it, expect, vi } from "vitest";
import fs from "node:fs";
import path from "node:path";
import providerExport from "../src/index.js";
import { ProviderContext } from "@zenith/sdk/broker";

const provider = providerExport.definition;

// Helper to load fixture files as strings
function loadFixture(filename: string): string {
  return fs.readFileSync(path.join(__dirname, "fixtures", filename), "utf-8");
}

describe("My Anime Provider Tests", () => {
  it("parses catalog search results accurately", async () => {
    const searchHtml = loadFixture("search_frieren.html");

    // Create a context and mock context.fetch()
    const context = new ProviderContext("test-provider", {
      mirror: "https://api.example.com"
    });

    vi.spyOn(context, "fetch").mockResolvedValueOnce({
      status: 200,
      statusText: "OK",
      ok: true,
      headers: new (await import("@zenith/sdk/broker")).FetchHeaders(),
      text: async () => searchHtml,
      json: async () => JSON.parse(searchHtml)
    } as any);

    const result = await provider.getSearch!("frieren", 1, 20, context);

    expect(result.items.length).toBeGreaterThan(0);

    const firstItem = result.items[0];
    expect(firstItem.mediaRef.id).toBeTruthy();
    expect(firstItem.title).toContain("Frieren");
    expect(firstItem.posterUrl).toMatch(/^https?:\/\//);
    expect(firstItem.score).toBeGreaterThan(8.0);
    expect(result.hasNextPage).toBe(true);
  });

  it("handles empty search results gracefully without crashing", async () => {
    const emptyHtml = loadFixture("search_empty.html");
    const context = new ProviderContext("test-provider");

    vi.spyOn(context, "fetch").mockResolvedValueOnce({
      status: 200,
      statusText: "OK",
      ok: true,
      headers: new (await import("@zenith/sdk/broker")).FetchHeaders(),
      text: async () => emptyHtml,
      json: async () => ({})
    } as any);

    const result = await provider.getSearch!("nonexistent_anime_xyz", 1, 20, context);

    expect(result.items).toEqual([]);
    expect(result.hasNextPage).toBe(false);
  });

  it("extracts full media details correctly", async () => {
    const detailsHtml = loadFixture("details_series.html");
    const context = new ProviderContext("test-provider");

    vi.spyOn(context, "fetch").mockResolvedValueOnce({
      status: 200,
      statusText: "OK",
      ok: true,
      headers: new (await import("@zenith/sdk/broker")).FetchHeaders(),
      text: async () => detailsHtml,
      json: async () => ({})
    } as any);

    const details = await provider.getDetails!(
      { id: "123", title: "Frieren" },
      context
    );

    expect(details.title).toBe("Frieren: Beyond Journey's End");
    expect(details.description).toBeTruthy();
    expect(details.episodesCount).toBe(28);
    expect(details.genres).toContain("Fantasy");
    expect(details.status).toBe("FINISHED");
  });
});
```

---

## Defensive Parsing & Edge Cases

Under Zenith's **Zero-Crash Policy**, a provider must never crash on malformed external HTML. Always write tests for the following edge cases:

### 1. Missing Poster Images
Ensure missing images return `null` or a fallback rather than throwing a TypeError:

```ts
const poster = $el.find("img.poster").attr("src") || null;
```

### 2. Invalid Scores or Numbers
Never call `parseFloat()` directly on untrusted text without a fallback:

```ts
const rawScore = $el.find(".score").text().trim();
const score = Number.isNaN(parseFloat(rawScore)) ? null : parseFloat(rawScore);
```

### 3. Missing Episodes or Sources
If a title has no available video sources, return an empty array `[]` rather than throwing:

```ts
async getSources(mediaRef, context) {
  const sources = await parseSources();
  return sources ?? [];
}
```

---

## Running Your Tests

Run the test suite from your terminal:

```bash
# Run all unit tests once
npm test

# Run tests in watch mode during development
npx vitest
```
