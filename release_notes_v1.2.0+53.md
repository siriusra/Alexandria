## Notas de la versión v1.2.0+53

### Biblioteca Virtual — Experiencia inmersiva mejorada

- **Fondo de estantería en todas las pantallas**: líneas de repisa visibles incluso con la biblioteca vacía
- **Viñeta envolvente**: sutil oscurecimiento en los bordes para sensación de ambiente
- **Vista de estantería rediseñada**: cada libro muestra su portada real, título horizontal legible, autor, altura variable según páginas, y estante de madera realista con textura
- **Estado vacío mejorado**: ilustración de estantería dibujada con Canvas en lugar de texto plano
- Corrección: `RoundedCornerShape` faltante que impedía la compilación
- Corrección: animación de prensado muerta en `ReadingNook`
- Corrección: comparación frágil de enum en `ShelfView`
