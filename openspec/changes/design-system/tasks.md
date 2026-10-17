# Tasks: Sistema de Componentes

## 1. Base

- [x] 1.1 Escribir las pruebas de las reglas puras (`ActionsLayout`: 1 → Full, 2 que caben → Row, 2 que no caben o 3 → Stack con la principal al final; `ChoiceLayout`: 2 opciones cortas → Segmented, 4 que no caben → ScrollRow, lista abierta → ScrollRow, 13 fijas → TopAndMore(4); `GridLayout.columns` entre 2 y 4 según el ancho) e implementarlas en `ui/components` hasta que pasen (req. Grupo de Acciones según sus Hermanos, Elección Única, Cuadrícula sin Opciones Sueltas).
- [x] 1.2 Implementar el catálogo de D1 (`CdAction`, `CdActions`, `CdIconAction`, `CdChoice`, `CdChoiceGrid`, `CdField`, `CdSection`, `CdCard`, `CdNotice`, `CdDialog`, `CdSwitchRow`, `CdCollapsible`, `CdSheet`, `CdHeaderAction`) con `LocalCdContainer` (D5). Se verifican en las pantallas reales en claro, oscuro y letra grande (tarea 4.2), sin pantalla de muestra aparte.
- [x] 1.3 Escribir `RawMaterialUsageTest` (D8) activo desde ya, con la lista de archivos pendientes de migrar, que solo puede achicarse y debe quedar vacía en la tarea 4.1.

## 2. Formulario y captura

- [x] 2.1 `DraftFormPresenter`: moneda derivada de la billetera, categorías más usadas (de `classificationsOf`) y "Más detalles" abierto si hay nota o referencia. Pruebas en la JVM para los escenarios Moneda de la billetera, Más detalles con referencia y Categorías (req. Formulario Completo).
- [x] 2.2 Migrar `DraftFormScreen`:
  - Tipo y Medio como `CdChoice` segmentado.
  - Billetera como `ScrollRow`.
  - Categoría como `TopAndMore` con `CdSheet`.
  - Monto con la moneda en `trailing`.
  - `CdCollapsible` "Más detalles".
  - Acciones por `CdActions`.
- [x] 2.3 Migrar `CaptureViews` (barra de captura, aviso y hoja del resultado) y `EvidenceScreen`.

## 3. Bandeja, inicio y resto de pantallas

- [x] 3.1 Tarjeta compacta `InboxRow` con el motivo (D6), con pruebas del motivo por borrador en el presentador, y migrar `ReviewInboxScreen` y el resumen del lote (req. Presentación de Borradores).
- [x] 3.2 Migrar `HomeScreen` según D7, y también `HistoryScreen` y `SpendingInsightsScreen`.
- [x] 3.3 Migrar `OnboardingScreens`:
  - Monedas con `ScrollRow` y el buscador sin tapar resultados (F1).
  - Moneda propia en `CdSection`, con el error junto al formulario (F2).
  - Tipos de billetera en `CdChoiceGrid`.
- [x] 3.4 Migrar `PerceptionScreens` (privacidad y modelos de voz), `WalletSettingsScreen` y `ui/LoadNotice`.

## 4. Cierre

- [x] 4.1 Activar `RawMaterialUsageTest` con 0 usos sueltos. Verificar que `npm test`, `npm run lint` y `npm run build:android` pasan.
- [ ] 4.2 En el emulador: capturas de antes y después de formulario, bandeja, inicio, onboarding (monedas y tipos) y ajustes, en claro, oscuro y letra 1,3. Verificar que ningún grupo deja una opción sola. Guardarlas en `docs/audits/evidence/2026-10-design-system/` y abrir el PR enlazando la issue #2.

## Workflow follow-up

- Archivar `design-system` tras unir el PR.
- Después: F5 (confianza sin verbo), F3, F4 y F6, cada uno en su PR.
