# Plan Firebase — Alternativas al scraping + guardado local

**Fecha:** 2026-08-04
**Objetivo:** eliminar el scraping de tiendas del dispositivo, resolver portadas/sinopsis solo vía APIs oficiales (con una Cloud Function como agregador y caché) y persistir localmente todo lo descargado para funcionar offline.

---

## Estado (2026-08-04 — checkpoint entregado)

**BUILD: `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL** (compilación verde tras los cambios de Fase A).

| Ítem Fase A | Estado |
|---|---|
| 1. Borrar scrapers HTML de tiendas + parseo HTML de Wikipedia "Personajes" | ✅ Hecho (`PortadaResolver.kt`: `fetchBuscalibreCover`/`fetchGandhiCover`/`fetchElSotanoCover`/`fetchLibreriaNacionalCover`/`fetchDescriptionFromCasaDelLibro`/`fetchDescriptionFromTodoTusLibros`/`parsePersonajesSection`/`addNameFromLi`/`addNameFromTd`/`findAnexoPageKey`/`extractCoverFromDoc` borrados). `import org.jsoup.Jsoup` y `webClient` eliminados (Jsoup quedó sin uso). Personajes pasa a resolverse **solo vía API oficial Wikidata** (`wbgetentities` + SPARQL `P674`). |
| 2. Integrar BNE (`datos.bne.es/sparql`, propiedad ISBN `http://datos.bne.es/def/P3013`) | ✅ Hecho. `fetchBneCover(isbn)` + `fetchDescriptionFromBne(isbn)` añadidos a la cadena de portadas y de sinopsis. Consultas SPARQL con `PREFIX bne/dct/schema`. **Nota:** el endpoint/ontología exacta no pudo validarse en runtime aquí por lo que ambas funciones son best-effort con `try/catch` que devuelve `null` y deja caer al siguiente source (graceful degradation). Validar ontología BNE en test runtime antes de release. |
| 3. Guardado automático de portadas en disco | ✅ Hecho. `CoverStore.kt` descarga a `filesDir/covers/{isbn}.jpg`; `BookDao.updateCoverLocalPath`; `BookRepository.ensureCoverPersisted`/`persistCover` integrado; UI ya renderiza `coverUrl ?: coverLocalPath` (BookCard/BookList/ShelfView/BookCarouselCard/ReadingNook/BookDetailScreen). No cambios de pantalla. |
| 4. Caché de metadatos (`metadata_cache`) | ✅ Hecho. Entidad `MetadataCacheEntity` + `MetadataCacheDao` (`get`/`put`/`evictExpired`), migración **Room v5→v6** (`MIGRATION_5_6`, `CREATE TABLE metadata_cache`, `index_metadata_cache_isbn`) registrada en `AlexandriaDatabase.getDatabase`. `exportSchema=false` (la migración se valida en runtime; evita schema-dir commit). Read/write-through: `BookDetailViewModel.fetchDescription` lee caché antes de la red y la escribe tras resolver (`repository.getCachedMetadata`/`cacheMetadata`). TTL 7 días. |
| 5. Ajustes UI (`PreferencesManager` + settings) | ✅ Hecho. `SynopsisSourceConfig.defaultOrder` → `["isbn","bne","openlibrary","wikipedia","google_books"]`; `FuentesSettingsScreen`/`SynopsisSettingsScreen` con BNE y sin tiendas; help-text actualizado. El toggle "guardar portadas offline" ya existía (`COVER_DOWNLOAD_ENABLED` → `CoverStore`). |
| 6. Limpieza código muerto | ✅ Hecho. Borrados `CoverService.kt`, `GoogleBooksApi.kt`, `OpenLibraryApi.kt` (Retrofit) y deps `retrofit`/`converter-gson` de `app/build.gradle.kts` (OkHttp/Logging/Json/Gson preservados). Los modelos compartidos (`GoogleBookItem`, `VolumeInfo`, `ImageLinks`, `IndustryIdentifier`, `GoogleBooksData`) se conservaron/refactorizaron en `GoogleBooksModels.kt`. |

**Fuentes de sinopsis activas ahora:** ISBN(OpenLibrary) → **BNE** → OpenLibrary(es) → Wikipedia(es) → Google Books(es).
**Fuentes de portada activas ahora:** OpenLibrary Covers → Internet Archive → **BNE** → **Google Books** → OpenLibrary Search.

