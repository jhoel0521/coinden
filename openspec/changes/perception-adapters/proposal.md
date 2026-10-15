# Proposal: Lectura de Fotos, PDF y Audios en el Teléfono (Perception Adapters)

## Why
Las fotos, los PDF y las notas de voz ya llegan a CoinDen y se guardan cifradas, pero quedan como borradores Capturados sin datos: Carlos tiene que completarlos a mano en la noche. Leerlos en el propio teléfono convierte un comprobante fotografiado o un audio de WhatsApp en un gasto propuesto, con monto, billetera y número de transacción, usando todo lo que ya existe (plantillas, referencia y la pregunta "¿es el mismo gasto?"). Al mismo tiempo, el manifiesto pide que la persona sepa desde el inicio qué información puede salir del teléfono.

## What Changes
- **Pantalla de privacidad al inicio**: antes de las billeteras, la app explica qué sale del teléfono (nada de los datos del usuario; consultas públicas que configure; descargas opcionales de paquetes, plantillas y modelos; y, solo si la acepta, la lectura mejorada de Google con sus métricas técnicas). Se puede volver a ver y cambiar desde Inicio.
- **Lectura de fotos**: siempre con un lector libre en el teléfono (Tesseract) y, si la persona lo aceptó, también con el de Google (ML Kit, que nunca envía imágenes ni textos, solo métricas técnicas de uso). Se usa la lectura que el intérprete entiende mejor.
- **Lectura de PDF**: el texto que ya trae el PDF del banco; si es un PDF escaneado, se lee cada página como foto.
- **Lectura de audios**: las notas de voz compartidas se transcriben en el teléfono con Whisper (modelo descargable a elección) o, si no se descargó, con el reconocimiento local del sistema cuando el teléfono lo ofrece.
- **La captura nunca espera a la lectura**: el borrador se guarda primero; cuando la lectura termina, el texto leído se agrega al historial del borrador y pasa por el mismo intérprete. Si la lectura falla o no entiende nada, el borrador sigue Capturado con su archivo.
- **Borradores ya guardados**: los Capturados con foto, PDF o audio que existan se pueden leer también.

### Non-Goals
- iOS (`ios-shell`, con los lectores de Apple detrás de los mismos puertos).
- Modelos de lenguaje para entender frases (`local-llm-interpreter`).
- Cotejar el extracto del banco (`statement-reconciliation`).
- Mejorar la imagen (recortar o enderezar el comprobante) más allá de lo que hagan los lectores.
- Leer audios largos (más de 5 minutos) o en idiomas que la app no ofrece.

## Capabilities

### New Capabilities
- `evidence-perception`: lectura en el teléfono de fotos, PDF y audios guardados (lectores combinables, lector de Google solo con consentimiento, modelos descargables opcionales) que convierte cada evidencia en texto para el intérprete.

### Modified Capabilities
- `draft-inbox`: un borrador Capturado incorpora el texto leído de su evidencia como un evento nuevo y pasa por el intérprete, sin perder su archivo.
- `capture-ui`: la foto, la galería y lo compartido (imagen, PDF y audio) se leen después de guardarse y la tarjeta se actualiza con lo leído.
- `app-onboarding`: suma la pantalla de privacidad con la elección del lector de Google, visible también desde Inicio.

## Impact
- **`core/domain`**: evento de lectura en el historial del borrador y caso de uso que interpreta el texto leído; sin dependencias nuevas.
- **`core/data-local`**: el evento nuevo en el JSON versionado y la preferencia del lector de Google.
- **`composeApp`**: puertos de lectura (imagen, PDF, audio) con adaptadores Android; pantalla de privacidad; descarga verificada de modelos; textos en es, en y pt.
- **Dependencias nuevas (Android)**: Tesseract4Android (Apache 2.0) con datos de es, en y pt; ML Kit text recognition (modelo incluido, opcional en tiempo de ejecución); PdfBox-Android (Apache 2.0); whisper.cpp (MIT) compilado con el NDK.
- **Permiso de internet**: solo para descargar modelos y para el lector de Google si se acepta; nunca para enviar datos del usuario.
