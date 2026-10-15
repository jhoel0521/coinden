# Proposal: Captura en Segundos (Capture UI)

## Why
Registrar un gasto hormiga tiene que tomar segundos, o no se registra. Hoy Inicio tiene un único botón que abre una pantalla con un campo de texto: es lento para el uso real (decenas de capturas al día) y no aprovecha lo más rápido que tiene un teléfono: la voz y la cámara. Además, los comprobantes ya existen en las apps del banco y de Yape: deben llegar a CoinDen compartiéndolos, sin copiar nada a mano.

## What Changes
- **Tres botones siempre visibles en Inicio**: 🎤 Dictar, 📷 Foto y ✍️ Escribir, abajo, al alcance del pulgar. Un toque para empezar a capturar.
- **Dictar (directo)**: se toca, se habla y se guarda el borrador con lo dicho, usando el reconocimiento de voz **del propio teléfono** (sin enviar el audio a ningún servidor), en el idioma de la app. Si el teléfono no tiene reconocimiento de voz local, el botón lo explica en lugar de usar uno en la nube.
- **Foto (directo)**: abre la cámara dentro de la app, como las apps de banca, con un botón para elegir de la galería. La imagen se guarda como evidencia del borrador, **cifrada** en el teléfono; leerla (OCR) llega con `perception-adapters`.
- **Escribir (formulario completo)**: una página con todos los campos del movimiento (tipo, monto, moneda, billetera, medio de pago, categoría, fecha y nota), con un campo "escríbelo como te salga" arriba que llena los campos con el intérprete. Guardar crea un borrador ya completo.
- **Compartir a CoinDen**: desde cualquier app (banco, Yape, WhatsApp) se comparte un texto, una imagen, un PDF o un audio, y CoinDen crea el borrador. Un texto pasa por el intérprete y sus plantillas de comprobantes al instante; una imagen, un PDF o una nota de voz se guardan cifrados como evidencia hasta que exista su lectura. Un texto que una app no deja compartir se pega en la frase del formulario.
- **Después de capturar**: "Guardado como borrador" con la tarjeta del borrador, y la opción de **confirmarlo ahora** (con la categoría sugerida) o dejarlo para la bandeja de la noche.
- **Por qué esa cuenta**: cuando la billetera es sugerida, la tarjeta explica el motivo ("pagaste por QR y BNB Bs es tu única cuenta de banco", "tienes 3 cuentas que aceptan QR, revisa"). El intérprete entrega el motivo como dato.

### Non-Goals
- Leer las imágenes y los PDF (OCR), transcribir audios ya grabados (por ejemplo, una nota de voz de WhatsApp) y guardar el audio del dictado: `perception-adapters`. Hasta entonces esos borradores quedan Capturados con su archivo cifrado.
- La bandeja de la noche para corregir, descartar y confirmar en lote (`review-inbox-ui`).
- Dictado en teléfonos sin reconocimiento de voz local (Android 11 o anterior, o sin el modelo del idioma): no se usa el de la nube.
- Editar un borrador ya guardado desde la tarjeta (llega con la bandeja).

## Capabilities

### New Capabilities
- `capture-ui`: captura desde Inicio con tres botones (dictado local, cámara y galería, formulario completo), compartir a CoinDen texto, imágenes, PDF y audios, evidencias cifradas, resultado con la tarjeta del borrador y confirmación inmediata opcional.

### Modified Capabilities
- `text-interpreter`: cuando sugiere una billetera, entrega el motivo de la sugerencia como dato.
- `app-onboarding`: Inicio muestra los tres botones de captura en lugar de un botón que abre una pantalla.
- `app-shell`: la demostración de captura queda reemplazada por la captura real.

## Impact
- **`core/domain`**: motivo de la billetera sugerida en la propuesta (enum, sin texto).
- **`core/data-local`**: el motivo en el JSON de la propuesta (campo opcional, compatible); almacén de imágenes cifradas con una llave del Keystore.
- **`composeApp`**: barra de captura en Inicio, dictado (`SpeechRecognizer` local), cámara (CameraX) y galería (selector de fotos del sistema, sin permisos), formulario completo, recepción de "compartir", resultado de captura con confirmación; textos en es, en y pt.
- **Permisos de Android**: cámara y micrófono, pedidos solo al tocar su botón. Sin permiso de internet nuevo ni de almacenamiento.
- **Dependencias**: CameraX.
