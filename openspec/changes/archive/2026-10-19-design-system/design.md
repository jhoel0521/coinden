# Design

## Context

- Las pantallas viven en `composeApp/commonMain`: `capture`, `home`, `inbox`, `history`, `insights`, `onboarding`, `perception`, `settings` y `ui/LoadNotice`. Usan Material 3 directamente: unos 61 botones, 8 `FilterChip`, 16 `OutlinedTextField`, 18 `Surface`, 3 `AlertDialog`, 8 `FlowRow` y 1 `Switch`.
- `theme/` ya define los colores "Cobre", las tipografías, los montos tabulares (`CoinDenTheme.amounts`) y los íconos (`CoinDenIcon`).
- Los `FlowRow` de chips son la causa de las opciones sueltas: cortan donde no entra la siguiente.
- `CategorizationRepositoryPort.classificationsOf(ownerId)` ya permite contar qué categorías usa más la persona.

## Goals / Non-Goals

**Goals:**
- Un único lugar, `ui/components`, que decide la estética y la composición de cada control.
- Las decisiones de forma (segmentado, fila deslizable o "Más…"; fila o pila de acciones; columnas de la cuadrícula) son **funciones puras** con pruebas en la JVM.
- Migrar las 11 pantallas sin cambiar su comportamiento ni sus presentadores, salvo lo pedido en los specs: moneda derivada, "Más detalles", tarjeta compacta, categorías más usadas e Inicio.

**Non-Goals:**
- Una librería separada o un módulo nuevo: es un paquete dentro de `composeApp`.
- Animaciones nuevas.

## Decisions

### D1. Catálogo de componentes (`com.coinden.app.ui.components`)

| Componente | Reemplaza | Notas |
|---|---|---|
| `CdAction(text, role, onClick, icon?, enabled?)` | `Button`, `OutlinedButton`, `TextButton`, `FilledTonalButton` | `role`: `Primary`, `Secondary` (tonal), `Tertiary` (texto) y `Danger` (contorno, con color de error e ícono). Altura de 48 dp, o 56 dp si es `Primary` a todo el ancho. |
| `CdActions { … }` | filas y columnas de botones | Grupo de hermanos: aplica D2. |
| `CdIconAction(icon, label, onClick)` | `IconButton` | La descripción es obligatoria. |
| `CdChoice(options, selected, onSelect, style = Auto)` | `FilterChip` sueltos y `FlowRow` | Aplica D3. Opciones con texto y, opcionalmente, ícono o marca ("sugerida"). |
| `CdChoiceGrid(options, columns)` | cuadrícula de "Más…" y tipos de billetera | Columnas fijas del mismo ancho (D4). |
| `CdField(value, onChange, label, before?, error?, keyboard, trailing?)` | `OutlinedTextField` | `trailing` muestra la moneda de la billetera junto al monto; `before` es el "antes: …". |
| `CdSection(title, before?, error?) { … }` | títulos de sección con `Column` | Mantiene juntos el título, el contenido, el "antes" y el error. |
| `CdCard(tone) { … }` | `Surface` y `Card` | `tone`: `Plain`, `Raised`, `Accent`, `Warning`, `Error`. Da el margen a sus hijos (D5). |
| `CdNotice(kind, text, action?)` | avisos y `LoadNotice` | Usa una región en vivo para lectores de pantalla. |
| `CdDialog(title, text, confirm, dismiss)` | `AlertDialog` | Las acciones van por `CdActions`. |
| `CdSwitchRow(title, text, checked, onChange)` | `Switch` | Toda la fila es tocable. |
| `CdCollapsible(title, initiallyOpen) { … }` | "Más detalles" | Se abre solo si `initiallyOpen` (por ejemplo, si trae referencia). |
| `CdSheet` | hoja de "Más…" | `ModalBottomSheet` con `CdChoiceGrid`. |

Las pantallas importan **solo** `ui.components` y `theme`, más `Text`, `Icon` y los layouts de Compose.

### D2. Acciones según sus hermanos (puro)

`ActionsLayout.of(count: Int, fitsInRow: Boolean)`:
- 1: `Full`.
- 2 que caben: `Row`, con el mismo peso para las dos.
- 2 que no caben, o 3 o más: `Stack`, con la principal al final.

`fitsInRow` se calcula midiendo los textos con `TextMeasurer` a la letra actual. Así, con letra grande, dos acciones largas pasan a pila en vez de cortarse.

### D3. Elección única según la cantidad de opciones (puro)

`ChoiceLayout.of(count, open: Boolean, labelWidths, available)`:
- `Segmented` si `!open`, `count <= 4` y la suma de anchos más los rellenos entra en `available`.
- `ScrollRow` si `open`, o si no entra. Al aparecer hace scroll hasta la elegida.
- `TopAndMore(n = 4)` si hay más de 8 opciones fijas, como las 13 categorías.

Las "más usadas" vienen del presentador: `DraftFormPresenter` cuenta `classificationsOf(owner)` por categoría y ordena por uso, con desempate por el orden base. Si la elegida no está entre esas 4, se agrega en el primer lugar.

Los segmentados usan `SingleChoiceSegmentedButtonRow` de Material 3, envuelto: las pantallas nunca lo ven.

### D4. Cuadrícula sin sueltas (puro)

`GridLayout.columns(available, minCell = 96.dp)` toma entre 2 y 4 columnas. Cada celda mide `available / columns`. La última fila conserva el ancho de celda y se alinea a la izquierda, de modo que ninguna opción se estira ni queda distinta. `columns` prefiere la cantidad que no deja una sola al final (4 opciones: 2 x 2); si ninguna lo logra, `GridLayout.rows` reparte las filas (13 en 3 columnas: 3, 3, 3, 2, 2). En 2 columnas no hay reparto posible: con una cantidad impar, la última ocupa la fila entera (`lastSpansRow`).

