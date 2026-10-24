import { describe, it, expect } from "vitest";
import { loadHtml } from "../src/dom.js";

describe("@zenith/sdk/dom (Cheerio HTML parsing)", () => {
  const sampleHtml = `
    <!DOCTYPE html>
    <html>
      <head><title>Anime Catalog</title></head>
      <body>
        <div class="container">
          <ul class="anime-list">
            <li class="anime-item" data-id="101">
              <a href="/anime/101" class="title">Frieren: Beyond Journey's End</a>
              <span class="score">9.14</span>
              <img src="https://example.com/posters/frieren.jpg" alt="Poster" />
            </li>
            <li class="anime-item" data-id="102">
              <a href="/anime/102" class="title">Fullmetal Alchemist: Brotherhood</a>
              <span class="score">9.10</span>
              <img src="https://example.com/posters/fmab.jpg" alt="Poster" />
            </li>
          </ul>
        </div>
      </body>
    </html>
  `;

  it("loads HTML and queries DOM using standard jQuery syntax", () => {
    const $ = loadHtml(sampleHtml);

    expect($("title").text()).toBe("Anime Catalog");

    const items = $(".anime-item");
    expect(items).toHaveLength(2);

    const firstItem = items.first();
    expect(firstItem.attr("data-id")).toBe("101");
    expect(firstItem.find(".title").text()).toBe("Frieren: Beyond Journey's End");
    expect(firstItem.find(".title").attr("href")).toBe("/anime/101");
    expect(firstItem.find(".score").text()).toBe("9.14");
    expect(firstItem.find("img").attr("src")).toBe("https://example.com/posters/frieren.jpg");

    const secondItem = items.eq(1);
    expect(secondItem.attr("data-id")).toBe("102");
    expect(secondItem.find(".title").text()).toBe("Fullmetal Alchemist: Brotherhood");
  });

  it("extracts structured arrays cleanly using .map().get()", () => {
    const $ = loadHtml(sampleHtml);

    const mapped = $(".anime-item")
      .map((_, el) => ({
        id: $(el).attr("data-id") ?? "",
        title: $(el).find(".title").text().trim(),
        score: parseFloat($(el).find(".score").text().trim()),
        poster: $(el).find("img").attr("src") ?? "",
      }))
      .get();

    expect(mapped).toEqual([
      {
        id: "101",
        title: "Frieren: Beyond Journey's End",
        score: 9.14,
        poster: "https://example.com/posters/frieren.jpg",
      },
      {
        id: "102",
        title: "Fullmetal Alchemist: Brotherhood",
        score: 9.1,
        poster: "https://example.com/posters/fmab.jpg",
      },
    ]);
  });
});
