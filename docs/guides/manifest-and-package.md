# Manifest V2 & Package Envelope

Zenith plugins are packaged as **`.zpk`** files (ZIP archives) that encapsulate your compiled JavaScript bundle, metadata manifest, declarative settings schema, and display icon.

To maintain cross-platform security, instant installation, and memory safety across mobile and desktop devices, Zenith enforces strict envelope constraints and manifest validation rules.

---

## Manifest V2 Specification (`manifest.json`)

Every provider archive must contain a `manifest.json` file in its root directory.

### Example `manifest.json`

```json
{
  "id": "my_anime_provider",
  "name": "My Anime Provider",
  "version": "1.0.0",
  "sdkVersion": "0.1.0",
  "packageFormat": "zpk/v2",
  "moduleApiVersion": "1.0.0",
  "entrypoint": "index.js",
  "author": "Community Developer",
  "description": "Fast anime catalog search and stream resolution.",
  "capabilities": [
    "CATALOG",
    "MEDIA_SOURCE"
  ],
  "hosts": [
    "api.mysite.com",
    "*.cdn.mysite.com"
  ],
  "entry": {
    "js": "index.js"
  },
  "settings": "settings.json",
  "icon": "icon.png",
  "localizedNames": {
    "en": "My Anime Provider",
    "ru": "Мой Аниме Провайдер"
  },
  "localizedDescriptions": {
    "en": "Fast anime catalog search and stream resolution.",
    "ru": "Быстрый поиск по каталогу и просмотр серий."
  }
}
```

### Manifest Fields Reference

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | `string` | **Yes** | Unique identifier (1–128 characters, matching `^[A-Za-z0-9._-]+$`). |
| `name` | `string` | **Yes** | Human-readable name displayed in Zenith Provider Store and UI. |
| `version` | `string` | **Yes** | Semantic version string (e.g. `"1.0.0"`). |
| `packageFormat` | `string` | **Yes** | Must be `"zpk/v2"` for portable TypeScript/QuickJS providers. |
| `moduleApiVersion`| `string` | No | Target Zenith Module API version (currently `"1.0.0"`). |
| `sdkVersion` | `string` | No | Zenith Provider SDK version used during build (e.g. `"0.1.0"`). |
| `entrypoint` | `string` | No | Path to the bundled JavaScript entrypoint (defaults to `"index.js"`). |
| `author` | `string` | No | Author name, handle, or organization. |
| `description` | `string` | No | Brief summary of provider features. |
| `capabilities` | `string[]`| **Yes** | List of capability identifiers supported by the provider. |
| `hosts` | `string[]`| **Yes** | Host allowlist for HTTP requests mediated by the Host Broker. |
| `settings` | `string` | No | Relative path to declarative `settings.json` file. |
| `icon` | `string` | No | Path to square 512x512 PNG icon file (`icon.png`). |
| `localizedNames`| `object` | No | Map of ISO language codes to localized names. |
| `localizedDescriptions`| `object`| No | Map of ISO language codes to localized descriptions. |

---

## Capabilities List

Declare only the capabilities your provider actively implements. Zenith uses this list to route search, stream extraction, and subtitle queries:

- `"CATALOG"`: Implements catalog search, details, recommendations, and episode listings.
- `"MEDIA_SOURCE"`: Implements direct video sources, audio dubbing groups, and stream resolution.
- `"TORRENT_SOURCE"`: Implements BitTorrent release discovery for streaming via `jlibtorrent`.
- `"SUBTITLES"`: Implements external subtitle file searches (`.ass`, `.srt`).
- `"SKIP_TIMINGS"`: Implements opening/ending skip timecodes.
- `"POSTER_SOURCE"`: Resolves standalone high-res posters and artwork.
- `"STREAM_EXTRACTOR"`: Resolves third-party video host embeds.
- `"TRACKER"`: Synchronizes episode progress and watch status with user accounts.
- `"IMPORT"`: Imports external watchlist exports into Zenith bookmarks.

---

## Host Allowlist Security

Zenith enforces a **strict host allowlist** on all outbound HTTP requests made via `context.fetch()`.

