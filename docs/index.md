---
layout: home

hero:
  name: "Zenith Provider SDK"
  text: "Portable TypeScript Plugins for Zenith Media Player"
  tagline: "Build fast, secure, cross-platform catalog scrapers, video resolvers, torrent searchers, and subtitle plugins running inside Zenith's isolated QuickJS engine."
  actions:
    - theme: brand
      text: Get Started
      link: /guides/getting-started
    - theme: alt
      text: API Reference
      link: /api/
    - theme: alt
      text: Provider Template
      link: https://github.com/ZenithAnimeORG/zenith-provider-template

features:
  - title: Pure TypeScript & Zero Native Tooling
    details: No Java, Gradle, or Android SDK needed. Authors develop providers with standard TypeScript, Node.js, and esbuild, resulting in lightweight bundles (~50-150 KiB).
  - title: In-Process QuickJS Isolation
    details: Providers run inside a dedicated QuickJS runtime instance with a 64 MiB memory ceiling, thread confinement, and preemptive execution deadlines.
  - title: Secure Host Broker Gateway
    details: All outbound HTTP requests, user preferences, and mirrors are mediated by Zenith's Ktor engine, enforcing manifest host allowlists and redirect safety.
  - title: In-Bundle Cheerio HTML Parsing
    details: Fast DOM queries using jQuery-like syntax via `@zenith/sdk/dom` (powered by Cheerio Slim). Operates 100% in JavaScript memory without host IPC delays.
  - title: Universal .zpk Package Envelope
    details: Packaged into zip-based `.zpk` envelopes that install and execute identically across Android, Linux, macOS, and Windows.
  - title: Rich Capabilities
    details: Implement Catalog search, Direct media streams (HLS/MP4), BitTorrent discovery for jlibtorrent, Subtitle search, and AniSkip timecodes.
---

## Quick Example

Writing a provider is as simple as defining your capabilities and parsing data:

```ts
import { defineProvider } from "@zenith/sdk/provider";
import { loadHtml } from "@zenith/sdk/dom";

export default defineProvider({
  async getSearch(query, page, pageSize, context) {
    const mirror = await context.getMirror();
    const res = await context.fetch(`${mirror}/search?q=${encodeURIComponent(query)}&page=${page}`);
    const $ = loadHtml(await res.text());

    const items = $(".anime-card").map((_, el) => ({
      mediaRef: { id: $(el).attr("data-id") || "", title: $(el).find(".title").text().trim() },
      title: $(el).find(".title").text().trim(),
      posterUrl: $(el).find("img").attr("src"),
      score: parseFloat($(el).attr("data-score") || "0")
    })).get();

    return { items, hasNextPage: false };
  },

  async resolveEpisode(episode, context) {
    return [{
      url: episode.url,
      quality: "1080p",
      isHls: episode.url.includes(".m3u8")
    }];
  }
});
```

Ready to build your first provider? Read the [Getting Started Guide](/guides/getting-started).
