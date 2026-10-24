---
layout: home

hero:
  name: "Zenith Provider SDK"
  text: "TypeScript-плагины для медиаплеера Zenith"
  tagline: "Разрабатывайте быстрые, кроссплатформенные парсеры каталогов, видеоисточников, торрентов и субтитров, работающие в изолированном движке QuickJS."
  actions:
    - theme: brand
      text: Начать работу
      link: /ru/guides/getting-started
    - theme: alt
      text: Справочник API
      link: /api/
    - theme: alt
      text: Репозиторий шаблона
      link: https://github.com/ZenithAnimeORG/zenith-provider-template

features:
  - title: Чистый TypeScript без Gradle и Android SDK
    details: Никакой возни с Java или NDK. Вы пишете обычный TypeScript и собираете через esbuild легковесный бандл (~50–150 КБ).
  - title: Изоляция QuickJS в едином процессе
    details: Плагины исполняются в выделенном пуле QuickJS с жестким лимитом памяти 64 МБ, привязкой к потоку и тайм-аутами прерывания.
  - title: Безопасный шлюз Host Broker
    details: Сетевые запросы, настройки и зеркала контролируются нативным клиентом Zenith (Ktor) с проверкой белого списка хостов и безопасностью редиректов.
  - title: Встроенный парсинг HTML (Cheerio)
    details: Быстрая выборка элементов с jQuery-синтаксисом через `@zenith/sdk/dom`. Работает на 100% в памяти JS без накладных расходов IPC.
  - title: Универсальный пакет .zpk
    details: Провайдер пакуется в zip-архив `.zpk`, который устанавливается и работает одинаково на Android, Linux, macOS и Windows.
  - title: Широкая матрица возможностей
    details: Поддержка поиска по каталогу, прямых видеопотоков (HLS/MP4), стриминга торрентов через jlibtorrent, субтитров и таймкодов опенингов.
---

## Быстрый пример

Создание провайдера сводится к объявлению нужных методов и парсингу данных:

```ts
import { defineProvider } from "@zenith/sdk/provider";
import { loadHtml } from "@zenith/sdk/dom";

export default defineProvider({
  async getSearch(query, page, pageSize, context) {
    const mirror = await context.getMirror();
    const res = await context.fetch(`${mirror}/search?q=${encodeURIComponent(query)}&page=${page}`);
    const $ = loadHtml(await res.text());

    const items = $(".anime-card").map((_, el) => ({
      mediaRef: { id: $(el).attr("data-id") || "", title: $(el).find(".title").text().trim() },
      title: $(el).find(".title").text().trim(),
      posterUrl: $(el).find("img").attr("src"),
      score: parseFloat($(el).attr("data-score") || "0")
    })).get();

    return { items, hasNextPage: false };
  },

  async resolveEpisode(episode, context) {
    return [{
      url: episode.url,
      quality: "1080p",
      isHls: episode.url.includes(".m3u8")
    }];
  }
});
```

Готовы создать свой первый плагин? Ознакомьтесь с [Руководством по быстрому старту](/ru/guides/getting-started).
