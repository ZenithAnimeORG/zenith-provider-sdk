# Provider Capabilities

Zenith utilizes a **modular capability model**. Rather than treating providers as monolithic scrapers, Zenith allows plugins to declare granular capabilities in `manifest.json`.

You can implement a single focused capability (e.g. a dedicated subtitle searcher or torrent tracker) or combine multiple capabilities into a comprehensive media provider.

---

## Capabilities Overview

| Capability | Manifest Value | Key Methods | Purpose |
|---|---|---|---|
| **Catalog** | `"CATALOG"` | `getSearch`, `getDetails`, `getHomeSections` | Search media titles, view details, browse carousels |
| **Media Source** | `"MEDIA_SOURCE"` | `getSources`, `resolveEpisode` | Audio tracks, dubbing groups, direct HLS/MP4 streams |
| **Torrent Source** | `"TORRENT_SOURCE"` | `searchTorrents`, `getTorrentSources` | BitTorrent releases, magnet URIs, seed counts |
| **Subtitles** | `"SUBTITLES"` | `searchSubtitles`, `getSubtitleDownloadLink` | External subtitle files (`.ass`, `.srt`) |
| **Skip Timings** | `"SKIP_TIMINGS"` | `getSkipTimes`, `getSkipTimings` | Opening (OP) and Ending (ED) skip intervals |
| **Stream Extractor** | `"STREAM_EXTRACTOR"` | `canExtractStream`, `extractStream` | Resolves third-party video host embeds |
| **Tracker** | `"TRACKER"` | `onEpisodeWatched`, `syncStatus` | Synchronizes watch history and episode progress |
| **Bookmark Importer**| `"IMPORT"` | `defineBookmarkImporter` | Migrates watchlists from external services |

---

## 1. Catalog Capability (`CATALOG`)

The Catalog capability powers the search bar, discovery tabs, and title details screens.

### `getSearch(query, page, pageSize, context)`

Invoked when the user types in the search bar. Returns a list of {@link CatalogMediaItem} cards and pagination flags.

```ts
async getSearch(query, page, pageSize, context) {
  const mirror = await context.getMirror();
  const url = `${mirror}/search?q=${encodeURIComponent(query)}&page=${page}`;
  const res = await context.fetch(url);
  const json = await res.json();

  const items = json.items.map(item => ({
    mediaRef: {
      id: String(item.id),
      title: item.title,
      externalIds: { mal: String(item.mal_id || "") }
    },
    title: item.title,
    originalTitle: item.romaji,
    posterUrl: item.poster_url,
    score: item.score,
    year: item.year,
    totalEpisodes: item.episodes_count
  }));

  return {
    items,
    hasNextPage: json.page < json.total_pages
  };
}
```

### `getDetails(mediaRef, context)`

Invoked when the user taps on a title to open its full details page. Returns {@link CatalogMediaDetails}.

```ts
async getDetails(mediaRef, context) {
  const mirror = await context.getMirror();
  const res = await context.fetch(`${mirror}/anime/${encodeURIComponent(mediaRef.id)}`);
  const data = await res.json();

  return {
    mediaRef,
    title: data.title,
    originalTitle: data.title_japanese,
    description: data.synopsis,
    posterUrl: data.images.jpg.large_image_url,
    bannerUrl: data.images.banner_image_url,
    score: data.score,
    year: data.year,
    episodesCount: data.episodes,
    genres: data.genres.map(g => g.name),
    studios: data.studios.map(s => s.name),
    status: data.airing ? "AIRING" : "FINISHED"
  };
}
```

---

## 2. Media Source Capability (`MEDIA_SOURCE`)

The Media Source capability powers video playback. It provides translation tracks (dubbing teams, audio languages) and resolves playable video streams for Zenith's MPV player engine.

### `getSources(mediaRef, context, episodeNumber?)`

Returns an array of {@link ProviderVideoSource} groups, each containing its episode list.