### Garantías de compatibilidad con backups (requisito estricto: *no perder libros al importar*)
- **Importaciones no borran ni sobreescritten libros**: `importFromJson` llama `repository.addBook(book.withDefaults().copy(id = 0))` → `BookDao.insertBook` (`@Insert(onConflict = REPLACE)`) con `id = 0` autogenerado; como **no hay `UNIQUE` sobre ISBN/título**, `REPLACE` nunca dispara y el import es *append‑only*. Los libros ya guardados se conservan.
- **`coverLocalPath` no viaja en backups**: `SettingsViewModel.export*` serializa `books.map { it.copy(coverLocalPath = null) }` (ruta `/data/…` no portable → se descarta). La UI prioriza `coverUrl ?: coverLocalPath`; al importar, `coverLocalPath` viene null → `coverUrl` presente → `ensureCoverPersisted` re‑materializa el local.
- **New fields en el modelo `Book` son additive w/ defaults** → backups viejos (sin el campo) importan sin romper (Gson apply defaults; `coverLocalPath: String? = null`).
- **`CoverSource`/`SynopsisSourceConfig` viejos en DataStore**: `fromJson` filtra enums/claves desconocidos (`GANDHI`, `todostuslibros`, …) preservando el resto; si queda vacío, aplica el default. **No crashea** ningún backup antiguo.
- **Migración Room v5→v6** solo `CREATE TABLE metadata_cache` (additivo); no elimina columnas ni datos.

---

## Diagnóstico actual

### Fuentes que YA son API oficial (JSON, no scraping) — se conservan
- OpenLibrary (`search.json`, `covers.openlibrary.org`, `/isbn/xxx.json`, `/books/xxx.json`)
- Google Books (`www.googleapis.com/books/v1/volumes`, `langRestrict=es`)
- Internet Archive (`archive.org/advancedsearch.php?...output=json`)
- Wikipedia es REST (`api.wikimedia.org`, `es.wikipedia.org/api/rest_v1/page/summary`)
- Wikidata (`w/api.php?action=wbgetentities`, `query.wikidata.org/sparql`)

### Fuentes que SÍ son scraping HTML (frágiles) — se eliminan
| Fuente | Uso | Referencia en PortadaResolver.kt |
|---|---|---|
| Buscalibre (.mx) | Portada | `fetchBuscalibreCover` |
| Gandhi (.mx) | Portada | `fetchGandhiCover` |
| El Sótano | Portada | `fetchElSotanoCover` |
| Librería Nacional (CO) | Portada | `fetchLibreriaNacionalCover` |
| Casa del Libro | Sinopsis | `fetchDescriptionFromCasaDelLibro` |
| TodoTusLibros | Sinopsis | `fetchDescriptionFromTodoTusLibros` |
| Wikipedia "Personajes" (HTML) | Personajes | parseo de `es.wikipedia.org/wiki/...` (se conserva Wikidata SPARQL) |

### Persistencia local hoy
- ✅ Descripción → `books.description` (Room)
- ✅ URL de portada → `books.coverUrl` (Room) + tabla `cover_cache` por ISBN
- ✅ Rating → `books.rating`
- ❌ **Imagen de portada nunca se guarda en disco**: `coverLocalPath` existe en el modelo pero nunca se asigna (solo se lee/mapea en `BookRepository.kt`). Sin red = sin portadas (solo caché efímera de Coil).
- `CoverService.kt` y `OpenLibraryApi.kt` (Retrofit) existen pero **no se usan** (código muerto).

---

## Arquitectura de datos (flujo read-through)

```
Abrir libro / resolver portada
  → 1. Room local (books.coverLocalPath, cover_cache, metadata_cache)   [sin red]
  → 2. Cloud Function resolveBook (Firestore cache)                      [con red]
  → 3. Fallback: APIs oficiales directas desde el cliente                [sin nube]
  → Guardar resultado:
       imagen  → filesDir/covers/{isbn}.jpg + coverLocalPath
       sinopsis/rating → metadata_cache (Room, con TTL)
```

---

## Fase A — Cliente (offline-first, sin depender de la nube)

1. **Eliminar scrapers** en `data/remote/PortadaResolver.kt`:
   - Borrar `fetchBuscalibreCover`, `fetchGandhiCover`, `fetchElSotanoCover`, `fetchLibreriaNacionalCover`, `fetchDescriptionFromCasaDelLibro`, `fetchDescriptionFromTodoTusLibros` y el parseo HTML de Wikipedia "Personajes" (dejar Wikidata SPARQL).
   - Quitar `BUSCALIBRE`, `GANDHI`, `EL_SOTANO`, `LIBRERIA_NACIONAL` de `data/model/CoverSource.kt` y de la configuración por defecto.

2. **Integrar BNE** (`fetchDescriptionFromBNE(isbn)`) como fuente oficial de metadatos ISBN en español (datos.bne.es, REST/SPARQL). Añadir a la cadena de sinopsis junto a OpenLibrary (spa), Google Books `langRestrict=es` y Wikipedia es.

