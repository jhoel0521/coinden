# Design

## Context

- Inicio solo muestra el número de borradores pendientes (`HomeState.pendingDrafts`). No existe pantalla de bandeja, visor de evidencia ni historial. `GetDraftInboxUseCase` ya ordena los pendientes y proyecta saldos, y `FindSameMovementQuestionsUseCase` ya calcula las preguntas.
- `DraftFormPresenter`/`DraftFormScreen` solo crean borradores nuevos de gasto o ingreso (`save()`), con frase libre, monto, moneda, billetera, medio, categoría, fecha, nota y referencia.
- El libro ya corrige por anulación: `LedgerEvent.Annulment` y `AnnulMovementUseCase`. `LedgerGuard.check` valida **un** candidato por vez contra el orden cronológico.
- Un borrador confirmado guarda su `ReadyMovement` (con nota y referencia). El id del movimiento en el libro es `draft:<draftId>`. La clasificación vive aparte, enlazada al id del movimiento.
- `ResolveSameMovementUseCase` hoy, cuando lo que queda es un confirmado, agrega la referencia sola (`DraftEvent.Merged(reference = …)`) y rechaza con `WalletMismatch` si la billetera difiere. Este cambio reemplaza ese comportamiento (ver spec delta de `capture-matching`).
- `FieldOrigin.EVIDENCE` marca lo leído de un comprobante (plantilla u OCR), y `STATED` lo dicho o escrito.

## Goals / Non-Goals

**Goals:**
- Corregir un confirmado solo con aprobación, de forma atómica en el libro y repetible sin duplicar en varios dispositivos.
- Un solo formulario para crear, revisar, completar y corregir.
- Las reglas (qué va en "Listos", diferencias, diferencia grande, comprobante idéntico) son lógica pura en `core/domain`, con pruebas.

**Non-Goals:**
- Resolver dos correcciones concurrentes del mismo movimiento desde dos dispositivos. Los ids deterministas evitan duplicados, pero elegir una corrección queda para la sincronización.
- Editar transferencias o ajustes en el formulario.

## Decisions

### D1. El cambio pendiente vive en el borrador del comprobante
"Son el mismo" entre un confirmado y un comprobante registra `DraftEvent.CorrectionProposed(targetDraftId)` en el borrador del **comprobante**, que sigue pendiente. Al proyectarlo, ese borrador expone `header.correctionTarget`.

La propuesta combinada **no se guarda**: se calcula al abrir el formulario con `ProposalMerge.combine(confirmado, comprobante)`, donde el comprobante gana en lo leído y la nota de la persona gana sobre la del comprobante. Así siempre refleja los datos actuales.

- *Alternativa:* guardar la propuesta combinada en el evento. Se descarta porque queda vieja si alguien corrige el confirmado mientras tanto.
- *Alternativa:* poner el pendiente en el borrador confirmado. Se descarta porque obliga a que un estado terminal tenga sub-estados; el comprobante ya es un pendiente natural de la bandeja.

Las tres respuestas:
- **Confirmar**: `CorrectMovementUseCase` con origen `EVIDENCE(sourceDraftId)` y la evidencia del comprobante. Después, `Discarded(mergedInto = target)` en el comprobante.
- **Descartar**: `Merged(absorbedEvidence)` en el confirmado, sin propuesta ni referencia, y `Discarded(mergedInto)` en el comprobante.
- **Son distintos**: `MarkedDistinct(target)` en el comprobante. La proyección limpia `correctionTarget`.

### D2. Qué es "un comprobante real"
Un borrador cuyo monto tiene origen `EVIDENCE` (lo leyó una plantilla o el OCR de una foto o PDF). El dictado, la frase escrita y la transcripción de audio dan `STATED` o `INFERRED`. Con ellos, `ResolveSameMovementUseCase` solo hace `Merged(absorbedEvidence)` sin referencia ni propuesta.

Se decide así porque el texto del banco compartido es `EvidenceSource.TEXT` y aun así es un comprobante: la fuente de la evidencia no alcanza para distinguirlo, el origen del campo sí.