- Any request or HTTP redirect leading to a host not in `hosts` is **immediately rejected** with a security error.
- Wildcards are supported (e.g. `"*.example.com"` matches `api.example.com` and `cdn.example.com`).
- Do not add catch-all wildcards like `"*"` or `"http*"`: providers with wildcard access may be rejected by community stores.

```json
"hosts": [
  "myanimelist.net",
  "api.myanimelist.net",
  "*.myanimelist.net"
]
```

---

## Declarative Settings (`settings.json`)

If your provider supports configurable options (such as custom mirrors, language selection, or API keys), define them declaratively in `settings.json`. Zenith's Material 3 settings screen renders native interactive UI directly from this schema.

```json
[
  {
    "key": "mirror",
    "title": "API Mirror",
    "description": "Choose which domain to query for catalog and stream data",
    "type": "CHOICE",
    "defaultValue": "https://api.example.com",
    "options": [
      { "value": "https://api.example.com", "label": "Main Server" },
      { "value": "https://backup.example.com", "label": "Backup Mirror" }
    ]
  },
  {
    "key": "include_r18",
    "title": "Include 18+ Content",
    "description": "Show adult releases in catalog search results",
    "type": "TOGGLE",
    "defaultValue": false
  },
  {
    "key": "api_key",
    "title": "Account API Token",
    "description": "Personal access token for authenticated API requests",
    "type": "TEXT",
    "isSecret": true
  }
]
```

### Setting Types

- `"TOGGLE"`: Material 3 Switch (boolean).
- `"TEXT"`: Interactive text entry modal with optional `isSecret: true` password masking.
- `"CHOICE"`: Single-choice selection dialog from `options`.
- `"MIRROR_LIST"`: Interactive server mirror selector.

In your TypeScript code, access these values asynchronously:

```ts
const mirror = (await context.getSetting("mirror")) || "https://api.example.com";
const includeAdult = (await context.getSetting("include_r18")) === "true";
```

---

## Package Envelope Limits (`zpk/v2`)

To protect user devices against zip bombs, excessive memory usage, and rogue native binaries, Zenith enforces strict envelope limits at installation time via `validateEnvelope()`:

| Envelope Metric | Limit | Description |
|---|---|---|
| **Compressed Package Size** | **16 MiB** | Maximum size of the `.zpk` file on disk. |
| **Total Uncompressed Size** | **32 MiB** | Sum of all extracted files in the archive. |
| **JavaScript Entrypoint** | **5 MiB** | Maximum size of `index.js`. (Typical bundles are ~50–150 KiB). |
| **Manifest Size** | **256 KiB** | Maximum size of `manifest.json`. |
| **Settings Size** | **128 KiB** | Maximum size of `settings.json`. |
| **Icon Size** | **256 KiB** | Maximum size of `icon.png`. |
| **Resource Size** | **2 MiB** | Maximum size of any individual static file. |
| **Maximum Entry Count** | **100 entries** | Maximum number of files permitted in the archive. |

### Absolute Ban on Compiled / Executable Binaries

In the portable `zpk/v2` format, compiled native or bytecode binaries are **strictly forbidden**. Any archive containing files ending with the following extensions will fail validation immediately:

- `.jar`, `.dex`, `.class`
- `.so`, `.dll`, `.dylib`
- `.exe`, `.apk`

Zenith providers run **exclusively** on QuickJS in-process runtime.

### Safe Path Sanitization

Archive entry names must satisfy `isSafeEntryName`:
- No directory traversal elements (`..`)
- No absolute root paths (`/` or `\`)
- No duplicate entries
- Allowed characters: `a-z`, `A-Z`, `0-9`, `_`, `-`, `.`, `/`

---

## Unsigned Community Model

Zenith uses an open, unsigned community plugin model:
- Authors do **not** need cryptographic signing certificates, developer keys, or paid developer accounts.
- Community catalogs use SHA-256 checksums to verify that downloaded archives match the catalog index.
- Security is guaranteed by **runtime sandboxing**: QuickJS memory bounds (64 MiB), CPU deadlines, and the Host Broker's network allowlist.