3. **Guardado automático de portadas**:
   - Nuevo `data/local/CoverStore.kt`: descarga la imagen con OkHttp y la escribe en `filesDir/covers/{isbn}.jpg`, devolviendo la ruta.
   - Nuevo método `updateCoverLocalPath` en `BookDao` → `BookRepository` lo llama tras resolver la portada para **asignar `coverLocalPath`**. La UI ya renderiza `coverUrl ?: coverLocalPath`, no se tocan pantallas.

4. **Caché de metadatos**:
   - Nueva entidad `MetadataCacheEntity` (clave ISBN, description, averageRating, ratingsCount, source, timestamp, ttl) + `MetadataCacheDao`.
   - Migración Room **v5 → v6** en `AlexandriaDatabase.kt` + registro en `AppModule.kt`.
   - `BookDetailViewModel` lee `metadata_cache` antes de llamar a la red.

5. **Ajustes** (`PreferencesManager` + `SettingsScreen`): toggle "guardar portadas para offline" (def. on), borrar caché, TTL de metadatos. Actualizar `FuentesSettingsScreen` a las nuevas fuentes (OpenLibrary, Google Books, Internet Archive, Wikipedia, BNE).

6. **Limpieza:** `CoverService.kt` y `OpenLibraryApi.kt` están muertos → eliminarlos o integrarlos. Actualizar `AGENTS.md` (cadena de sinopsis).

---

## Fase B — Cloud Functions (agregador, sin scraping)

- Proyecto `functions/` (TypeScript, Gen 2): callable `resolveBook({isbn?, titulo, autor})`.
- Cadena en servidor con **solo APIs oficiales**: OpenLibrary (search + isbn.json) → Google Books (`langRestrict=es`, API key ya en `keystore.properties` → `GOOGLE_BOOKS_API_KEY`) → BNE → Internet Archive → Wikipedia es → Wikidata (personajes).
- Caché en Firestore `metadata/{key}` con TTL; respuesta consolidada `{coverUrl, description, rating, ratingsCount, source, characters[]}`.
- Protegido con **App Check**; `Remote Config` controla si el cliente usa la nube o el fallback local.
- Cliente: `PortadaResolver.resolveViaCloud(...)` como primera fuente cuando hay red; ante timeout/offline → APIs oficiales directas (Fase A).

---

## Fase C — Observabilidad (medir el resultado)

Crashlytics + Analytics: eventos de hit/miss y duración por fuente (portada y sinopsis) para validar que BNE/OpenLibrary/Google cubren lo que antes cubrían las tiendas.

---

## Riesgos

- **BNE**: verificar endpoint exacto y cobertura de ISBN comerciales durante implementación (incógnita real).
- **Configs viejas en DataStore**: JSONs guardados con los enum de tiendas ya removidos → `fromJson` falla → `PreferencesManager` ya cae al default por try/catch (comportamiento deseado).
- **Quotas**: mitigadas por Firestore cache + Remote Config.

---

## Archivos implicados (Fase A)

- `app/src/main/java/com/alexandria/app/data/remote/PortadaResolver.kt`
- `app/src/main/java/com/alexandria/app/data/model/CoverSource.kt`
- `app/src/main/java/com/alexandria/app/data/local/` (nueva entidad `metadata_cache`, `CoverStore.kt`)
- `app/src/main/java/com/alexandria/app/data/local/entity/BookEntity.kt` (sin cambios, ya tiene `coverLocalPath`)
- `app/src/main/java/com/alexandria/app/data/local/BookDao.kt` (`updateCoverLocalPath`)
- `app/src/main/java/com/alexandria/app/data/local/AlexandriaDatabase.kt` (migración v5→v6)
- `app/src/main/java/com/alexandria/app/data/repository/BookRepository.kt`
- `app/src/main/java/com/alexandria/app/ui/screens/detail/BookDetailViewModel.kt`
- `app/src/main/java/com/alexandria/app/di/AppModule.kt`
- `app/src/main/java/com/alexandria/app/data/local/PreferencesManager.kt` + `ui/screens/settings/*`
- `AGENTS.md`

## Fase B/C � Arquitectura Cloud (2026-08-04)

**Decisi�n (confirmada por el usuario):** Opci�n 1 (Cloud Functions + APIs oficiales) con **IA gratuita Gemini 1.5-flash como fallback**, single-user. `metadata/{isbn}` en Firestore como cach� compartida; notas/progreso/rating-personal quedan en device (INSERT OR IGNORE, nunca clobber). Free tier: \$0.

