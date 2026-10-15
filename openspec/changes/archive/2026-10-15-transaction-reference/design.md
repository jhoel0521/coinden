# Design: Número de Transacción y Gastos Repetidos

## Context
Ver proposal.md. Las plantillas (`ReceiptTemplate`) y el lector genérico (`GenericReceiptReader`) recorren las líneas del comprobante con etiquetas (`ReceiptLines.valueOf`, misma línea o la siguiente). Los paquetes de idioma ya traen etiquetas por campo. El libro (`LedgerEvent`) solo guarda lo que mueve saldos; la nota vive en el borrador confirmado (`DraftEvent.Confirmed` con su `ReadyMovement`), enlazado al libro por el id derivado del borrador. Cada borrador es una secuencia append-only de `DraftEvent` proyectada por `DraftProjection`. Los comprobantes de prueba (`AnonymizedReceipts`) ya traen números inventados para todos los escenarios.

## Goals / Non-Goals

**Goals:**
- La referencia es un dato opcional y exacto.
- La coincidencia es una consulta determinista sobre los eventos: funciona igual con borradores llegados de otro dispositivo.
- Toda decisión de la persona es un evento nuevo; nada se borra ni se pisa.

**Non-Goals:**
- Índices persistentes (se evalúan si el volumen lo pide).
- Corregir la billetera de un movimiento ya confirmado al unir.

## Decisions

### D1. Referencia en la propuesta y en el movimiento listo, no en el libro
`MovementProposal.Expense`, `Income` y `Transfer` y `ReadyMovement.Expense`, `Income` y `Transfer` ganan `reference: String? = null`. `DraftProjection` la copia al movimiento listo; `markedCorrected()` la conserva. No es un `DraftField` con origen: no cambia la confianza ni hace falta para estar Listo.
- *Alternativa descartada*: en `LedgerEvent`. Cambiaría `wallet-management`, cuyo libro solo describe saldos.

### D2. Lectura de la referencia con etiquetas como datos
- `ReceiptTemplate.referenceLabel: String?` (BE y Yape: "nro. de transaccion").
- `LanguagePack.referenceLabels`:
  - es: "nro. de transaccion", "nro de transaccion", "numero de transaccion", "nro. de operacion", "numero de operacion", "operacion n", "codigo de transaccion", "referencia"
  - pt: "id da transacao", "codigo da transacao", "autenticacao"
  - en: "transaction id", "confirmation", "confirmation number", "reference", "reference number", "txid", "tx hash", "transaction hash"
- Valor: primer token tras la etiqueta (sin ":", "°", "º", "#", "N°" iniciales), de letras, dígitos y guiones, con al menos un dígito.
- Con plantilla se usa solo su etiqueta; sin plantilla, la primera etiqueta del paquete presente. `PhraseReader` nunca la llena.

### D3. Huella de la evidencia
`Evidence.fingerprint: String? = null`: SHA-256 en hexadecimal del archivo, calculado en la app antes de cifrarlo (`CapturePresenter`), nunca del archivo cifrado. Para evidencias de texto de varias líneas (un comprobante pegado o compartido), la coincidencia compara el texto normalizado (`TextNormalizer`) y no hace falta huella. Una frase de una línea repetida no cuenta como texto repetido: dos "gasté 10 bs por qr" son la pregunta de mismo día y monto.

### D4. Coincidencias por consulta
`FindSameMovementQuestionsUseCase(ownerId, utcOffsetMinutes): List<SameMovementQuestion>` con `SameMovementQuestion(draftId, otherDraftId, reason)` y `reason` ∈ {`SAME_FILE`, `SAME_TEXT`, `SAME_REFERENCE`, `SAME_DAY_AMOUNT`}.
- Candidatos: borradores no descartados del usuario. Clave de monto: el del movimiento listo o confirmado, o el de la propuesta con su moneda o la de su billetera. Día local: el momento propuesto o, si falta, el de captura, con el desfase del usuario.
- Pares: mismo archivo o mismo texto (sin importar monto ni día); misma referencia normalizada (`ReferenceKey`: mayúsculas, solo letras y dígitos) y mismo monto; o mismo tipo, mismo monto y moneda y mismo día.
- La pregunta va en el borrador capturado después y apunta al otro más cercano en el tiempo. Se excluyen los pares con una decisión "son distintos". Al responder una, puede aparecer la siguiente.

### D5. Decisiones como eventos
- `DraftEvent.MarkedDistinct(otherDraftId)`, en el borrador que preguntó: no cambia su estado; la consulta lo lee para no volver a preguntar.
- `DraftEvent.Merged(absorbedDraftId, absorbedEvidence, proposal?, reference?)`, en el que queda:
  - Si está pendiente, lleva la propuesta combinada (D6) y la proyección la aplica como una revisión, conservando los orígenes de cada campo.
  - Si está confirmado, solo lleva la referencia: la proyección la agrega al movimiento confirmado. El libro no cambia.
  - `DraftHeader.extraEvidence` acumula la evidencia absorbida.
- El absorbido recibe `Discarded(mergedInto = keptDraftId)`: `Discarded` gana el campo opcional `mergedInto`.
- Caso de uso `ResolveSameMovementUseCase(draftId, otherDraftId, same: Boolean)`: elige el que queda (el confirmado o el capturado primero). Si el que queda está confirmado y la billetera del comprobante es otra, rechaza con `DraftError.WalletMismatch`.

### D6. Combinar dos propuestas
Para monto, moneda, billetera y momento gana el campo con origen `EVIDENCE`; si ninguno lo es, el del que queda. La nota: la del borrador cuya evidencia es de la persona (texto, voz o manual) si tiene nota; si no, la del otro. Medio de pago y referencia: el que exista, prefiriendo el que queda. El tipo debe coincidir; ya lo exige la pregunta.

### D7. Persistencia compatible
JSON versión 1 con campos opcionales: `ProposalDto.reference`, `ReadyMovementDto.reference`, `EvidenceDto.fingerprint` y el `mergedInto` del descarte; DTOs nuevos para `draft.marked_distinct` y `draft.merged`. Los dispositivos con una versión anterior conservan esos eventos sin entenderlos, como ya hace la persistencia con los tipos desconocidos.

### D8. Interfaz
- Formulario: campo "Referencia" opcional; `fillFromPhrase` lo llena.
- `DraftCard`: `reference`, `extraEvidence` y `question: SameMovementView?` (motivo y datos del otro: nota, monto, hora y billetera).
- Tarjeta: referencia con ícono "#" y la pregunta con "Son el mismo" y "Son distintos"; si es repetido, el texto dice "ya enviaste este comprobante/archivo".
- `CapturePresenter.resolve(draftId, same)` y recarga del resultado; el aviso de billetera distinta se muestra en la tarjeta.
- Textos en es, en y pt.

## Risks / Trade-offs
- [Dos gastos reales iguales el mismo día] → es una pregunta, no una unión; "son distintos" se recuerda.
- [Un pago a las 23:55 y su comprobante a las 00:05] → no se pregunta por el día; el cotejo con el extracto lo cubrirá.
- [Etiquetas genéricas como "referencia" toman otro número] → al menos un dígito y el primer token; las plantillas usan su etiqueta exacta.
- [Recorrido lineal de borradores] → suficiente para uso personal; un índice local si crece.