### D3. `DraftEvent.Corrected` y la corrección atómica en el libro
`Corrected(movement: ReadyMovement, ledgerEventId, annulledLedgerEventId?, origin: CorrectionOrigin, sourceDraftId?, absorbedEvidence?, category?)`.

`CorrectionOrigin` es `MANUAL` o `EVIDENCE`. Al proyectar, `Confirmed` pasa a tener el movimiento y el `ledgerEventId` nuevos, y suma la evidencia absorbida.

`CorrectMovementUseCase(draftId, proposal, origin, …)`:
1. Exige un borrador `Confirmed`. Evalúa la propuesta con `DraftEvaluator`, que debe dar Listo; si no, se rechaza con `NotReady`.
2. Calcula `MovementDiff`. Si no hay diferencias, no registra nada (solo une la evidencia, si la hay).
3. **Si cambia el dinero** (tipo, monto, moneda, billetera o momento):
   - Construye la `Annulment` del id vigente (`annul:<idVigente>`) y el movimiento nuevo con id `draft:<draftId>:<correctedEventId>`.
   - Los valida **juntos** con `LedgerGuard.checkAll(wallets, [annulment, replacement])`, una extensión que aplica los candidatos en secuencia.
   - Los escribe con `WalletRepositoryPort.appendEvents(list)`, atómico: en memoria es una lista; en `data-local`, una transacción de SQLDelight.
4. **Si no cambia el dinero:** no toca el libro y `annulledLedgerEventId` queda null.
5. Clasifica el id vigente con la nota y la categoría del formulario si cambiaron, o si cambió el id (copia la clasificación anterior).
6. Registra `Corrected`.

El id del `Corrected` se genera antes que los ids del libro, y los ids del libro se derivan de él. Si el proceso se corta después del libro y antes del borrador, al reintentar se encuentra el evento (`findEvent`) y no se duplica. Para eso, `CorrectMovementUseCase` acepta un `correctionId` opcional que el presentador conserva mientras el formulario está abierto.

- *Alternativa:* anular y registrar con dos llamadas separadas. Se descarta porque si la segunda la rechaza el guard, el gasto queda anulado sin reemplazo.

### D4. Diferencias y diferencia grande (puro)
`MovementDiff.between(before: ReadyMovement, beforeCategory, after: ReadyMovement, afterCategory): List<FieldChange>`. Cada `FieldChange` tiene un `CorrectionField` (KIND, AMOUNT, CURRENCY, WALLET, OCCURRED_AT, NOTE, CATEGORY, REFERENCE) con su valor anterior y el nuevo como datos tipados (sin texto).

`touchesMoney` indica si hay que anular. `isLargeAmountChange(before, after)` es verdadero, en la misma moneda, si `after >= before + before` o `after + after <= before`. Solo usa sumas de `Money`, sin `Double`.

### D5. Comprobante idéntico
`SettleIdenticalReceiptUseCase(draftId)` se llama después de cada interpretación: en `QuickCapture` (texto) y en `PerceptionService`, tras `InterpretPerceivedUseCase`. Busca un confirmado del mismo dueño con la **misma referencia**. Si `MovementDiff` contra la propuesta combinada está vacío, registra `Merged(absorbedEvidence)` en el confirmado y `Discarded(mergedInto)` en el comprobante. Si no, no hace nada, y la pregunta de mismo movimiento sigue su curso.

Sin referencia nunca es idéntico: dos pagos de 10 Bs el mismo día sin número son indistinguibles.

### D6. Bandeja: "Listos" se decide en el dominio
`GetDraftInboxUseCase` agrega `batchReady: List<String>`: los Listos de confianza ALTA sin pregunta pendiente (usa `FindSameMovementQuestionsUseCase`) y sin `correctionTarget`.

`ConfirmReadyDraftsUseCase(ids)` confirma uno por uno con `ConfirmDraftUseCase` y la categoría sugerida (la recomendada por el categorizador). Devuelve los resultados por id.

El resumen (cantidad y total por billetera) lo arma el presentador sumando `Money` por billetera.

