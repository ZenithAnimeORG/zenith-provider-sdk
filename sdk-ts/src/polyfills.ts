/**
 * Standard Web polyfills for QuickJS runtime environment where atob and btoa
 * are not built-in.
 */
if (typeof globalThis !== "undefined") {
  const b64chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

  if (typeof globalThis.atob === "undefined") {
    globalThis.atob = function (input: string): string {
      const str = String(input ?? "").replace(/[\t\n\f\r ]+/g, "").replace(/=+$/, "");
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
      const str = String(input ?? "");
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
