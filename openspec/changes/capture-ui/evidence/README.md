# Evidencia de capture-ui (emulador Android 14, API 34)

Carlos (Bolivia) con "Billetera" en efectivo (100 Bs, por defecto) y "BNB Bs" en banco (500 Bs).

| Captura | Escenario |
|---|---|
| 01–03 | Onboarding y Botones visibles: Inicio con Dictar, Foto y Escribir |
| 04 | Sin reconocimiento local: el emulador no tiene modelo de voz local; se avisa y no se usa la nube |
| 05–06 | Foto del comprobante: cámara con disparador y galería; borrador Capturado con archivo cifrado |
| 07–08 | Imagen de la galería con el selector de fotos del sistema (sin permisos de almacenamiento) |
| 09–12 | Llenar con una frase ("almuerzo 45 bnb" → gasto, 45, BNB Bs, comida) y Confirmar ahora: BNB Bs baja a 455 Bs |
| 13–14 | Formulario completo a mano: 120 Bs, BNB Bs, tarjeta, supermercado, ayer → Listo con confianza alta |
| 15 | Confirmación rechazada: 300 Bs en "Billetera" con 100 Bs → faltan 200 Bs, sigue pendiente |
| 16 | Texto de un comprobante compartido (datos inventados) → gasto de 25 Bs leído con la plantilla |
| 17–20 | Imagen, PDF y nota de voz compartidos desde la app Archivos (CoinDen aparece en la hoja de compartir) → Capturados con su archivo cifrado |
| 21 | Inicio final: 10 borradores pendientes; solo el confirmado cambió saldos |

Imagen ilegible fuera de la app (con `adb shell run-as`): el archivo guardado de la foto empieza con
`7f 2e ff 9a …` (no con la firma JPEG `ff d8`) y no contiene la marca `JFIF`. La prueba instrumentada
`EncryptedAttachmentStoreTest` lo comprueba también para PDF y audio.

Hallazgo durante la verificación: al compartir con la app abierta, el borrador se guardaba pero la hoja
de resultado no aparecía (el efecto se cancelaba al marcar lo compartido como tomado). Se corrigió
capturando en el alcance de la navegación.

Nota: compartir un archivo con `adb shell am start` no funciona porque la consola no puede conceder la
lectura del `Uri`; desde una app real (Archivos) sí, que es el caso del usuario.
