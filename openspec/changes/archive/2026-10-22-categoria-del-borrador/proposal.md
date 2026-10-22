# Proposal

## Why

En la verificación de la issue #2 apareció un fallo (issue #14): en Revisar, si Carlos elige una categoría y toca **Guardar** (sin confirmar), la categoría se pierde. Al reabrir el borrador vuelve "Sin categoría" o la sugerencia, y al confirmarlo el gasto queda sin la categoría que eligió. La categoría solo se registra al confirmar, y la revisión del borrador no la guarda.

## What Changes

- Guardar un borrador conserva la categoría elegida, base o propia, como parte de su revisión (append-only, se sincroniza con el resto del historial del borrador).
- Al reabrir el borrador, el formulario muestra la categoría elegida antes que cualquier sugerencia.
- Confirmar sin elegir otra categoría clasifica el gasto con la elegida al guardar.
- Elegir otra categoría y guardar de nuevo la reemplaza; la vigente es la más reciente.

## Capabilities

### New Capabilities
<!-- Ninguna. -->

### Modified Capabilities
- `expense-categorization`: la categoría elegida en un borrador pendiente se conserva hasta confirmarlo.

## Non-Goals

- Aprender una regla al guardar: se sigue aprendiendo al confirmar, como hoy.
- Recordar que la persona eligió explícitamente "Sin categoría": sin categoría elegida, se muestra la sugerencia, como hoy.
- Cambiar cómo se reclasifica un gasto ya confirmado.

## Impact

- `core/domain`: `DraftEvent.Revised` y el encabezado del borrador llevan la categoría elegida; `ConfirmDraftUseCase` la usa por defecto.
- `core/data-local`: el evento de revisión guarda la categoría opcional; los eventos guardados antes se leen sin ella.
- `composeApp`: `DraftFormPresenter` guarda la categoría al tocar Guardar y la carga al reabrir.
