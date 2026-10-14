# Spec Delta

## ADDED Requirements

### Requirement: Billeteras Reconocidas por su Nombre
El intérprete DEBE (MUST) reconocer una billetera, como dicha por el usuario, cuando la frase contiene una palabra de su nombre, sin apodos configurados. No cuentan las palabras de moneda, medio de pago o genéricas del paquete de idioma, ni una palabra que comparten dos billeteras activas. Se toleran errores de tipeo como en los verbos.

#### Scenario: Banco por su sigla
- **WHEN** Carlos tiene "BNB Bs" y escribe "almuerzo 45 bnb"
- **THEN** el intérprete propone el gasto en "BNB Bs" como dicho por el usuario

#### Scenario: Palabra genérica descartada
- **WHEN** Carlos tiene "Banco Económico" y "BNB Bs", y escribe "pague 20 economico"
- **THEN** el intérprete propone "Banco Económico"; la palabra "banco" sola no identifica ninguna billetera

#### Scenario: Error de tipeo
- **WHEN** Carlos tiene "Banco Económico" y escribe "pague 20 economco"
- **THEN** el intérprete propone "Banco Económico"

#### Scenario: Palabra compartida
- **WHEN** Carlos tiene "Dólares colchón" y "Dólares banco", y escribe "pague 20 colchon"
- **THEN** el intérprete propone "Dólares colchón"; la palabra "dolares" no identifica a ninguna de las dos

#### Scenario: La moneda en el nombre no es un apodo
- **WHEN** Carlos tiene "BNB Bs" y "Billetera" en bolivianos, y escribe "page 3 bs para trasmporte"
- **THEN** "bs" se lee como la moneda y la billetera sugerida sigue siendo la por defecto

### Requirement: Moneda de la Región desde el Catálogo
Cuando una frase usa un símbolo o palabra de moneda compartida por varios países ("$", "pesos"), el intérprete DEBE (MUST) resolverla con la moneda de la región del dispositivo obtenida del catálogo mundial de monedas, no de una tabla de países propia, y marcarla como inferida.

#### Scenario: Pesos en Argentina
- **WHEN** Facundo, con la región Argentina, escribe "pague 5000 pesos"
- **THEN** el intérprete propone 5000 ARS con la moneda marcada como inferida

#### Scenario: Pesos en una región que el dominio no listaba
- **WHEN** una persona con la región República Dominicana escribe "pague 500 pesos"
- **THEN** el intérprete propone 500 DOP con la moneda marcada como inferida

