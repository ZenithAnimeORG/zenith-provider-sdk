/**
 * Host broker integration providing secure network, logging, and settings access.
 *
 * All operations cross the security boundary into Zenith's host Kotlin Ktor client.
 * Direct filesystem, arbitrary network, and shell access are denied by QuickJS runtime confinement.
 *
 * @module broker
 */

import { ProviderMediaStream } from "./models.js";

/**
 * Options for configuring host-mediated HTTP fetch requests.
 */
export interface FetchInit {
  /**
   * HTTP request method (e.g. `"GET"`, `"POST"`, `"PUT"`, `"HEAD"`). Defaults to `"GET"`.
   */
  method?: string;

  /**
   * Key-value map of HTTP request headers.
   */
  headers?: Record<string, string>;

  /**
   * Request body string for POST/PUT requests (e.g. JSON string or form data).
   */
  body?: string;

  /**
   * Request timeout in milliseconds. Host defaults to 15,000 ms if unspecified.
   */
  timeoutMillis?: number;
}

/**
 * Case-insensitive HTTP header collection matching the standard Web Headers interface.
 *
 * @example
 * ```ts
 * const headers = new FetchHeaders({
 *   "Content-Type": "application/json",
 *   "User-Agent": "Zenith-Provider/1.0"
 * });
 *
 * console.log(headers.get("content-type")); // "application/json"
 * console.log(headers.has("User-Agent"));   // true
 * ```
 */
export class FetchHeaders {
  private readonly map = new Map<string, string>();

  /**
   * Creates a new FetchHeaders instance from a dictionary of header values.
   *
   * @param init Optional dictionary of key-value header pairs.
   */
  constructor(init?: Record<string, any> | null) {
    if (!init) return;
    for (const [key, val] of Object.entries(init)) {
      if (val === null || val === undefined) continue;
      const lowerKey = key.toLowerCase();
      const stringVal = Array.isArray(val) ? val.join(", ") : String(val);
      this.map.set(lowerKey, stringVal);
      (this as any)[key] = stringVal;
      (this as any)[lowerKey] = stringVal;
    }
  }

  /**
   * Retrieves the value of a header by name (case-insensitive).
   *
   * @param name The header name to look up.
   * @returns The header value, or `null` if the header is not present.
   */
  get(name: string): string | null {
    return this.map.get(name.toLowerCase()) ?? null;
  }

  /**
   * Checks whether a header exists in the collection (case-insensitive).
   *
   * @param name The header name to check.
   * @returns `true` if the header exists, otherwise `false`.
   */
  has(name: string): boolean {
    return this.map.has(name.toLowerCase());
  }

  /**
   * Executes a callback function for each header entry.
   *
   * @param callback Function receiving value and key.
   */
  forEach(callback: (value: string, key: string) => void): void {
    this.map.forEach((value, key) => callback(value, key));
  }

  /**
   * Returns an iterator of `[key, value]` pairs.
   */
  entries(): IterableIterator<[string, string]> {
    return this.map.entries();
  }

  /**
   * Returns an iterator of header names.
   */
  keys(): IterableIterator<string> {
    return this.map.keys();
  }

  /**
   * Returns an iterator of header values.
   */
  values(): IterableIterator<string> {
    return this.map.values();
  }

  [key: string]: any;
}

/**
 * Brokered HTTP response returned by `context.fetch()`.
 *
 * Implements a subset of the standard Fetch API Response object.
 *
 * @example
 * ```ts
 * const response = await context.fetch("https://api.example.com/items");
 * if (response.ok) {
 *   const data = await response.json<{ results: any[] }>();
 *   console.log(data.results.length);
 * }
 * ```
 */
export interface FetchResponse {
  /**
   * HTTP status code (e.g. 200, 404, 500).
   */
  status: number;

  /**
   * HTTP status message (e.g. "OK", "Not Found").
   */
  statusText: string;

  /**
   * Boolean indicating whether the request succeeded with a 2xx status code.
   */
  ok: boolean;

  /**
   * Response headers collection.
   */
  headers: FetchHeaders;

  /**
   * Reads and decodes the response body as a UTF-8 text string.
   */
  text(): Promise<string>;

  /**
   * Reads and parses the response body as JSON.
   *
   * @typeParam T Target object type.
   */
  json<T = any>(): Promise<T>;
}

