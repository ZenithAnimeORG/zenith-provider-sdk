import { ProviderMediaStream } from "./models.js";

export interface FetchInit {
  method?: string;
  headers?: Record<string, string>;
  body?: string;
  timeoutMillis?: number;
}

export class FetchHeaders {
  private readonly map = new Map<string, string>();

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

  get(name: string): string | null {
    return this.map.get(name.toLowerCase()) ?? null;
  }

  has(name: string): boolean {
    return this.map.has(name.toLowerCase());
  }

  forEach(callback: (value: string, key: string) => void): void {
    this.map.forEach((value, key) => callback(value, key));
  }

  entries(): IterableIterator<[string, string]> {
    return this.map.entries();
  }

  keys(): IterableIterator<string> {
    return this.map.keys();
  }

  values(): IterableIterator<string> {
    return this.map.values();
  }

  [key: string]: any;
}

export interface FetchResponse {
  status: number;
  statusText: string;
  ok: boolean;
  headers: FetchHeaders;
  text(): Promise<string>;
  json<T = any>(): Promise<T>;
}

export interface ProviderLogger {
  debug(message: string): void;
  info(message: string): void;
  warn(message: string): void;
  error(message: string, error?: unknown): void;
}

/**
 * Declares the host broker functions attached to globalThis by the Kotlin host runtime.
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
 * Host-mediated provider execution context.
 * Strictly adheres to the host broker boundary: HTTP, settings, mirrors, and stream resolution
 * are mediated by Zenith host Ktor client and security policies.
 */
export class ProviderContext {
  readonly logger: ProviderLogger;

  constructor(
    readonly providerId: string = "provider",
    private readonly settingsSnapshot: Record<string, string> = {}
  ) {
    this.logger = new DefaultProviderLogger(providerId);
  }

  /**
   * Performs an HTTP request mediated by the Zenith host Ktor broker.
   * Host enforces redirect domain allowlists, user-agent headers, and body byte quotas.
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
   */
  async setSetting(key: string, value: string): Promise<void> {
    this.settingsSnapshot[key] = value;
    if (typeof globalThis.__zenith_host_set_setting === "function") {
      await globalThis.__zenith_host_set_setting(key, value);
    }
  }

  /**
   * Resolves the active provider mirror URL chosen by user preference.
   */
  async getMirror(): Promise<string> {
    if (typeof globalThis.__zenith_host_get_mirror === "function") {
      return await globalThis.__zenith_host_get_mirror();
    }
    return this.settingsSnapshot["base_url"] ?? "";
  }

  /**
   * Mediates stream extraction through host-registered StreamExtractors.
   */
  async resolveStream(extractorName: string, url: string): Promise<ProviderMediaStream[]> {
    if (typeof globalThis.__zenith_host_resolve_stream === "function") {
      const resultJson = await globalThis.__zenith_host_resolve_stream(extractorName, url);
      return JSON.parse(resultJson) as ProviderMediaStream[];
    }
    return [];
  }
}
