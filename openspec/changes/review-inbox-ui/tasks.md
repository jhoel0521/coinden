# Tasks: Bandeja de Revisión y Corrección con Aprobación

## 1. Dominio: diferencias y corrección

- [x] 1.1 Escribir las pruebas unitarias de `MovementDiff` y `isLargeAmountChange` (diferencias del almuerzo: monto, referencia vacía → "400000001" y hora; la nota igual no figura; 35 → 320 es grande; 35 → 32 no; el límite exacto del doble y de la mitad) e implementar D4 hasta que pasen con `:core:domain:jvmTest` (req. Diferencias Visibles con su Valor Anterior, Aviso de Diferencia Grande).
- [x] 1.2 Agregar `LedgerGuard.checkAll` y `WalletRepositoryPort.appendEvents` en memoria. Pruebas: anulación más reemplazo que en conjunto dejan un faltante se rechazan sin escribir nada, y el par válido se escribe completo (D3).
- [x] 1.3 Escribir las pruebas de feature de Carlos para `CorrectMovementUseCase` (almuerzo 35 → 32 con anulación y clasificación "comida" copiada; taxi de "Billetera" a "BNB Bs"; solo la referencia sin tocar el libro; solo la categoría como nueva clasificación; corrección rechazada por saldo sin aplicar nada; reintento con el mismo `correctionId` sin duplicar; historial con origen manual y nota anterior) e implementar `DraftEvent.Corrected` y su proyección hasta que pasen (req. Ningún Cambio sin Aprobación, Corrección Completa de una Vez, Corregir el Dinero con Anulación, Corregir sin Tocar el Dinero, Historial de la Corrección).

## 2. Dominio: comprobantes sobre confirmados y bandeja

- [x] 2.1 Escribir las pruebas de feature de Carlos para "Son el mismo" con un confirmado: un comprobante crea `CorrectionProposed` sin cambiar el taxi ni el saldo; billetera distinta queda como cambio pendiente; un dictado solo suma el audio; Confirmar aplica la corrección y descarta el comprobante enlazado; Descartar une solo la evidencia; "Son distintos" deja el comprobante pendiente y no vuelve a preguntar. Después, cambiar `ResolveSameMovementUseCase` según D1 y D2 y retirar `WalletMismatch` hasta que pasen, incluidas las pruebas existentes de `capture-matching` ajustadas al spec delta.
- [x] 2.2 Escribir las pruebas de `SettleIdenticalReceiptUseCase`: el mismo comprobante del taxi con la referencia "400000777" queda como evidencia sin nada pendiente; sin referencia en el confirmado se crea el cambio pendiente. Implementar D5 (req. Comprobante Idéntico, El Texto y el Audio No Corrigen).
- [x] 2.3 Agregar `batchReady` a `GetDraftInboxUseCase` y `ConfirmReadyDraftsUseCase` (D6). Pruebas: el comprobante ALTA va en "Listos"; la frase BAJA y el borrador con pregunta o cambio pendiente van en "Revisar"; de 3 listos con uno sin saldo se confirman 2 y el tercero queda con su motivo.
- [x] 2.4 Agregar `GetMovementHistoryUseCase` y `GetMovementTimelineUseCase` (D9). Pruebas: el historial muestra solo el almuerzo de 32.00 Bs vigente, ordenado por fecha; la línea de tiempo muestra que el comprobante leyó 320.00 Bs y que se aprobaron 32.00 Bs.

## 3. Persistencia local

- [x] 3.1 Serializar `CorrectionProposed` y `Corrected` en el sobre JSON y hacer `appendEvents` transaccional en `data-local` (D10). Agregar casos a la batería de contrato que corren memoria y local (ida y vuelta de los eventos; corrección atómica) y verificar que `:core:data-memory:jvmTest :core:data-local:jvmTest` pasan.

## 4. App: formulario y bandeja

- [x] 4.1 Escribir las pruebas de `DraftFormPresenter` en los modos `Review` y `Correct` (precarga; Guardar revisa sin confirmar; Confirmar revisa y confirma con categoría; rechazo por saldo deja el formulario abierto con el faltante; `changes` y `largeAmount` se recalculan al editar 320 → 32; salir sin confirmar no cambia nada; Descartar y "Son distintos" con comprobante fuente) e implementar D7 en el presentador y en `DraftFormScreen`, resaltando los campos con "antes: …".
- [x] 4.2 Escribir las pruebas de `ReviewInboxPresenter` (secciones, saldos proyectados con faltante, vacío, resumen de "Confirmar los N listos" con total por billetera, cancelar no confirma, tarjeta de transferencia con Confirmar y Descartar) e implementar la pantalla de bandeja y el botón "Revisar (N)" en Inicio.
- [x] 4.3 Escribir las pruebas del `HistoryPresenter` (orden, vigentes, Corregir abre el modo `Correct`) e implementar la pantalla de historial con su acceso desde Inicio. Cambiar "Son el mismo" en la tarjeta del resultado para que, con un confirmado y un comprobante, abra el formulario de corrección (req. Pregunta de Mismo Gasto en el Resultado).
- [x] 4.4 Agregar las rutas de D11 y los textos en es, en y pt. Verificar que `npm test` y `npm run lint` pasan.

## 5. Android: visor de evidencia

- [x] 5.1 Implementar `EvidenceViewerPort` (D8): imagen desde bytes, PDF con pdfbox-android página por página y audio con `MediaDataSource`, sin escribir en disco. Prueba instrumentada: el PDF y la imagen inventados se renderizan y, al cerrar, `cacheDir` y `filesDir` no tienen copias descifradas. Prueba en la JVM: se elige el visor según el MIME. Verificar `npm run build:android`.

## 6. Verificación en el dispositivo

- [x] 6.1 En el emulador, con datos inventados:
  - capturar tres gastos y confirmarlos juntos con "Confirmar los N listos";
  - abrir una foto leída mal, corregir el monto y confirmar;
  - confirmar el taxi por dictado y compartir su comprobante con referencia; en "Son el mismo", revisar los cambios resaltados y confirmar;
  - repetir con Descartar;
  - compartir otra vez el mismo comprobante y verificar que no aparece nada para revisar;
  - corregir a mano desde el historial;
  - ver la foto, el PDF y escuchar el audio.

  Guardar capturas de pantalla como evidencia en `openspec/changes/review-inbox-ui/evidence/`.

## Workflow follow-up

- Archivar `review-inbox-ui` con `opsx-archive` tras la revisión.
- Siguientes: `local-llm-interpreter` (un paso más de la cola) y `statement-reconciliation`, que generará cambios pendientes con estas mismas reglas.