/**
 * Structured logger writing into Zenith's centralized Kermit logging subsystem.
 *
 * Log messages are tagged with the provider ID and rendered in developer tools and logcats.
 *
 * @example
 * ```ts
 * context.logger.info("Searching catalog for: " + query);
 * context.logger.warn("Received empty response from mirror");
 * context.logger.error("Parsing failed", err);
 * ```
 */
export interface ProviderLogger {
  /**
   * Records a verbose debug trace message.
   *
   * @param message Debug log text.
   */
  debug(message: string): void;

  /**
   * Records an informational status message.
   *
   * @param message Informational log text.
   */
  info(message: string): void;

  /**
   * Records a warning message indicating non-critical issues or unexpected states.
   *
   * @param message Warning log text.
   */
  warn(message: string): void;

  /**
   * Records an error message and optional exception trace.
   *
   * @param message Error description.
   * @param error Optional Error instance or cause.
   */
  error(message: string, error?: unknown): void;
}

/**
 * Host broker function interfaces injected into the QuickJS global context by the Zenith Kotlin runtime.
 * @internal
 */
declare global {
  var __zenith_host_fetch:
    | ((requestJson: string) => Promise<string>)
    | undefined;
  var __zenith_host_get_setting:
    | ((key: string) => Promise<string | null | undefined>)
    | undefined;
  var __zenith_host_set_setting:
    | ((key: string, value: string) => Promise<void>)
    | undefined;
  var __zenith_host_get_mirror: (() => Promise<string>) | undefined;
  var __zenith_host_resolve_stream:
    | ((extractor: string, url: string) => Promise<string>)
    | undefined;
  var __zenith_host_log:
    | ((level: string, message: string) => void)
    | undefined;
}

class DefaultProviderLogger implements ProviderLogger {
  constructor(private readonly providerId: string = "provider") {}

  debug(message: string): void {
    this.log("DEBUG", message);
  }

  info(message: string): void {
    this.log("INFO", message);
  }

  warn(message: string): void {
    this.log("WARN", message);
  }

  error(message: string, error?: unknown): void {
    const errorDetails = error instanceof Error ? `: ${error.message}` : error ? `: ${String(error)}` : "";
    this.log("ERROR", `${message}${errorDetails}`);
  }

  private log(level: string, message: string): void {
    if (typeof globalThis.__zenith_host_log === "function") {
      globalThis.__zenith_host_log(level, `[${this.providerId}] ${message}`);
    } else if (typeof console !== "undefined") {
      const fn = level === "ERROR" ? console.error : level === "WARN" ? console.warn : console.log;
      fn(`[${level}] [${this.providerId}] ${message}`);
    }
  }
}

/**
 * Host-mediated provider execution context passed to all {@link ProviderDefinition} methods.
 *
 * Encapsulates outbound HTTP communication, settings persistence, mirror resolution,
 * and structured logging. All external calls are mediated by Zenith host Ktor client
 * ensuring strict domain allowlist adherence and safe memory quotas.
 *
 * @example
 * ```ts
 * const mirror = await context.getSetting("mirror");
 * const response = await context.fetch(`${mirror}/search?q=${encodeURIComponent(query)}`);
 * const html = await response.text();
 * ```
 */
export class ProviderContext {
  /**
   * Structured logger writing into Zenith's Kermit logging engine.
   */
  readonly logger: ProviderLogger;

  /**
   * Constructs a new ProviderContext.
   *
   * @param providerId Unique ID of the provider plugin.
   * @param settingsSnapshot In-memory dictionary of current settings (primarily for unit tests).
   */
  constructor(
    readonly providerId: string = "provider",
    private readonly settingsSnapshot: Record<string, string> = {}
  ) {
    this.logger = new DefaultProviderLogger(providerId);
  }

