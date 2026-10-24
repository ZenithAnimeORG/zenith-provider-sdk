import { defineConfig } from "vitepress";

export default defineConfig({
  title: "Zenith Provider SDK",
  description: "Official TypeScript SDK for building Zenith media player plugins and catalog extensions",
  base: process.env.VITEPRESS_BASE || "/",
  lastUpdated: true,
  cleanUrls: true,

  head: [
    ["meta", { name: "theme-color", content: "#6750A4" }],
    ["link", { rel: "icon", href: "/favicon.ico" }],
  ],

  locales: {
    root: {
      label: "English",
      lang: "en",
      title: "Zenith Provider SDK",
      description: "Official TypeScript SDK for building Zenith media player plugins and catalog extensions",
      themeConfig: {
        nav: [
          { text: "Guides", link: "/guides/getting-started" },
          { text: "API Reference", link: "/api/" },
          {
            text: "Ecosystem",
            items: [
              { text: "Provider Template", link: "https://github.com/ZenithAnimeORG/zenith-provider-template" },
              { text: "Zenith App", link: "https://github.com/ZenithAnimeORG/Zenith.App" },
            ],
          },
        ],
        sidebar: [
          {
            text: "Guides",
            items: [
              { text: "Getting Started", link: "/guides/getting-started" },
              { text: "Manifest V2 & Packaging", link: "/guides/manifest-and-package" },
              { text: "Provider Capabilities", link: "/guides/capabilities" },
              { text: "DOM Parsing with Cheerio", link: "/guides/dom-parsing" },
              { text: "Network & Host Broker", link: "/guides/network-and-broker" },
              { text: "Testing & Fixtures", link: "/guides/testing-and-fixtures" },
            ],
          },
          {
            text: "API Reference",
            items: [
              { text: "Overview", link: "/api/" },
              { text: "defineProvider", link: "/api/functions/defineProvider" },
              { text: "ProviderDefinition", link: "/api/interfaces/ProviderDefinition" },
              { text: "ProviderContext", link: "/api/classes/ProviderContext" },
              { text: "FetchHeaders", link: "/api/classes/FetchHeaders" },
              { text: "loadHtml (DOM)", link: "/api/functions/loadHtml" },
              { text: "Media Models", link: "/api/interfaces/MediaRef" },
              { text: "Package Envelope", link: "/api/functions/validateEnvelope" },
              { text: "Bookmark Importer", link: "/api/interfaces/BookmarkImporterDefinition" },
            ],
          },
        ],
        editLink: {
          pattern: "https://github.com/ZenithAnimeORG/zenith-provider-sdk/edit/main/docs/:path",
          text: "Edit this page on GitHub",
        },
        docFooter: {
          prev: "Previous page",
          next: "Next page",
        },
      },
    },
    ru: {
      label: "Русский",
      lang: "ru",
      link: "/ru/",
      title: "Zenith Provider SDK",
      description: "Официальный TypeScript SDK для разработки плагинов и каталогов медиаплеера Zenith",
      themeConfig: {
        nav: [
          { text: "Руководства", link: "/ru/guides/getting-started" },
          { text: "API Справочник", link: "/api/" },
          {
            text: "Экосистема",
            items: [
              { text: "Шаблон провайдера", link: "https://github.com/ZenithAnimeORG/zenith-provider-template" },
              { text: "Приложение Zenith", link: "https://github.com/ZenithAnimeORG/Zenith.App" },
            ],
          },
        ],
        sidebar: [
          {
            text: "Руководства",
            items: [
              { text: "Быстрый старт", link: "/ru/guides/getting-started" },
              { text: "Манифест V2 и пакет .zpk", link: "/ru/guides/manifest-and-package" },
              { text: "Возможности провайдера", link: "/ru/guides/capabilities" },
              { text: "Парсинг DOM через Cheerio", link: "/ru/guides/dom-parsing" },
              { text: "Сеть и Host Broker", link: "/ru/guides/network-and-broker" },
              { text: "Тестирование и фикстуры", link: "/ru/guides/testing-and-fixtures" },
            ],
          },
          {
            text: "API Справочник",
            items: [
              { text: "Обзор API", link: "/api/" },
              { text: "defineProvider", link: "/api/functions/defineProvider" },
              { text: "ProviderDefinition", link: "/api/interfaces/ProviderDefinition" },
              { text: "ProviderContext", link: "/api/classes/ProviderContext" },
              { text: "FetchHeaders", link: "/api/classes/FetchHeaders" },
              { text: "loadHtml (DOM)", link: "/api/functions/loadHtml" },
              { text: "Модели данных", link: "/api/interfaces/MediaRef" },
              { text: "Пакет и лимиты", link: "/api/functions/validateEnvelope" },
              { text: "Импорт закладок", link: "/api/interfaces/BookmarkImporterDefinition" },
            ],
          },
        ],
        editLink: {
          pattern: "https://github.com/ZenithAnimeORG/zenith-provider-sdk/edit/main/docs/:path",
          text: "Редактировать страницу на GitHub",
        },
        docFooter: {
          prev: "Предыдущая страница",
          next: "Следующая страница",
        },
      },
    },
  },

  themeConfig: {
    socialLinks: [
      { icon: "github", link: "https://github.com/ZenithAnimeORG/zenith-provider-sdk" },
    ],
    search: {
      provider: "local",
    },
    footer: {
      message: "Released under the MIT License.",
      copyright: "Copyright © 2026 Zenith Anime Team",
    },
  },
});