```ts
async getSources(mediaRef, context) {
  return [
    {
      name: "Original Japanese (English Subs)",
      translationName: "Subtitles",
      translationType: "SUB",
      episodes: [
        { number: 1, url: `https://api.example.com/stream/${mediaRef.id}/1` },
        { number: 2, url: `https://api.example.com/stream/${mediaRef.id}/2` }
      ]
    },
    {
      name: "Studio Dub (Multi-Voice)",
      translationName: "Studio Dub",
      translationType: "DUB",
      episodes: [
        { number: 1, url: `https://api.example.com/stream/${mediaRef.id}/1?dub=true` }
      ]
    }
  ];
}
```

### `resolveEpisode(episode, context)`

Resolves the chosen episode into playable {@link ProviderMediaStream} instances.

```ts
async resolveEpisode(episode, context) {
  // If the episode URL requires API resolution or token signing
  const res = await context.fetch(`https://api.example.com/resolve?url=${encodeURIComponent(episode.url)}`);
  const data = await res.json();

  return [
    {
      url: data.hlsMasterUrl,
      quality: "1080p",
      isHls: true,
      headers: {
        "Referer": "https://player.example.com/",
        "User-Agent": "ZenithPlayer/1.0"
      }
    }
  ];
}
```

---

## 3. Torrent Source Capability (`TORRENT_SOURCE`)

Zenith includes an embedded BitTorrent streaming engine powered by `jlibtorrent`. Torrent providers discover releases, seeds, and magnet links.

### `searchTorrents(mediaRef, context)`

Returns an array of {@link ProviderTorrentSource} items.

```ts
async searchTorrents(mediaRef, context) {
  const query = mediaRef.title || "";
  const res = await context.fetch(`https://nyaa.si/?f=0&c=1_2&q=${encodeURIComponent(query)}`);
  const $ = loadHtml(await res.text());

  const torrents: ProviderTorrentSource[] = [];
  $("tr.default, tr.success").each((_, el) => {
    const $row = $(el);
    const title = $row.find("td:nth-child(2) a:not(.comments)").text().trim();
    const magnetUri = $row.find("td:nth-child(3) a[href^='magnet:']").attr("href");
    const seeders = parseInt($row.find("td:nth-child(6)").text().trim(), 10) || 0;
    const leechers = parseInt($row.find("td:nth-child(7)").text().trim(), 10) || 0;

    if (magnetUri) {
      torrents.push({
        title,
        magnetUri,
        seeders,
        leechers,
        quality: title.includes("1080p") ? "1080p" : "720p",
        trackerName: "Nyaa"
      });
    }
  });

  return torrents;
}
```

---

## 4. Subtitles Capability (`SUBTITLES`)

External subtitle providers allow users to load third-party `.ass` or `.srt` subtitle files during playback.

### `searchSubtitles(query, languages, episodeNumber, seasonNumber, context)`

```ts
async searchSubtitles(query, languages, episodeNumber, seasonNumber, context) {
  const res = await context.fetch(
    `https://api.example.com/subtitles?query=${encodeURIComponent(query)}&episode=${episodeNumber || ""}&langs=${languages}`
  );
  const data = await res.json();

  return data.results.map(sub => ({
    fileId: sub.id,
    language: sub.lang,
    releaseName: sub.release,
    isHearingImpaired: sub.is_sdh,
    directUrl: sub.download_url
  }));
}
```

---

## 5. Skip Timings Capability (`SKIP_TIMINGS`)

Enables automatic or manual "Skip Intro" / "Skip Outro" buttons in the Zenith video player.

### `getSkipTimes(mediaRef, episodeNumber, episodeLength, translationName, context)`

Returns {@link SkipInterval} segments in seconds:

```ts
async getSkipTimes(mediaRef, episodeNumber, episodeLength, translationName, context) {
  const malId = mediaRef.externalIds?.mal;
  if (!malId) return [];

  const res = await context.fetch(`https://api.aniskip.com/v2/skip-times/${malId}/${episodeNumber}?types=op&types=ed`);
  if (!res.ok) return [];

  const data = await res.json();
  const intervals: SkipInterval[] = [];

  for (const item of data.results || []) {
    intervals.push({
      startTime: item.interval.startTime,
      endTime: item.interval.endTime,
      skipType: item.skipType // "op" or "ed"
    });
  }

  return intervals;
}
```

---

## 6. Bookmark Importer Capability (`IMPORT`)

Bookmark importers parse user export files (e.g. MyAnimeList XML/CSV, Shikimori JSON, AniList JSON) and import entries into Zenith's local database.

Importers use `defineBookmarkImporter` from `@zenith/sdk/bookmark-importer`:

```ts
import { defineBookmarkImporter } from "@zenith/sdk/bookmark-importer";

export default defineBookmarkImporter({
  supportedExtensions: [".csv"],
  instructions: [
    {
      stepNumber: 1,
      title: "Export your list",
      description: "Log in to your account and download your anime list as a CSV file.",
      linkUrl: "https://example.com/export",
      linkText: "Go to Export Page"
    }
  ],

  async preview(source, context) {
    const rows = await source.readHeaderAndSample(5);
    return { format: "csv", rows };
  },

  async *readBookmarks(source, context) {
    for await (const row of source.readCsvRows()) {
      yield context.bookmark({
        title: row["anime_title"] || "",
        releaseYear: parseInt(row["year"] || "0", 10) || null,
        watchedEpisodes: parseInt(row["watched_episodes"] || "0", 10),
        totalEpisodes: parseInt(row["total_episodes"] || "0", 10) || null,
        targetStatus: row["status"] === "Completed" ? "COMPLETED" : "WATCHING",
        rating: parseFloat(row["score"] || "0") || null
      });
    }
  }
});
```
