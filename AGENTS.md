# AGENTS.md — Alexandria

> **Changelog 2026-08-06 — "IA gratis vía OpenRouter + estabilidad"**
> - **IA fallback migrada de Gemini → OpenRouter** (`openRouterResolve` en `functions/src/index.ts`): endpoint OpenAI-compatible `openrouter.ai/api/v1/chat/completions`, **sin SDK** ni tarjeta. Secret `OPENROUTER_API_KEY` (`sk-or-v1-…`) seteado. La cuenta alexandria quedó en **Blaze free tier** (2M invocaciones/mes cubren 1 usuario; la tarjeta NO se usa).
> - **Estrategia 100% gratis**: fallback multi-modelo `OPENROUTER_MODELS` (default `google/gemma-4-26b-a4b-it:free,openai/gpt-oss-20b:free`; los `:free` se pueden consultar en openrouter.ai/models). El primero que devuelva JSON válido gana; los 429/errores saltan al siguiente. `response_format: json_object`.
> - **Prompt mejorado**: pide sinopsis en español (120-180 palabras) + hasta 10 personajes reales con `emoji` por personaje → el app lo mapea a `BookCharacter.iconKey` (avatar emoji, sin migración de DB).
> - **BUGS ARREGLADOS**:
>   - Secrets v2 NO se inyectan solos: ahora `defineSecret('GOOGLE_BOOKS_API_KEY')` / `defineSecret('OPENROUTER_API_KEY')` + `secrets: [...]` en `onCall` (antes `process.env.…` era undefined en runtime → GB sin key y OpenRouter muerto).
>   - `fetchJson` sin timeout: ahora AbortController 12s (evita colgar la invocación en BNE/SPARQL).
>   - `CloudResolver.parseResult` descartaba la respuesta si venía sin cover ni descripción (perdía personajes): ahora acepta personajes solos.
> - **Deploy**: `resolveBook` us-central1 nodejs20 v2, secrets montados (Secret Manager accessor grant a `476827780707-compute@…`). 401 sin App Check = enforcement OK.
> - **Validado en runtime**: `gemma-4-26b:free` + `gpt-oss-20b:free` responden sinopsis ~920 chars + 10 personajes en español. `Google Books key` funcional (The Hunger Games → Suzanne Collins).

> **Changelog 2026-08-05 — "Deploy producido + App Check arreglado"**
> - **Blaze activado** en `alexandria-d3397` (se requería upgrade pay-as-you-go; free tier cubre el uso de 1 usuario).
> - **Runtime**: Node 18 fue retirado por GCP (2025-10-30) → `functions` subido a **Node 20** (`package.json` engines + CI).
> - **Función desplegada**: `resolveBook` (callable, us-central1, v2) — `firebase deploy --only functions` ✅. App Check enforcement activo (401 sin token).
> - **Bug de proyecto resuelto**: `com.alexandria.app` estaba **DELETED** en Firebase y la app activa era `com.cuba.messenger` (registro erróneo). Se restauró `com.alexandria.app` (ACTIVE) y `google-services.json` del repo coincide exactamente con el config oficial del servidor.
> - **SHA-1/SHA-256 registrados** en la app (debug `81:BB…`, release `7D:15…`) vía API.
> - **App Check debug token** registrado (`4dfe84ce-8f14-4694-b873-1c1267828db5`) + `app/src/debug/AndroidManifest.xml` lo inyecta; `AlexandriaApp.kt` usa `DebugAppCheckProviderFactory` en debug y `PlayIntegrity` en release (`debugImplementation firebase-appcheck-debug`).
> - **Pendiente (cerrado 08-06)**: secrets `GEMINI_API_KEY` / `GOOGLE_BOOKS_API_KEY` → reemplazados por `OPENROUTER_API_KEY` + `GOOGLE_BOOKS_API_KEY` (ambos seteados y montados).

