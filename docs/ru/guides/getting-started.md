# Быстрый старт

Добро пожаловать в документацию **Zenith Provider SDK**!

Плагины для медиаплеера Zenith (называемые **провайдерами**) позволяют сообществу подключать каталоги аниме и фильмов, источники видеопотоков, торрент-релизы, субтитры и таймкоды опенингов непосредственно в единый интерфейс приложения.

В отличие от классических мобильных приложений, требующих компиляции Java/Kotlin, установки Android SDK и сложных сборок в Gradle, **провайдеры Zenith — это 100% переносимые TypeScript-плагины**. Они собираются в один легковесный JavaScript-бандл и исполняются быстрым изолированным движком **QuickJS**.

---

## Ключевые преимущества

- **Никаких нативных зависимостей**: Не нужны Java, Gradle, NDK или Android SDK. Достаточно стандартного Node.js.
- **Единый кроссплатформенный пакет**: Один архив `.zpk` (Zenith Package) работает совершенно одинаково на **Android**, **Linux**, **macOS** и **Windows**.
- **Высокая производительность в одном процессе**: Плагины работают в пуле QuickJS с потолком памяти **64 МБ** без накладных расходов сериализации IPC.
- **Архитектура с приоритетом безопасности**: Сеть, куки и настройки проходят через нативный Host Broker приложения; произвольный доступ к файловой системе и системным командам заблокирован.

---

## Системные требования

Перед началом убедитесь, что у вас установлены:

- **Node.js**: версии 20 или выше (рекомендуется v22 LTS)
- **npm** (v10+) или **pnpm** (v9+)
- Любой удобный редактор кода (VS Code, WebStorm и т. д.)

---

## Создание первого провайдера

