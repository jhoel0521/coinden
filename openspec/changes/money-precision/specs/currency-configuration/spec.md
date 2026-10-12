# Spec Delta

## ADDED Requirements

### Requirement: Aritmética Monetaria Segura
El dominio DEBE (MUST) operar montos solo mediante operaciones de dinero: sumar, restar y comparar montos de la misma moneda y en su unidad mínima exacta. Operar montos de monedas distintas sin una tasa de cambio es un error. Si un resultado no cabe en la representación del monto, la operación DEBE fallar con un error explícito en lugar de producir un valor incorrecto.

#### Scenario: Suma exacta en la misma moneda
- **WHEN** Carlos suma 0.10 Bs y 0.20 Bs
- **THEN** el resultado es exactamente 0.30 Bs

#### Scenario: Monedas distintas sin tasa de cambio
- **WHEN** el sistema intenta sumar 10.00 USD y 35.50 Bs directamente
- **THEN** la operación falla con un error de monedas distintas

#### Scenario: Desbordamiento explícito
- **WHEN** una suma o una conversión produce un monto mayor al máximo representable
- **THEN** la operación falla con un error de desbordamiento y no devuelve ningún monto

### Requirement: Conversión Exacta mediante Tasas de Cambio
El sistema DEBE (MUST) convertir montos entre monedas únicamente aplicando una tasa de cambio, definida como "una cantidad de una moneda equivale a una cantidad de otra". La conversión es exacta y redondea una sola vez, al final, a la unidad mínima de la moneda destino con redondeo half-even. Las tasas pueden invertirse y componerse sin redondeo, de modo que una conversión encadenada (por ejemplo BTC → USD → BOB) redondea una única vez.

#### Scenario: Conversión con la tasa del paralelo
- **WHEN** Carlos convierte 50.00 USD con la tasa "1.00 USD = 11.88 BOB"
- **THEN** el resultado es exactamente 594.00 Bs

#### Scenario: Conversión encadenada con un solo redondeo
- **WHEN** el sistema convierte 0.15 BTC a BOB componiendo "1 BTC = 62,200.00 USD" con "1.00 USD = 11.88 BOB"
- **THEN** el resultado es exactamente 110,840.40 Bs, sin redondear el paso intermedio en USD

#### Scenario: Redondeo half-even al centavo
- **WHEN** el sistema convierte 0.05 USD con la tasa "1.00 USD = 0.25 EUR", cuyo valor exacto es 0.0125 EUR
- **THEN** el resultado es 0.01 EUR, porque half-even redondea el empate al dígito par

#### Scenario: Tasa definida por una operación real
- **WHEN** Carlos vende 50.00 USD y recibe 594.00 Bs
- **THEN** la tasa de esa operación es "50.00 USD = 594.00 BOB" y, aplicada a 1.00 USD, da 11.88 Bs

#### Scenario: Tasa inversa
- **WHEN** el sistema invierte la tasa "1.00 USD = 11.20 BOB" y convierte 1,120.00 Bs
- **THEN** el resultado es exactamente 100.00 USD

#### Scenario: Cotización leída de una fuente web sin coma flotante
- **WHEN** el selector de Facundo extrae el texto "1420.50" de DolarApi
- **THEN** el sistema construye la tasa "1.00 USD = 1,420.50 ARS" a partir del texto decimal, sin pasar por números de coma flotante

#### Scenario: Consolidado igual a la suma de su desglose
- **WHEN** el usuario consulta su patrimonio consolidado
- **THEN** cada posición se convierte con un único redondeo y el total unificado es exactamente la suma de los montos convertidos que muestra la vista desglosada
