# Tasks: Lectura de Fotos, PDF y Audios en el Teléfono

## 1. Dominio

- [ ] 1.1 Escribir las pruebas de `InterpretPerceivedUseCase` (foto Capturada con texto de un pago QR → propuesta de 25.00 BOB con referencia y la foto conservada; comprobante con dos gastos → un segundo borrador con la misma foto; borrador ya completado a mano no cambia; lectura sin resultado deja Capturado; historial sincronizado no vuelve a leer; la coincidencia de "mismo comprobante" usa el texto leído) e implementar D1 hasta que pasen con `:core:domain:jvmTest`.
- [ ] 1.2 Escribir las pruebas de `ModelRecommender` (64 bits con 6 GB → mediano; 12 GB → el más preciso; 3 GB → pequeño; poco espacio → baja de tamaño o reconocimiento del sistema; sin 64 bits → reconocimiento del sistema) e implementarlo.

## 2. Persistencia

- [ ] 2.1 Agregar `draft.perceived` al JSON versionado y las preferencias `googleReaderEnabled` y `privacyNoticeSeen`, con ida y vuelta, lectura de datos guardados sin ellas y la batería de contrato de memoria y base local; verificar con `:core:data-local:jvmTest`.

## 3. Orquestación (común)

- [ ] 3.1 Escribir las pruebas de `PerceptionService` con lectores falsos (elige la lectura con plantilla sobre la que solo trae monto; empate → lector libre; sin consentimiento nunca llama al lector de Google; PDF sin texto pasa sus páginas al lector de imágenes; audio de más de 5 minutos no se transcribe; sin modelo ni reconocimiento local queda Capturado con la oferta de descarga; "Leer capturas guardadas" recorre los Capturados con archivo) e implementar D2 y D3.

## 4. Adaptadores Android

- [ ] 4.1 Tesseract (D4): dependencia fijada, datos `eng`/`spa`/`por` en assets y adaptador; prueba instrumentada con la imagen de un comprobante inventado generada en la prueba.
- [ ] 4.2 ML Kit (D5): adaptador creado solo con consentimiento; prueba instrumentada de que sin consentimiento no se instancia.
- [ ] 4.3 PDF (D6): PdfBox con texto y `PdfRenderer` para escaneados; prueba instrumentada con un PDF inventado generado en la prueba.
- [ ] 4.4 Audio (D7): decodificación a PCM 16 kHz y respaldo del reconocimiento del sistema; pedir permiso para instalar CMake con el SDK Manager antes de compilar whisper.cpp; fuente de whisper.cpp fijada en `third_party`, puente JNI y prueba instrumentada de transcripción con un audio sintético.
- [ ] 4.5 Modelos (D8): catálogo con tamaños y huellas oficiales, `DeviceProfile` de Android, descarga a temporal con verificación de huella, borrado; prueba de que una huella distinta se borra.

## 5. Pantallas y textos

- [ ] 5.1 Pantalla de privacidad en el onboarding y desde Inicio con el interruptor del lector de Google (D9), "Leyendo…" y oferta de descarga en la tarjeta, sección de modelos con el recomendado, "Leer capturas guardadas"; textos en es, en y pt; verificar que `npm test`, `npm run lint` y `npm run build:android` pasan.

## 6. Verificación en el dispositivo

- [ ] 6.1 En el emulador: ver la pantalla de privacidad; fotografiar o elegir de la galería la imagen de un comprobante inventado y ver el gasto propuesto; compartir un PDF con texto y uno escaneado; descargar el modelo recomendado y transcribir una nota de voz; activar y apagar el lector de Google; en modo avión, leer una foto; guardar capturas y anotar tiempos de lectura y tamaño del APK.

## Workflow follow-up

- Archivar `perception-adapters` con `opsx-archive` tras la revisión.
- Siguientes: el arreglo de "por" en las notas, `local-llm-interpreter`, `statement-reconciliation` e `ios-shell`.