Самый быстрый способ — использовать официальный репозиторий шаблона [`zenith-provider-template`](https://github.com/ZenithAnimeORG/zenith-provider-template).

### 1. Создание проекта из шаблона

1. Откройте [`zenith-provider-template`](https://github.com/ZenithAnimeORG/zenith-provider-template) на GitHub.
2. Нажмите **Use this template** -> **Create a new repository**.
3. Укажите название репозитория (например, `zenith-provider-mysite`).
4. Склонируйте репозиторий локально:

```bash
git clone https://github.com/your-username/zenith-provider-mysite.git
cd zenith-provider-mysite
```

### 2. Установка зависимостей

Установите пакеты проекта (включая `@zenith/sdk`, `esbuild` и `vitest`):

```bash
npm install
```

---

## Структура проекта

Внутри репозитория находится каноническая структура файлов:

```
├── manifest.json       # Метаданные, возможности, белый список хостов сети
├── settings.json       # Декларативная схема настроек для нативного Material 3 UI
├── icon.png            # Квадратная иконка 512x512 для каталога провайдеров
├── package.json        # Зависимости, скрипты сборки и тестирования
├── tsconfig.json       # Строгая конфигурация TypeScript
├── esbuild.config.mjs  # Конфигурация сборщика esbuild в формат IIFE
├── src/
│   └── index.ts        # Исходный код провайдера
├── test/
│   ├── fixtures/       # Сохраненные HTML/JSON для детерминированных тестов
│   │   ├── search.html
│   │   └── details.html
│   └── provider.test.ts # Модульные тесты селекторов и логики на Vitest
└── tools/
    ├── package.mjs     # Упаковщик в архив .zpk
    └── verify.mjs      # Проверка лимитов размера и безопасности конверта
```

---

## Базовая реализация провайдера

Откройте `src/index.ts`. Плагин описывается с помощью функции `defineProvider` из `@zenith/sdk/provider`:

```ts
import { defineProvider } from "@zenith/sdk/provider";
import { loadHtml } from "@zenith/sdk/dom";
import "@zenith/sdk/dispatcher"; // Подключает диспетчер вызовов

export default defineProvider({
  // Диагностический тест соединения из настроек Zenith
  async test(context) {
    const mirror = (await context.getSetting("mirror")) || "https://api.example.com";
    const res = await context.fetch(`${mirror}/ping`);
    return {
      success: res.ok,
      message: res.ok ? "Сервер доступен" : `Ошибка с кодом ${res.status}`
    };
  },

  // Поиск по каталогу
  async getSearch(query, page, pageSize, context) {
    const mirror = (await context.getSetting("mirror")) || "https://api.example.com";
    const url = `${mirror}/search?q=${encodeURIComponent(query)}&page=${page}`;
    const response = await context.fetch(url);
    const html = await response.text();
    const $ = loadHtml(html);

    const items = $(".anime-card").map((_, el) => {
      const $el = $(el);
      return {
        mediaRef: {
          id: $el.attr("data-id") || "",
          title: $el.find(".title").text().trim()
        },
        title: $el.find(".title").text().trim(),
        posterUrl: $el.find("img.poster").attr("src") || "",
        score: parseFloat($el.attr("data-score") || "0") || null,
        year: parseInt($el.attr("data-year") || "0", 10) || null,
        totalEpisodes: parseInt($el.attr("data-episodes") || "0", 10) || null
      };
    }).get();

    return {
      items,
      hasNextPage: $(".pagination .next").length > 0
    };
  },

  // Полная информация о релизе
  async getDetails(mediaRef, context) {
    const mirror = (await context.getSetting("mirror")) || "https://api.example.com";
    const url = `${mirror}/anime/${encodeURIComponent(mediaRef.id)}`;
    const response = await context.fetch(url);
    const $ = loadHtml(await response.text());

    return {
      mediaRef,
      title: $(".title").text().trim() || mediaRef.title || "Неизвестное название",
      description: $(".description").text().trim(),
      posterUrl: $("img.poster").attr("src"),
      score: 9.14,
      year: 2023,
      episodesCount: 28,
      genres: $(".genre-tag").map((_, el) => $(el).text().trim()).get(),
      status: "FINISHED"
    };
  },

  // Источники озвучки и список серий
  async getSources(mediaRef, context) {
    return [
      {
        name: "Субтитры",
        translationName: "Субтитры",
        translationType: "SUB",
        episodes: [
          {
            number: 1,
            url: `https://api.example.com/episode/${encodeURIComponent(mediaRef.id)}/1`
          }
        ]
      }
    ];
  },

  // Разрешение прямого потока для воспроизведения
  async resolveEpisode(episode, context) {
    return [
      {
        url: episode.url,
        quality: "1080p",
        isHls: true
      }
    ];
  }
});
```

---

## Разработка и тестирование

### Запуск тестов

Тесты Vitest проверяют ваши селекторы Cheerio и корректность моделей данных на сохраненных фикстурах HTML:

```bash
npm test
```

### Проверка типов

Убедитесь, что код полностью соответствует типам Zenith SDK:

```bash
npm run typecheck
```

### Сборка пакета .zpk

Соберите JavaScript-бандл и упакуйте его в `.zpk`:

```bash
npm run package
```

Скрипт собирает бандл через `esbuild`, проверяет лимиты пакета через `tools/verify.mjs` и создает готовый архив:

```
dist/template_provider.zpk
```

### Проверка лимитов пакета

Для быстрой валидации архива без пересборки:

```bash
npm run verify
```

---

## Установка в Zenith

Чтобы протестировать плагин в приложении:

1. Запустите Zenith на компьютере или Android-устройстве.
2. Откройте **Настройки** -> **Провайдеры**.
3. Нажмите **Установить из файла** и выберите ваш файл `.zpk`.
4. Провайдер сразу появится в списке активных и будет доступен в поиске и каталоге!

---

## Следующие шаги

- Изучите [Манифест V2 и лимиты конверта пакета](/ru/guides/manifest-and-package)
- Ознакомьтесь с [Возможностями провайдеров](/ru/guides/capabilities)
- Освойте [Парсинг DOM через Cheerio](/ru/guides/dom-parsing)
- Узнайте подробнее про [Безопасность сети и Host Broker](/ru/guides/network-and-broker)
