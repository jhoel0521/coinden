# Spec Delta

## MODIFIED Requirements

### Requirement: Compartir a CoinDen
La app DEBE (MUST) aparecer como destino al compartir texto, imágenes, PDF o audios desde otras apps. Al compartir, CoinDen guarda la captura, muestra un aviso corto y devuelve a la otra app, sin abrir su pantalla; el texto pasa por el intérprete y la imagen, el PDF o el audio se guardan cifrados y se procesan en la cola. Si el onboarding no terminó, abre la app para terminarlo.

#### Scenario: Texto de un comprobante
- **WHEN** Carlos comparte desde su banco el texto de un comprobante de pago por QR
- **THEN** ve "Guardado en CoinDen", sigue en la app del banco y CoinDen tiene un borrador con los datos leídos del comprobante

#### Scenario: Imagen compartida
- **WHEN** Carlos comparte la imagen de un comprobante desde Yape
- **THEN** ve "Guardado en CoinDen", sigue en Yape y la imagen cifrada se lee en la cola

#### Scenario: Comprobante en PDF
- **WHEN** Carlos comparte desde su banca por internet el comprobante en PDF
- **THEN** ve "Guardado en CoinDen" y el PDF cifrado se lee en la cola

#### Scenario: Nota de voz de WhatsApp
- **WHEN** Carlos comparte desde WhatsApp una nota de voz
- **THEN** ve "Guardado en CoinDen", sigue en WhatsApp y el audio cifrado se transcribe en la cola si hay un modelo de voz o reconocimiento local

#### Scenario: Antes de terminar el onboarding
- **WHEN** alguien comparte un comprobante sin haber terminado el onboarding
- **THEN** CoinDen se abre para terminarlo y, al llegar a Inicio, guarda la captura

#### Scenario: Pegar un texto que no se puede compartir
- **WHEN** Carlos copia un mensaje en WhatsApp y lo pega en la frase del formulario
- **THEN** el formulario se llena con lo que el intérprete entiende de ese texto

## REMOVED Requirements

### Requirement: Leer Capturas Guardadas
**Reason**: Con la cola en segundo plano, los borradores Capturados con archivo se procesan solos; ya no hace falta un botón para leerlos.
**Migration**: Ver "Lo Pendiente Sale de los Datos" en `background-processing`, escenarios "Capturas de antes de la lectura" y "Audios que esperaban un modelo de voz".
