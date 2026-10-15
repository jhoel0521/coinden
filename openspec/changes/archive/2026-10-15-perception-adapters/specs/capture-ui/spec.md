# Spec Delta

## MODIFIED Requirements

### Requirement: Foto o Galería con Evidencia Cifrada
Foto DEBE (MUST) abrir la cámara dentro de la app, con un botón para elegir una imagen de la galería sin pedir permisos de almacenamiento. La imagen se guarda cifrada en el almacenamiento privado de la app como evidencia de un borrador y, en seguida, se lee en el teléfono; la tarjeta del resultado se actualiza con lo leído.

#### Scenario: Foto del comprobante
- **WHEN** Carlos toca Foto y toma la foto de un comprobante
- **THEN** se guarda un borrador con la imagen como evidencia, se muestra su tarjeta y, al terminar la lectura, la tarjeta muestra el gasto propuesto

#### Scenario: Imagen de la galería
- **WHEN** Carlos toca Foto y elige una captura de pantalla de la galería
- **THEN** se guarda un borrador con esa imagen como evidencia y se lee igual que una foto

#### Scenario: Imagen ilegible fuera de la app
- **WHEN** alguien copia el archivo guardado de una imagen fuera de la app
- **THEN** el archivo no se puede abrir como imagen sin la llave de la app

### Requirement: Compartir a CoinDen
La app DEBE (MUST) aparecer como destino al compartir texto, imágenes, PDF o audios desde otras apps. Un texto compartido se guarda como borrador y pasa por el intérprete y sus plantillas de comprobantes; una imagen, un PDF o un audio se guarda cifrado como evidencia de un borrador y luego se lee en el teléfono.

#### Scenario: Texto de un comprobante
- **WHEN** Carlos comparte desde su banco el texto de un comprobante de pago por QR
- **THEN** CoinDen crea un borrador con los datos leídos del comprobante y muestra su tarjeta

#### Scenario: Imagen compartida
- **WHEN** Carlos comparte la imagen de un comprobante desde Yape
- **THEN** CoinDen crea un borrador con la imagen cifrada como evidencia y la lee

#### Scenario: Comprobante en PDF
- **WHEN** Carlos comparte desde su banca por internet el comprobante en PDF
- **THEN** CoinDen crea un borrador con el PDF cifrado como evidencia y lee su texto

#### Scenario: Nota de voz de WhatsApp
- **WHEN** Carlos comparte desde WhatsApp una nota de voz
- **THEN** CoinDen crea un borrador de voz con el audio cifrado como evidencia y lo transcribe si hay un modelo de voz o reconocimiento local

#### Scenario: Pegar un texto que no se puede compartir
- **WHEN** Carlos copia un mensaje en WhatsApp y lo pega en la frase del formulario
- **THEN** el formulario se llena con lo que el intérprete entiende de ese texto

## ADDED Requirements

### Requirement: Leer Capturas Guardadas
La app DEBE (MUST) ofrecer leer los borradores Capturados que tengan foto, PDF o audio guardados antes de que existiera la lectura, uno por uno o todos, sin bloquear la pantalla.

#### Scenario: Fotos de antes de la lectura
- **WHEN** Carlos tiene dos borradores Capturados con fotos de la semana pasada y toca "Leer capturas guardadas"
- **THEN** ambos se leen en el teléfono y sus tarjetas muestran lo propuesto
