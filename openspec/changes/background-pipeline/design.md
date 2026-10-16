# Design: Procesamiento en Segundo Plano

## Context
Ver proposal.md. Hoy `CapturePresenter` lee la evidencia dentro de su propia corrutina (`read(draftId)` llama a `PerceptionService.perceive`), y "Leer capturas guardadas" recorre `PerceptionService.pendingDraftIds()`. Los lectores (`PerceptionReaders`) los crea `AndroidCapturePlatform` en `MainActivity`: Whisper queda cargado en un mapa y ML Kit en un `lazy`, sin liberarse nunca. Compartir pasa por `MainActivity` (`SharedIntents` → `App` → `CapturePresenter.shared`). El contenedor (`AppContainer`) vive en `CoinDenApplication`, una vez por proceso. Lo pendiente ya se deduce de los eventos: Capturado con `attachmentRef` y sin `perceivedText`.

## Goals / Non-Goals

**Goals:**
- Una sola cola para todo lo pesado; la IA se suma después como un paso más sin tocar la cola.
- La lógica de la cola (orden, reintentos, liberar memoria) es común y se prueba en la JVM; Android solo la dispara.

**Non-Goals:**
- Trabajo en primer plano con notificación (foreground service): lo pendiente se procesa en trabajos cortos y se retoma si el sistema los corta.
- Prioridades entre tipos de captura: orden de captura.

## Decisions

### D1. `QueueRunner` común
`QueueRunner(service: PerceptionService, readers: PerceptionReaders, failures: FailureLog)` en `commonMain` con `suspend fun drain(): DrainResult`:
- Toma `service.pendingDraftIds()` en orden de captura, sin los que `FailureLog` marca como agotados.
- Los procesa de a uno. Un error cuenta un intento en `FailureLog`; al tercero, el borrador se marca agotado y queda Capturado.
- Al terminar llama `readers.release()`.
- Devuelve cuántos procesó, cuántos fallaron y si quedó algo por reintentar.
- Cada borrador procesado se emite en `processed: SharedFlow<String>` para que la UI abierta se actualice.

### D2. WorkManager como disparador
- `androidx.work:work-runtime-ktx` 2.12.0 (publicada el 23-sep-2026; si exige más que compileSdk 36, se usa 2.11.2).
- `PerceptionWorker : CoroutineWorker` llama a `QueueRunner.drain()`. Si quedó algo por reintentar, devuelve `Result.retry()` con espera exponencial; si no, `success()`.
- `ProcessingQueue` (interfaz en `commonMain`, `kick()` y `processed`), implementada con `enqueueUniqueWork("coinden-queue", ExistingWorkPolicy.APPEND_OR_REPLACE, …)`: una captura nueva mientras corre la cola encadena otra pasada, nunca dos a la vez.
- WorkManager guarda los trabajos encolados y los retoma después de un reinicio.
- Se llama a `kick()` al capturar un archivo, al abrir la app, al descargar un modelo de voz y al terminar el onboarding.

### D3. Lectores a nivel de aplicación y liberación
- Los lectores pasan de `AndroidCapturePlatform` (por actividad) a `CoinDenApplication` (por proceso), para que el trabajador y la UI compartan los mismos.
- `PerceptionReaders` gana `release: suspend () -> Unit`: cierra los `WhisperTranscriber` cargados (`close()` libera el contexto nativo) y el cliente de ML Kit, que se vuelve a crear al próximo uso. Tesseract ya libera su motor en cada lectura.
- Con la app abierta: después de cada lectura se programa `release()` a los 60 s; una lectura nueva lo cancela.

### D4. Fallas por dispositivo
`FailureLog` guarda intentos por borrador en un archivo privado (`files/queue-failures.json`). Es un dato operativo de este dispositivo, no un evento del dominio: otro dispositivo puede intentar leer el mismo borrador. Un borrador que se lee bien o deja de estar Capturado se quita del registro.

### D5. Compartir sin abrir la app
- `ShareActivity` sin interfaz (tema translúcido, `excludeFromRecents`, `noHistory`) recibe los `intent-filter` de `ACTION_SEND` que hoy tiene `MainActivity`.
- Si `container.isOnboarded()`: lee el contenido (`SharedIntents`), lo guarda con `QuickCapture` (común: el texto pasa por el intérprete y los archivos van a `AttachmentStore` con su huella), llama a `kick()`, muestra un `Toast` "Guardado en CoinDen" y termina.
- Si no: abre `MainActivity` con el mismo intent, que conserva el flujo actual de captura pendiente hasta llegar a Inicio.
- `CapturePresenter.shared` y `photo` usan el mismo `QuickCapture` y, en vez de leer en línea, llaman a `kick()` y escuchan `processed` para refrescar su tarjeta ("Leyendo…" mientras el borrador siga pendiente).

### D6. Se retira "Leer capturas guardadas"
Se quitan el botón de Inicio y `CapturePresenter.readSaved`/`pendingReads`. Los audios que esperan modelo siguen ofreciendo "Ver modelos de voz"; al descargarlo, `kick()` los procesa.

## Risks / Trade-offs
- [Android puede demorar o cortar trabajos en segundo plano (ahorro de batería)] → el trabajo es idempotente y se retoma; la captura nunca depende de él.
- [Un audio largo con un modelo grande puede pasar el límite de un trabajo] → se corta en 5 minutos de audio; si el sistema lo interrumpe, se reintenta y, tras 3 intentos, queda para revisar.
- [Liberar y volver a cargar Whisper cuesta unos segundos] → solo al vaciarse la cola o tras 60 s sin uso.
- [`APPEND_OR_REPLACE` con un trabajo fallado lo reemplaza] → el estado real está en los datos, así que reemplazarlo no pierde nada.
