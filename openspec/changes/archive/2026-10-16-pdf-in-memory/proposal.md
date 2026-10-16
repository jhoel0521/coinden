# Proposal

## Why

Para leer un PDF escaneado, la app guarda por un momento una copia **sin cifrar** del PDF en la caché del teléfono y la borra al terminar. Si el sistema cierra la app justo en ese momento (algo común con la cola en segundo plano), la copia descifrada queda en el teléfono. Eso contradice la promesa de privacidad: la evidencia solo se descifra en memoria.

## What Changes

- Las páginas de un PDF escaneado se dibujan en memoria, sin escribir el archivo en el almacenamiento.
- Al abrir la app se borra cualquier copia temporal de un PDF que haya quedado de una versión anterior.

## Non-Goals

- Cambiar cómo se lee el texto de un PDF que ya trae texto (ya se hace en memoria).
- Cambiar la calidad o la cantidad de páginas que se leen (siguen siendo las primeras 5).

## Capabilities

### New Capabilities

### Modified Capabilities
- `evidence-perception`: se agrega que leer la evidencia nunca deja copias descifradas en el almacenamiento.

## Impact

- `composeApp` (Android): `AndroidPdfReader` dibuja con pdfbox-android, ya incluido, en lugar de `PdfRenderer` del sistema (que exige un archivo). `CoinDenApplication` limpia restos al iniciar.
- Sin dependencias nuevas ni cambios de datos.
