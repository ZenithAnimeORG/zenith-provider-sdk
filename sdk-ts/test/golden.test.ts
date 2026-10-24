import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { PluginManifest, CatalogSearchResult, CatalogMediaDetails, ProviderVideoSource, ImportBatch } from "../src/models.js";
import { validateManifest } from "../src/package.js";

describe("Cross-Language Golden Fixtures (TypeScript)", () => {
  const fixturesDir = path.resolve(__dirname, "../../fixtures");

  it("validates manifest_golden.json matches TypeScript PluginManifest schema", () => {
    const raw = fs.readFileSync(path.join(fixturesDir, "manifest_golden.json"), "utf-8");
    const manifest = JSON.parse(raw) as PluginManifest;

    expect(manifest.id).toBe("shikimori");
    expect(manifest.name).toBe("Shikimori");
    expect(manifest.packageFormat).toBe("zpk/v2");
    expect(manifest.moduleApiVersion).toBe("1.0.0");
    expect(manifest.entrypoint).toBe("index.js");
    expect(manifest.capabilities).toContain("CATALOG");
    expect(manifest.capabilities).toContain("TRACKER");
    expect(manifest.localizedNames?.["ru"]).toBe("Шикимори");

    const validation = validateManifest(manifest);
    expect(validation.valid).toBe(true);
    expect(validation.errors).toHaveLength(0);
  });

  it("verifies search_response.json matches CatalogSearchResult schema", () => {
    const raw = fs.readFileSync(path.join(fixturesDir, "search_response.json"), "utf-8");
    const payload = JSON.parse(raw);

    expect(payload.type).toBe("Success");
    expect(payload.value.type).toBe("CatalogSearch");
    const result = payload.value.result as CatalogSearchResult;

    expect(result.hasNextPage).toBe(true);
    expect(result.items).toHaveLength(1);
    const item = result.items[0];
    expect(item.mediaRef.id).toBe("1");
    expect(item.title).toBe("Cowboy Bebop");
    expect(item.score).toBe(8.75);
    expect(item.year).toBe(1998);
  });

  it("verifies details_response.json matches CatalogMediaDetails schema", () => {
    const raw = fs.readFileSync(path.join(fixturesDir, "details_response.json"), "utf-8");
    const payload = JSON.parse(raw);

    expect(payload.type).toBe("Success");
    expect(payload.value.type).toBe("CatalogDetails");
    const details = payload.value.result as CatalogMediaDetails;

    expect(details.title).toBe("Cowboy Bebop");
    expect(details.originalTitle).toBe("カウボーイビバップ");
    expect(details.genres).toEqual(["Action", "Sci-Fi", "Space"]);
    expect(details.studios).toEqual(["Sunrise"]);
    expect(details.episodesCount).toBe(26);
  });

  it("verifies sources_response.json matches ProviderVideoSource schema", () => {
    const raw = fs.readFileSync(path.join(fixturesDir, "sources_response.json"), "utf-8");
    const payload = JSON.parse(raw);

    expect(payload.type).toBe("Success");
    expect(payload.value.type).toBe("VideoSources");
    const sources = payload.value.result as ProviderVideoSource[];

    expect(sources).toHaveLength(1);
    expect(sources[0].name).toBe("Kodik");
    expect(sources[0].translationType).toBe("DUB");
    expect(sources[0].episodes).toHaveLength(1);
    expect(sources[0].episodes[0].number).toBe(1);
    expect(sources[0].episodes[0].quality?.["1080p"]).toContain("1080.m3u8");
  });

  it("verifies import_response.json matches ImportBatch schema", () => {
    const raw = fs.readFileSync(path.join(fixturesDir, "import_response.json"), "utf-8");
    const payload = JSON.parse(raw);

    expect(payload.type).toBe("Success");
    expect(payload.value.type).toBe("ImportData");
    const batch = payload.value.result as ImportBatch;

    expect(batch.sourceName).toBe("anixart");
    expect(batch.entries).toHaveLength(1);
    expect(batch.entries[0].title).toBe("Steins;Gate");
    expect(batch.entries[0].targetStatus).toBe("COMPLETED");
    expect(batch.entries[0].rating).toBe(10);
    expect(batch.entries[0].watchedEpisodes).toBe(24);
  });
});
