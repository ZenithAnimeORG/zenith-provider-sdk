import * as esbuild from "esbuild";

const atobBanner = `
if (typeof globalThis !== "undefined") {
  const b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
  if (typeof globalThis.atob === "undefined") {
    globalThis.atob = function(input) {
      const str = String(input ?? "").replace(/[\\t\\n\\f\\r ]+/g, "").replace(/=+$/, "");
      let out = "";
      for (let i = 0; i < str.length; ) {
        const b0 = b64.indexOf(str.charAt(i++));
        const b1 = b64.indexOf(str.charAt(i++));
        const b2 = b64.indexOf(str.charAt(i++));
        const b3 = b64.indexOf(str.charAt(i++));
        if (b0 < 0 || b1 < 0) break;
        out += String.fromCharCode((b0 << 2) | (b1 >> 4));
        if (b2 >= 0) out += String.fromCharCode(((b1 & 15) << 4) | (b2 >> 2));
        if (b3 >= 0) out += String.fromCharCode(((b2 & 3) << 6) | b3);
      }
      return out;
    };
  }
  if (typeof globalThis.btoa === "undefined") {
    globalThis.btoa = function(input) {
      const str = String(input ?? "");
      let out = "";
      for (
        let block = 0, charCode = 0, idx = 0, map = b64;
        str.charAt(idx | 0) || ((map = "="), idx % 1);
        out += map.charAt(63 & (block >> (8 - (idx % 1) * 8)))
      ) {
        charCode = str.charCodeAt((idx += 3 / 4));
        if (charCode > 0xff) {
          throw new Error("btoa failed: string contains character outside Latin1 range");
        }
        block = (block << 8) | charCode;
      }
      return out;
    };
  }
}
`;

import * as fs from "node:fs";
import * as path from "node:path";

await esbuild.build({
  entryPoints: {
    index: "src/index.ts",
    provider: "src/provider.ts",
    "bookmark-importer": "src/bookmark-importer.ts",
    dom: "src/dom.ts",
    package: "src/package.ts",
    dispatcher: "src/dispatcher.ts",
  },
  bundle: true,
  outdir: "dist",
  format: "esm",
  platform: "browser",
  conditions: ["browser", "import"],
  target: "es2022",
  sourcemap: true,
  banner: {
    js: atobBanner,
  },
});

await esbuild.build({
  entryPoints: ["test/fixtures/sample-provider.ts"],
  bundle: true,
  outfile: "test/fixtures/sample-provider.bundle.js",
  format: "iife",
  target: "es2020",
  banner: {
    js: atobBanner,
  },
});

const bundleContent = fs.readFileSync("test/fixtures/sample-provider.bundle.js", "utf-8");
const sdkFixturePath = path.resolve("../fixtures/sample-provider.bundle.js");
if (fs.existsSync(path.dirname(sdkFixturePath))) {
  fs.writeFileSync(sdkFixturePath, bundleContent, "utf-8");
}
const appResourcePath = path.resolve("../../Zenith.App/core/data/src/jvmTest/resources/quickjs/sample-provider.bundle.js");
if (fs.existsSync(path.dirname(appResourcePath))) {
  fs.writeFileSync(appResourcePath, bundleContent, "utf-8");
}

