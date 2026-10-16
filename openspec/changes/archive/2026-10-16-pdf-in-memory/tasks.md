# Tasks: PDF en Memoria

## 1. Lectura sin copias

- [x] 1.1 Cambiar `AndroidPdfReader` para dibujar las páginas de un PDF escaneado con pdfbox-android en memoria (escala para 2000 px de ancho, JPEG, primeras 5 páginas), sin `PdfRenderer` ni archivo temporal; ampliar `AndroidPdfReaderTest` para verificar que no aparecen archivos nuevos en `cacheDir` ni en `filesDir` (req. Lectura sin Copias Descifradas, escenario PDF escaneado).
- [x] 1.2 Borrar al iniciar la app los `pdf-*.pdf` que hayan quedado en `cacheDir` (escenario Restos de una versión anterior) y verificar que `npm test`, `npm run lint` y `npm run build:android` pasan.

## Workflow follow-up

- Archivar `pdf-in-memory` tras la revisión.
