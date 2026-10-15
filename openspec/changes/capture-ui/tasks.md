# Tasks: Captura en Segundos

## 1. Dominio y persistencia

- [ ] 1.1 Escribir en `PhraseReaderTest` los escenarios del motivo (única cuenta que acepta QR, recomendada entre 3, billetera por defecto, billetera dicha sin motivo) e implementar `WalletSuggestion` (D1) hasta que pasen con `:core:domain:jvmTest`.
- [ ] 1.2 Agregar el motivo opcional a `ProposalDto` con su ida y vuelta y la lectura de una propuesta sin el campo; verificar con `:core:data-local:jvmTest`.

## 2. Presentadores

- [ ] 2.1 Escribir pruebas de `CapturePresenter` con `DictationPort` y `AttachmentStore` falsos (dictado "almuerzo 45 bnb" → borrador de voz interpretado; dictado no disponible → aviso sin borrador; foto → borrador Capturado con la referencia; texto compartido de un comprobante → borrador con datos leídos; imagen, PDF y nota de voz compartidos → Capturados con su archivo; confirmar "almuerzo 45 bnb" → gasto en BNB Bs clasificado como comida; confirmar con saldo insuficiente → sigue pendiente con el faltante) e implementarlo hasta que pasen.
- [ ] 2.2 Escribir pruebas de `DraftFormPresenter` (frase "almuerzo 45 bnb" precarga gasto, 45, BNB Bs y comida; formulario completo a mano → Listo con confianza alta; sin monto → Incompleto) e implementarlo hasta que pasen.

## 3. Android

- [ ] 3.1 Implementar `AttachmentStore` cifrado (D5) con su prueba instrumentada (no es un JPEG legible y se lee de vuelta) y verificar en el emulador con `connectedDebugAndroidTest`.
- [ ] 3.2 Implementar el dictado local (D3) con permiso de micrófono y el caso "no disponible"; la cámara con CameraX y la galería con el selector de fotos (D4); y la recepción de "compartir" de texto, imágenes, PDF y audio (D7); verificar que compila.

## 4. Pantallas y textos

- [ ] 4.1 Implementar la barra de captura en Inicio, el formulario completo, la hoja de resultado con Confirmar y el motivo de la billetera en la tarjeta (D2, D6, D8); retirar la demo (D9); textos en es, en y pt; verificar que `npm test`, `npm run lint` y `npm run build:android` pasan y que no hay textos fijos.

## 5. Verificación en el dispositivo

- [ ] 5.1 En el emulador: dictar un gasto (o verificar el aviso si no hay reconocimiento local), tomar una foto y elegir otra de la galería, llenar el formulario con "almuerzo 45 bnb" y a mano, compartir un texto de comprobante, una imagen, un PDF y un audio desde otra app, confirmar uno y ver bajar el saldo en Inicio; comprobar con `adb` que la imagen guardada no es legible; guardar capturas como evidencia.

## Workflow follow-up

- Archivar `capture-ui` con `opsx-archive` tras la revisión.
- Siguiente del MVP: `perception-adapters` (OCR de las fotos ya guardadas).
