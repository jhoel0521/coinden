# Design: Lectura de Fotos, PDF y Audios en el Teléfono

## Context
Ver proposal.md. Hoy una foto, un PDF o un audio se guarda cifrado con `AttachmentStore` y crea un borrador Capturado (`Evidence(PHOTO|VOICE, attachmentRef, fingerprint)`) sin texto. El intérprete (`TextDraftInterpreter`) ya lee comprobantes por plantilla o por estructura (con `fromPhoto` para fotos), su número de transacción, y la pregunta "¿es el mismo gasto?" ya existe. La app no tiene permiso de internet. Solo hay destino Android; el NDK 30 está instalado y CMake no.

## Goals / Non-Goals

**Goals:**
- Un único camino: archivo → texto (en el teléfono) → mismo intérprete. El dominio no sabe qué lector se usó, salvo como dato del historial.
- Los lectores son adaptadores intercambiables detrás de puertos en `commonMain`, para que iOS enchufe Vision, PDFKit y Speech sin tocar lo común.
- Nada del usuario sale del teléfono; el lector de Google solo con consentimiento.

**Non-Goals:**
- WorkManager o lectura con la app cerrada (la cola vive mientras la app está abierta; lo pendiente se retoma al volver).
- Mejoras de imagen (recorte, perspectiva).

## Decisions

### D1. Evento de lectura en el borrador (dominio)
`DraftEvent.Perceived(text, engine: String)` y caso de uso `InterpretPerceivedUseCase(draftId, text, engine)`:
- Si el borrador no está Capturado, no hace nada.
- Si lo está, agrega `Perceived` e interpreta `Evidence(source, text)` (la foto y el PDF como `PHOTO`, para que el lector genérico los trate como comprobante).
- La primera propuesta va como `Interpreted` en ese borrador; las siguientes crean borradores nuevos con la misma evidencia más su `Perceived` e `Interpreted`.
- `DraftHeader.perceivedText` lo expone, y la coincidencia de "mismo comprobante" compara también el texto leído.
- `engine` es un código (`tesseract`, `mlkit`, `pdf-text`, `whisper`, `system-speech`), sin textos.

### D2. Puertos de lectura (`composeApp/commonMain`)
- `ImageTextReader.read(jpeg): String?`
- `PdfTextReader.read(pdf): PdfReading` (texto, o las páginas como imágenes si no hay texto)
- `AudioTranscriber.transcribe(audio, mimeType, language): String?`
- `PerceptionService` (común, probado en la JVM con lectores falsos) decide qué lectores usar, elige la mejor lectura y llama a `InterpretPerceivedUseCase`.
- Una cola de a una lectura por vez en el alcance de la app; la tarjeta muestra "Leyendo…" mientras tanto.

### D3. Elegir la mejor lectura de una foto
Con dos lecturas, puntaje por lo que el intérprete obtiene de cada una: 3 si coincide una plantilla, 2 si trae referencia, 1 si trae monto, 0 si nada. Gana el puntaje mayor; si empatan, la del lector libre (resultado reproducible sin Google). Es lógica pura en `PerceptionService`.

### D4. Tesseract (fotos y PDF escaneados)
- `cz.adaptech.tesseract4android:tesseract4android:4.9.0` (Apache 2.0, JitPack; se agrega el repositorio fijado a ese grupo).
- Datos `tessdata_fast` 4.0.0 de `eng`, `spa` y `por` dentro de la app (`assets`), copiados a `filesDir/tesseract/tessdata` en el primer uso.
- Idiomas: el de la app más inglés.

### D5. ML Kit (opcional)
- `com.google.mlkit:text-recognition` (modelo latino incluido en la app, sin descargas de Play services).
- Se instancia solo si la preferencia `googleReader` está activa; si está apagada, el cliente nunca se crea.
- Telemetría: solo métricas técnicas según la divulgación de datos de Google; la pantalla de privacidad lo explica.

### D6. PDF
- `com.tom-roush:pdfbox-android:2.0.27.0` (Apache 2.0) con `PDFTextStripper` ordenado por posición.
- Si el texto tiene menos de 20 caracteres útiles, `android.graphics.pdf.PdfRenderer` dibuja las primeras 5 páginas a 2000 px y van a `ImageTextReader`.

