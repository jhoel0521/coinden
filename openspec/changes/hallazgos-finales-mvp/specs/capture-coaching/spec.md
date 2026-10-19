## MODIFIED Requirements

### Requirement: Consejo Según lo que Faltó
La tarjeta de un borrador pendiente DEBE (MUST) mostrar un consejo cuando a su propuesta le faltó el monto o la billetera, o cuando la billetera quedó sugerida porque nadie la dijo. El consejo dice qué dato decir la próxima vez y nunca bloquea ni interrumpe. Una frase escrita o dictada que no se entendió (sin monto no se entiende) aconseja decir el monto. Un borrador sin faltantes, uno confirmado, una foto, un PDF, un audio que todavía no se leyó o un comprobante (una foto, un PDF o un texto de varias líneas compartido) no muestra consejo: el consejo es para lo que la persona escribe o dicta.

#### Scenario: Sin billetera dicha
- **WHEN** Carlos dicta "gasté 10 en power" y la billetera queda sugerida
- **THEN** la tarjeta muestra el consejo de decir la billetera

#### Scenario: Sin monto
- **WHEN** Carlos dicta "almuerzo bnb" sin monto
- **THEN** la tarjeta muestra el consejo de decir el monto

#### Scenario: Foto sin leer
- **WHEN** Carlos toma la foto de un comprobante que todavía no se leyó
- **THEN** la tarjeta no muestra consejo

#### Scenario: Captura completa
- **WHEN** Carlos dicta "pagué 25 de almuerzo bnb"
- **THEN** la tarjeta no muestra consejo

#### Scenario: Comprobante en PDF
- **WHEN** Carlos comparte el PDF de un pago de "StreamingPremium" y la billetera queda sugerida
- **THEN** la tarjeta no muestra el consejo de decir la billetera

#### Scenario: Texto de comprobante compartido
- **WHEN** Carlos comparte desde su banco el texto de varias líneas de un comprobante sin cuenta conocida
- **THEN** la tarjeta no muestra consejo
