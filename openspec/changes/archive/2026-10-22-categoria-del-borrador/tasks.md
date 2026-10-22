# Tasks

## 1. Dominio (expense-categorization: Categoría Elegida en un Borrador)

- [x] 1.1 Prueba de feature de Carlos: guardar con "Mascotas" y reabrir; guardar con "comida" y confirmar sin categoría; cambiar de idea; sin categoría elegida.
- [x] 1.2 `DraftEvent.Revised` y `DraftHeader` con la categoría elegida opcional; `ReviseDraftUseCase` la recibe; `ConfirmDraftUseCase` la usa si no recibe otra.

## 2. Persistencia

- [x] 2.1 Batería de contrato: la revisión con categoría base y propia se guarda y se lee; una revisión guardada antes se lee sin categoría.
- [x] 2.2 `EventCodec`: la categoría opcional en el DTO de la revisión.

## 3. Formulario

- [x] 3.1 Prueba del presentador: Guardar con una categoría y volver a abrir el borrador la muestra marcada.
- [x] 3.2 `DraftFormPresenter`: `keep()` guarda la categoría y `loadReview` la carga antes que la sugerencia.

## 4. Cierre

- [x] 4.1 Verificar en el emulador el caso de la issue #14 ("veterinario 30 bnb", Guardar, reabrir y confirmar).
- [x] 4.2 `npm test`, `npm run lint`, `npm run build:android` y `openspec validate --all` en verde.

## Workflow follow-up

- Archivar tras la unión y cerrar la issue #14.
