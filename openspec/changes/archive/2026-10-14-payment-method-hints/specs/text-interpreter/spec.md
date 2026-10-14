# Spec Delta

## ADDED Requirements

### Requirement: Medio de Pago como Pista de Billeteras Candidatas
El intérprete DEBE (MUST) reconocer en una frase el medio de pago con palabras del paquete de idioma, sin tratarlo como billetera: efectivo, transferencia (QR, Pix, banca por internet o ventanilla) o tarjeta. Las candidatas son las billeteras activas que aceptan ese medio según su tipo y, si se dijo, la moneda. La propuesta conserva el medio dicho para la revisión.

#### Scenario: Varias cuentas que aceptan QR
- **WHEN** Carlos tiene "Billetera" en efectivo (por defecto), "BNB Bs", "BE Bs" y "Yape", y escribe "almuerzo 45 qr"
- **THEN** el intérprete recomienda "BNB Bs", la primera de sus cuentas que aceptan transferencias, como sugerida y con confianza baja, y la propuesta conserva el medio transferencia

#### Scenario: Una sola cuenta que acepta QR
- **WHEN** Carlos tiene "Billetera" en efectivo (por defecto) y solo "BNB Bs" como banco, y escribe "almuerzo 45 qr"
- **THEN** el intérprete sugiere "BNB Bs" con la billetera marcada como sugerida

#### Scenario: La billetera por defecto acepta el medio
- **WHEN** la billetera por defecto de Carlos es "BE Bs" y escribe "almuerzo 45 qr" teniendo también "BNB Bs"
- **THEN** el intérprete recomienda "BE Bs" como sugerida

#### Scenario: Ninguna billetera acepta el medio
- **WHEN** Carlos solo tiene billeteras en efectivo y escribe "almuerzo 45 qr"
- **THEN** la billetera queda sin proponer y el borrador queda Incompleto

#### Scenario: Pix en Brasil
- **WHEN** Ana tiene dos cuentas digitales en reales y escribe "paguei 30 reais no pix"
- **THEN** el intérprete recomienda una de sus cuentas como sugerida, con confianza baja

#### Scenario: El alias gana sobre el medio
- **WHEN** Carlos definió el alias "bnb" para "BNB Bs" y escribe "almuerzo 45 qr bnb"
- **THEN** el intérprete propone "BNB Bs" como dicho por el usuario

### Requirement: Efectivo y Moneda Separan Billeteras
El efectivo en cada moneda es una billetera distinta. Si la frase dice una moneda que la billetera elegida no acepta, el intérprete DEBE (MUST) recomendar entre las billeteras activas de esa moneda (primero las de efectivo si el medio no se dijo). Entre varias candidatas recomienda la billetera por defecto si es candidata; si no, la de la moneda local del usuario; si no, la primera en el orden de sus billeteras.

#### Scenario: Efectivo sin moneda
- **WHEN** Carlos tiene "Billetera" en bolivianos (por defecto) y "Dólares colchón" en efectivo, y escribe "pague 20 en efectivo"
- **THEN** el intérprete sugiere "Billetera"

#### Scenario: Efectivo en dólares
- **WHEN** Carlos tiene "Billetera" en bolivianos (por defecto) y "Dólares colchón" en efectivo, y escribe "pague 20 dolares en efectivo"
- **THEN** el intérprete sugiere "Dólares colchón"

#### Scenario: Moneda dicha sin medio
- **WHEN** Carlos tiene "Billetera" en bolivianos (por defecto), "Dólares colchón" en efectivo y "Dólares banco", y escribe "pague 20 dolares"
- **THEN** el intérprete recomienda "Dólares colchón", el efectivo en esa moneda, como sugerida y con confianza baja

#### Scenario: Efectivo en una moneda que no tiene
- **WHEN** Carlos no tiene billeteras en reales y escribe "pague 30 reais en efectivo"
- **THEN** la billetera queda sin proponer y el borrador queda Incompleto

## MODIFIED Requirements

### Requirement: Billetera Sugerida por Defecto
Cuando una frase o un audio no nombra billetera ni medio de pago, el intérprete DEBE (MUST) proponer la billetera por defecto del usuario como sugerida, salvo que la frase diga una moneda que esa billetera no acepta. Un comprobante nunca usa la billetera por defecto como origen: usa la cuenta del comprobante o la del emisor de la plantilla.

#### Scenario: Gasto hormiga en efectivo
- **WHEN** la billetera por defecto de Carlos es "Billetera" y escribe "gaste 2 para un semillita"
- **THEN** el intérprete propone un gasto de 2 en "Billetera" con la billetera marcada como sugerida

#### Scenario: Comprobante con cuenta desconocida
- **WHEN** un comprobante trae una cuenta de origen que no es alias de ninguna billetera
- **THEN** la billetera queda faltante en lugar de usar la billetera por defecto

### Requirement: Interpretación de Frases
El intérprete DEBE (MUST) leer frases escritas o dictadas tolerando errores comunes de tipeo: el tipo de movimiento por los verbos del paquete (gasto si no hay verbo), el monto, la moneda si se nombra, la billetera por alias, la nota (lo que sigue a la preposición de nota o, si no hay, las palabras restantes) y varios movimientos unidos por un conector.

#### Scenario: Frase con errores de tipeo
- **WHEN** Carlos escribe "page 3 bs para trasmporte"
- **THEN** el intérprete propone un gasto de 3.00 BOB con la nota "trasmporte"

#### Scenario: Alias en la frase
- **WHEN** Carlos escribe "almuerzo 45 bnb" y "bnb" es alias de "BNB Bs"
- **THEN** el intérprete propone un gasto de 45 en "BNB Bs" con la nota "almuerzo"

#### Scenario: Dos movimientos en una frase
- **WHEN** Carlos escribe "almuerzo 45 y micro 3"
- **THEN** el intérprete propone dos gastos, de 45 y de 3

#### Scenario: Cambio de moneda dictado
- **WHEN** Carlos dice "vendi 50 dolares a 11.88"
- **THEN** el intérprete propone una transferencia de 50.00 USD que recibe 594.00 en la moneda de la billetera por defecto