### D7. Audio
- Decodificar con `MediaExtractor` + `MediaCodec` a PCM 16 bits mono, remuestrear a 16 kHz y cortar en 5 minutos.
- **Whisper**: whisper.cpp en una versión fijada (MIT), con su fuente en `third_party/whisper.cpp`, compilada con NDK y CMake mediante `externalNativeBuild` (ABIs `arm64-v8a` y `x86_64` para el emulador) y un puente JNI mínimo (cargar modelo, transcribir PCM con idioma fijado).
- **Respaldo**: `SpeechRecognizer.createOnDeviceSpeechRecognizer` con `EXTRA_AUDIO_SOURCE` (Android 13+) alimentado con el PCM, solo si `isOnDeviceRecognitionAvailable`.

### D8. Modelos descargables como datos
- Catálogo fijo en la app: id, tamaño, URL pública (el repositorio de modelos ggml de whisper.cpp en Hugging Face) y SHA-256.
- La descarga se hace a un archivo temporal en `filesDir/models`, se verifica la huella y se renombra; si no coincide, se borra.
- Permiso `INTERNET` nuevo, usado solo por esta descarga y, si se acepta, por ML Kit.
- **Recomendación según el teléfono** (`ModelRecommender`, lógica pura en `commonMain`): recibe un `DeviceProfile` (memoria total, si es de 64 bits, espacio libre) que da la plataforma (`ActivityManager.MemoryInfo`, `Build.SUPPORTED_64_BIT_ABIS`, `StatFs`).
  - Sin 64 bits → reconocimiento del sistema.
  - Memoria < 4 GB → `base`; de 4 a menos de 8 GB → `small`; 8 GB o más → `large-v3-turbo`.
  - Si el espacio libre es menor que el doble del modelo, baja al siguiente más chico; si ninguno cabe, el reconocimiento del sistema.
  - Los umbrales son datos del catálogo y se ajustan con mediciones reales.
- La descarga siempre la inicia la persona con un toque: la app recomienda, no descarga sola.

### D9. Preferencias y pantalla de privacidad
- `PreferencesRepositoryPort` gana `googleReaderEnabled` y `privacyNoticeSeen` (filas en `app_settings`, sin migración de esquema).
- Ruta `privacy` después de la bienvenida y, para quien ya terminó el onboarding, una vez antes de Inicio.
- En Inicio, un enlace "Privacidad" y, si hay Capturados con archivo sin leer, "Leer capturas guardadas".

## Risks / Trade-offs
- [Tesseract lee peor fotos con mala luz] → ML Kit opcional; medir con capturas de comprobantes inventados; lo no leído queda Capturado.
- [Whisper es pesado en RAM y tiempo] → modelos de varios tamaños a elección; lectura en cola y en segundo plano; corte en 5 minutos.
- [Tamaño del APK] → ML Kit (~4 MB por ABI), Tesseract y whisper.cpp nativos; se limitan las ABIs a `arm64-v8a` y `x86_64`.
- [JitPack como repositorio] → versión fijada y repositorio restringido al grupo de Tesseract4Android.
- [Opus en Android antiguos] → probar con una nota de voz real; si falla, el audio sigue Capturado.
- [El permiso de internet preocupa] → solo descargas pedidas por la persona y el lector de Google si lo acepta; la pantalla de privacidad lo dice.

## Resolved Questions
- **Modelo de voz.** Se ofrecen los tres y la app recomienda según el teléfono (D8). La persona decide con un toque.

  | Modelo | Tamaño aprox. | Para |
  |---|---|---|
  | `base` q5_1 | ≈ 60 MB | Teléfonos modestos; menos preciso en español |
  | `small` q5_1 | ≈ 190 MB | Teléfonos medios; buen español |
  | `large-v3-turbo` q5_0 | ≈ 550 MB | Teléfonos potentes; el más preciso |

  Los tamaños exactos y las huellas se fijan al implementar, desde la fuente oficial.
