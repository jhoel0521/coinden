# Spec Delta

## Purpose

Ayuda a que lo que se captura en el día llegue más completo: la app muestra, solo cuando hace falta, qué dato faltó y un ejemplo con las palabras de la persona, y cuenta cuántas capturas quedaron listas sin revisar.

## ADDED Requirements

### Requirement: Consejo Según lo que Faltó
La tarjeta de un borrador pendiente DEBE (MUST) mostrar un consejo cuando a su propuesta le faltó el monto o la billetera, o cuando la billetera quedó sugerida porque nadie la dijo. El consejo dice qué dato decir la próxima vez y nunca bloquea ni interrumpe. Una frase escrita o dictada que no se entendió (sin monto no se entiende) aconseja decir el monto. Un borrador sin faltantes, uno confirmado o una foto, un PDF o un audio que todavía no se leyó no muestra consejo.

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

### Requirement: Ejemplo con las Palabras de la Persona
El consejo de la billetera DEBE (MUST) traer una frase de ejemplo formada por la nota y el monto de esa captura y una palabra que nombra a una billetera activa de la persona: su alias más corto o, si no tiene, una palabra de su nombre que el intérprete reconoce. Se usa la billetera sugerida y, si no hay, la por defecto. Si no hay ninguna palabra que la nombre, el consejo va sin ejemplo.

#### Scenario: Ejemplo con el alias
- **WHEN** Carlos tiene el alias "bnb" para "BNB Bs", dicta "gasté 10 en power" y la billetera sugerida es "BNB Bs"
- **THEN** el ejemplo es «power 10 bnb»

#### Scenario: Ejemplo con una palabra del nombre
- **WHEN** la billetera sugerida es "Billetera" sin alias y Carlos dicta "gasté 10 en power"
- **THEN** el ejemplo es «power 10 billetera»

### Requirement: Contador de Capturas Listas
La bandeja DEBE (MUST) mostrar cuántas capturas de los últimos 7 días quedaron listas sin revisar sobre el total de esas capturas. Una captura cuenta como lista sin revisar si su primera interpretación ya era un borrador Listo de confianza ALTA. Las descartadas no cuentan; sin capturas en esos días, no se muestra.

#### Scenario: La semana de Carlos
- **WHEN** en los últimos 7 días Carlos capturó "pagué 25 de almuerzo bnb", "pagué 10 de taxi bnb" y "page 3 bs para trasmporte"
- **THEN** la bandeja muestra que 2 de 3 capturas quedaron listas sin revisar

#### Scenario: Capturas viejas
- **WHEN** Carlos capturó un gasto hace 10 días
- **THEN** ese gasto no cuenta en el contador
