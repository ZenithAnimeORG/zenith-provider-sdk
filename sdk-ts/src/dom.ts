/**
 * Pure JavaScript HTML parsing powered by Cheerio Slim (htmlparser2).
 * Runs 100% inside the QuickJS heap without Node.js buffer dependencies,
 * eliminating host-provided selector round-trips.
 */

// Standard web atob/btoa polyfill for QuickJS where base64 Web APIs are not built-in
if (typeof globalThis !== "undefined") {
  const b64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

  if (typeof globalThis.atob === "undefined") {
    globalThis.atob = function (input: string): string {
      const str = String(input).replace(/=+$/, "");
      if (str.length % 4 === 1) {
        throw new Error("Invalid character: the string to be decoded is not correctly encoded.");
      }
      let output = "";
      for (
        let bc = 0, bs = 0, buffer = 0, idx = 0;
        (buffer = b64chars.indexOf(str.charAt(idx++))), ~buffer;
        output += String.fromCharCode(255 & (bs >> ((-2 * bc) & 6)))
      ) {
        bs = bc % 4 ? bs * 64 + buffer : buffer;
        if (bc++ % 4 === 0) continue;
      }
      return output;
    };
  }

  if (typeof globalThis.btoa === "undefined") {
    globalThis.btoa = function (input: string): string {
      const str = String(input);
      let output = "";
      for (
        let block = 0, charCode = 0, idx = 0, map = b64chars;
        str.charAt(idx | 0) || ((map = "="), idx % 1);
        output += map.charAt(63 & (block >> (8 - (idx % 1) * 8)))
      ) {
        charCode = str.charCodeAt((idx += 3 / 4));
        if (charCode > 0xff) {
          throw new Error("btoa failed: string contains character outside Latin1 range");
        }
        block = (block << 8) | charCode;
      }
      return output;
    };
  }
}

import * as cheerio from "cheerio/slim";

export type CheerioAPI = cheerio.CheerioAPI;

/**
 * Loads an HTML document string into a Cheerio DOM querying instance.
 * Supports standard jQuery-like selector syntax: $('div.item').map(...)
 *
 * @param html The raw HTML string to parse
 * @returns Cheerio API instance rooted at the parsed HTML
 */
export function loadHtml(html: string): cheerio.CheerioAPI {
  return cheerio.load(html);
}

export { cheerio };
