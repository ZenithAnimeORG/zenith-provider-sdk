import { defineProvider } from "../../src/provider.js";
import { loadHtml } from "../../src/dom.js";
import "../../src/dispatcher.js";

export default defineProvider({
  async test() {
    return { success: true, message: "QuickJS Pilot Provider is operational" };
  },

  async getSearch(query, page, pageSize, context) {
    const res = await context.fetch(`https://api.example.com/search?q=${encodeURIComponent(query)}`);
    const html = await res.text();
    const $ = loadHtml(html);
    const items = $(".item").map((_, el) => ({
      mediaRef: {
        id: $(el).attr("data-id") ?? "",
        title: $(el).find(".title").text().trim(),
      },
      title: $(el).find(".title").text().trim(),
      score: 9.14,
      year: 2023,
      totalEpisodes: 28,
    })).get();

    return {
      items,
      hasNextPage: false,
    };
  },

  async resolveEpisode(episode, context) {
    const url = episode.url || "https://stream.example.com/master.m3u8";
    return [
      {
        url,
        quality: "1080p",
        isHls: true,
      },
    ];
  },

  async searchTorrents(mediaRef, context) {
    return [
      {
        title: `${mediaRef.title} [1080p Web-DL]`,
        magnetUri: "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567",
        seeders: 120,
        leechers: 8,
      },
    ];
  },

  async getSkipTimes(mediaRef, episodeNumber, episodeLength, translationName, context) {
    return [
      {
        startTime: 90,
        endTime: 180,
        skipType: "op",
      },
    ];
  },
});
