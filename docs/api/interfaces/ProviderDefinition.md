[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderDefinition

# Interface: ProviderDefinition

Defined in: [provider.ts:45](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L45)

Interface implemented by Zenith modular providers.

All capability methods are optional. A provider implements only the methods
declared in its `manifest.json` capabilities array.

Capabilities map to methods:
- `"CATALOG"`: [getSearch](#getsearch), [getDetails](#getdetails), [getHomeSections](#gethomesections), [getPopular](#getpopular), [getLatest](#getlatest), [getRelated](#getrelated), [getEpisodes](#getepisodes)
- `"MEDIA_SOURCE"`: [getSources](#getsources), [resolveEpisode](#resolveepisode)
- `"TORRENT_SOURCE"`: [searchTorrents](#searchtorrents), [getTorrentSources](#gettorrentsources)
- `"SUBTITLES"`: [searchSubtitles](#searchsubtitles), [getSubtitleDownloadLink](#getsubtitledownloadlink), [getSubtitles](#getsubtitles)
- `"SKIP_TIMINGS"`: [getSkipTimes](#getskiptimes), [getSkipTimings](#getskiptimings)
- `"POSTER_SOURCE"`: [getPoster](#getposter)
- `"STREAM_EXTRACTOR"`: [canExtractStream](#canextractstream), [extractStream](#extractstream), [resolveStream](#resolvestream)
- `"TRACKER"`: [onEpisodeWatched](#onepisodewatched), [syncStatus](#syncstatus), [getUserMediaStatus](#getusermediastatus), [updateUserMediaStatus](#updateusermediastatus)

Lifecycle methods [initialize](#initialize), [dispose](#dispose), [test](#test), [probeStatus](#probestatus), [executeAction](#executeaction)
are available to all providers regardless of declared capabilities.

## Methods

### canExtractStream()?

> `optional` **canExtractStream**(`extractorName`, `url`, `context`): `Promise`\<`boolean`\>

Defined in: [provider.ts:490](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L490)

Checks whether this provider can extract streams from a video hosting embed URL.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `extractorName` | `string` | Extractor identifier. |
| `url` | `string` | Embed URL. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<`boolean`\>

`true` if extractor supports the given URL.

***

### dispose()?

> `optional` **dispose**(): `Promise`\<`void`\>

Defined in: [provider.ts:77](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L77)

Cleans up provider resources when unloaded or replaced.

#### Returns

`Promise`\<`void`\>

#### Example

```ts
async dispose() {
  this.cache.clear();
}
```

***

### executeAction()?

> `optional` **executeAction**(`actionId`, `context`): `Promise`\<`string`\>

Defined in: [provider.ts:139](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L139)

Executes a custom interactive action triggered from Zenith provider settings UI.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `actionId` | `string` | Action identifier matching one declared in `settings.json`. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<`string`\>

Text message displayed in user-facing confirmation dialog.

#### Example

```ts
async executeAction(actionId, context) {
  if (actionId === "clear_session") {
    await context.setSetting("session_token", "");
    return "Session cleared successfully.";
  }
  throw new Error(`Unknown action: ${actionId}`);
}
```

***

### extractStream()?

> `optional` **extractStream**(`extractorName`, `url`, `referer`, `context`): `Promise`\<[`ProviderMediaStream`](ProviderMediaStream.md)[]\>

Defined in: [provider.ts:505](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L505)

Extracts playable video streams from a video hosting embed URL.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `extractorName` | `string` | Extractor identifier. |
| `url` | `string` | Embed URL. |
| `referer` | `string` \| `null` | HTTP Referer header to include. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderMediaStream`](ProviderMediaStream.md)[]\>

Array of extracted media streams.

***

### getDetails()?

> `optional` **getDetails**(`mediaRef`, `context`): `Promise`\<[`CatalogMediaDetails`](CatalogMediaDetails.md)\>

Defined in: [provider.ts:201](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L201)

Loads full metadata for a specific title.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media identifier reference. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`CatalogMediaDetails`](CatalogMediaDetails.md)\>

Full title details (synopsis, genres, score, episodes count).

#### Example

```ts
async getDetails(mediaRef, context) {
  const res = await context.fetch(`https://api.example.com/anime/${mediaRef.id}`);
  const data = await res.json();
  return {
    mediaRef,
    title: data.name,
    description: data.synopsis,
    posterUrl: data.poster,
    score: data.score,
    genres: data.genres
  };
}
```

***

### getEpisodes()?

> `optional` **getEpisodes**(`mediaRef`, `context`): `Promise`\<[`ProviderEpisode`](ProviderEpisode.md)[]\>

Defined in: [provider.ts:257](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L257)

Retrieves the raw list of episodes for a given title directly without grouping into sources.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderEpisode`](ProviderEpisode.md)[]\>

Array of provider episodes.

***

### getHomeSections()?

> `optional` **getHomeSections**(`context`): `Promise`\<[`CatalogSection`](CatalogSection.md)[]\>

Defined in: [provider.ts:219](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L219)

Loads curated horizontal sections/carousels for the Zenith Home screen.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`CatalogSection`](CatalogSection.md)[]\>

Array of named sections containing preview media cards.

#### Example

```ts
async getHomeSections(context) {
  return [
    { title: "Trending", items: trendingList },
    { title: "Top Rated", items: topList }
  ];
}
```

***

### getLatest()?

> `optional` **getLatest**(`page`, `pageSize`, `context`): `Promise`\<[`CatalogMediaItem`](CatalogMediaItem.md)[]\>

Defined in: [provider.ts:239](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L239)

Loads a paginated list of newly released or updated media items.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `page` | `number` | 1-indexed page number. |
| `pageSize` | `number` | Requested items per page. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`CatalogMediaItem`](CatalogMediaItem.md)[]\>

Array of catalog media items.

***

### getPopular()?

> `optional` **getPopular**(`page`, `pageSize`, `context`): `Promise`\<[`CatalogMediaItem`](CatalogMediaItem.md)[]\>

Defined in: [provider.ts:229](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L229)

Loads a paginated list of currently popular media items.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `page` | `number` | 1-indexed page number. |
| `pageSize` | `number` | Requested items per page. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`CatalogMediaItem`](CatalogMediaItem.md)[]\>

Array of catalog media items.

***

### getPoster()?

> `optional` **getPoster**(`mediaRef`, `context`): `Promise`\<`string` \| `null`\>

Defined in: [provider.ts:476](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L476)

Resolves a high-resolution poster artwork URL for a media item.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<`string` \| `null`\>

Poster image URL or `null`.

***

### getRelated()?

> `optional` **getRelated**(`mediaRef`, `context`): `Promise`\<[`CatalogMediaItem`](CatalogMediaItem.md)[]\>

Defined in: [provider.ts:248](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L248)

Loads related or recommended titles for a given media item.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`CatalogMediaItem`](CatalogMediaItem.md)[]\>

Array of recommended media items.

***

### getSearch()?

> `optional` **getSearch**(`query`, `page`, `pageSize`, `context`): `Promise`\<[`CatalogSearchResult`](CatalogSearchResult.md)\>

Defined in: [provider.ts:171](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L171)

Searches the provider's media catalog by query string.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `query` | `string` | Search keywords entered by user. |
| `page` | `number` | 1-indexed page number. |
| `pageSize` | `number` | Requested items per page. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`CatalogSearchResult`](CatalogSearchResult.md)\>

List of matching media items and pagination flag.

#### Example

```ts
async getSearch(query, page, pageSize, context) {
  const res = await context.fetch(`https://api.example.com/search?q=${encodeURIComponent(query)}&page=${page}`);
  const json = await res.json();
  return {
    items: json.results.map(r => ({
      mediaRef: { id: r.id, title: r.title },
      title: r.title,
      posterUrl: r.cover,
      score: r.rating
    })),
    hasNextPage: json.page < json.totalPages
  };
}
```

***

### getSkipTimes()?

> `optional` **getSkipTimes**(`mediaRef`, `episodeNumber`, `episodeLength`, `translationName`, `context`): `Promise`\<[`SkipInterval`](SkipInterval.md)[]\>

Defined in: [provider.ts:443](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L443)

Retrieves intro/outro skip intervals for an episode.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `episodeNumber` | `number` | Episode number. |
| `episodeLength` | `number` \| `null` | Total episode duration in seconds, if known. |
| `translationName` | `string` \| `null` | Translation name or release group. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`SkipInterval`](SkipInterval.md)[]\>

Array of skip intervals (OP/ED).

#### Example

```ts
async getSkipTimes(mediaRef, episodeNumber, episodeLength, translationName, context) {
  return [{
    startTime: 90,
    endTime: 180,
    skipType: "op"
  }];
}
```

***

### getSkipTimings()?

> `optional` **getSkipTimings**(`mediaRef`, `episodeNumber`, `context`): `Promise`\<[`SkipInterval`](SkipInterval.md)[]\>

Defined in: [provider.ts:459](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L459)

Simplified overload for skip intervals.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `episodeNumber` | `number` | Episode number. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`SkipInterval`](SkipInterval.md)[]\>

Array of skip intervals.

***

### getSources()?

> `optional` **getSources**(`mediaRef`, `context`, `episodeNumber?`): `Promise`\<[`ProviderVideoSource`](ProviderVideoSource.md)[]\>

Defined in: [provider.ts:285](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L285)

Retrieves available video translation sources (dubbing groups, audio tracks, sub groups) and their episodes.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |
| `episodeNumber?` | `number` | Optional episode number filter. |

#### Returns

`Promise`\<[`ProviderVideoSource`](ProviderVideoSource.md)[]\>

Array of video translation sources.

#### Example

```ts
async getSources(mediaRef, context) {
  return [{
    name: "AniLibria",
    translationName: "AniLibria",
    translationType: "DUB",
    episodes: [
      { number: 1, url: "https://example.com/ep1" }
    ]
  }];
}
```

***

### getSubtitleDownloadLink()?

> `optional` **getSubtitleDownloadLink**(`fileId`, `context`): `Promise`\<`string` \| `null`\>

Defined in: [provider.ts:399](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L399)

Resolves the direct download URL for a subtitle file ID.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `fileId` | `number` | Numeric subtitle file ID. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<`string` \| `null`\>

Download URL or `null` if expired/unavailable.

***

### getSubtitles()?

> `optional` **getSubtitles**(`mediaRef`, `episodeNumber`, `context`): `Promise`\<[`ProviderSubtitle`](ProviderSubtitle.md)[]\>

Defined in: [provider.ts:412](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L412)

Retrieves subtitles for a specific media item and episode.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `episodeNumber` | `number` | Episode number. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderSubtitle`](ProviderSubtitle.md)[]\>

Array of subtitle tracks.

***

### getTorrentSources()?

> `optional` **getTorrentSources**(`mediaRef`, `episodeNumber`, `context`): `Promise`\<[`ProviderTorrentSource`](ProviderTorrentSource.md)[]\>

Defined in: [provider.ts:353](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L353)

Retrieves torrent releases filtered for a specific episode number.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `episodeNumber` | `number` | Episode number. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderTorrentSource`](ProviderTorrentSource.md)[]\>

Array of matching torrent releases.

***

### getUserMediaStatus()?

> `optional` **getUserMediaStatus**(`mediaRef`, `context`): `Promise`\<[`UserMediaStatus`](../type-aliases/UserMediaStatus.md)\>

Defined in: [provider.ts:563](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L563)

Retrieves the current user tracking status for a media item.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`UserMediaStatus`](../type-aliases/UserMediaStatus.md)\>

User media status.

***

### initialize()?

> `optional` **initialize**(`manifest`, `context`): `Promise`\<`void`\>

Defined in: [provider.ts:65](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L65)

Initializes the provider when the QuickJS instance is spun up.

Use this to prepare in-memory caches, parse settings, or establish session states.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `manifest` | [`PluginManifest`](PluginManifest.md) | The parsed plugin manifest. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<`void`\>

#### Example

```ts
async initialize(manifest, context) {
  context.logger.info(`Initializing provider: ${manifest.name} v${manifest.version}`);
}
```

***

### onEpisodeWatched()?

> `optional` **onEpisodeWatched**(`mediaRef`, `episode`, `context`): `Promise`\<`void`\>

Defined in: [provider.ts:537](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L537)

Invoked when an episode has been watched in Zenith.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `episode` | `number` | Watched episode number. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<`void`\>

***

### probeStatus()?

> `optional` **probeStatus**(`context`): `Promise`\<[`ProviderStatusInfo`](ProviderStatusInfo.md)\>

Defined in: [provider.ts:119](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L119)

Probes provider operational health and response latency.

Lightweight health check called by Zenith's background monitoring to display status dots.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderStatusInfo`](ProviderStatusInfo.md)\>

Health status information.

#### Example

```ts
async probeStatus(context) {
  return {
    status: "OK",
    isOperational: true,
    latencyMs: 35
  };
}
```

***

### resolveEpisode()?

> `optional` **resolveEpisode**(`episode`, `context`): `Promise`\<[`ProviderMediaStream`](ProviderMediaStream.md)[]\>

Defined in: [provider.ts:310](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L310)

Resolves direct playable video streams (HLS m3u8 or MP4) for an episode.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `episode` | [`ProviderEpisode`](ProviderEpisode.md) | The episode selected for playback. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderMediaStream`](ProviderMediaStream.md)[]\>

Array of playable streams with quality tags and headers.

#### Example

```ts
async resolveEpisode(episode, context) {
  return [{
    url: "https://cdn.example.com/master.m3u8",
    quality: "1080p",
    isHls: true,
    headers: { Referer: "https://example.com" }
  }];
}
```

***

### resolveStream()?

> `optional` **resolveStream**(`extractorName`, `url`, `context`): `Promise`\<[`ProviderMediaStream`](ProviderMediaStream.md)[]\>

Defined in: [provider.ts:520](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L520)

Resolves streams using a registered extractor.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `extractorName` | `string` | Extractor identifier. |
| `url` | `string` | Embed URL. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderMediaStream`](ProviderMediaStream.md)[]\>

