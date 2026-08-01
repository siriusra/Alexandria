## Notas de la versión v1.2.0+58

### Sinopsis

- **Novedad**: la sinopsis ahora se **guarda en la biblioteca**; al volver a abrir un libro se muestra al instante y funciona sin conexión
- **Corrección**: al editar un libro ya no se pierde la sinopsis guardada
- **Búsqueda más precisa en Google Books**: cuando el libro tiene ISBN, se busca directamente por ISBN (en formato ISBN-13) antes de buscar por título y autor

### Búsqueda de personajes

- **Corrección**: ya no se extraen personajes de páginas de películas, series, videojuegos, mangas u otros tipos de obras; solo se aceptan entradas de tipo libro

### Detalles técnicos

- Normalización de ISBN-10 a ISBN-13 con verificación de dígito de control
- Migración de base de datos a la versión 5 (columna de sinopsis)
