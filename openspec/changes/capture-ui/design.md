# Design: Captura en Segundos

## Context
Ver proposal.md y los specs `capture-ui`, `text-interpreter`, `app-onboarding` y `app-shell`. Hoy Inicio tiene un botón "Capturar gasto" que navega a `CaptureDemoScreen` (campo de texto, lista de borradores). El dominio ya tiene todo lo que la captura necesita: `CaptureDraftUseCase` (evidencia de texto, voz, foto o manual), `ReviseDraftUseCase` (propuesta completa), `ConfirmDraftUseCase` (con categoría) y la tarjeta `DraftCardView`. `Evidence.attachmentRef` ya existe para referenciar una imagen guardada fuera del dominio. Solo hay destino Android; minSdk 26, targetSdk 36.

## Goals / Non-Goals

**Goals:**
- Un toque para empezar a dictar o fotografiar; nada se pierde aunque el intérprete no entienda.
- Ninguna evidencia sale del teléfono ni queda legible fuera de la app.

**Non-Goals:**
- OCR, guardar audio y la bandeja de la noche (ver proposal).

## Decisions

### D1. Motivo de la billetera sugerida (dominio)
Enum `WalletSuggestionReason` en `model/draft`: `DEFAULT_WALLET`, `ONLY_CANDIDATE` (la única que acepta el medio o la moneda dichos), `RECOMMENDED_AMONG_SEVERAL`. Las propuestas de gasto e ingreso ganan `walletSuggestion: WalletSuggestion? = null` (`reason` + `candidates: Int`), llenado por `PhraseReader.suggestWallet` (que también cubre la billetera por defecto); una transferencia nombra sus billeteras y no lleva motivo. Corregir un campo del borrador borra el motivo. Persistido como campo opcional en `ProposalDto` (versión 1 compatible). La presentación arma el texto ("pagaste por QR y BNB Bs es tu única cuenta de banco") con el medio y el motivo.

### D2. Barra de captura
`CaptureBar` en `commonMain`: tres botones de 64 dp de alto con ícono y texto, fijos al pie de Inicio. Dictar y Foto llaman a lanzadores de plataforma (`expect`/`actual` o lambdas provistas por `MainActivity`); Escribir navega a la ruta `form`.

### D3. Dictado local
- Android: `SpeechRecognizer.createOnDeviceSpeechRecognizer(context)` (API 31+), solo si `SpeechRecognizer.isOnDeviceRecognitionAvailable(context)`; idioma = idioma de la app (`RecognizerIntent.EXTRA_LANGUAGE`). Nunca `createSpeechRecognizer` (puede usar la nube). En API 26-30 o sin modelo local: el botón muestra "Dictado no disponible en este teléfono".
- Permiso `RECORD_AUDIO` pedido al tocar; si se niega, se explica para qué sirve.
- Un toque empieza a escuchar; el reconocedor termina solo al detectar silencio; un segundo toque cancela. El texto final se captura como `Evidence(VOICE, texto)` con `CaptureDraftUseCase`. El audio no se guarda.
- Detrás de una interfaz `DictationPort` en `composeApp` para probar el flujo con un fake en la JVM.

### D4. Cámara y galería
- Pantalla `CameraScreen` en `androidMain` con CameraX 1.6 (`camera-camera2`, `camera-lifecycle`, `camera-view`): vista previa, disparador y botón de galería. Permiso `CAMERA` pedido al abrirla.
- Galería: `PickVisualMedia` (selector de fotos del sistema), sin permisos de almacenamiento.
- La imagen (JPEG, reducida a 2000 px por lado) se guarda con `AttachmentStore` y se captura `Evidence(PHOTO, attachmentRef = id)`. Si CameraX 1.6 exige un compileSdk mayor que 36, se usa la versión estable más alta compatible y se anota aquí.