  /**
   * Performs an HTTP request mediated by the Zenith host Ktor broker.
   *
   * Security & Quota Guarantees:
   * - Destination URL and all redirect targets MUST match the `hosts` allowlist declared in `manifest.json`.
   * - Maximum response size is capped at 16 MiB per request.
   * - Cookie jars and User-Agent headers are managed securely by the host.
   *
   * @param url Fully qualified HTTP/HTTPS URL.
   * @param init Optional request parameters (headers, body, method, timeout).
   * @returns Brokered HTTP response with `.text()` and `.json()` decoders.
   *
   * @example
   * ```ts
   * const res = await context.fetch("https://api.example.com/items", {
   *   method: "POST",
   *   headers: { "Content-Type": "application/json" },
   *   body: JSON.stringify({ page: 1 })
   * });
   * const json = await res.json();
   * ```
   */
  async fetch(url: string, init?: FetchInit): Promise<FetchResponse> {
    if (typeof globalThis.__zenith_host_fetch === "function") {
      const requestPayload = {
        url,
        method: init?.method ?? "GET",
        headers: init?.headers ?? {},
        body: init?.body ?? null,
        timeoutMillis: init?.timeoutMillis,
      };
      const responseRaw = await globalThis.__zenith_host_fetch(JSON.stringify(requestPayload));
      const parsed = JSON.parse(responseRaw);
      const status = parsed.statusCode ?? parsed.status ?? 200;
      const body = parsed.body ?? "";
      const headers = new FetchHeaders(parsed.headers);

      return {
        status,
        statusText: status >= 200 && status < 300 ? "OK" : `Error ${status}`,
        ok: status >= 200 && status < 300,
        headers,
        async text() {
          return body;
        },
        async json<T = any>() {
          return JSON.parse(body) as T;
        },
      };
    }

    // Fallback to standard fetch when running in unit test or mock environments
    if (typeof fetch === "function") {
      const res = await fetch(url, {
        method: init?.method ?? "GET",
        headers: init?.headers,
        body: init?.body,
      });
      const text = await res.text();
      const headersRecord: Record<string, string> = {};
      res.headers.forEach((v, k) => {
        headersRecord[k] = v;
      });
      const headers = new FetchHeaders(headersRecord);

      return {
        status: res.status,
        statusText: res.statusText,
        ok: res.ok,
        headers,
        async text() {
          return text;
        },
        async json<T = any>() {
          return JSON.parse(text) as T;
        },
      };
    }

    throw new Error("No host fetch broker available in current runtime environment");
  }

  /**
   * Retrieves a setting value scoped to this provider.
   *
   * Reads from Zenith's encrypted secure preference store.
   *
   * @param key The setting key defined in `settings.json` or `manifest.json`.
   * @returns The configured string value, or `undefined` if unset.
   *
   * @example
   * ```ts
   * const apiKey = await context.getSetting("api_key");
   * ```
   */
  async getSetting(key: string): Promise<string | undefined> {
    if (typeof globalThis.__zenith_host_get_setting === "function") {
      const val = await globalThis.__zenith_host_get_setting(key);
      return val ?? undefined;
    }
    return this.settingsSnapshot[key];
  }

  /**
   * Persists a setting value scoped to this provider.
   *
   * Writes into Zenith's persistent preference store.
   *
   * @param key The setting key to persist.
   * @param value The value to save.
   *
   * @example
   * ```ts
   * await context.setSetting("token", sessionToken);
   * ```
   */
  async setSetting(key: string, value: string): Promise<void> {
    this.settingsSnapshot[key] = value;
    if (typeof globalThis.__zenith_host_set_setting === "function") {
      await globalThis.__zenith_host_set_setting(key, value);
    }
  }

  /**
   * Resolves the active provider mirror URL chosen by user preference.
   *
   * If the user selected a custom mirror in provider settings, this returns that URL.
   *
   * @returns The active mirror base URL.
   *
   * @example
   * ```ts
   * const mirror = await context.getMirror();
   * const url = `${mirror}/catalog`;
   * ```
   */
  async getMirror(): Promise<string> {
    if (typeof globalThis.__zenith_host_get_mirror === "function") {
      return await globalThis.__zenith_host_get_mirror();
    }
    return this.settingsSnapshot["base_url"] ?? "";
  }

  /**
   * Mediates stream extraction through host-registered StreamExtractors.
   *
   * Allows a provider to delegate video stream extraction to built-in or plugin extractors.
   *
   * @param extractorName The extractor identifier (e.g. `"sibnet"`, `"kodik"`).
   * @param url The embed URL to resolve.
   * @returns Array of resolved playable media streams.
   *
   * @example
   * ```ts
   * const streams = await context.resolveStream("sibnet", embedUrl);
   * ```
   */
  async resolveStream(extractorName: string, url: string): Promise<ProviderMediaStream[]> {
    if (typeof globalThis.__zenith_host_resolve_stream === "function") {
      const resultJson = await globalThis.__zenith_host_resolve_stream(extractorName, url);
      return JSON.parse(resultJson) as ProviderMediaStream[];
    }
    return [];
  }
}
