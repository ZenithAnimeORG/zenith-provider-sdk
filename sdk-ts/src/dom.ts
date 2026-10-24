/**
 * Pure JavaScript HTML parsing powered by Cheerio Slim (htmlparser2).
 * Runs 100% inside the QuickJS heap without Node.js buffer dependencies,
 * eliminating host-provided selector round-trips.
 */

import "./polyfills";
import * as cheerio from "cheerio/slim";

export type CheerioAPI = cheerio.CheerioAPI;

/**
 * Loads an HTML document string into a Cheerio DOM querying instance.
 * Supports standard jQuery-like selector syntax: $('div.item').map(...)
 *
 * @param html The raw HTML string to parse
 * @param options Optional Cheerio parser options
 * @returns Cheerio API instance rooted at the parsed HTML
 */
export function loadHtml(html: string | null | undefined, options?: any): cheerio.CheerioAPI {
  return cheerio.load(html ?? "", options);
}

export { cheerio };
