import { describe, it, expect, beforeEach } from "vitest";
import { defineProvider } from "../src/provider.js";
import { handleZenithDispatch } from "../src/dispatcher.js";
import { CatalogSearchResult, MediaRef } from "../src/models.js";

describe("defineProvider & dispatcher", () => {
  beforeEach(() => {
    globalThis.__zenith_provider = undefined;
    globalThis.__zenith_host_fetch = undefined;
  });

  it("registers provider on globalThis and returns export object", () => {
    const exported = defineProvider({
      async test() {
        return { success: true, message: "Custom test" };
      },
    });

    expect(exported.type).toBe("provider");
    expect(globalThis.__zenith_provider).toBe(exported.definition);
  });

  it("dispatches CatalogSearch operation to provider implementation", async () => {
    defineProvider({
      async getSearch(query, page, pageSize, context) {
        expect(query).toBe("Attack on Titan");
        expect(page).toBe(1);
        expect(pageSize).toBe(20);
        expect(context.providerId).toBe("test-catalog");

        return {
          items: [
            {
              mediaRef: { providerId: "test-catalog", mediaId: "aot-1" },
              titles: { romaji: "Shingeki no Kyojin", english: "Attack on Titan" },
              year: 2013,
              score: 8.9,
            },
          ],
          hasNextPage: true,
        };
      },
    });

    const request = JSON.stringify({
      type: "CatalogSearch",
      providerId: "test-catalog",
      query: "Attack on Titan",
      page: 1,
      pageSize: 20,
    });

    const rawResponse = await handleZenithDispatch(request);
    const response = JSON.parse(rawResponse);

    expect(response.type).toBe("Success");
    expect(response.value.type).toBe("CatalogSearch");
    expect(response.value.result.hasNextPage).toBe(true);
    expect(response.value.result.items).toHaveLength(1);
    expect(response.value.result.items[0].mediaRef.mediaId).toBe("aot-1");
  });

  it("dispatches MediaSources and handles streams", async () => {
    const mediaRef: MediaRef = { providerId: "test-stream", mediaId: "media-42" };

    defineProvider({
      async getSources(ref, ep) {
        expect(ref.mediaId).toBe("media-42");
        expect(ep).toBe(3);
        return [
          {
            id: "src-1080p",
            name: "HLS 1080p",
            url: "https://stream.example.com/master.m3u8",
            quality: "1080p",
            isDirect: false,
          },
        ];
      },
    });

    const request = JSON.stringify({
      type: "MediaSources",
      mediaRef,
      episodeNumber: 3,
    });

    const rawResponse = await handleZenithDispatch(request);
    const response = JSON.parse(rawResponse);

    expect(response.type).toBe("Success");
    expect(response.value.type).toBe("VideoSources");
    expect(response.value.result).toHaveLength(1);
    expect(response.value.result[0].id).toBe("src-1080p");
    expect(response.value.result[0].quality).toBe("1080p");
  });

  it("catches runtime errors and serializes as typed ProviderWorkerResult.Failed", async () => {
    defineProvider({
      async getSearch() {
        throw new Error("Simulated upstream network timeout");
      },
    });

    const request = JSON.stringify({
      type: "CatalogSearch",
      query: "Fails",
      page: 1,
      pageSize: 10,
    });

    const rawResponse = await handleZenithDispatch(request);
    const response = JSON.parse(rawResponse);

    expect(response.type).toBe("Failed");
    expect(response.code).toBe("PROVIDER_FAILURE");
    expect(response.message).toContain("Simulated upstream network timeout");
  });
});
