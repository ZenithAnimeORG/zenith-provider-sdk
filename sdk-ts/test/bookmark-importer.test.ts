import { describe, it, expect, beforeEach } from "vitest";
import { defineBookmarkImporter } from "../src/bookmark-importer.js";
import { handleZenithDispatch } from "../src/dispatcher.js";

describe("defineBookmarkImporter & dispatcher", () => {
  beforeEach(() => {
    globalThis.__zenith_bookmark_importer = undefined;
  });

  it("registers bookmark importer on globalThis and exports object", () => {
    const exported = defineBookmarkImporter({
      async preview() {
        return { format: "csv", rows: [] };
      },
      async *readBookmarks() {
        // empty
      },
    });

    expect(exported.type).toBe("bookmark-importer");
    expect(globalThis.__zenith_bookmark_importer).toBe(exported.definition);
  });

  it("dispatches ImportPreview operation", async () => {
    defineBookmarkImporter({
      async preview(source) {
        const header = await source.readHeaderAndSample(2);
        return {
          format: "csv",
          rows: header,
          totalDetectedCount: 150,
        };
      },
      async *readBookmarks() {},
    });

    const request = JSON.stringify({
      type: "ImportPreview",
      sampleLines: ["title,url,status", "Steins;Gate,https://example.com/sg,completed"],
    });

    const rawResponse = await handleZenithDispatch(request);
    const response = JSON.parse(rawResponse);

    expect(response.type).toBe("Success");
    expect(response.value.type).toBe("ImportPreviewResult");
    expect(response.value.result.format).toBe("csv");
    expect(response.value.result.rows).toHaveLength(2);
    expect(response.value.result.totalDetectedCount).toBe(150);
  });

  it("dispatches ImportReadBatch and yields parsed candidate bookmarks", async () => {
    defineBookmarkImporter({
      async preview() {
        return { format: "csv", rows: [] };
      },
      async *readBookmarks(source, context) {
        for await (const row of source.readCsvRows()) {
          yield context.bookmark({
            title: row.title ?? "",
            targetStatus: row.status === "completed" ? "COMPLETED" : "WATCHING",
            rating: row.score ? Number(row.score) : undefined,
          });
        }
      },
    });

    const request = JSON.stringify({
      type: "ImportReadBatch",
      providerId: "test-importer",
      lines: [
        "title,url,status,score",
        "Clannad,https://example.com/clannad,watching,10",
        "Angel Beats,https://example.com/ab,completed,9",
      ],
    });

    const rawResponse = await handleZenithDispatch(request);
    const response = JSON.parse(rawResponse);

    expect(response.type).toBe("Success");
    expect(response.value.type).toBe("ImportData");
    expect(response.value.result.sourceName).toBe("test-importer");
    expect(response.value.result.entries).toHaveLength(2);
    expect(response.value.result.entries[0].title).toBe("Clannad");
    expect(response.value.result.entries[0].rating).toBe(10);
    expect(response.value.result.entries[0].targetStatus).toBe("WATCHING");
    expect(response.value.result.entries[1].title).toBe("Angel Beats");
    expect(response.value.result.entries[1].rating).toBe(9);
    expect(response.value.result.entries[1].targetStatus).toBe("COMPLETED");
  });
});
