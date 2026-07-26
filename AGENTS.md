# AGENTS.md — Alexandria

## Build & CI

- **Build**: `./gradlew assembleRelease` (no `test` target in CI; compilation is the only gate)
- **CI**: `.github/workflows/build.yml` — runs on push to `main`, tags `v*`, and PRs to `main`
- **Release**: Creating a tag `v{versionName}+{versionCode}` (e.g. `v1.1.0+28`) triggers CI to build and publish a GitHub Release with the APK attached
- **No local JDK** — CI is the sole compilation gate; mentally verify every push
- **Keystore**: `keystore/alexandria-release.jks` with `KEYSTORE_PASSWORD` / `KEY_PASSWORD` secrets
- **Remote URL**: HTTPS remote must be cleaned after any PAT‑authenticated push

## Stack

- **minSdk 26, targetSdk 34**, JDK 17, Kotlin, Compose BOM `2024.02.00`
- **Hilt** via KSP, **Room** via KSP, **Coil** for images, **OkHttp** (raw) for all API calls
- **Navigation**: `NavGraph.kt` sealed `Screen` routes; `MainNavGraph.kt` wires them

## Compose BOM `2024.02.00` constraints

- **No `Modifier.zIndex()`** — use Row/Box draw order instead
- `Dp * Int` is not a valid operator — always use `Dp * index.toFloat()` (or `(count - 1).toFloat()`)

## Description lookup chain (`BookDetailViewModel`)

`fetchDescription(book)` calls in order, skipping disabled sources, until a description is found:

1. `portadaResolver.fetchDescriptionFromIsbn(book.isbn)` — OpenLibrary via ISBN
2. `portadaResolver.fetchDescriptionFromTodoTusLibros(book.isbn)` — todostuslibros.com via ISBN redirect
3. `portadaResolver.fetchDescriptionFromCasaDelLibro(title, author)` — casadellibro.com search + scrape
4. `portadaResolver.fetchDescriptionBySearch(title, author, lang = "spa")` — OpenLibrary Spanish search (also fetches OpenLibrary ratings)
5. `portadaResolver.fetchDescriptionFromWikipedia(title, author)` — Spanish Wikipedia (two‑step: search via `api.wikimedia.org/core/v1/wikipedia/es/search/page`, then fetch `extract`)
6. `portadaResolver.fetchFromGoogleBooks(title, author)` — Google Books API with `langRestrict=es` (also fetches Google Books ratings)
7. Null → UI shows "No hay sinopsis disponible"

External ratings are sourced from OpenLibrary (step 4) and Google Books (step 6), whichever provides them first.

## Key project layout

- `domain/model/Book.kt` — Book data class with `ReadingStatus` enum
- `data/remote/PortadaResolver.kt` — covers + descriptions via OkHttp + JSONObject (not Retrofit)
- `data/repository/BookRepository.kt` — single repository bridging Room entities ↔ domain models
- `ui/components/BookCarousel.kt` — carousel with `CarouselItem.Single` / `CarouselItem.Series` pages
- `ui/components/SagaCarouselCard.kt` — saga deck spread (all books visible as overlapping cards)
- `ui/navigation/NavGraph.kt` — `Screen` sealed class defines all routes
- Version tag format: `v{versionName}+{versionCode}` (e.g. `v1.1.0+28`)
