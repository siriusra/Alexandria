## Notas de la versión v1.2.0+45

### Novedades
- **Nuevos estados de lectura**: Quiero leer, Leyendo, Pausado, Releyendo, Abandonado, Terminado y Favoritos.
- **Selector con diseño mejorado**: ahora es un desplegable (dropdown) con icono y color distintivo para cada estado, tanto al añadir libros como en la pantalla de detalle.
- **Colores e iconos exclusivos** para cada estado, visibles en la ficha del libro, el carrusel, la saga y la insignia de estado.

### Mejoras
- Los estados antiguos (Pendiente → Quiero leer, Leyendo, Terminado) se migran automáticamente al actualizar la app.
- Nueva clase `ReadingStatusUi.kt` que centraliza la configuración visual de cada estado (color + icono).
