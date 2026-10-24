# Getting Started

Welcome to the **Zenith Provider SDK** documentation!

Zenith media player plugins (called **providers**) allow the community to integrate anime and media catalogs, video stream sources, BitTorrent releases, subtitles, and skip timecodes directly into the Zenith user interface.

Unlike traditional Android media apps that require Java/Kotlin source compilation, Android SDK installations, and complex Gradle builds, **Zenith providers are 100% portable TypeScript plugins**. They compile to a single self-contained JavaScript bundle executed by Zenith's high-performance, sandboxed **QuickJS** runtime engine.

---

## Key Highlights

- **Zero Native Dependencies**: You do not need Java, Gradle, NDK, or the Android SDK installed. A standard Node.js environment is all you need.
- **Single Cross-Platform Package**: A single `.zpk` (Zenith Package) file runs identically on **Android**, **Linux**, **macOS**, and **Windows**.
- **In-Process Performance**: Providers execute in-process within QuickJS with a **64 MiB** memory ceiling and zero IPC serialization latency.
- **Security-First Architecture**: Network requests, cookies, and preferences are mediated by Zenith's native host broker; arbitrary filesystem or command execution is blocked.

---

## Prerequisites

Before creating your first provider, ensure you have:

- **Node.js**: v20 or higher (v22 LTS recommended)
- **npm** (v10+) or **pnpm** (v9+)
- A code editor (such as VS Code or WebStorm)

---

## Creating Your First Provider

The fastest way to create a provider is using the official [`zenith-provider-template`](https://github.com/ZenithAnimeORG/zenith-provider-template).

### 1. Initialize from Template

1. Navigate to [`zenith-provider-template`](https://github.com/ZenithAnimeORG/zenith-provider-template) on GitHub.
2. Click **Use this template** -> **Create a new repository**.
3. Name your repository (e.g. `zenith-provider-mysite`).
4. Clone your repository locally:

```bash
git clone https://github.com/your-username/zenith-provider-mysite.git
cd zenith-provider-mysite
```

### 2. Install Dependencies

Install the project dependencies, including `@zenith/sdk`, `esbuild`, and `vitest`:

```bash
npm install
```

---

## Project Structure Overview

Inside your cloned repository, you will find the following canonical layout:

```
├── manifest.json       # Plugin metadata, permissions, host allowlists, zpk/v2
├── settings.json       # Declarative UI settings rendered natively in Material 3
├── icon.png            # 512x512 PNG square icon displayed in the Zenith Store
├── package.json        # Dependencies, npm scripts, and build tooling
├── tsconfig.json       # Strict TypeScript compiler options
├── esbuild.config.mjs  # ES2020 IIFE bundler configuration
├── src/
│   └── index.ts        # Primary provider implementation code
├── test/
│   ├── fixtures/       # Saved HTML/JSON responses for deterministic unit tests
│   │   ├── search.html
│   │   └── details.html
│   └── provider.test.ts # Vitest unit tests verifying selectors and mock flow
└── tools/
    ├── package.mjs     # Assembles the .zpk ZIP archive
    └── verify.mjs      # Enforces envelope size, entry count, and security bounds
```

---

## Basic Provider Implementation

Open `src/index.ts`. A provider is defined using `defineProvider` from `@zenith/sdk/provider`:

```ts
import { defineProvider } from "@zenith/sdk/provider";
import { loadHtml } from "@zenith/sdk/dom";
import "@zenith/sdk/dispatcher"; // Registers IPC dispatcher

export default defineProvider({
  // Diagnostic self-test called from Zenith Settings
  async test(context) {
    const mirror = (await context.getSetting("mirror")) || "https://api.example.com";
    const res = await context.fetch(`${mirror}/ping`);
    return {
      success: res.ok,
      message: res.ok ? "Server is operational" : `Failed with status ${res.status}`
    };
  },

  // Catalog search capability
  async getSearch(query, page, pageSize, context) {
    const mirror = (await context.getSetting("mirror")) || "https://api.example.com";
    const url = `${mirror}/search?q=${encodeURIComponent(query)}&page=${page}`;
    const response = await context.fetch(url);
    const html = await response.text();
    const $ = loadHtml(html);

    const items = $(".anime-card").map((_, el) => {
      const $el = $(el);
      return {
        mediaRef: {
          id: $el.attr("data-id") || "",
          title: $el.find(".title").text().trim()
        },
        title: $el.find(".title").text().trim(),
        posterUrl: $el.find("img.poster").attr("src") || "",
        score: parseFloat($el.attr("data-score") || "0") || null,
        year: parseInt($el.attr("data-year") || "0", 10) || null,
        totalEpisodes: parseInt($el.attr("data-episodes") || "0", 10) || null
      };
    }).get();

    return {
      items,
      hasNextPage: $(".pagination .next").length > 0
    };
  },

  // Full media details
  async getDetails(mediaRef, context) {
    const mirror = (await context.getSetting("mirror")) || "https://api.example.com";
    const url = `${mirror}/anime/${encodeURIComponent(mediaRef.id)}`;
    const response = await context.fetch(url);
    const $ = loadHtml(await response.text());

    return {
      mediaRef,
      title: $(".title").text().trim() || mediaRef.title || "Unknown Title",
      description: $(".description").text().trim(),
      posterUrl: $("img.poster").attr("src"),
      score: 9.14,
      year: 2023,
      episodesCount: 28,
      genres: $(".genre-tag").map((_, el) => $(el).text().trim()).get(),
      status: "FINISHED"
    };
  },

  // Video source groups (voice acting, dubbing, subs)
  async getSources(mediaRef, context) {
    return [
      {
        name: "Original Voice (Subtitles)",
        translationName: "Original Sub",
        translationType: "SUB",
        episodes: [
          {
            number: 1,
            url: `https://api.example.com/episode/${encodeURIComponent(mediaRef.id)}/1`
          }
        ]
      }
    ];
  },

  // Direct video stream resolution
  async resolveEpisode(episode, context) {
    return [
      {
        url: episode.url,
        quality: "1080p",
        isHls: true
      }
    ];
  }
});
```

---

## Developing & Testing

### Running Tests

Deterministic unit tests verify your Cheerio selectors and data models against offline HTML fixtures:

```bash
npm test
```

### Type Checking

Ensure your TypeScript implementation strictly conforms to Zenith SDK types:

```bash
npm run typecheck
```

### Building the Bundle & Package

Build your JavaScript bundle and package it into a `.zpk` archive:

```bash
npm run package
```

This compiles your TypeScript into a single bundle with `esbuild`, runs package boundary validation via `tools/verify.mjs`, and outputs the archive to:

```
dist/template_provider.zpk
```

### Verifying Envelope Boundaries

To check that your package respects file count, uncompressed size, and binary restrictions:

```bash
npm run verify
```

---

## Installing into Zenith

To test your new provider inside the Zenith app:

1. Launch Zenith on your Desktop or Android device.
2. Open **Settings** -> **Providers**.
3. Tap **Install from File** and select your generated `.zpk` file.
4. Your provider will immediately be registered in the provider list and available in search and catalog views!

---

## Next Steps

- Learn about [Manifest V2 and Package Envelope Limits](/guides/manifest-and-package)
- Explore available [Provider Capabilities](/guides/capabilities)
- Master [DOM Parsing with Cheerio](/guides/dom-parsing)
- Read about [Network & Host Broker Security](/guides/network-and-broker)
