# Proposal

## Why

La validación del MVP (issue #2, hallazgo F6) mostró que no hay pantalla para crear, renombrar ni archivar categorías propias: el dominio las soporta, pero la persona no puede usarlas desde la app, y el check de "Mascotas" no se puede hacer.

## What Changes

- Pantalla "Categorías propias" en Perfil → Configuración: crear una categoría con el nombre que la persona escriba, renombrarla, archivarla y reactivarla.
- Renombrar cambia el nombre en todos los gastos que ya la tenían (guardan la categoría por su id).
- Reactivar devuelve una categoría archivada a las sugerencias y al formulario.
- Nuevos casos de uso en el dominio: renombrar y reactivar.

## Capabilities

### New Capabilities
<!-- Ninguna. -->

### Modified Capabilities
- `expense-categorization`: las categorías propias también se renombran y se reactivan.
- `app-navigation`: Perfil ofrece "Categorías propias".

## Impact

- `core/domain`: `RenameCustomCategoryUseCase` y `ReactivateCustomCategoryUseCase` (`usecase/category/CategoryUseCases.kt`), prueba de feature de Carlos.
- `composeApp`: `CustomCategoriesPresenter` y `CustomCategoriesScreen` (`profile/`), ruta protegida por el bloqueo, enlace en Perfil, textos en es, en y pt.
- Se apoya en el cambio `publicacion` (mismo requisito de Perfil); se une después de él.
