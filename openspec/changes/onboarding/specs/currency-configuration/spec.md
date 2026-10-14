# Spec Delta

## ADDED Requirements

### Requirement: Catálogo Mundial de Monedas
El sistema DEBE (MUST) conocer todas las monedas oficiales vigentes (código ISO 4217 y decimales) y la moneda de cada país a partir de los datos de la plataforma, mediante un puerto, sin tablas de países ni de monedas escritas en el dominio. Los nombres y símbolos de cada moneda en el idioma de la app los resuelve la presentación con los mismos datos de la plataforma.

#### Scenario: Decimales oficiales
- **WHEN** se consultan el boliviano, el peso chileno y el dinar de Baréin
- **THEN** el catálogo indica 2, 0 y 3 decimales respectivamente

#### Scenario: Moneda de un país
- **WHEN** se consulta la moneda de Bolivia, de Francia y de China
- **THEN** el catálogo responde BOB, EUR y CNY

#### Scenario: Moneda que la plataforma no conoce
- **WHEN** la persona necesita una moneda que el catálogo del teléfono no tiene
- **THEN** puede registrarla como moneda propia con su código, símbolo y decimales

### Requirement: Tasa de Referencia Solo para Totales
La tasa de referencia de una moneda DEBE (MUST) usarse únicamente para mostrar totales y equivalencias en otra moneda; nunca determina el monto de una operación registrada. Una transferencia entre monedas guarda los dos montos reales y su tasa derivada, aunque difiera de la tasa de referencia.

#### Scenario: El cambio real no usa la referencia
- **WHEN** Carlos tiene una tasa de referencia de 1 USD = 10.20 BOB y registra que vendió 11 USD y le dieron 112 BOB
- **THEN** el libro guarda 11.00 USD y 112.00 BOB con la tasa 112/11, y la tasa de referencia no cambia

## MODIFIED Requirements

### Requirement: Moneda Local con Estrategia Manual o Fija
El sistema DEBE (MUST) permitir al usuario definir una divisa local propia, las demás monedas que usa (del catálogo o propias) y, para cada una, una tasa de referencia opcional hacia la moneda local con una estrategia fija, manual periódica, verificada o por selector web. Una moneda sin tasa de referencia queda fuera de los totales y se muestra aparte.

#### Scenario: Configuración en Bolivia con tasa manual fija o periódica
- **WHEN** el usuario selecciona moneda "BOB" (Bs) y define una tasa manual de 11.20 BOB por 1 USD
- **THEN** el sistema registra el perfil monetario y liquida conversiones de activos en USD multiplicando por 11.20 BOB de forma determinista y offline

#### Scenario: Configuración en China con tasa fija (Wei)
- **WHEN** el usuario selecciona moneda "CNY" (¥) con tasa fija de 7.15 CNY por 1 USD
- **THEN** el sistema aplica el factor 7.15 para convertir saldos entre USD y CNY sin requerir conexión a internet

#### Scenario: Varias monedas con su propia tasa
- **WHEN** Carlos usa BOB como moneda local, USD a 10.20 y BRL a 1.85, y tiene 100 USD y 50 BRL
- **THEN** su total en BOB suma 1020.00 por los dólares y 92.50 por los reales

#### Scenario: Moneda sin tasa
- **WHEN** Carlos usa reales sin tasa de referencia y tiene 50 BRL
- **THEN** los 50 BRL no se suman al total en BOB y se informan aparte como moneda sin tasa
