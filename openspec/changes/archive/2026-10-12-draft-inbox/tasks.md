# Tasks: Bandeja de Borradores (TDD: primero la prueba)

## 1. Modelo y evaluación

- [x] 1.1 Definir `Evidence`, `EvidenceSource`, `ProposedAmount`, `WalletRef`, `MovementProposal`, `NonMovementIntent`, `DraftField`, `DraftIssue` y `WalletDirectory` en `model/draft/`; verificar que `./gradlew :core:domain:compileKotlinJvm` compila y que `model/draft/` no tiene textos de usuario.
- [x] 1.2 Escribir `DraftEvaluatorTest` (gasto completo, monto sin moneda toma la de la billetera, "45,50" da INVALID_AMOUNT, moneda incompatible, alias en mayúsculas resuelto, pista desconocida, billetera archivada, transferencia a la misma billetera, misma moneda con montos distintos, cambio de moneda Listo con tasa 11.88, AdjustTo, NotAMovement → Unsupported, Unknown → Incomplete con KIND). Luego implementar `Draft`, `ReadyMovement` y `DraftEvaluator` hasta que pase.

## 2. Historial y proyección

- [x] 2.1 Escribir `DraftProjectionTest` (Captured → Capturado; Interpreted → Listo/Incompleto; Revised reemplaza la propuesta; orden cronológico con revisiones de dos dispositivos; Confirmed y Discarded terminales que ignoran eventos posteriores; archivar la billetera recalcula a Incompleto). Luego implementar `DraftEvent` y `DraftProjection` hasta que pase.

## 3. Puertos y adaptadores de prueba

- [x] 3.1 Definir `DraftRepositoryPort` (append de eventos, eventos por borrador, borradores por dueño), `DraftInterpreterPort` y `WalletAliasRepositoryPort`; implementar en `feature/` los fakes `InMemoryDraftRepository`, `InMemoryWalletAliasRepository` y `ScriptedInterpreter` (incluido un modo que lanza error); verificar con una prueba mínima que el repositorio de borradores es append-only.

## 4. Casos de uso

- [x] 4.1 Implementar `SetWalletAliasesUseCase` con pruebas: normalización a minúsculas y rechazo `AliasAlreadyUsed` si el alias ya apunta a otra billetera.
- [x] 4.2 Implementar `CaptureDraftUseCase` con pruebas: captura entendida, foto que nadie entiende, intérprete que lanza error (queda Capturado), "almuerzo 45 y micro 3" crea dos borradores con la misma evidencia.
- [x] 4.3 Implementar `ReviseDraftUseCase` y `DiscardDraftUseCase` con pruebas: completar un Incompleto a Listo, convertir un No soportado en gasto, rechazo `TerminalState` en Confirmado o Descartado.
- [x] 4.4 Agregar el parámetro opcional `eventId` a `RegisterMovementUseCase` y `RegisterTransferUseCase`; verificar que las pruebas de wallet siguen pasando sin cambios y agregar una que use un id dado.
- [x] 4.5 Implementar `ConfirmDraftUseCase` con pruebas: confirmar el almuerzo (ocurrencia 12:58, id `draft:<id>`), rechazo por saldo (el borrador sigue Listo con el motivo), confirmar un Incompleto (`NotReady`), ajuste a 140 desde 147 (-7.00), nada que ajustar, cambio de moneda, y confirmación idempotente cuando el libro ya tiene el evento derivado.
- [x] 4.6 Implementar `GetDraftInboxUseCase` con pruebas: pendientes sin terminales, proyección -38.00 Bs con aviso en "Billetera", sin aviso en crédito.

## 5. Pruebas de feature por persona

- [x] 5.1 `CarlosDraftDayFeatureTest`: alias "qr" y "efectivo"; capturas del día (almuerzo, micro, foto sin datos, "créame un pago recurrente", venta de 50 USD a 11.88); bandeja con aviso; confirmación rechazada del almuerzo hasta capturar y confirmar el retiro del cajero; completar la foto; convertir el recurrente en gasto; verificar saldos finales y que el libro solo contiene los confirmados.
- [x] 5.2 `JohnDraftFeatureTest`: gasto de 0.00100000 BTC por voz confirmado en satoshis exactos; y el mismo borrador confirmado dos veces (dos dispositivos simulados) deja un único movimiento.
- [x] 5.3 `FacundoDraftFeatureTest`: gasto en tarjeta de crédito sin aviso de faltante y ajuste "en Galicia tengo 820000".

## 6. Verificación integral

- [x] 6.1 Ejecutar `npm test` y `npm run lint` con todo en verde, confirmar que las 107 pruebas previas siguen pasando y actualizar el roadmap del README (draft-inbox hecho; agregar las limpiezas `domain-i18n-cleanup` y `money-precision`).

## Workflow follow-up

- Archivar `draft-inbox` con `opsx-archive` tras la revisión.
- Siguientes cambios: un intérprete de texto con reglas (normaliza "45,50", "Bs 45", alias) y luego `expense-categorization`.
