[**@zenith/sdk**](../index.md)

***

[@zenith/sdk](../index.md) / ProviderCapability

# Type Alias: ProviderCapability

> **ProviderCapability** = `"MEDIA_SOURCE"` \| `"POSTER_SOURCE"` \| `"SKIP_TIMINGS"` \| `"SUBTITLES"` \| `"TORRENT_SOURCE"` \| `"STREAM_EXTRACTOR"` \| `"CATALOG"` \| `"TRACKER"` \| `"IMPORT"` \| `"UNKNOWN"`

Defined in: [models.ts:27](https://github.com/ZenithAnimeORG/zenith-provider-sdk/blob/089a8e3b65981c189d4129603af1951c324df527/sdk-ts/src/models.ts#L27)

Declares the capabilities supported by a Zenith provider plugin.

A provider declares its capabilities in its `manifest.json` file. Each capability
unlocks corresponding methods in [ProviderDefinition](../interfaces/ProviderDefinition.md) that Zenith will invoke
when performing search, playback, or synchronization actions.

Capabilities:
- `"CATALOG"`: Provides media discovery, search, catalog details, and recommendation carousels.
- `"MEDIA_SOURCE"`: Provides direct video sources, translations, and playable media streams (HLS/MP4).
- `"TORRENT_SOURCE"`: Provides BitTorrent release search, magnet URIs, and torrent file hashes for playback via jlibtorrent.
- `"SUBTITLES"`: Provides external subtitle file searches and download links.
- `"SKIP_TIMINGS"`: Provides opening (OP) and ending (ED) skip timecodes for video playback.
- `"POSTER_SOURCE"`: Provides high-resolution artwork and poster resolution.
- `"STREAM_EXTRACTOR"`: Resolves third-party video host embed URLs into direct playback streams.
- `"TRACKER"`: Synchronizes user watch history, episode progress, and media statuses.
- `"IMPORT"`: Imports external watchlist bookmarks from CSV, XML, or JSON files.
- `"UNKNOWN"`: Placeholder for unrecognized or future capabilities.
