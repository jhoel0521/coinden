# Tasks

## 1. Red de seguridad: actualizar sin perder datos (local-persistence: Actualizar sin Perder Datos)

- [x] 1.1 Guardar el esquema de la 0.1.0 (`schema-2.db`) y activar `verifyMigrations` en SQLDelight.
- [x] 1.2 Prueba en `core/data-local` (jvmTest):
  - crea una base con el esquema 2;
  - guarda los datos de Carlos de la 0.1.0 (billeteras, libro, borradores con foto, categorías propias, configuración);
  - los lee con el código nuevo: todo igual.
- [x] 1.3 Prueba: un borrador de la 0.1.0 se proyecta como paquete virtual cerrado, sin escribir eventos.

## 2. Paquete de captura (capture-bundle)

- [x] 2.1 Pruebas de feature de Carlos:
  - un comprobante es un paquete;
  - varias evidencias en orden;
  - la app se cierra con el paquete abierto y no pierde nada;
  - otro dispositivo;
  - cerrar;
  - quitar sin borrar;
  - huella repetida;
  - reabrir;
  - todo confirmado no se reabre.
- [x] 2.2 `BundleEvent`, `BundleProjection` y los casos de uso abrir, agregar, quitar, cerrar, reabrir y separar sobre un `BundleContext`.
- [x] 2.3 `DraftEvent.Captured` con `bundleId` y `paymentKey`; `Interpreted` con `reviewReasons`. Todos opcionales.
- [x] 2.4 `BundleReconciler` con sus pruebas:
  - actualiza los pendientes sin pisar lo `CORRECTED`;
  - no toca los confirmados;
  - crea los nuevos;
  - descarta los huérfanos enlazados.

## 3. Intérprete del paquete (text-interpreter, intent-interpreter)

> Depende de `interprete-por-modelo`: interpretar lo que la persona quiso decir es tarea del
> modelo local, y las reglas verifican lo que propone. Este cambio ya no agrega palabras de
> contexto a ningún paquete de idioma.

- [ ] 3.1 Pruebas del paquete con cada escenario de la spec:
  - comprobante y audio;
  - sin comprobante;
  - ambulante;
  - comida y somó;
  - comprobante equivocado;
  - otro pago entre varios;
  - mismo pago dicho dos veces;
  - dos pagos y una frase;
  - la frase explica el único;
  - billetera dicha;
  - foto sin leer;
  - modelo y reglas no coinciden.
- [ ] 3.2 `BundleInterpreter` pide la interpretación al `IntentInterpreterPort` y valida cada propuesta con `ProposalValidator`; sin modelo, el respaldo determinista arma un pago por comprobante.
- [ ] 3.3 Lo que no pasa la validación, y aquello en lo que el modelo y las reglas no coinciden, queda con su `ReviewReason` y va a revisar.

## 4. Persistencia

- [x] 4.1 Batería de contrato (memoria y base local): todos los `BundleEvent` y los campos nuevos de `DraftEvent` se guardan y se leen iguales; los eventos de la 0.1.0 se leen sin los campos nuevos.
- [x] 4.2 `EventCodec` y `DraftTypes`/`BundleTypes` con los DTO versionados.

## 5. Cola de lectura (background-processing)

- [ ] 5.1 Pruebas:
  - un paquete abierto no se procesa;
  - un paquete cerrado lee foto y audio y recién interpreta una vez;
  - lo ya leído o quitado no se relee;
  - el corte a mitad se retoma.
- [ ] 5.2 `PerceptionService` por paquete; los borradores `Captured` de la 0.1.0 siguen por el camino actual.

## 6. Captura y página de contexto (capture-ui)

- [ ] 6.1 Puerto `ContextRecorder` y adaptador Android con `MediaRecorder` (AAC/m4a), que cifra con `attachments`.
- [x] 6.2 `ContextPagePresenter` y sus pruebas:
  - agregar en loop;
  - quitar;
  - huella repetida;
  - aviso "¿Es del último borrador?";
  - cerrar con Listo o al salir;
  - sin permiso de micrófono;
  - con la app bloqueada no muestra montos.
- [ ] 6.3 `ContextPage` (Compose) en `ShareActivity`: `ACTION_SEND_MULTIPLE`, y cerrar en `onStop`.
- [ ] 6.4 Captura en la app (dictar, foto, escribir) pasa por `ContextPage`. La tarjeta del resultado muestra los N borradores.
- [ ] 6.5 "Agregar contexto" en la tarjeta, en Revisar y en el formulario; "Agregar al último" en la hoja de Capturar.
- [ ] 6.6 Textos en es, en y pt.

## 7. Revisión (review-inbox)

- [ ] 7.1 Pruebas del visor: evidencias en orden con su texto, las quitadas marcadas, y un audio sin leer dice que todavía no se leyó.
- [ ] 7.2 `EvidenceViewer` por paquete con Quitar y Separar.
- [ ] 7.3 Motivo de revisión en la bandeja y el formulario; "Usar X" para la contradicción (`Revised` con el campo `CORRECTED`).

## 8. Cierre

- [ ] 8.1 Emulador:
  - instalar la 0.1.0 con datos;
  - **actualizar a la nueva versión sin desinstalar** y comprobar que todo sigue;
  - después, compartir un comprobante más un audio;
  - compartir dos fotos de WhatsApp;
  - el caso del ambulante;
  - reabrir desde Revisar;
  - Quitar y Separar.
- [ ] 8.2 `npm test`, `npm run lint`, `npm run build:android`, `bundleRelease` y `openspec validate --all` en verde.

## Workflow follow-up

- Un solo PR. Al unir: etiqueta `v0.2.0` y subir el AAB a la prueba cerrada como actualización.
- Archivar tras la unión.
- Próximo cambio: `repaso-del-dia`.
