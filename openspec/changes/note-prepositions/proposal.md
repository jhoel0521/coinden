# Proposal: Notas sin "por" y Requisito de Resultado Dividido

## Why
Al probar la pregunta "¿es el mismo gasto?" apareció «por» como nota: "gaste 10 bs por qr" deja la nota «por», y "gasté 10 por power" deja «por power». El paquete español ignora "para", "de" y "en", pero no "por", y la tarjeta muestra una nota sin sentido. Además, el requisito "Resultado de la Captura y Confirmación Inmediata" de `capture-ui` creció al sumarle la pregunta y supera el largo recomendado; conviene separar la pregunta en su propio requisito.

## What Changes
- **"por" como preposición de nota** en español y portugués, más "pelo" y "pela" en portugués y "by" y "via" en inglés (datos de los paquetes, no código).
- **Nota de respaldo**: si después de la preposición no queda ninguna palabra (por ejemplo, "almuerzo 45 por qr", donde "qr" es el medio de pago), la nota son las demás palabras sin preposiciones ("almuerzo"), en lugar de quedarse vacía.
- **Requisito dividido**: el requisito "Resultado de la Captura y Confirmación Inmediata" de `capture-ui` se reemplaza por dos: "Resultado de la Captura" y "Pregunta de Mismo Gasto en el Resultado", con los mismos seis escenarios. No cambia el comportamiento.

### Non-Goals
- Limpiar otras palabras que pueden quedar sueltas en la nota, como "con" en "pagué 20 con tarjeta". Queda para un cambio que defina qué palabras son relleno sin romper notas como "almuerzo con amigos".
- Cambiar cómo se elige la categoría.

## Capabilities

### New Capabilities
<!-- Ninguna. -->

### Modified Capabilities
- `text-interpreter`: la nota ignora "por" (y sus equivalentes) y, si después de la preposición no queda nada, usa las demás palabras.
- `capture-ui`: el requisito del resultado de la captura se divide en dos, sin cambiar el comportamiento.

## Impact
- **`core/domain`**: paquetes de idioma (es, pt, en) y la regla de la nota en `PhraseReader`.
- **Sin cambios** en la app, la persistencia ni las dependencias.