### Flujo
```
BookDetailViewModel.fetchCover/fetchDescription
  ? tryCloudResolve(book) [CloudResolver]
      FirebaseAuth anonymous sign-in (lazy) + subscribe FCM topic user-{uid}
      ? HTTPS callable resolveBook(uid, isbn, titulo, autor)  [App Check enforced]
          Firestore metadata/{isbn} hit (TTL 7d) ? devolver
          chain: OL byISBN ? GB byISBN (key) ? BNE byISBN (best-effort, CF-bloqueado) ? OL byTitle (+works desc) ? GB byTitle ? Gemini fallback
          cachear metadata/{isbn} + FCM push topic user-{uid}
  ? si falla/timeout (15s) ? fallback a las APIs oficiales directas del dispositivo (Fase A, intactas)
```
Offline-first intacto: la cach� Room (metadata_cache) + portadas en disco siguen siendo la 1.� fuente sin red.

### Archivos nuevos/ahora
- `app/src/main/java/com/alexandria/app/data/remote/CloudResolver.kt` � wrapper callable `resolveBook` (timeout 15s, `kotlinx.coroutines.tasks.await`).
- `app/src/main/java/com/alexandria/app/AlexandriaApp.kt` � AppCheck (Play Integrity) init con try/catch (sin google-services ? no bloquea).
- `app/src/main/java/com/alexandria/app/di/AppModule.kt` � providers FirebaseAuth/Firestore/Functions/RemoteConfig.
- `app/build.gradle.kts` � BOM 33.1.2 + messaging/auth/firestore/functions/config/analytics/crashlytics/appcheck(+playintegrity).
- `BookDetailViewModel.kt` � `tryCloudResolve` cloud-first con fallback local; `currentUid()` (anonymous), `subscribeToUserTopic`.
- `functions/` � `package.json` (Node 20; Node 18 retirado por GCP el 2025-10-30), `tsconfig.json`, `src/index.ts` (`resolveBook` onCall + enforceAppCheck + cache + push).
- `firebase.json` � emuladores (functions 5001, firestore 8080, auth 9099).
- `functions/scripts/validate_sources.mjs` � validaci�n runtime local de fuentes.

### Hallazgos de validaci�n runtime (2026-08-04, importante)
- **OpenLibrary search + works**: ? OK (200). `search.json?fields=key,title,author_name,cover_i,isbn` ? cover_i ? `covers.openlibrary.org/b/id/{cover_i}-L.jpg`; descripci�n fiable v�a `/works/{key}.json`. Es la fuente m�s robusta.
- **Google Books**: 429 sin key (quota global agotada) ? el Function usa `GOOGLE_BOOKS_API_KEY` (env) para funcionar. La API key debe crearse en Google Cloud Console.
- **BNE SPARQL (`datos.bne.es/sparql`)**: **403 Cloudflare "Attention Required"** � bloqueo bot por IP de datacenter; ni User-Agent de browser ni Referer lo evitan. `datos.bne.es/sru` igual. Se mantiene como best-effort en la cadena (falla ? siguiente fuente). **Requiere validaci�n manual en navegador** o, si se quiere forzar, servicio intermedio con resoluci�n de Cloudflare challenge (no recomendado).
- **Wikipedia es + Wikidata P674**: ? 200 (extracts OK; P674 query vac�a para la muestra � best-effort).
- **Gemini 1.5-flash**: no probado sin `GEMINI_API_KEY`; free tier 1.500 req/d�a. **SUSTITUIDO (2026-08-06)** por **OpenRouter** con modelos `:free` (sin tarjeta): `google/gemma-4-26b-a4b-it:free`, `openai/gpt-oss-20b:free`, etc. Fallback multi-modelo v�a `OPENROUTER_MODELS`; secret `OPENROUTER_API_KEY`. Validado: sinopsis ~920 chars + 10 personajes con emoji.

### Deploy (requiere Firebase CLI + cuenta)
```bash
cd Alexandria-main/Alexandria-main/functions
firebase login
firebase use --add          # seleccionar proyecto de google-services.json
npm install
# secrets:
firebase functions:secrets:set OPENROUTER_API_KEY
firebase functions:secrets:set GOOGLE_BOOKS_API_KEY
firebase deploy --only functions
```
- **App Check**: activar en Firebase Console ? App Check ? Play Integrity, a�adir SHA-1 de debug (`keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey`) y de release.
- **Emuladores locales**: `npm run serve` en `functions/` (Firestore + Functions + Auth) � el cliente no cambia URLs (usa default).
- **Remote Config**: claves `use_cloud_resolution` (bool, default true), `metadata_ttl_days` (7), `cloud_resolve_timeout_ms` (15000).

### Coste (1 usuario, free tier)
Functions 125k invocaciones/mes + Firestore 50k reads/d�a + Auth 10k + App Check 1M + Gemini Flash 1.5k req/d�a ? **\$0**.