### D5. Almacén de imágenes cifradas
`AttachmentStore` (interfaz en `commonMain`): `save(bytes, mimeType): String` y `read(id): Attachment?` (bytes y tipo: imagen, PDF o audio). Implementación Android: archivos en `files/attachments/<uuid>.bin`, cifrados con AES-256-GCM (IV aleatorio por archivo, guardado al inicio) con una llave no exportable del Android Keystore distinta de la de la base (alias `coinden.attachments`). Implementación en memoria para las pruebas de la JVM. Prueba instrumentada: el archivo no empieza con la firma JPEG (`FF D8`) y se lee de vuelta igual.

### D6. Formulario completo
`DraftFormPresenter` (sin Compose, probado en la JVM): campos tipo, monto, moneda, billetera, medio, categoría, fecha y nota; `fillFromPhrase(texto)` usa `TextDraftInterpreter` y la sugerencia de categoría para precargar los campos; `save()` captura `Evidence(MANUAL, frase o null)` y luego `ReviseDraftUseCase` con la propuesta armada con origen `STATED` en todos los campos indicados (confianza alta). La evidencia se guarda con una captura que no interpreta (la frase no se vuelve a partir en varios borradores); `ReviseDraftUseCase` marca lo indicado como corregido, que también da confianza alta. El monto acepta coma o punto decimal, sin separador de miles. Lo que falte deja el borrador Incompleto. La categoría elegida viaja en la tarjeta del resultado (`CategoryView` sin motivo) y se usa al confirmar en el acto.

### D7. Compartir a CoinDen
`MainActivity` declara `intent-filter` para `ACTION_SEND` con `text/plain`, `image/*`, `application/pdf` y `audio/*`. Al recibirlo: texto → `Evidence(TEXT, texto)` por el intérprete (plantillas incluidas); imagen o PDF → leído del `Uri` recibido, guardado con `AttachmentStore` → `Evidence(PHOTO, ref)`; audio (por ejemplo, la nota de voz `.opus` de WhatsApp) → guardado igual → `Evidence(VOICE, ref)`. Los archivos se copian enteros al almacén cifrado: el `Uri` de la otra app deja de ser válido después. Si un texto no se puede compartir desde su app, se pega en la frase del formulario (D6). Luego se muestra el resultado de captura. Si el onboarding no terminó, se guarda la captura pendiente y se muestra después.

### D8. Resultado y confirmación
`CaptureResult` (hoja inferior sobre Inicio): "Guardado como borrador", la `DraftCardView` (ahora con el motivo de la billetera) y dos botones: Confirmar (si el borrador está Listo; usa `ConfirmDraftUseCase` con la categoría sugerida o la elegida) y Listo. Un rechazo del libro (por ejemplo, saldo insuficiente) se muestra en la tarjeta con el faltante. Inicio se recarga al cerrar la hoja.

### D9. Se retira la demo
`CaptureDemoScreen` y su ruta se eliminan; `CaptureDemoPresenter` se reemplaza por `CapturePresenter` (capturar texto/voz/foto/manual, confirmar, último resultado). Sus pruebas se trasladan a los escenarios nuevos.

## Risks / Trade-offs
- [Reconocimiento local no disponible en muchos teléfonos antiguos] → se informa y se ofrece Escribir o Foto; nunca se cae a la nube.
- [El reconocedor local puede necesitar descargar el modelo del idioma] → lo gestiona Android; la app solo lo informa.
- [Las imágenes ocupan espacio] → se reducen a 2000 px y JPEG; PDF y audios se guardan tal cual (los de WhatsApp pesan pocos KB por minuto); borrar evidencias queda para cuando exista descartar en la bandeja.
- [Transcribir audios ya grabados no es lo mismo que dictar] → requiere decodificar el formato (opus, m4a) y un reconocedor local que acepte archivos; se hace en `perception-adapters`, junto con el OCR.
- [CameraX y compileSdk 36] → ver D4.

## Migration Plan
Sin migración de datos: el campo nuevo de la propuesta es opcional.
