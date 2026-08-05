# Alexandria v1.2.0+62 — "Cloud Resolution"

## Alternativas al scraping + guardado local + Cloud Functions + IA gratuita

### Scraping eliminado → APIs oficiales
- Borrados todos los scrapers HTML de tiendas (Buscalibre, Gandhi, El Sótano, Librería Nacional, Casa del Libro, TodoTusLibros) y el parseo HTML de Wikipedia "Personajes".
- Cadena de portadas: OpenLibrary Covers → Internet Archive → BNE (SPARQL) → Google Books → OpenLibrary Search.
- Cadena de sinopsis: ISBN(OpenLibrary) → BNE → OpenLibrary(es) → Wikipedia(es) → Google Books(es).
- Personajes ahora vía API oficial Wikidata (SPARQL P674 + wbgetentities).

### Guardado local de portadas
- `CoverStore.kt` escribe a `filesDir/covers/{isbn}.jpg`.
- `BookDao.updateCoverLocalPath` + `BookRepository.ensureCoverPersisted`.
- UI usa `coverUrl ?: coverLocalPath` → sin red = portadas offline.

### Caché de metadatos offline (Room v5→v6)
- Nueva entidad `MetadataCacheEntity` (isbn, description, averageRating, ratingsCount, source, timestamp, ttlMs).
- Read/write-through en `BookDetailViewModel.fetchDescription` (TTL 7 días).

### Cloud resolution (Functions)
- `resolveBook` callable: OpenLibrary→Google Books→BNE→IA gratuita (OpenRouter).
- **IA gratis vía OpenRouter** (sin tarjeta): fallback multi-modelo `:free` (`google/gemma-4-26b-a4b-it:free`, `openai/gpt-oss-20b:free`, configurable con `OPENROUTER_MODELS`). El primero que devuelva JSON válido gana; los 429/errores saltan al siguiente.
- Sinopsis en español (120-180 palabras) + hasta 10 personajes reales **con emoji** (mapeado a `iconKey`, sin migración de DB).
- Caché Firestore `metadata/{isbn}` (TTL 7d) + App Check (Play Integrity).
- Push FCM topic `user-{uid}` al cachear nuevos metadatos (API v1 `messaging().send()`, no la legacy).
- Sign-in anónimo + fallback automático a APIs locales en timeout/offline.

### Estabilidad (2026-08-06)
- **Push arreglado**: migrado de `sendToTopic` (legacy FCM, deshabilitada para proyectos nuevos) a `messaging().send()` (v1). `AlexandriaMessagingService` prioriza `notification.title/body` con fallback al `data`. Solicitud del permiso `POST_NOTIFICATIONS` en Android 13+.
- Secrets v2 montados correctamente (`defineSecret` + `secrets: [...]` en el callable).
- `fetchJson` con timeout 12s (evita colgar en BNE/SPARQL).
- `CloudResolver.parseResult` ya no descarta respuestas solo con personajes.
- GitHub Actions: los secrets se setean solo si existen (guard `if`), evitando sobreescribir los de Firebase con vacíos.

### Backward-compat (backups)
- Import: append-only (`id=0`, sin UNIQUE en ISBN → nunca borra/sobre‑escribe).
- Export: `coverLocalPath=null` (ruta no portable se descarta).
- Config viejas: enums/claves eliminados filtrados graceful (sin crash).

### Limpieza
- Borrados `CoverService.kt`, `GoogleBooksApi.kt`, `OpenLibraryApi.kt` (Retrofit) + deps.
- Jsoup dep eliminado (sin scraping HTML).
- Modelos reagrupados en `GoogleBooksModels.kt`.

### CI/Deploy
- Functions deployable vía `firebase deploy --only functions` (Node 20, Gen2; Node 18 retirado por GCP 2025-10-30).
- Secrets: `OPENROUTER_API_KEY`, `GOOGLE_BOOKS_API_KEY` (Secret Manager, montados vía `secrets:` en el callable).
