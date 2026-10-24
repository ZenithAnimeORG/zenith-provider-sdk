# Возможности провайдеров (Capabilities)

В Zenith используется **модульная модель возможностей**. Вместо создания монолитных парсеров система позволяет плагинам объявлять точечные возможности в файле `manifest.json`.

Вы можете создать узкоспециализированный плагин (например, только для поиска субтитров или торрент-раздач) либо объединить несколько возможностей в полнофункциональный медиа-провайдер.

---

## Таблица возможностей

| Возможность | Значение в манифесте | Ключевые методы | Назначение |
|---|---|---|---|
| **Каталог** | `"CATALOG"` | `getSearch`, `getDetails`, `getHomeSections` | Поиск тайтлов, карточки описания, подборки на главном экране |
| **Видеоисточник** | `"MEDIA_SOURCE"` | `getSources`, `resolveEpisode` | Озвучки, субтитры, прямые потоки HLS/MP4 |
| **Торренты** | `"TORRENT_SOURCE"` | `searchTorrents`, `getTorrentSources` | Торрент-релизы, magnet-ссылки, число сидеров |
| **Субтитры** | `"SUBTITLES"` | `searchSubtitles`, `getSubtitleDownloadLink` | Внешние файлы субтитров (`.ass`, `.srt`) |
| **Таймкоды** | `"SKIP_TIMINGS"` | `getSkipTimes`, `getSkipTimings` | Интервалы пропуска опенингов (OP) и эндингов (ED) |
| **Экстрактор** | `"STREAM_EXTRACTOR"` | `canExtractStream`, `extractStream` | Парсинг сторонних видеохостингов (embed-плееров) |
| **Трекер** | `"TRACKER"` | `onEpisodeWatched`, `syncStatus` | Синхронизация отметок просмотра с внешним аккаунтом |
| **Импортер** | `"IMPORT"` | `defineBookmarkImporter` | Перенос закладок из файлов сторонних сервисов |

---

## 1. Возможность Каталога (`CATALOG`)

Каталог отвечает за строку поиска, вкладки рекомендаций и экран подробной информации.

### `getSearch(query, page, pageSize, context)`

Вызывается при вводе запроса в строке поиска. Возвращает массив карточек [`CatalogMediaItem`](/api/interfaces/CatalogMediaItem) и флаг наличия следующей страницы.

```ts
async getSearch(query, page, pageSize, context) {
  const mirror = await context.getMirror();
  const url = `${mirror}/search?q=${encodeURIComponent(query)}&page=${page}`;
  const res = await context.fetch(url);
  const json = await res.json();

  const items = json.items.map(item => ({
    mediaRef: {
      id: String(item.id),
      title: item.title,
      externalIds: { mal: String(item.mal_id || "") }
    },
    title: item.title,
    originalTitle: item.romaji,
    posterUrl: item.poster_url,
    score: item.score,
    year: item.year,
    totalEpisodes: item.episodes_count
  }));

  return {
    items,
    hasNextPage: json.page < json.total_pages
  };
}
```

### `getDetails(mediaRef, context)`

Вызывается при открытии экрана тайтла. Возвращает объект [`CatalogMediaDetails`](/api/interfaces/CatalogMediaDetails).

```ts
async getDetails(mediaRef, context) {
  const mirror = await context.getMirror();
  const res = await context.fetch(`${mirror}/anime/${encodeURIComponent(mediaRef.id)}`);
  const data = await res.json();

  return {
    mediaRef,
    title: data.title,
    originalTitle: data.title_japanese,
    description: data.synopsis,
    posterUrl: data.images.jpg.large_image_url,
    bannerUrl: data.images.banner_image_url,
    score: data.score,
    year: data.year,
    episodesCount: data.episodes,
    genres: data.genres.map(g => g.name),
    studios: data.studios.map(s => s.name),
    status: data.airing ? "AIRING" : "FINISHED"
  };
}
```

---

## 2. Возможность Видеоисточника (`MEDIA_SOURCE`)

Отвечает за воспроизведение видео. Предоставляет список доступных переводов (студии озвучки, субтитры) и разрешает прямые ссылки на видеофайлы для плеера MPV.

### `getSources(mediaRef, context, episodeNumber?)`

Возвращает массив групп переводов [`ProviderVideoSource`](/api/interfaces/ProviderVideoSource), каждая из которых содержит свой список серий.

