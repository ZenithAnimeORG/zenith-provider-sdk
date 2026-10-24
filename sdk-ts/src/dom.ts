/**
 * Pure JavaScript HTML parsing powered by Cheerio Slim (`htmlparser2`).
 *
 * Runs 100% inside the QuickJS heap without Node.js buffer dependencies,
 * eliminating host-provided selector round-trips and enabling fast, jQuery-style
 * DOM querying directly in-bundle.
 *
 * @module dom
 */

import "./polyfills.js";
import * as cheerio from "cheerio/slim";

/**
 * Root Cheerio API querying instance.
 * Provides standard jQuery-compatible traversal methods (`$()`, `.find()`, `.text()`, `.attr()`, `.map()`, `.filter()`).
 */
export type CheerioAPI = cheerio.CheerioAPI;

/**
 * Loads an HTML document string into a Cheerio DOM querying instance.
 *
 * Runs entirely inside the QuickJS virtual machine without any Node.js native dependencies.
 * Supports standard jQuery-like selector syntax: `$('div.item').map(...)`.
 *
 * @param html The raw HTML string to parse. Handles `null` or `undefined` safely by loading an empty document.
 * @param options Optional Cheerio parser options (such as XML mode or decode entities).
 * @returns Cheerio API instance rooted at the parsed HTML.
 *
 * @example
 * ```ts
 * import { loadHtml } from "@zenith/sdk/dom";
 *
 * const html = `
 *   <div class="anime-card" data-id="123" data-score="8.5">
 *     <h2 class="title">Frieren</h2>
 *     <img class="poster" src="https://example.com/cover.jpg" />
 *   </div>
 * `;
 *
 * const $ = loadHtml(html);
 * const title = $(".title").text().trim();
 * const id = $(".anime-card").attr("data-id");
 * const poster = $("img.poster").attr("src");
 * ```
 *
 * @example Iterating over collections with map and get
 * ```ts
 * const items = $(".anime-card").map((_, el) => {
 *   const $el = $(el);
 *   return {
 *     id: $el.attr("data-id") || "",
 *     title: $el.find(".title").text().trim(),
 *     score: parseFloat($el.attr("data-score") || "0")
 *   };
 * }).get();
 * ```
 */
export function loadHtml(html: string | null | undefined, options?: any): cheerio.CheerioAPI {
  return cheerio.load(html ?? "", options);
}

/**
 * Direct re-export of the Cheerio Slim module for advanced parser customization.
 */
export { cheerio };
