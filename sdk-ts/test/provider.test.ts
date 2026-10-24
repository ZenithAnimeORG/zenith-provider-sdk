import { describe, it, expect, beforeEach } from "vitest";
import { defineProvider } from "../src/provider.js";
import { handleZenithDispatch } from "../src/dispatcher.js";
import { CatalogSearchResult, MediaRef, ProviderVideoSource } from "../src/models.js";
import { FetchHeaders } from "../src/broker.js";

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
              mediaRef: { id: "aot-1", title: "Attack on Titan" },
              title: "Attack on Titan",
              originalTitle: "Shingeki no Kyojin",
              year: 2013,
              score: 8.9,
            },
          ],
          hasNextPage: true,
        };
      },
    });

    const request = JSON.stringify({
      kind: "CatalogSearch",
      type: "CatalogSearch",
      providerId: "test-catalog",
      query: "Attack on Titan",
      page: 1,
      pageSize: 20,
    });

    const rawResponse = await handleZenithDispatch(request);
    const response = JSON.parse(rawResponse);

    expect(response.type).toBe("Success");
    expect(response.kind).toBe("Success");
    expect(response.value.type).toBe("CatalogSearch");
    expect(response.value.kind).toBe("CatalogSearch");
    expect(response.value.result.hasNextPage).toBe(true);
    expect(response.value.result.items).toHaveLength(1);
    expect(response.value.result.items[0].mediaRef.id).toBe("aot-1");
  });

  it("dispatches MediaSources and handles streams", async () => {
    const mediaRef: MediaRef = { id: "media-42", title: "Sample Anime" };

    defineProvider({
      async getSources(ref) {
        expect(ref.id).toBe("media-42");
        return [
          {
            name: "HLS 1080p",
            translationName: "Subtitles",
            translationType: "SUB",
            episodes: [
              {
                number: 1,
                url: "https://stream.example.com/ep1.m3u8",
                quality: { "1080p": "https://stream.example.com/ep1.m3u8" },
              },
            ],
          },
        ];
      },
    });

    const request = JSON.stringify({
      kind: "MediaSources",
      type: "MediaSources",
      mediaRef,
    });

    const rawResponse = await handleZenithDispatch(request);
    const response = JSON.parse(rawResponse);

    expect(response.type).toBe("Success");
    expect(response.value.type).toBe("VideoSources");
    expect(response.value.result).toHaveLength(1);
    expect(response.value.result[0].name).toBe("HLS 1080p");
    expect(response.value.result[0].episodes[0].number).toBe(1);
  });

  it("dispatches TorrentSearch with searchTorrents capability", async () => {
    defineProvider({
      async searchTorrents(mediaRef) {
        return [
          {
            title: `${mediaRef.title} [1080p]`,
            magnetUri: "magnet:?xt=urn:btih:abcdef123456",
            seeders: 50,
            leechers: 5,
          },
        ];
      },
    });

    const request = JSON.stringify({
      kind: "TorrentSearch",
      type: "TorrentSearch",
      mediaRef: { id: "101", title: "Frieren" },
    });

    const raw = await handleZenithDispatch(request);
    const res = JSON.parse(raw);

    expect(res.type).toBe("Success");
    expect(res.value.type).toBe("TorrentSources");
    expect(res.value.result[0].seeders).toBe(50);
  });

  it("dispatches SubtitleSearch and SubtitleDownloadLink", async () => {
    defineProvider({
      async searchSubtitles(query, languages) {
        expect(query).toBe("Steins;Gate");
        expect(languages).toBe("ru,en");
        return [
          {
            fileId: 999,
            language: "ru",
            releaseName: "Subs-Team",
            directUrl: "https://sub.example.com/999.srt",
          },
        ];
      },
      async getSubtitleDownloadLink(fileId) {
        expect(fileId).toBe(999);
        return "https://sub.example.com/download/999";
      },
    });

    const searchReq = JSON.stringify({
      type: "SubtitleSearch",
      query: "Steins;Gate",
      languages: "ru,en",
    });
    const searchRes = JSON.parse(await handleZenithDispatch(searchReq));
    expect(searchRes.value.type).toBe("Subtitles");
    expect(searchRes.value.result[0].fileId).toBe(999);

    const dlReq = JSON.stringify({
      type: "SubtitleDownloadLink",
      fileId: 999,
    });
    const dlRes = JSON.parse(await handleZenithDispatch(dlReq));
    expect(dlRes.value.type).toBe("DownloadLink");
    expect(dlRes.value.url).toBe("https://sub.example.com/download/999");
  });

  it("dispatches SkipTimes with full metadata", async () => {
    defineProvider({
      async getSkipTimes(mediaRef, episodeNumber, episodeLength, translationName) {
        expect(episodeNumber).toBe(1);
        expect(episodeLength).toBe(1420);
        expect(translationName).toBe("AniLibria");
        return [
          {
            startTime: 90,
            endTime: 180,
            skipType: "op",
          },
        ];
      },
    });

    const req = JSON.stringify({
      type: "SkipTimes",
      mediaRef: { id: "1", title: "Show" },
      episodeNumber: 1,
      episodeLength: 1420,
      translationName: "AniLibria",
    });

    const res = JSON.parse(await handleZenithDispatch(req));
    expect(res.value.type).toBe("SkipTimes");
    expect(res.value.result[0].skipType).toBe("op");
    expect(res.value.result[0].startTime).toBe(90);
  });

  it("dispatches StreamCanExtract and StreamExtract", async () => {
    defineProvider({
      async canExtractStream(name, url) {
        return name === "kodik" && url.includes("kodik");
      },
      async extractStream(name, url) {
        return [{ url: "https://stream.example.com/direct.mp4", quality: "1080p", isHls: false }];
      },
    });

    const canReq = JSON.stringify({
      type: "StreamCanExtract",
      extractorName: "kodik",
      url: "https://kodik.info/video/1",
    });
    const canRes = JSON.parse(await handleZenithDispatch(canReq));
    expect(canRes.value.type).toBe("BooleanResult");
    expect(canRes.value.value).toBe(true);

    const extReq = JSON.stringify({
      type: "StreamExtract",
      extractorName: "kodik",
      url: "https://kodik.info/video/1",
    });
    const extRes = JSON.parse(await handleZenithDispatch(extReq));
    expect(extRes.value.type).toBe("MediaStreams");
    expect(extRes.value.result[0].url).toContain("direct.mp4");
  });

  it("dispatches Tracker onEpisodeWatched and syncStatus", async () => {
    let watchedCalled = false;
    let syncCalled = false;

    defineProvider({
      async onEpisodeWatched(ref, ep) {
        expect(ep).toBe(5);
        watchedCalled = true;
      },
      async syncStatus(ref, status) {
        expect(status).toBe("COMPLETED");
        syncCalled = true;
      },
    });

    const watchReq = JSON.stringify({
      type: "EpisodeWatched",
      mediaRef: { id: "10" },
      episode: 5,
    });
    const watchRes = JSON.parse(await handleZenithDispatch(watchReq));
    expect(watchRes.value.type).toBe("UnitResult");
    expect(watchedCalled).toBe(true);

    const syncReq = JSON.stringify({
      type: "SyncStatus",
      mediaRef: { id: "10" },
      status: "COMPLETED",
    });
    const syncRes = JSON.parse(await handleZenithDispatch(syncReq));
    expect(syncRes.value.type).toBe("UnitResult");
    expect(syncCalled).toBe(true);
  });

  it("parses CSV with quoted commas in BookmarkImporter", async () => {
    const lines = [
      'title,episodes,status',
      '"Steins;Gate, The Movie",1,COMPLETED',
      '"Cowboy Bebop",26,WATCHING',
    ];

    const req = JSON.stringify({
      type: "ImportPreview",
      sampleLines: lines,
    });

    // Provide default importer
    globalThis.__zenith_bookmark_importer = {
      async preview(source) {
        const rows = await source.readHeaderAndSample(10);
        return { format: "csv", rows, totalDetectedCount: rows.length };
      },
      async *readBookmarks(source, context) {
        for await (const row of source.readCsvRows()) {
          yield context.bookmark({
            title: row.title,
            targetStatus: row.status as any,
            watchedEpisodes: Number(row.episodes),
          });
        }
      },
    };

    const previewRes = JSON.parse(await handleZenithDispatch(req));
    expect(previewRes.value.result.totalDetectedCount).toBe(3);

    const batchReq = JSON.stringify({
      type: "ImportBackup",
      lines,
    });
    const batchRes = JSON.parse(await handleZenithDispatch(batchReq));
    expect(batchRes.value.result.entries).toHaveLength(2);
    expect(batchRes.value.result.entries[0].title).toBe("Steins;Gate, The Movie");
    expect(batchRes.value.result.entries[0].watchedEpisodes).toBe(1);
    expect(batchRes.value.result.entries[1].title).toBe("Cowboy Bebop");
  });

  it("handles FetchHeaders with case-insensitivity and array values", () => {
    const headers = new FetchHeaders({
      "Content-Type": ["text/html", "charset=utf-8"],
      "X-Custom-Token": "secret123",
    });

    expect(headers.get("content-type")).toBe("text/html, charset=utf-8");
    expect(headers.get("Content-Type")).toBe("text/html, charset=utf-8");
    expect(headers.get("x-custom-token")).toBe("secret123");
    expect(headers.has("x-custom-token")).toBe(true);
    expect(headers.has("non-existent")).toBe(false);
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
    expect(response.kind).toBe("Failed");
    expect(response.code).toBe("PROVIDER_FAILURE");
    expect(response.message).toContain("Simulated upstream network timeout");
  });
});