Array of extracted media streams.

***

### searchSubtitles()?

> `optional` **searchSubtitles**(`query`, `languages`, `episodeNumber`, `seasonNumber`, `context`): `Promise`\<[`ProviderSubtitle`](ProviderSubtitle.md)[]\>

Defined in: [provider.ts:384](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L384)

Searches for subtitle tracks matching a title or episode.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `query` | `string` | Search text or release name. |
| `languages` | `string` | Comma-separated ISO language codes (e.g. `"en,ru"`). |
| `episodeNumber` | `number` \| `null` | Episode number, if applicable. |
| `seasonNumber` | `number` \| `null` | Season number, if applicable. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderSubtitle`](ProviderSubtitle.md)[]\>

Array of found subtitle tracks.

#### Example

```ts
async searchSubtitles(query, languages, episodeNumber, seasonNumber, context) {
  return [{
    language: "en",
    releaseName: "SubsPlease Frieren 01 1080p",
    directUrl: "https://example.com/subs/01.ass"
  }];
}
```

***

### searchTorrents()?

> `optional` **searchTorrents**(`mediaRef`, `context`): `Promise`\<[`ProviderTorrentSource`](ProviderTorrentSource.md)[]\>

Defined in: [provider.ts:340](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L340)

Searches for BitTorrent releases matching a title.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderTorrentSource`](ProviderTorrentSource.md)[]\>

