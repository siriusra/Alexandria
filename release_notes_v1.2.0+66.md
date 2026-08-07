# Alexandria v1.2.0+66 — "IA y caché definitiva"

## Arreglo total de la resolución en la nube (resolveBook)

### Cloud resolution funcionando (2026-08-07)
- Se activó el sign-in anónimo en Firebase Auth (requisito para las callable functions).
- Se eliminó App Check del APK release (ya no se exige token de App Check desde la app).
- `resolveBook` ahora corre con `enforceAppCheck: false` y el Cloud Run quedó en acceso público, así el framework acepta peticiones sin verificar App Check.
- Se habilitó la API de Cloud Firestore y se creó la base de datos `(default)` en `us-central1` (no existía: la función fallaba con `NOT_FOUND`).

### Caché de metadatos indefinida
- Los metadatos (portada, sinopsis, personajes, rating) se guardan en Firestore `metadata/{isbn}` **indefinidamente** (antes TTL de 7 días; ahora 10 años de retención y sin vencimiento en lectura).
- El botón "Buscar con IA" ahora envía `force: true` al backend, que **ignora la caché y re-resuelve desde cero**, sobrescribiendo los datos viejos (libros cacheados antes de los personajes se actualizan con personajes + sinopsis nuevos).

### Personajes de la IA aunque existan portada/sinopsis
- Antes la IA (OpenRouter) solo corría cuando las APIs oficiales no encontraban nada.
- Ahora la IA se ejecuta siempre que falte la **descripción** *o* los **personajes**, rellenando los huecos y conservando portada/rating de las APIs oficiales.
- Ejemplo verificado: "Cien años de soledad" → 8 personajes con emoji (José Arcadio Buendía 👴, Úrsula 👵, Coronel Aureliano 🎖️, Amaranta 🧵, Rebeca 🌑, Melquíades 📜, Aureliano Babilonia 📖, Pilar Ternera 🔮); "El Hobbit" → Bilbo 👣, Gandalf 🧙♂, Thorin 👑, Smaug 🐉, Gollum 👁️, Elrond 🧝♂, Bard 🏹.

### Notas
- Node 20 (funciones) deprecado en Cloud Functions (Oct 2026): migrar a Node 22/24 próximamente.
