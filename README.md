# Zenith Provider SDK & Testkit

Official Kotlin Multiplatform SDK and testing kit for building modular media providers, scrapers, trackers, and subtitle plugins for the [Zenith](https://github.com/ZenithAnimeORG) media player ecosystem.

---

## Architecture Overview

The Zenith Provider ecosystem is 100% modular, sandboxed, and plugin-driven. The SDK decouples media discovery, stream scraping, torrent tracking, and subtitle retrieval from the core player application.

### Modules

- **`:provider-sdk`**: Core interfaces, data models, manifests, settings schemas, and contract models.
  - `ZenithProvider`: Base lifecycle interface for all plugins (`init`, `test`, `executeAction`, `probeStatus`).
  - `MediaSourceProvider`: Resolves direct and embed video streams (`getSources`, `resolveStream`).
  - `CatalogProvider`: Search, discovery, popular releases, and episode catalog listings.
  - `TorrentSourceProvider`: Resolves torrent magnets and peer swarms.
  - `SubtitleSourceProvider`: Queries external subtitle tracks (`.ass`, `.vtt`, `.srt`).
  - `SkipTimingsProvider`: AniSkip/AnimeSkip anime opening/ending skip timecodes.
  - `TrackerProvider`: Synchronizes watch progress with external tracking platforms.
  - `ImportProvider`: Batch imports user watchlists and bookmark history.
  - `StreamExtractor`: Modular iframe/embed video extractor interface.

- **`:provider-sdk-testkit`**: Deterministic test fixtures and contract verification harnesses.
  - `ProviderContractTestBase`: Verifies plugin adherence to SDK invariants.
  - `FakeProviderContext`: In-memory sandbox simulating plugin settings and mirror selection.
  - `MockProviderHttpClient`: Ktor MockEngine DSL for synthetic HTTP responses without external network access.

---

## Installation

Add the dependency to your Kotlin Multiplatform or JVM Gradle build:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// build.gradle.kts
dependencies {
    implementation("com.pilldev.zenith:provider-sdk:2.0.0")
    testImplementation("com.pilldev.zenith:provider-sdk-testkit:2.0.0")
}
```

---

## Building & Testing

To compile both modules and execute the unit test suites:

```bash
./gradlew check
```

To publish artifacts to your local Maven cache (`~/.m2/repository`):

```bash
./gradlew publishToMavenLocal
```

---

## License

MIT License. See [LICENSE](LICENSE) for details.
