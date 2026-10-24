import { defineConfig } from "vitepress";

const base = process.env.VITEPRESS_BASE || "/";

const apiSidebar = [
  {
    text: "API Reference",
    items: [
      { text: "Overview", link: "/api/" },
    ],
  },
  {
    text: "Classes",
    items: [
      { text: "ProviderContext", link: "/api/classes/ProviderContext" },
      { text: "FetchHeaders", link: "/api/classes/FetchHeaders" },
    ],
  },
  {
    text: "Core Functions",
    items: [
      { text: "defineProvider", link: "/api/functions/defineProvider" },
      { text: "defineBookmarkImporter", link: "/api/functions/defineBookmarkImporter" },
      { text: "loadHtml", link: "/api/functions/loadHtml" },
      { text: "validateEnvelope", link: "/api/functions/validateEnvelope" },
      { text: "validateManifest", link: "/api/functions/validateManifest" },
      { text: "isSafeEntryName", link: "/api/functions/isSafeEntryName" },
      { text: "handleZenithDispatch", link: "/api/functions/handleZenithDispatch" },
    ],
  },
  {
    text: "Interfaces",
    collapsed: false,
    items: [
      { text: "ProviderDefinition", link: "/api/interfaces/ProviderDefinition" },
      { text: "ProviderExport", link: "/api/interfaces/ProviderExport" },
      { text: "ProviderStatusInfo", link: "/api/interfaces/ProviderStatusInfo" },
      { text: "ProviderLogger", link: "/api/interfaces/ProviderLogger" },
      { text: "DispatchResponse", link: "/api/interfaces/DispatchResponse" },
      { text: "MediaRef", link: "/api/interfaces/MediaRef" },
      { text: "CatalogMediaItem", link: "/api/interfaces/CatalogMediaItem" },
      { text: "CatalogMediaDetails", link: "/api/interfaces/CatalogMediaDetails" },
      { text: "CatalogSearchResult", link: "/api/interfaces/CatalogSearchResult" },
      { text: "CatalogSection", link: "/api/interfaces/CatalogSection" },
      { text: "ProviderVideoSource", link: "/api/interfaces/ProviderVideoSource" },
      { text: "ProviderEpisode", link: "/api/interfaces/ProviderEpisode" },
      { text: "ProviderMediaStream", link: "/api/interfaces/ProviderMediaStream" },
      { text: "ProviderTorrentSource", link: "/api/interfaces/ProviderTorrentSource" },
      { text: "ProviderSubtitle", link: "/api/interfaces/ProviderSubtitle" },
      { text: "SkipInterval", link: "/api/interfaces/SkipInterval" },
      { text: "PluginManifest", link: "/api/interfaces/PluginManifest" },
      { text: "SettingItemSpec", link: "/api/interfaces/SettingItemSpec" },
      { text: "ValidationResult", link: "/api/interfaces/ValidationResult" },
      { text: "PluginArtifactEntry", link: "/api/interfaces/PluginArtifactEntry" },
      { text: "BookmarkImporterDefinition", link: "/api/interfaces/BookmarkImporterDefinition" },
      { text: "BookmarkImporterExport", link: "/api/interfaces/BookmarkImporterExport" },
      { text: "BookmarkSource", link: "/api/interfaces/BookmarkSource" },
      { text: "ImportContext", link: "/api/interfaces/ImportContext" },
      { text: "RawImportEntry", link: "/api/interfaces/RawImportEntry" },
      { text: "ImportPreviewResult", link: "/api/interfaces/ImportPreviewResult" },
      { text: "ImportInstructionStep", link: "/api/interfaces/ImportInstructionStep" },
      { text: "ImportBatch", link: "/api/interfaces/ImportBatch" },
      { text: "FetchInit", link: "/api/interfaces/FetchInit" },
      { text: "FetchResponse", link: "/api/interfaces/FetchResponse" },
    ],
  },
  {
    text: "Type Aliases",
    collapsed: true,
    items: [
      { text: "ProviderCapability", link: "/api/type-aliases/ProviderCapability" },
      { text: "ProviderTranslationType", link: "/api/type-aliases/ProviderTranslationType" },
      { text: "UserMediaStatus", link: "/api/type-aliases/UserMediaStatus" },
      { text: "ProviderTestResult", link: "/api/type-aliases/ProviderTestResult" },
      { text: "CheerioAPI", link: "/api/type-aliases/CheerioAPI" },
    ],
  },
  {
    text: "Variables & Limits",
    collapsed: true,
    items: [
      { text: "MAX_PACKAGE_SIZE_BYTES", link: "/api/variables/MAX_PACKAGE_SIZE_BYTES" },
      { text: "MAX_TOTAL_UNCOMPRESSED_BYTES", link: "/api/variables/MAX_TOTAL_UNCOMPRESSED_BYTES" },
      { text: "MAX_ENTRYPOINT_BYTES", link: "/api/variables/MAX_ENTRYPOINT_BYTES" },
      { text: "MAX_ENTRY_COUNT", link: "/api/variables/MAX_ENTRY_COUNT" },
      { text: "MAX_MANIFEST_BYTES", link: "/api/variables/MAX_MANIFEST_BYTES" },
      { text: "MAX_SETTINGS_BYTES", link: "/api/variables/MAX_SETTINGS_BYTES" },
      { text: "MAX_ICON_BYTES", link: "/api/variables/MAX_ICON_BYTES" },
      { text: "MAX_RESOURCE_BYTES", link: "/api/variables/MAX_RESOURCE_BYTES" },
      { text: "CURRENT_PACKAGE_FORMAT", link: "/api/variables/CURRENT_PACKAGE_FORMAT" },
      { text: "CURRENT_MODULE_API_VERSION", link: "/api/variables/CURRENT_MODULE_API_VERSION" },
      { text: "PACKAGE_FORMAT_V2", link: "/api/variables/PACKAGE_FORMAT_V2" },
      { text: "PACKAGE_FORMAT_V1", link: "/api/variables/PACKAGE_FORMAT_V1" },
    ],
  },
];

export default defineConfig({
  title: "Zenith Provider SDK",
  description: "Official TypeScript SDK for building Zenith media player plugins and catalog extensions",
  base,
  lastUpdated: true,
  cleanUrls: true,

  head: [
    ["meta", { name: "theme-color", content: "#6750A4" }],
    ["link", { rel: "icon", type: "image/svg+xml", href: `${base}favicon.svg` }],
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
        sidebar: {
          "/guides/": [
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
                { text: "API Overview", link: "/api/" },
              ],
            },
          ],
          "/api/": apiSidebar,
        },
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
        sidebar: {
          "/ru/guides/": [
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
              ],
            },
          ],
          "/api/": apiSidebar,
        },
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
    logo: "/favicon.svg",
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
