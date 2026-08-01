## Notas de la versión v1.2.0+56

### Personajes

- **Corrección**: la extracción automática ya no mezcla personajes de otros libros (ya no aparecen Shrek, Dragon Ball o El rey león en libros como *El gato negro* o *El cielo de piedra*)
- **Corrección**: la búsqueda del libro ya no se queda con otra novela del mismo autor cuando no encuentra la página exacta; si no hay página del libro se usa la sinopsis/descripción como antes
- **Corrección**: al deseleccionar personajes en el diálogo de sugerencias ahora solo se añaden los que dejas marcados
- **Búsqueda más fiable**: se consulta primero **Wikidata** (propiedad `characters`, con nombres en español) y se usa Wikipedia solo como respaldo
- Anexos de Wikipedia: se aceptan solo si el título del anexo coincide con el título de tu libro (ya no se acepta el primer "Personajes de..." que aparezca)
