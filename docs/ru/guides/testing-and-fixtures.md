# Тестирование и фикстуры (Vitest)

Сторонние сайты регулярно меняют верстку, селекторы или закрываются защитой Cloudflare, что делает прямое обращение к сети в CI нестабильным.

Чтобы гарантировать надежность вашего плагина, Zenith Provider SDK рекомендует **детерминированное модульное тестирование на сохраненных фикстурах HTML** с использованием **Vitest**.

---

## Преимущества тестирования на фикстурах

- **Независимость от сети**: Тесты выполняются локально за доли секунды и не зависят от доступности сайта или блокировок по IP.
- **Стабильный CI**: Тесты `npm test` надежно и быстро проходят в GitHub Actions при каждом коммите.
- **Проверка граничных случаев**: Возможность протестировать редкие сценарии (тайтлы без постеров, пустые результаты поиска, спецвыпуски), которые сложно воспроизвести «вживую».

---

## Организация фикстур

Сохраняйте примеры реальных ответов HTML или JSON в каталог `test/fixtures/`:

```
test/
├── fixtures/
│   ├── search_frieren.html     # Реальный ответ поиска по слову "frieren"
│   ├── search_empty.html       # Ответ при отсутствии результатов
│   ├── details_series.html     # Страница описания тайтла
│   └── episodes.json           # JSON со списком серий и видеопотоков
└── provider.test.ts            # Набор тестов Vitest
```

### Сохранение фикстур через cURL

Быстро сохранить разметку со страницы можно через `curl`:

```bash
curl -s "https://example.com/search?q=frieren" > test/fixtures/search_frieren.html
curl -s "https://example.com/anime/frieren" > test/fixtures/details_series.html
```

---

## Написание тестов на Vitest

В файле `test/provider.test.ts` замокайте вызов `context.fetch()` и проверьте селекторы провайдера:

```ts
import { describe, it, expect, vi } from "vitest";
import fs from "node:fs";
import path from "node:path";
import providerExport from "../src/index.js";
import { ProviderContext } from "@zenith/sdk/broker";

const provider = providerExport.definition;

// Вспомогательная функция для чтения фикстур
function loadFixture(filename: string): string {
  return fs.readFileSync(path.join(__dirname, "fixtures", filename), "utf-8");
}

describe("Тестирование аниме-провайдера", () => {
  it("корректно разбирает результаты поиска", async () => {
    const searchHtml = loadFixture("search_frieren.html");

    const context = new ProviderContext("test-provider", {
      mirror: "https://api.example.com"
    });

    vi.spyOn(context, "fetch").mockResolvedValueOnce({
      status: 200,
      statusText: "OK",
      ok: true,
      headers: new (await import("@zenith/sdk/broker")).FetchHeaders(),
      text: async () => searchHtml,
      json: async () => JSON.parse(searchHtml)
    } as any);

    const result = await provider.getSearch!("frieren", 1, 20, context);

    expect(result.items.length).toBeGreaterThan(0);

    const firstItem = result.items[0];
    expect(firstItem.mediaRef.id).toBeTruthy();
    expect(firstItem.title).toContain("Frieren");
    expect(firstItem.posterUrl).toMatch(/^https?:\/\//);
    expect(firstItem.score).toBeGreaterThan(8.0);
    expect(result.hasNextPage).toBe(true);
  });

  it("корректно обрабатывает пустой поиск без падений", async () => {
    const emptyHtml = loadFixture("search_empty.html");
    const context = new ProviderContext("test-provider");

    vi.spyOn(context, "fetch").mockResolvedValueOnce({
      status: 200,
      statusText: "OK",
      ok: true,
      headers: new (await import("@zenith/sdk/broker")).FetchHeaders(),
      text: async () => emptyHtml,
      json: async () => ({})
    } as any);

    const result = await provider.getSearch!("несуществующий_тайтл_xyz", 1, 20, context);

    expect(result.items).toEqual([]);
    expect(result.hasNextPage).toBe(false);
  });

  it("извлекает полную информацию о тайтле", async () => {
    const detailsHtml = loadFixture("details_series.html");
    const context = new ProviderContext("test-provider");

    vi.spyOn(context, "fetch").mockResolvedValueOnce({
      status: 200,
      statusText: "OK",
      ok: true,
      headers: new (await import("@zenith/sdk/broker")).FetchHeaders(),
      text: async () => detailsHtml,
      json: async () => ({})
    } as any);

    const details = await provider.getDetails!(
      { id: "123", title: "Frieren" },
      context
    );

    expect(details.title).toBe("Frieren: Beyond Journey's End");
    expect(details.description).toBeTruthy();
    expect(details.episodesCount).toBe(28);
    expect(details.genres).toContain("Fantasy");
    expect(details.status).toBe("FINISHED");
  });
});
```

---

## Защитное программирование и краевые случаи

В соответствии с **Политикой нулевых падений Zenith**, провайдер ни при каких условиях не должен вызывать непредвиденных исключений из-за изменений верстки:

### 1. Отсутствующие постеры
Если обложка не найдена, верните `null`, но не допускайте ошибки:

```ts
const poster = $el.find("img.poster").attr("src") || null;
```

### 2. Нечисловые оценки
Никогда не используйте `parseFloat()` без проверки на `NaN`:

```ts
const rawScore = $el.find(".score").text().trim();
const score = Number.isNaN(parseFloat(rawScore)) ? null : parseFloat(rawScore);
```

### 3. Отсутствие серий или озвучек
Если у тайтла нет источников, возвращайте пустой массив `[]`, а не исключение:

```ts
async getSources(mediaRef, context) {
  const sources = await parseSources();
  return sources ?? [];
}
```

---

## Запуск тестов

Выполните команду в терминале:

```bash
# Однократный запуск всех тестов
npm test

# Режим наблюдения при разработке
npx vitest
```
