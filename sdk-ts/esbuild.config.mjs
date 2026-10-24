import * as esbuild from "esbuild";

const atobBanner = `
if (typeof globalThis.atob === "undefined") {
  const b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
  globalThis.atob = function(str) {
    str = String(str).replace(/=+$/, "");
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
`;

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
