# Proposal: Categorización de Gastos (Expense Categorization)

## Why
CoinDen ya sabe **cuánto** gastaste y **de qué billetera** salió el dinero, pero no **en qué**. La primera pregunta del producto, "¿en qué se me fue el dinero este mes?", no tiene respuesta. Además, el libro de movimientos no guarda la nota ni el comercio del gasto ("almuerzo", "POLLOS DEL VALLE"): esa descripción solo vive en el borrador confirmado, así que no hay de qué aprender ni qué reportar.

## What Changes
- **Categorías base más propias**: 12 categorías base con identificadores neutrales que la presentación traduce (comida, mercado, transporte, salud, suscripciones, servicios, hogar, salidas, compras, educación, personas, comisiones). El usuario puede crear y archivar las suyas.
- **Recomendación desde el primer día, en orden**: (1) la regla aprendida del usuario (confianza ALTA), (2) una palabra típica del paquete de idioma (MEDIA), (3) nada, con lo que el gasto queda "Sin categoría". La regla del usuario siempre gana sobre el paquete.
- **Aprendizaje determinista por corrección**: cuando el usuario elige o cambia la categoría de un gasto con descripción, nace o se actualiza una regla local "descripción → categoría", explicable y sin modelos.
- **Clasificación separada del dinero**: la descripción y la categoría de cada gasto se guardan en un registro append-only propio, fuera del libro de movimientos. Recategorizar no toca el dinero ni anula movimientos.
- **Solo los gastos se categorizan**: transferencias, retiros y cambios de moneda mueven dinero entre billeteras propias y no son "en qué gastaste".
- **En la revisión de la noche** (modifica `draft-inbox`): la bandeja muestra la categoría sugerida de cada borrador de gasto; al confirmar, el usuario puede aceptarla, cambiarla o dejarla vacía. La categoría nunca bloquea la confirmación.
- **Reporte "¿en qué gasté?"**: el gasto de un período por categoría, por moneda y consolidado en la moneda local con las tasas configuradas, sin contar gastos anulados.

### Non-Goals
- Pantallas del reporte y de la revisión (`insights-ui`, `review-inbox-ui`).
- Subcategorías, presupuestos por categoría y alertas de gasto.
- Categorizar ingresos.
- Sugerir categorías con modelos de IA.
- Dividir un gasto en varias categorías (por ítem de un ticket).

## Capabilities

### New Capabilities
- `expense-categorization`: catálogo de categorías base y propias, recomendación por reglas aprendidas y palabras del paquete de idioma, aprendizaje por corrección, clasificación append-only de gastos con su descripción, y reporte de gasto por categoría.

### Modified Capabilities
- `draft-inbox`: la bandeja muestra la categoría sugerida de cada borrador de gasto; la confirmación acepta una categoría opcional y registra la clasificación con la descripción del borrador.

## Impact
- **Código nuevo** en `core/domain`: modelo de categorías y referencias, eventos de clasificación y su proyección, reglas aprendidas, categorizador puro, puertos de categorías, reglas y clasificaciones, y casos de uso para crear o archivar categorías, reclasificar gastos y obtener el reporte por categoría.
- **text-interpreter**: los paquetes de idioma suman palabras típicas por categoría como datos (en, es y pt).
- **draft-inbox**: `ConfirmDraftUseCase` recibe la categoría elegida y registra la clasificación; `GetDraftInboxUseCase` expone la sugerencia por borrador.
- **Pruebas**: unitarias del categorizador, de las reglas y del reporte; de feature con el mes de Carlos (aprende "POLLOS DEL VALLE" y "trasmporte", reporte por categoría con YouTube como suscripción) y con John y Ana en inglés y portugués.
