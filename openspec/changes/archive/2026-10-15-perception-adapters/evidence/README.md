# Evidencia de perception-adapters (emulador Android 14, API 34, 2,5 GB de memoria)

Comprobantes y nota de voz **inventados**, generados para la prueba (ningún dato es real). Carlos tiene "Billetera" (efectivo, 100 Bs) y "BNB Bs" (banco, 500 Bs).

| Captura | Escenario | Tiempo |
|---|---|---|
| 01 | Pantalla de privacidad después de la bienvenida, con el lector de Google apagado | — |
| 02 | Modelos de voz: con 2,5 GB recomienda "Pequeño (59 MB)" y avisa que los otros pueden ser lentos | — |
| 03 | Descarga del modelo recomendado desde la app, verificado con su huella SHA-256 | 18 s |
| 04 | Foto de la galería (pago QR) leída con Tesseract → gasto de 25,00 BOB, Nro. 400000001 | 4 s |
| 05 | PDF con texto compartido desde Archivos → 118,71 BOB, Nro. 1000000001 (sin OCR) | 4 s |
| 06 | PDF escaneado (solo imagen) → página dibujada y leída → 33,00 BOB, Nro. 400000555 | 3 s |
| 07 | Nota de voz "Gasté cuarenta bolivianos en taxi" transcrita con Whisper → 40,00 BOB, Transporte | 8 s |
| 08 | Lector de Google activado en Privacidad; el log muestra que ML Kit cargó su modelo local; la misma foto avisa "Ya enviaste este archivo" | — |
| 09 | Modo avión: el PDF escaneado se lee igual | 3 s |
| 10 | La tarjeta muestra lo transcrito y "Audio cifrado en este teléfono." | 7 s |

Tamaño del APK de depuración: **80,0 MB**. Librerías nativas: 22,4 MB en `arm64-v8a` y 23,9 MB en `x86_64` (ML Kit unos 11 MB por arquitectura, Tesseract y Leptonica unos 7 MB, Whisper unos 2 MB). Datos de Tesseract: 8,4 MB. Un APK de publicación por arquitectura (o un App Bundle) bajaría a la mitad de las librerías.

Pruebas instrumentadas en el emulador: Tesseract (2), ML Kit (1), PDF con texto, escaneado e inválido (3), decodificación de audio, descarga con huella distinta y transcripción con Whisper de una frase sintetizada (3).

Hallazgo corregido durante la verificación: la tarjeta seguía diciendo "se leerá cuando llegue la lectura"; ahora indica si el archivo está leído y muestra el texto leído o transcrito.

No probado: el reconocimiento de voz del sistema como respaldo (el emulador no tiene reconocimiento local) y un teléfono real.
