## MODIFIED Requirements

### Requirement: Compartir a CoinDen
La app DEBE (MUST) aparecer como destino al compartir texto, imágenes, PDF o audios desde otras apps. Al compartir, CoinDen guarda la captura, muestra un aviso corto y devuelve a la otra app, sin abrir su pantalla; el texto pasa por el intérprete y la imagen, el PDF o el audio se guardan cifrados y se procesan en la cola. Si el onboarding no terminó, abre la app para terminarlo. Al volver a CoinDen, Inicio y la barra ya cuentan lo compartido entre los pendientes.

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

#### Scenario: Volver después de compartir
- **WHEN** Carlos tiene CoinDen abierta con 1 borrador pendiente, comparte dos comprobantes desde su banco y vuelve a CoinDen
- **THEN** Inicio y la pestaña Revisar muestran 3 pendientes sin cambiar de pantalla

## ADDED Requirements

### Requirement: Formulario que Sobrevive a Recrear la Pantalla
El formulario de captura DEBE (MUST) conservar lo que la persona escribió o eligió cuando Android recrea la pantalla (al rotar el teléfono o cambiar el tamaño de letra o el idioma del sistema). Recrear la pantalla nunca guarda ni descarta el borrador por su cuenta.

#### Scenario: Rotar con el formulario a medias
- **WHEN** Carlos escribe "almuerzo 45", elige "Transferencia o QR" y rota el teléfono
- **THEN** el formulario sigue con "almuerzo 45" y "Transferencia o QR" elegidos

#### Scenario: Cambiar el tamaño de letra
- **WHEN** Carlos tiene el formulario a medias y cambia el tamaño de letra del sistema
- **THEN** al volver a CoinDen el formulario conserva lo que tenía