Array of torrent releases for streaming via `jlibtorrent`.

#### Example

```ts
async searchTorrents(mediaRef, context) {
  return [{
    title: "[SubsPlease] Sousou no Frieren - 01 (1080p) [98D8D5D5].mkv",
    magnetUri: "magnet:?xt=urn:btih:...",
    size: 1400000000,
    seeders: 45,
    leechers: 3,
    quality: "1080p"
  }];
}
```

***

### syncStatus()?

> `optional` **syncStatus**(`mediaRef`, `status`, `context`): `Promise`\<`void`\>

Defined in: [provider.ts:550](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L550)

Synchronizes user watch status to the tracking service.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `status` | [`UserMediaStatus`](../type-aliases/UserMediaStatus.md) | Watchlist status (`"WATCHING"`, `"COMPLETED"`, etc.). |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<`void`\>

***

### test()?

> `optional` **test**(`context`): `Promise`\<[`ProviderTestResult`](../type-aliases/ProviderTestResult.md)\>

Defined in: [provider.ts:98](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L98)

Performs a self-test diagnostic check to verify API connectivity and server health.

Triggered by user tap on "Test Connection" in Zenith provider settings.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<[`ProviderTestResult`](../type-aliases/ProviderTestResult.md)\>

Test result with success/failure status and diagnostic latency.

#### Example

```ts
async test(context) {
  const start = Date.now();
  const res = await context.fetch("https://api.example.com/ping");
  return res.ok
    ? { success: true, message: "Mirror reachable", latencyMs: Date.now() - start }
    : { success: false, error: `HTTP ${res.status}` };
}
```

***

### updateUserMediaStatus()?

> `optional` **updateUserMediaStatus**(`mediaRef`, `status`, `progress`, `context`): `Promise`\<`void`\>

Defined in: [provider.ts:573](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/provider.ts#L573)

Updates user watch status and episode progress.

#### Parameters

| Parameter | Type | Description |
| ------ | ------ | ------ |
| `mediaRef` | [`MediaRef`](MediaRef.md) | Media reference. |
| `status` | `string` | Watchlist status string. |
| `progress` | `number` | Watched episode count. |
| `context` | [`ProviderContext`](../classes/ProviderContext.md) | Host execution context. |

#### Returns

`Promise`\<`void`\>
