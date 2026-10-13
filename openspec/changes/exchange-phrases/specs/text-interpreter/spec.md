# Spec Delta

## ADDED Requirements

### Requirement: Cambio de Moneda por Entrada y Salida
En una frase de cambio de moneda, el intérprete DEBE (MUST) reconocer el monto recibido que sigue a un marcador de recepción del paquete de idioma y proponerlo como lo que entra, dicho por el usuario. Si la frase también dice una tasa, el monto recibido tiene prioridad. Un conector seguido de un marcador de recepción no separa la frase en dos movimientos.

#### Scenario: Carlos en el librecambista
- **WHEN** Carlos escribe "vendí 11 dólares, me dieron 112 bs"
- **THEN** el intérprete propone una sola transferencia que entrega 11.00 USD y recibe 112.00 BOB, sin nota

#### Scenario: Cambio unido por un conector
- **WHEN** Carlos escribe "vendi 100 dolares y me dieron 1190 bs"
- **THEN** el intérprete propone una sola transferencia de 100.00 USD que recibe 1190.00 BOB, y ningún gasto

#### Scenario: Cambio con "por"
- **WHEN** Carlos escribe "cambié 100 usd por 1190 bs"
- **THEN** el intérprete propone una transferencia de 100.00 USD que recibe 1190.00 BOB

#### Scenario: Lo recibido manda sobre la tasa
- **WHEN** Carlos escribe "vendí 100 dólares a 11.80, me dieron 1190 bs"
- **THEN** el intérprete propone recibir 1190.00 BOB, no 1180.00

#### Scenario: Monto recibido sin moneda
- **WHEN** Carlos escribe "vendí 100 dólares, me dieron 1190" y su billetera por defecto es de bolivianos
- **THEN** el intérprete propone recibir 1190 en la billetera por defecto, en su moneda

#### Scenario: Inglés y portugués
- **WHEN** John escribe "sold 100 usd, got 1190 bob" y Ana escribe "vendi 100 dólares, recebi 530 reais"
- **THEN** el intérprete propone para John recibir 1190.00 BOB y para Ana recibir 530.00 BRL

#### Scenario: La forma con tasa sigue funcionando
- **WHEN** Carlos dice "vendi 50 dolares a 11.88"
- **THEN** el intérprete propone, como hasta ahora, una transferencia de 50.00 USD que recibe 594.00 en la moneda de la billetera por defecto

### Requirement: Billeteras Sugeridas por Moneda en un Cambio
En un cambio de moneda que no nombra billeteras, el intérprete DEBE (MUST) sugerir como origen la única billetera activa en la moneda entregada, y como destino la billetera por defecto si es de la moneda recibida o, si no, la única billetera activa de esa moneda. Si hay varias candidatas o ninguna, ese campo queda sin proponer. Ambas sugerencias se marcan como sugeridas.

#### Scenario: Origen y destino sugeridos
- **WHEN** Carlos, con una sola billetera en dólares ("Dólares colchón") y su billetera de efectivo en bolivianos por defecto, escribe "vendí 11 dólares, me dieron 112 bs"
- **THEN** el intérprete sugiere "Dólares colchón" como origen y la billetera de efectivo como destino, y el borrador queda Listo con confianza baja

#### Scenario: Varias billeteras en la moneda entregada
- **WHEN** Carlos tiene dos billeteras en dólares y escribe "vendí 11 dólares, me dieron 112 bs"
- **THEN** el origen queda sin proponer y el borrador queda Incompleto para elegirlo en la revisión

#### Scenario: Confirmar con la tasa exacta
- **WHEN** Carlos confirma el borrador de "vendí 11 dólares, me dieron 112 bs"
- **THEN** el libro registra la salida de 11.00 USD y la entrada de 112.00 BOB con la tasa derivada exacta 112/11
