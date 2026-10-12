# Zenith Provider SDK & Testkit

Official Kotlin Multiplatform SDK and testing kit for building modular media providers, scrapers, trackers, and subtitle plugins for the [Zenith](https://github.com/ZenithAnimeORG) media player ecosystem.

---

## Architecture Overview

The Zenith Provider ecosystem is modular and plugin-driven. Providers execute in-process and are not sandboxed; the SDK decouples media discovery, stream scraping, torrent tracking, and subtitle retrieval from the core player application.

### Modules

- **`:provider-sdk`**: Core interfaces, data models, manifests, settings schemas, and contract models.
  - `ZenithProvider`: Lifecycle interface for all plugins (`initialize`, `dispose`, `test`, `executeAction`, `probeStatus`).
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
  - `FakeProviderContext`: In-memory context simulating provider settings and mirror selection.
  - `MockProviderHttpClient`: Ktor MockEngine DSL for synthetic HTTP responses without external network access.

---

## Installation

Artifacts are currently consumed from Maven Local; they are not available from Maven Central. Publish the SDK and testkit locally first:

```bash
git clone https://github.com/ZenithAnimeORG/zenith-provider-sdk.git
cd zenith-provider-sdk
./gradlew publishToMavenLocal
```

Then add Maven Local to your Kotlin Multiplatform or JVM Gradle build:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

// build.gradle.kts
dependencies {
    implementation("com.pilldev.zenith:provider-sdk:0.1.0")
    testImplementation("com.pilldev.zenith:provider-sdk-testkit:0.1.0")
}
```

The SDK and provider contract are both at internal version `0.1.0`. These artifacts are not publicly released; breaking changes are expected during development, so no artificial major-version bump is needed.

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
