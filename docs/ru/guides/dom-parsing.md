# Парсинг DOM через Cheerio

Большинство сторонних сайтов с аниме и фильмами не имеют публичного JSON API. Чтобы получить названия, обложки, ссылки на серии и плееры, плагин должен парсить HTML-страницы.

Zenith предоставляет модуль **`@zenith/sdk/dom`**, включающий встроенный парсер HTML на базе **Cheerio Slim** (`htmlparser2`).

---

## Преимущества `@zenith/sdk/dom`

Традиционные плагины часто пересылают запросы селекторов хост-приложению (например, через IPC в Kotlin Jsoup) или поднимают тяжелый скрытый WebView.

В Zenith:
- **100% чистый JavaScript**: Cheerio Slim выполняется прямо в куче **QuickJS** без нативных привязок Node.js или зависимости от Buffer.
- **Нулевые задержки IPC**: Обход дерева, фильтрация и выборка текста происходят за микросекунды внутри виртуальной машины.
- **Синтаксис jQuery**: Привычный и мощный синтаксис `$` знаком миллионам веб-разработчиков.

---

## Базовое использование

Импортируйте `loadHtml` из `@zenith/sdk/dom`:

```ts
import { loadHtml } from "@zenith/sdk/dom";

const response = await context.fetch("https://example.com/catalog");
const html = await response.text();

// Загрузка HTML в экземпляр запросов Cheerio
const $ = loadHtml(html);

// Выборка элементов через CSS-селекторы
const pageTitle = $("h1.page-title").text().trim();
const mainCover = $("img.hero-cover").attr("src");
```

---

## Типовые шаблоны парсинга

### 1. Извлечение списка карточек (`.map().get()`)

При парсинге результатов поиска или страниц каталога используйте связку `.map()` и `.get()`, чтобы получить стандартный массив JavaScript:

```ts
const items = $(".anime-grid .card").map((index, element) => {
  const $card = $(element);

  const id = $card.attr("data-anime-id") || "";
  const title = $card.find(".card-title").text().trim();
  const posterUrl = $card.find("img").attr("data-src") || $card.find("img").attr("src");
  const rawScore = $card.find(".badge-score").text().trim();
  const score = parseFloat(rawScore) || null;

  return {
    mediaRef: { id, title },
    title,
    posterUrl,
    score
  };
}).get();
```

::: tip Зачем вызывать `.get()`?
Метод Cheerio `.map()` возвращает обертку коллекции Cheerio. Вызов `.get()` разворачивает ее в обычный массив `T[]`, который ожидают модели данных Zenith SDK.
:::

---

### 2. Разрешение относительных ссылок

Страницы сайтов часто содержат относительные пути к постерам (`/images/cover.jpg`) или сериям (`/watch/123`). Преобразуйте их в абсолютные URL:

```ts
function resolveUrl(base: string, path: string | undefined): string | null {
  if (!path) return null;
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  if (path.startsWith("//")) return `https:${path}`;
  const trimmedBase = base.replace(/\/+$/, "");
  const trimmedPath = path.replace(/^\/+/, "");
  return `${trimmedBase}/${trimmedPath}`;
}

const mirror = await context.getMirror();
const posterPath = $("img.poster").attr("src");
const fullPosterUrl = resolveUrl(mirror, posterPath);
```

---

### 3. Извлечение встроенного JSON (`__NEXT_DATA__` / теги `<script>`)

Современные сайты на Next.js, Nuxt или React часто содержат полные данные состояния прямо в тегах `<script>`:

```ts
// Извлечение состояния страницы Next.js
const nextDataScript = $("#__NEXT_DATA__").text();
if (nextDataScript) {
  try {
    const data = JSON.parse(nextDataScript);
    const animeData = data.props?.pageProps?.anime;
    // Работайте напрямую со структурированным объектом!
  } catch (err) {
    context.logger.warn("Не удалось распарсить __NEXT_DATA__ JSON", err);
  }
}
```

Либо извлечение конфигурации плеера регулярным выражением:

```ts
let playerUrl = "";
$("script").each((_, script) => {
  const code = $(script).html() || "";
  const match = code.match(/var\s+videoSrc\s*=\s*["']([^"']+)["']/);
  if (match && match[1]) {
    playerUrl = match[1];
  }
});
```

---

### 4. Парсинг таблиц HTML

Для парсинга трекеров торрентов или каталогов субтитров с разметкой `<table>`:

```ts
const rows: TorrentRow[] = [];

$("table.tracker-list tbody tr").each((_, tr) => {
  const $tr = $(tr);
  const title = $tr.find("td.title a").text().trim();
  const magnet = $tr.find("a[href^='magnet:']").attr("href");
  const size = $tr.find("td.size").text().trim();
  const seeders = parseInt($tr.find("td.seeders").text().trim(), 10) || 0;

  if (magnet) {
    rows.push({ title, magnet, size, seeders });
  }
});
```

---

## Рекомендации по оптимизации памяти

1. **Не парсите один и тот же HTML повторно**:
   Вызовите `loadHtml(html)` один раз и используйте полученный экземпляр `$` на протяжении всего метода.
2. **Не храните DOM в глобальных переменных**:
   Среда QuickJS ограничена **64 МБ** оперативной памяти. Сохранение деревьев DOM в глобальных переменных модуля приведет к утечке памяти. Извлеките необходимые данные в простые объекты и дайте сборщику мусора освободить DOM.
3. **Безопасная обработка `null`**:
   `loadHtml(null)` или `loadHtml(undefined)` возвращает пустой валидный экземпляр Cheerio без генерации ошибок.
