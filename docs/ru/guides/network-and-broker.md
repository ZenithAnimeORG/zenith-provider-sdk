# Сеть и Host Broker

В Zenith провайдеры исполняются в изолированных виртуальных машинах **QuickJS** без прямого доступа к сокетам операционной системы, сетевым интерфейсам или файловой системе устройства.

Все исходящие сетевые запросы, сохранение настроек и логирование контролируются нативным Kotlin Ktor **Host Broker** приложения Zenith.

---

## Архитектура Host Broker

```
┌────────────────────────────────────────────────────────┐
│               Изоляция QuickJS (Плагин)                │
│                                                        │
│   context.fetch("https://api.mysite.com/search")       │
└──────────────────────────┬─────────────────────────────┘
                           │ Вызов хоста через IPC
┌──────────────────────────▼─────────────────────────────┐
│                   Zenith Host Broker                   │
│                                                        │
│   1. Проверка адреса по белому списку `hosts`          │
│   2. Подстановка User-Agent и кук сессии               │
│   3. Выполнение запроса через нативный Ktor Client     │
│   4. Проверка редиректов по белому списку              │
│   5. Ограничение объема тела ответа до 16 МБ           │
└──────────────────────────┬─────────────────────────────┘
                           │ Зашифрованный HTTPS
┌──────────────────────────▼─────────────────────────────┐
│                    Удаленный веб-сервер                │
└────────────────────────────────────────────────────────┘
```

### Гарантии безопасности и лимиты

1. **Контроль белого списка хостов**:
   Каждый запрос и каждый HTTP-редирект сверяются с массивом `hosts` из вашего `manifest.json`. Запросы к неуказанным хостам немедленно блокируются.
2. **Лимит объема ответа**:
   Тело отдельного ответа не может превышать **16 МБ**. Загрузка тяжелых видеофайлов через `context.fetch()` запрещена (видеопотоки должны передаваться плееру MPV в виде ссылок).
3. **Тайм-ауты исполнения**:
   По умолчанию для сетевых вызовов установлен тайм-аут 15 секунд (`timeoutMillis: 15000`), чтобы интерфейс плеера оставался отзывчивым.

---

## Исходящие запросы через `context.fetch()`

Интерфейс `context.fetch()` полностью повторяет стандартный браузерный Fetch API:

```ts
// Простой GET-запрос
const response = await context.fetch("https://api.example.com/anime/123");
if (!response.ok) {
  throw new Error(`Ошибка HTTP-запроса: ${response.status}`);
}
const data = await response.json<MyAnimeResponse>();
```

### POST-запросы с телом JSON

```ts
const response = await context.fetch("https://api.example.com/graphql", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Accept": "application/json"
  },
  body: JSON.stringify({
    query: `query ($id: Int) { Media(id: $id) { title { romaji } } }`,
    variables: { id: 1535 }
  }),
  timeoutMillis: 10000
});

const result = await response.json();
```

---

## Работа с заголовками через `FetchHeaders`

Zenith предоставляет регистронезависимый класс {@link FetchHeaders}:

```ts
const response = await context.fetch("https://api.example.com/data");

// Регистронезависимый поиск
const contentType = response.headers.get("content-type"); // "application/json; charset=utf-8"
const hasETag = response.headers.has("etag");

// Итерация по всем полученным заголовкам
response.headers.forEach((value, name) => {
  context.logger.debug(`Заголовок ${name}: ${value}`);
});
```

---

## Обход антибот-систем и ограничений CDN

Многие сайты используют защиту Cloudflare, DDoS-Guard или валидацию токенов плеера.

### 1. Передача `Referer` и `Origin`

Серверы эмбед-плееров часто требуют, чтобы заголовок `Referer` указывал на основной сайт:

```ts
const response = await context.fetch(videoApiUrl, {
  headers: {
    "Referer": "https://embed.example.com/",
    "Origin": "https://embed.example.com",
    "X-Requested-With": "XMLHttpRequest"
  }
});
```

### 2. Автоматическое сохранение сессионных кук

Host Broker автоматически хранит куки в сессии плагина:
- Если сервер возвращает заголовок `Set-Cookie`, клиент Ktor сохраняет их.
- Следующие вызовы `context.fetch()` к тому же домену автоматически передают сохраненные куки.
- Если сайт требует начального рукопожатия, достаточно запросить главную страницу перед вызовом защищенного API:

```ts
// Инициализация сессии и получение кук
await context.fetch("https://example.com/");

// Вызов защищенного API с сохраненными куками
const apiResponse = await context.fetch("https://example.com/ajax/episodes");
```

---

## Ограничение частоты запросов и повторные попытки (Retry)

Если сервер вернул ошибку `429 Too Many Requests` или временный сбой `503`, используйте повторные запросы с экспоненциальной задержкой:

```ts
async function fetchWithRetry(
  context: ProviderContext,
  url: string,
  maxRetries = 3
): Promise<FetchResponse> {
  let delay = 1000;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    const res = await context.fetch(url);

    if (res.status !== 429 && res.status < 500) {
      return res;
    }

    context.logger.warn(`Получен статус ${res.status} на попытке ${attempt}. Повтор через ${delay} мс...`);
    await new Promise((resolve) => setTimeout(resolve, delay));
    delay *= 2;
  }

  throw new Error(`Не удалось загрузить ${url} после ${maxRetries} попыток`);
}
```

---

## Управление настройками и зеркалами

Host Broker предоставляет доступ к зашифрованному хранилищу настроек:

### Чтение и запись параметров

```ts
// Чтение параметра из settings.json
const customDomain = await context.getSetting("custom_domain");

// Сохранение значения (например, токена сессии)
await context.setSetting("auth_token", newToken);
```

### Разрешение зеркал (Mirrors)

Пользователь может выбрать предпочтительный сервер-зеркало в настройках провайдера:

```ts
// Получение активного зеркала, выбранного пользователем
const mirror = await context.getMirror();
const url = `${mirror}/api/search?q=${encodeURIComponent(query)}`;
```

---

## Структурированное логирование через `context.logger`

Не используйте `console.log()` в коде плагинов. Используйте `context.logger` для записи в централизованную систему Kermit:

```ts
// Debug: Подробная трассировка (отключена в релизных сборках)
context.logger.debug(`Найдено элементов: ${items.length} на странице ${page}`);

// Info: Основные события жизненного цикла
context.logger.info(`Сессия инициализирована с зеркалом: ${mirror}`);

// Warn: Нештатные, но не критические ситуации
context.logger.warn(`Отсутствует постер для тайтла ID: ${id}`);

// Error: Ошибки с передачей стека исключения
context.logger.error("Не удалось разобрать список серий", err);
```
