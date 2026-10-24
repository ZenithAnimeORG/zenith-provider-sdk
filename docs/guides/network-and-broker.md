# Network & Host Broker

In Zenith, providers execute inside sandboxed **QuickJS** virtual machines without direct access to raw operating system sockets, network interfaces, or filesystem paths.

All outbound HTTP communications, preference persistence, and diagnostic logging are mediated through Zenith's native Kotlin Ktor **Host Broker**.

---

## Host Broker Architecture

```
┌────────────────────────────────────────────────────────┐
│               QuickJS Sandbox (Provider)               │
│                                                        │
│   context.fetch("https://api.mysite.com/search")       │
└──────────────────────────┬─────────────────────────────┘
                           │ IPC Bridge (Host Call)
┌──────────────────────────▼─────────────────────────────┐
│                   Zenith Host Broker                   │
│                                                        │
│   1. Validate URL against manifest.json `hosts`        │
│   2. Inject standard User-Agent and Session Cookies    │
│   3. Execute via native Ktor Client                    │
│   4. Validate redirects against allowlist              │
│   5. Enforce 16 MiB response body ceiling              │
└──────────────────────────┬─────────────────────────────┘
                           │ Encrypted HTTPS
┌──────────────────────────▼─────────────────────────────┐
│                    Remote Media Server                 │
└────────────────────────────────────────────────────────┘
```

### Security & Quota Invariants

1. **Host Allowlist Enforcement**:
   Every request and every HTTP redirect target is checked against the `hosts` array in your `manifest.json`. Requests to unlisted domains are rejected immediately.
2. **Response Size Ceiling**:
   A single response body cannot exceed **16 MiB**. Attempting to download larger streams via `context.fetch()` will result in an error (video streams must be passed to MPV via stream URLs, not buffered in JavaScript memory).
3. **Execution Deadlines**:
   Network calls default to a 15-second timeout (`timeoutMillis: 15000`) to keep the UI responsive.

---

## Outbound Requests via `context.fetch()`

The `context.fetch()` API mirrors the standard Web Fetch API:

```ts
// Simple GET request
const response = await context.fetch("https://api.example.com/anime/123");
if (!response.ok) {
  throw new Error(`HTTP request failed with status: ${response.status}`);
}
const data = await response.json<MyAnimeResponse>();
```

### POST Requests with JSON Payloads

```ts
const response = await context.fetch("https://api.example.com/graphql", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Accept": "application/json"
  },
  body: JSON.stringify({
    query: `query ($id: Int) { Media(id: $id) { title { romaji } } }`,
    variables: { id: 1535 }
  }),
  timeoutMillis: 10000
});

const result = await response.json();
```

---

## Working with `FetchHeaders`

Zenith provides a case-insensitive {@link FetchHeaders} implementation:

```ts
const response = await context.fetch("https://api.example.com/data");

// Case-insensitive lookups
const contentType = response.headers.get("content-type"); // "application/json; charset=utf-8"
const hasETag = response.headers.has("etag");

// Iterate through all returned headers
response.headers.forEach((value, name) => {
  context.logger.debug(`Header ${name}: ${value}`);
});
```

---

## Bypassing Anti-Bot & CDN Restrictions

Many streaming websites use Cloudflare, DDoS-Guard, or CDN token verification to prevent unauthorized scraping.

### 1. Custom `Referer` and `Origin` Headers

Video embed hosts often block requests unless the `Referer` header matches the parent website:

```ts
const response = await context.fetch(videoApiUrl, {
  headers: {
    "Referer": "https://embed.example.com/",
    "Origin": "https://embed.example.com",
    "X-Requested-With": "XMLHttpRequest"
  }
});
```

### 2. Automatic Cookie Persistence

The Zenith host Ktor client automatically manages an in-memory cookie jar for each provider session:
- When a server responds with `Set-Cookie`, the host stores those cookies.
- Subsequent calls to `context.fetch()` on the same domain automatically include the stored cookies.
- If a site requires an initial session handshake, simply fetch the homepage first before calling the protected API:

```ts
// Establish session and receive cookies
await context.fetch("https://example.com/");

// Call API with preserved session cookies
const apiResponse = await context.fetch("https://example.com/ajax/episodes");
```

---

## Rate Limiting & Retry Strategies

If a remote server responds with `429 Too Many Requests` or intermittent `503 Service Unavailable`, implement an exponential backoff retry:

```ts
async function fetchWithRetry(
  context: ProviderContext,
  url: string,
  maxRetries = 3
): Promise<FetchResponse> {
  let delay = 1000;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    const res = await context.fetch(url);

    if (res.status !== 429 && res.status < 500) {
      return res;
    }

    context.logger.warn(`Received status ${res.status} on attempt ${attempt}. Retrying in ${delay}ms...`);
    await new Promise((resolve) => setTimeout(resolve, delay));
    delay *= 2; // exponential backoff
  }

  throw new Error(`Failed to fetch ${url} after ${maxRetries} attempts`);
}
```

---

## Managing Preferences and Mirrors

The Host Broker provides secure key-value storage and mirror management:

### Reading and Writing Settings

```ts
// Read setting configured in settings.json
const customDomain = await context.getSetting("custom_domain");

// Persist an updated state (e.g. auth token)
await context.setSetting("auth_token", newToken);
```

### Resolving Mirrors

Users can configure their preferred server mirror in provider settings:

```ts
// Resolves the currently active mirror URL selected by the user
const mirror = await context.getMirror();
const url = `${mirror}/api/search?q=${encodeURIComponent(query)}`;
```

---

## Structured Logging via `context.logger`

Never use `console.log()` in production provider code. Use `context.logger` to write structured, leveled log entries directly into Zenith's Kermit logging engine:

```ts
// Debug: Detailed tracing (disabled in production release builds)
context.logger.debug(`Found ${items.length} items on page ${page}`);

// Info: General lifecycle events
context.logger.info(`Initialized session with mirror: ${mirror}`);

// Warn: Non-critical unexpected conditions
context.logger.warn(`Missing poster for anime ID: ${id}`);

// Error: Failures with optional Error stack traces
context.logger.error("Failed to parse episode list", err);
```
