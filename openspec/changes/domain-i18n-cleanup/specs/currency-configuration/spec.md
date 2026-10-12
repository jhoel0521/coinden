# Spec Delta

## MODIFIED Requirements

### Requirement: Visualización Dual del Patrimonio (Unificada vs Desglosada) con Transparencia de Fuente
El sistema DEBE permitir alternar entre una vista unificada (consolidada en moneda local con indicación visible del proveedor y fecha de la cotización) y una vista desglosada con la lista total de todas las monedas y su equivalencia estimada. La fuente de conversión DEBE (MUST) entregarse como datos estructurados (tipo de fuente, referencia, tasa, fecha y si es un respaldo degradado), nunca como un texto en un idioma.

#### Scenario: Visualización unificada con trazabilidad de proveedor
- **WHEN** el usuario visualiza su balance consolidado en modo unificado
- **THEN** el sistema muestra el valor total en la moneda local e indica visiblemente la fuente de conversión (ej. "Tasa Frankfurter al día", "CoinGecko hace 10m", "DolarBlueBolivia a 11.88" o "Tasa manual fijada por ti"), armada por la presentación en el idioma del usuario a partir de los datos estructurados de la fuente

#### Scenario: Fuente de conversión como datos estructurados
- **WHEN** Facundo consulta su consolidado con la estrategia de fuente verificada "ar-dolar-blue"
- **THEN** el sello de la fuente indica tipo VERIFICADA, referencia "ar-dolar-blue", la tasa aplicada y la fecha, sin ningún texto en español ni en otro idioma

#### Scenario: Fuente degradada por selector web caído
- **WHEN** Carlos consulta su consolidado y su selector web está en estado de reconfirmación
- **THEN** el sello indica tipo SELECTOR_WEB, la URL configurada como referencia, la última tasa conocida y la marca de respaldo degradado

#### Scenario: Visualización desglosada por moneda individual
- **WHEN** el usuario elige el modo de visualización desglosado
- **THEN** el sistema muestra la lista completa de saldos en sus divisas nativas individuales junto a su equivalencia calculada en moneda base

## ADDED Requirements

### Requirement: Representación de Monedas y Montos Neutral al Idioma
El dominio DEBE (MUST) identificar las monedas del catálogo solo por su código y su precisión decimal, sin nombres ni símbolos, y conservar nombre y símbolo únicamente en las monedas personalizadas tal como los escribió el usuario. Los montos se representan y se leen en una forma canónica neutral (punto decimal y código), y el formato regional queda a cargo de la presentación.

#### Scenario: Moneda del catálogo sin textos
- **WHEN** el sistema resuelve la moneda "BOB" del catálogo
- **THEN** obtiene el código BOB con 2 decimales, sin nombre ni símbolo asociados

#### Scenario: Moneda personalizada conserva lo que escribió el usuario
- **WHEN** Carlos registra la moneda personalizada "VALE" con símbolo "V", nombre "Vale comunitario" y 0 decimales
- **THEN** el sistema conserva exactamente ese símbolo y ese nombre para que la presentación los muestre

#### Scenario: Representación canónica de un monto
- **WHEN** el sistema representa 1188 Bs (118800 centavos de BOB) para pruebas o registros
- **THEN** la representación es "1188.00 BOB", sin símbolo ni separadores de miles

#### Scenario: Lectura canónica rechaza formatos regionales
- **WHEN** el sistema recibe el monto "35,50" o "1.234,56" para leerlo en forma canónica
- **THEN** rechaza la lectura con un error de formato, porque interpretar formatos regionales corresponde a la presentación o al parser de borradores