> **Changelog 2026-08-04 — "Cloud resolution (Fase B/C)"**
> - **Cloud-first metadata**: `BookDetailViewModel.tryCloudResolve` llama al HTTPS callable `resolveBook` (Cloud Functions, `functions/`) → OpenLibrary→Google Books→BNE→Gemini con caché Firestore `metadata/{isbn}` (TTL 7d) + push FCM topic `user-{uid}`. Fallback a las APIs oficiales directas del device si falla/timeout (15s) — offline-first intacto.
> - Firebase: BOM 33.1.2 + `firebase-auth-ktx` (anonymous), `firebase-firestore-ktx`, `firebase-functions-ktx`, `firebase-config-ktx`, `analytics`, `crashlytics`, `appcheck`(+play-integrity) en `app/build.gradle.kts`; providers en `di/AppModule.kt`; AppCheck init en `AlexandriaApp` (try/catch-safe).
> - `CloudResolver.kt` (callable wrapper, `kotlinx.coroutines.tasks.await`); topic FCM suscrito tras sign-in anónimo.
> - **Validación runtime**: BNE SPARQL/SRU devuelven **403 Cloudflare** (bloqueo bot por IP de datacenter) → best-effort en la cadena; Google Books necesita `GOOGLE_BOOKS_API_KEY` (429 keyless); OpenLibrary search+works OK (desc vía `/works/{key}.json`). Detalles en `FIREBASE_PLAN.md`.
> - Deploy: `firebase login && firebase use --add && firebase functions:secrets:set GEMINI_API_KEY / GOOGLE_BOOKS_API_KEY && firebase deploy --only functions` (App Check Play Integrity con SHA-1 debug+release en consola).
> - ✅ `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL + `functions/` tsc limpio.

> **Changelog 2026-08-04 — "Alternativas al scraping + guardado local" (Fase A)**
> - Se eliminaron los scrapers HTML de tiendas: `fetchBuscalibreCover`, `fetchGandhiCover`, `fetchElSotanoCover`, `fetchLibreriaNacionalCover` (portadas) y `fetchDescriptionFromCasaDelLibro` (sinopsis), `fetchDescriptionFromTodoTusLibros` (sinopsis), y el parseo HTML de Wikipedia "Personajes" (`parsePersonajesSection` + `addNameFromLi`/`addNameFromTd` + `findAnexoPageKey`). Se quitó `import org.jsoup.Jsoup` y el `webClient` dedicado. Los enums `CoverSource` perdieron `BUSCALIBRE/GANDHI/EL_SOTANO/LIBRERIA_NACIONAL` y se añadieron `BNE` (SPARQL `datos.bne.es`) y `GOOGLE_BOOKS`.
> - Nuevas fuentes oficiales en la cadena: **BNE** `fetchBneCover(isbn)` + `fetchDescriptionFromBne(isbn)` (SPARQL, best-effort con graceful fallback), y **Google Books cover** `fetchGoogleBooksCover(isbn, title, author)` (API oficial).
> - Sinopsis ahora: ISBN(OpenLibrary) → **BNE** → OpenLibrary(es) → Wikipedia(es) → Google Books(es). Portadas: OpenLibrary Covers → Internet Archive → BNE → Google Books → OpenLibrary Search.
> - Persistencia local de portadas: `CoverStore.kt` escribe a `filesDir/covers/{isbn}.jpg`, `BookDao.updateCoverLocalPath`, `BookRepository.ensureCoverPersisted`. UI ya usa `coverUrl ?: coverLocalPath`.
> - **Backwards-compat guarantees (no books lost on import):** `BookDao.insertBook` usa `@Insert(onConflict=REPLACE)` con `id=0` autogenerado y **sin `UNIQUE` sobre ISBN** → el import es *append‑only* (nunca borra/sobre escribe). `SettingsViewModel` exporta `coverLocalPath=null` (ruta `/data/…` no portátil). `CoverSourceConfig`/`SynopsisSourceConfig` filtran enums/claves eliminados preservando el resto (sin crashear en backups antiguos).
> - **Caché de metadatos offline:** `MetadataCacheEntity` + `MetadataCacheDao`, migración **Room v5→v6** (`MIGRATION_5_6`, `exportSchema=false`, validada en runtime) + read/write-through en `BookDetailViewModel.fetchDescription` (TTL 7 días).
> - **Código muerto borrado:** `CoverService.kt`/`GoogleBooksApi.kt`/`OpenLibraryApi.kt` (Retrofit) + deps `retrofit`/`converter-gson`; modelos reagrupados en `GoogleBooksModels.kt` (usados por `PortadaResolver.buscarCovers*`/`CoverPicker`/`AddBookViewModel`).
> - ✅ `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL.

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
2. `portadaResolver.fetchDescriptionFromBne(book.isbn)` — BNE official SPARQL (`datos.bne.es/sparql`, ISBN property `P3013`; best-effort, graceful fallback)
3. `portadaResolver.fetchDescriptionBySearch(title, author, lang = "spa")` — OpenLibrary Spanish search (also fetches OpenLibrary ratings)
4. `portadaResolver.fetchDescriptionFromWikipedia(title, author)` — Spanish Wikipedia REST (`api.wikimedia.org` + `es.wikipedia.org/api/rest_v1/page/summary`)
5. `portadaResolver.fetchFromGoogleBooks(title, author)` — Google Books API with `langRestrict=es` (also fetches Google Books ratings)
6. Null → UI shows "No hay sinopsis disponible"

External ratings are sourced from OpenLibrary (step 3) and Google Books (step 5), whichever provides them first.

## Key project layout

- `domain/model/Book.kt` — Book data class with `ReadingStatus` enum
- `data/remote/PortadaResolver.kt` — covers + descriptions via OkHttp + JSONObject (not Retrofit)
- `data/repository/BookRepository.kt` — single repository bridging Room entities ↔ domain models
- `ui/components/BookCarousel.kt` — carousel with `CarouselItem.Single` / `CarouselItem.Series` pages
- `ui/components/SagaCarouselCard.kt` — saga deck spread (all books visible as overlapping cards)
- `ui/navigation/NavGraph.kt` — `Screen` sealed class defines all routes
- Version tag format: `v{versionName}+{versionCode}` (e.g. `v1.1.0+28`)