### D5. Componentes que saben dónde están

`LocalCdContainer` es un `CompositionLocal` con `Screen`, `Card`, `Section`, `Sheet` o `Dialog`, que `CdCard`, `CdSection`, `CdSheet` y `CdDialog` proveen a sus hijos:
- En `Screen`, los grupos usan el margen de pantalla de 16 dp.
- Dentro de `Card`, `Sheet` o `Dialog`, usan el ancho interior sin margen propio.
- `CdActions` dentro de `Dialog` se alinea a la derecha con `Tertiary` y `Primary`, según Material.
- Los errores y el "antes" de un `CdSection` siempre van dentro de la sección. Esto resuelve F2: el aviso de moneda propia pasa al `CdSection` del formulario de moneda.

### D6. Tarjeta compacta de la bandeja

`InboxRow(card, reason)` tiene dos líneas:
- nota (o la evidencia) y el monto a la derecha;
- billetera (con "sugerida" si corresponde) y el motivo.

El motivo sale del borrador:
- `CaptureCoach.tipsFor` da **Falta el monto**, **Falta la billetera** o **Billetera sugerida**;
- la confianza media da **Revisa el tipo**;
- la pregunta de mismo gasto da **¿Mismo gasto?**;
- un cambio pendiente da **Cambio pendiente**.

Tocar la fila abre el formulario, como hoy. La tarjeta completa (`DraftCardView`) queda para el resultado de la captura.

### D7. Inicio

- Se quitan los textos "Borradores por revisar: N" y "No cambian tus saldos…"; queda `CdActions` con "Revisar (N)" (`Primary`) e "Historial" (`Secondary`).
- "Billeteras y tasas" pasa a `CdAction(Tertiary)` en la misma fila que el título "Billeteras".
- "Gastos del mes" pasa a `Secondary` (tonal), con mejor contraste en el tema oscuro.

### D8. Prueba que prohíbe los controles sueltos

`androidUnitTest/.../RawMaterialUsageTest.kt` recorre `src/commonMain/kotlin` fuera de `ui/components`. Falla si encuentra imports de `androidx.compose.material3` que no estén en la lista permitida (`Text`, `Icon`, `MaterialTheme`, `LocalContentColor`, `HorizontalDivider`, `CircularProgressIndicator`, `ExperimentalMaterial3Api`, `ModalBottomSheet` y el selector de fecha). Corre en `npm test`.

### D9. Mosaicos con ícono

`CdOption` gana un `icon` opcional. `CdChoice` usa `TileLayout.of(count, available, minTile = 64.dp)` cuando todas sus opciones tienen ícono y son 5 o menos:
- `Row` si `available / count >= minTile`: una barra de mosaicos del mismo ancho (ícono de 24 dp arriba y etiqueta de hasta 2 líneas debajo, 72 dp de alto mínimo).
- `Grid(columns)` si no: los mismos mosaicos en columnas iguales, con `GridLayout.columns`.

La elegida lleva relleno `secondaryContainer`, borde `primary` y el ícono en `primary`; las demás, borde suave. El ícono sale del catálogo `CoinDenIcon`, que suma `HELP` para "Sin indicar". Con más de 5 opciones (las categorías), la fila muestra `TileLayout.top = 3` más usadas, con la elegida siempre entre ellas, y un mosaico "Más…" que abre `CdChoiceGrid` en una hoja; la cuadrícula también dibuja mosaicos cuando todas sus opciones tienen ícono. Cada categoría base tiene su ícono (`BaseCategory.icon()`), las propias usan `TAG` y "Sin categoría" usa `HELP`. Lo usan el medio de pago, la categoría del formulario y el tipo de billetera (onboarding y ajustes). La cuadrícula de texto queda para opciones sin ícono, como las monedas.

### D10. La tecla del teclado

`ImeRule.of(hasAction, search)` decide la tecla: `Done` si el campo trae `onImeAction`, `Search` en un buscador y `Next` en los demás. `CdField` la aplica con `KeyboardActions`: `Next` llama a `focusManager.moveFocus(FocusDirection.Next)` y, si no hay otro campo, cierra el teclado; `Done` y `Search` ejecutan la acción (si hay) y cierran el teclado. Una frase de varias líneas con acción también usa `Done`, porque lo dicho es una sola frase. Las pantallas pasan como acción la misma lambda del botón de su formulario: "Llenar", "Agregar billetera", "Agregar moneda" o "Guardar" la tasa.

## Risks / Trade-offs

- [Migrar 11 pantallas puede romper pruebas de presentadores] → Los presentadores no cambian, salvo la moneda derivada, el "Más detalles" abierto y las categorías más usadas, que tienen pruebas nuevas. Las pantallas no tienen pruebas de UI, así que se verifican en el emulador.
- [`TextMeasurer` en una decisión de layout] → La decisión es pura y recibe anchos. Solo la medición está en el composable; se prueba con anchos dados.
- [Quitar "Moneda" del formulario] → Hoy elegir otra moneda solo producía un error de moneda incompatible, así que no se pierde ninguna capacidad.

## Migration Plan

Un PR desde `feat/sistema-de-componentes`, con un commit por grupo de pantallas para poder revisarlo por partes. La prueba de D8 se activa en el último commit, cuando ya no quedan usos sueltos.