### D7. Un formulario, tres modos
`DraftFormPresenter(mode)`:
- `New`: lo de hoy.
- `Review(draftId)`: precarga la propuesta. Guardar = `ReviseDraftUseCase`; Confirmar = revisar y luego `ConfirmDraftUseCase(category)`; Descartar.
- `Correct(confirmedDraftId, sourceDraftId?)`: precarga el movimiento confirmado, o la propuesta combinada (D1). Expone `changes: List<FieldChange>` recalculado en cada edición, con `largeAmount`. Confirmar = `CorrectMovementUseCase`; Descartar y "Son distintos" solo cuando hay `sourceDraftId`.

La pantalla resalta los campos con cambios y muestra "antes: …", formateado por la presentación según el locale.

Los borradores de transferencia, cambio o ajuste no abren el formulario: su tarjeta de la bandeja trae Confirmar y Descartar.

### D8. Visor de evidencia sin archivos descifrados
`EvidenceViewerPort` en `CapturePlatform`. En Android:
- **Imagen:** `BitmapFactory.decodeByteArray`.
- **PDF:** pdfbox-android, ya incluido. `PDDocument.load(bytes)` y `PDFRenderer.renderImage(page)`, página por página.
- **Audio:** `MediaPlayer` con un `MediaDataSource` sobre el `ByteArray` (API 23 o superior; el minSdk es 26).

Nada se escribe en `cacheDir`. En la JVM, el puerto es falso y las pruebas validan el tipo de visor elegido según el MIME.

- *Alternativa:* `PdfRenderer` del sistema. Se descarta porque exige un `ParcelFileDescriptor`, es decir, un archivo descifrado en disco.

### D9. Historial de movimientos
`GetMovementHistoryUseCase(ownerId)`: los borradores `Confirmed` cuyo `ledgerEventId` vigente no está anulado, ordenados por momento de ocurrencia descendente, con su clasificación vigente y la evidencia (original más la extra).

`GetMovementTimelineUseCase(draftId)` arma la línea de tiempo de un movimiento (confirmado, correcciones con sus `FieldChange`, evidencias unidas). Para una corrección desde un comprobante, muestra lo que leyó el comprobante (la propuesta del borrador fuente) frente a lo aprobado.

### D10. Persistencia
`data-local` serializa los nuevos `DraftEvent` (`CorrectionProposed`, `Corrected`) en el sobre JSON con su tipo, y agrega `appendEvents` transaccional al repositorio del libro. No hay migración de tablas. La batería de contrato corre igual en memoria y en local.

### D11. Navegación y textos
Rutas nuevas:
- `INBOX`, desde el botón "Revisar (N)" de Inicio.
- `REVIEW/{draftId}`.
- `CORRECT/{draftId}?source={id}`.
- `HISTORY`, desde Inicio.
- `EVIDENCE/{draftId}/{index}`.

Los textos nuevos van en `composeResources` en es, en y pt.

## Risks / Trade-offs

- [El corte entre libro y borrador deja un movimiento anulado y reemplazado sin `Corrected`] → Los ids son deterministas y se reintenta con el mismo `correctionId`. Además, `GetMovementHistoryUseCase` tolera el caso: si el id vigente está anulado y existe `draft:<id>:<x>`, lo usa.
- [Dos correcciones en dos dispositivos] → Hoy no hay sincronización. Con ids deterministas, la segunda anulación es rechazada por `AlreadyAnnulled`. La política de conflicto queda para sync.
- [La propuesta combinada calculada al vuelo cambia si se reinterpreta el comprobante] → Es lo deseado: se muestra lo último leído y la persona igual lo aprueba.
- [Un PDF grande en memoria] → Se renderiza una página por vez y se libera al cerrar el visor.
- [Cambia el comportamiento de "Son el mismo" con un confirmado (BREAKING)] → Los `Merged(reference)` ya guardados se siguen proyectando igual. Solo cambia lo que se registra de aquí en adelante.

## Migration Plan

Sin migración de datos: los eventos nuevos se agregan y los viejos se proyectan igual. Si hay que volver atrás, una versión anterior ignora los tipos de evento que no conoce (el sobre ya lo admite) y se perderían de vista las correcciones, sin corromper el libro.
