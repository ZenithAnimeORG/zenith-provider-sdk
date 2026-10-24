import { describe, it, expect } from "vitest";
import {
  validateManifest,
  validateEnvelope,
  MAX_PACKAGE_SIZE_BYTES,
  MAX_TOTAL_UNCOMPRESSED_BYTES,
  MAX_ENTRYPOINT_BYTES,
  PluginManifest,
} from "../src/index.js";

describe("package and manifest validation", () => {
  const validManifest: PluginManifest = {
    id: "sample_provider",
    name: "Sample Provider",
    version: "1.0.0",
    packageFormat: "zpk/v2",
    moduleApiVersion: "1.0.0",
    entrypoint: "index.js",
    capabilities: ["CATALOG", "MEDIA_SOURCE"],
  };

  it("accepts a well-formed manifest", () => {
    const res = validateManifest(validManifest);
    expect(res.valid).toBe(true);
    expect(res.errors).toHaveLength(0);
  });

  it("rejects manifest with invalid id characters", () => {
    const res = validateManifest({ ...validManifest, id: "invalid provider/id!" });
    expect(res.valid).toBe(false);
    expect(res.errors.some((e) => e.includes("id"))).toBe(true);
  });

  it("rejects manifest with empty capabilities", () => {
    const res = validateManifest({ ...validManifest, capabilities: [] });
    expect(res.valid).toBe(false);
    expect(res.errors.some((e) => e.includes("capabilities"))).toBe(true);
  });

  it("accepts valid package envelope entries", () => {
    const entries = [
      { name: "manifest.json", size: 512 },
      { name: "index.js", size: 120_000 },
      { name: "icon.png", size: 16_000 },
      { name: "settings.json", size: 1024 },
    ];
    const res = validateEnvelope(entries, validManifest);
    expect(res.valid).toBe(true);
    expect(res.errors).toHaveLength(0);
  });

  it("rejects package envelope containing forbidden executable binaries", () => {
    const entries = [
      { name: "manifest.json", size: 512 },
      { name: "index.js", size: 50_000 },
      { name: "malicious.jar", size: 100_000 },
    ];
    const res = validateEnvelope(entries, validManifest);
    expect(res.valid).toBe(false);
    expect(res.errors.some((e) => e.includes("Forbidden executable binary"))).toBe(true);
  });

  it("rejects package envelope containing forbidden .apk binary", () => {
    const entries = [
      { name: "manifest.json", size: 512 },
      { name: "index.js", size: 50_000 },
      { name: "payload.apk", size: 100_000 },
    ];
    const res = validateEnvelope(entries, validManifest);
    expect(res.valid).toBe(false);
    expect(res.errors.some((e) => e.includes("Forbidden executable binary"))).toBe(true);
  });

  it("rejects unsafe entry paths including dot and double-slashes", () => {
    const entries = [
      { name: "manifest.json", size: 512 },
      { name: "index.js", size: 50_000 },
      { name: ".", size: 100 },
    ];
    const res = validateEnvelope(entries, validManifest);
    expect(res.valid).toBe(false);
    expect(res.errors.some((e) => e.includes("Unsafe entry path"))).toBe(true);
  });

  it("rejects envelope when entrypoint is missing", () => {
    const entries = [{ name: "manifest.json", size: 512 }];
    const res = validateEnvelope(entries, validManifest);
    expect(res.valid).toBe(false);
    expect(res.errors.some((e) => e.includes("missing declared entrypoint"))).toBe(true);
  });

  it("rejects envelope exceeding max entrypoint size", () => {
    const entries = [
      { name: "manifest.json", size: 512 },
      { name: "index.js", size: MAX_ENTRYPOINT_BYTES + 1 },
    ];
    const res = validateEnvelope(entries, validManifest);
    expect(res.valid).toBe(false);
    expect(res.errors.some((e) => e.includes("exceeds size limit"))).toBe(true);
  });
});