```ts
async getSources(mediaRef, context) {
  return [
    {
      name: "Оригинал (Русские субтитры)",
      translationName: "Субтитры",
      translationType: "SUB",
      episodes: [
        { number: 1, url: `https://api.example.com/stream/${mediaRef.id}/1` },
        { number: 2, url: `https://api.example.com/stream/${mediaRef.id}/2` }
      ]
    },
    {
      name: "Дубляж",
      translationName: "Дубляж",
      translationType: "DUB",
      episodes: [
        { number: 1, url: `https://api.example.com/stream/${mediaRef.id}/1?dub=true` }
      ]
    }
  ];
}
```

### `resolveEpisode(episode, context)`

Преобразует выбранную серию в массив доступных потоков [`ProviderMediaStream`](/api/interfaces/ProviderMediaStream).

```ts
async resolveEpisode(episode, context) {
  const res = await context.fetch(`https://api.example.com/resolve?url=${encodeURIComponent(episode.url)}`);
  const data = await res.json();

  return [
    {
      url: data.hlsMasterUrl,
      quality: "1080p",
      isHls: true,
      headers: {
        "Referer": "https://player.example.com/",
        "User-Agent": "ZenithPlayer/1.0"
      }
    }
  ];
}
```

---

## 3. Возможность Торрентов (`TORRENT_SOURCE`)

Zenith включает встроенный движок стриминга торрентов на базе `jlibtorrent`. Провайдеры торрентов выполняют поиск релизов и предоставляют magnet-ссылки.

### `searchTorrents(mediaRef, context)`

Возвращает массив найденных раздач [`ProviderTorrentSource`](/api/interfaces/ProviderTorrentSource).

```ts
async searchTorrents(mediaRef, context) {
  const query = mediaRef.title || "";
  const res = await context.fetch(`https://rutracker.org/forum/tracker.php?nm=${encodeURIComponent(query)}`);
  const $ = loadHtml(await res.text());

  const torrents: ProviderTorrentSource[] = [];
  $("#tor-tbl tr.tCenter").each((_, el) => {
    const $row = $(el);
    const title = $row.find("a.tLink").text().trim();
    const magnetUri = $row.find("a[href^='magnet:']").attr("href");
    const seeders = parseInt($row.find(".seedmed").text().trim(), 10) || 0;
    const leechers = parseInt($row.find(".leechmed").text().trim(), 10) || 0;

    if (magnetUri) {
      torrents.push({
        title,
        magnetUri,
        seeders,
        leechers,
        quality: title.includes("1080p") ? "1080p" : "720p",
        trackerName: "RuTracker"
      });
    }
  });

  return torrents;
}
```

---

## 4. Возможность Субтитров (`SUBTITLES`)

Позволяет подгружать дорожки субтитров (`.ass`, `.srt`, `.vtt`) напрямую в плеер.

### `searchSubtitles(query, languages, episodeNumber, seasonNumber, context)`

```ts
async searchSubtitles(query, languages, episodeNumber, seasonNumber, context) {
  const res = await context.fetch(
    `https://api.example.com/subtitles?query=${encodeURIComponent(query)}&episode=${episodeNumber || ""}&langs=${languages}`
  );
  const data = await res.json();

  return data.results.map(sub => ({
    fileId: sub.id,
    language: sub.lang,
    releaseName: sub.release,
    isHearingImpaired: sub.is_sdh,
    directUrl: sub.download_url
  }));
}
```

---

## 5. Возможность Таймкодов (`SKIP_TIMINGS`)

Активирует кнопки быстрого пропуска опенинга и эндинга в плеере Zenith.

### `getSkipTimes(mediaRef, episodeNumber, episodeLength, translationName, context)`

Возвращает интервалы [`SkipInterval`](/api/interfaces/SkipInterval) в секундах:

```ts
async getSkipTimes(mediaRef, episodeNumber, episodeLength, translationName, context) {
  const malId = mediaRef.externalIds?.mal;
  if (!malId) return [];

  const res = await context.fetch(`https://api.aniskip.com/v2/skip-times/${malId}/${episodeNumber}?types=op&types=ed`);
  if (!res.ok) return [];

  const data = await res.json();
  const intervals: SkipInterval[] = [];

  for (const item of data.results || []) {
    intervals.push({
      startTime: item.interval.startTime,
      endTime: item.interval.endTime,
      skipType: item.skipType // "op" или "ed"
    });
  }

  return intervals;
}
```

---

## 6. Возможность Импорта закладок (`IMPORT`)

Импортеры разбирают файлы списков пользователя (MyAnimeList XML/CSV, Shikimori JSON, AniList JSON) и сохраняют их в локальную базу Zenith.

Импортер описывается через `defineBookmarkImporter` из `@zenith/sdk/bookmark-importer`:

```ts
import { defineBookmarkImporter } from "@zenith/sdk/bookmark-importer";

export default defineBookmarkImporter({
  supportedExtensions: [".csv"],
  instructions: [
    {
      stepNumber: 1,
      title: "Экспорт списка",
      description: "Войдите на сайт и сохраните ваш список аниме в формате CSV.",
      linkUrl: "https://example.com/export",
      linkText: "Страница экспорта"
    }
  ],

  async preview(source, context) {
    const rows = await source.readHeaderAndSample(5);
    return { format: "csv", rows };
  },

  async *readBookmarks(source, context) {
    for await (const row of source.readCsvRows()) {
      yield context.bookmark({
        title: row["anime_title"] || "",
        releaseYear: parseInt(row["year"] || "0", 10) || null,
        watchedEpisodes: parseInt(row["watched_episodes"] || "0", 10),
        totalEpisodes: parseInt(row["total_episodes"] || "0", 10) || null,
        targetStatus: row["status"] === "Completed" ? "COMPLETED" : "WATCHING",
        rating: parseFloat(row["score"] || "0") || null
      });
    }
  }
});
```
