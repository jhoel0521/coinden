# Proposal: Hub de Monedas y Estrategias Cambiarias (Currency Hub)

## Why
Para que CoinDen funcione como una aplicación financiera global, privada y de cero coste de servidores ($0 backend), la configuración monetaria y de tipos de cambio debe ser un módulo completamente autónomo. Esto permite resolver dos realidades opuestas:
1. Un onboarding inmediato de un solo paso para usuarios en economías ancla (EE.UU., Europa con USD/EUR/BTC) respaldado por proveedores públicos abiertos desde el Día 1.
2. Soberanía cambiaria total para usuarios en economías emergentes con brecha cambiaria (Bolivia, Argentina con tasas fijas, manuales del mercado paralelo o recomendaciones de endpoints locales), garantizando operatividad 100% offline.

## What Changes
- Creación del módulo de dominio desacoplado para gestión monetaria (`currency-configuration`).
- Integración de proveedores públicos abiertos de confianza desde el Día 1 (sin API Keys ni servidores propios):
  - **Frankfurter API (Banco Central Europeo)** para tipos de cambio oficiales entre divisas globales (USD, EUR, CNY).
  - **CoinGecko Public API** para cotizaciones en tiempo real de Bitcoin (BTC) frente a USD y EUR.
- Directorio integrado de fuentes y endpoints recomendados por país (ej. DolarApi para Argentina, feeds de bancos centrales) para guiar al usuario en la configuración de su moneda local.
- Definición de monedas ancla globales (`USD`, `EUR`, `CNY`, `BTC`) que actúan como pivotes internacionales de conversión.
- Definición del modelo de Moneda Local y Monedas Personalizadas (`Currency`) con soporte de 0 a 8 decimales.
- Introducción del patrón de Estrategias Cambiarias (`ExchangeRateStrategy`):
  - Tasa Identidad ($1.0$ para usuarios que operan directamente en USD).
  - Proveedor Público Automático (Frankfurter / CoinGecko).
  - Tasa Fija definida por el usuario.
  - Tasa Manual actualizable periódicamente (útil para mercados con tipos de cambio paralelos/blue como Bolivia y Argentina).
  - Tasa por Endpoint Remoto configurable (URL y extractor JSON, preparada para inferencia futura con IA local).
- Contratos de persistencia local offline y resolución de cotizaciones con respaldo en última tasa conocida (`lastKnownRate`).

## Casos de Uso del Mundo Real y Perfiles de Usuario

### Perfil 1: John (Estados Unidos - Moneda Ancla USD)
- **Moneda Base:** USD. Estrategia: Identidad (1.0).
- **Cotizaciones Automáticas Día 1:** EUR vía Frankfurter API, BTC vía CoinGecko Public.
- **Cuentas en 5 monedas:**
  - Efectivo local: $300 USD.
  - Banco Chase: $4,500 USD.
  - Cuenta en Euros: 1,000 EUR (convertido a USD por tasa EUR/USD 1.08 = $1,080 USD).
  - Cripto desconocida: Token "MOON" (creado con 4 decimales: 5,000 MOON a $0.05 = $250 USD).
  - Bitcoin: 0.05 BTC (convertido a USD según CoinGecko = $3,250 USD).
- **Patrimonio Consolidado:** $9,380 USD.

### Perfil 2: Carlos (Bolivia - Brecha Cambiaria Oficial vs Paralelo)
- **Moneda Base:** BOB. Estrategia: Manual/Variable (tasa de mercado paralelo de 11.20 BOB por 1 USD).
- **Monedas Ancla:** USD/EUR/BTC resueltas automáticamente por la app; Carlos solo gestiona su par BOB/USD.
- **Cuentas en 5 monedas:**
  - Efectivo: 1,500 BOB.
  - Banco BNB: 20,000 BOB.
  - Ahorro en Dólares: $500 USD (a 11.20 BOB = 5,600 BOB).
  - Cripto desconocida: Token "VALE" (0 decimales: 80 VALES a 10 BOB = 800 BOB).
  - Bitcoin: 0.01 BTC ($650 USD x 11.20 = 7,280 BOB).
- **Patrimonio Consolidado:** 35,180 BOB.

### Perfil 3: Facundo (Argentina - Dólar Blue y Recomendación Local)
- **Moneda Base:** ARS. Estrategia: Endpoint recomendado (DolarApi) o Manual Diaria (1 USD = 1,280 ARS).
- **Cuentas en 5 monedas:**
  - Efectivo: 80,000 ARS.
  - Banco Galicia: 850,000 ARS.
  - Dólares bajo el colchón: $400 USD (a 1,280 ARS = 512,000 ARS).
  - Cripto desconocida: Memecoin "PAMP" (2 decimales).
  - Bitcoin: 0.02 BTC (convertido a ARS Blue).
- **Patrimonio Consolidado:** En ARS unificado.

### Perfil 4: Sophie (Francia - Eurozona)
- **Moneda Base:** EUR. Estrategia: Proveedor Público Día 1 (Frankfurter / CoinGecko).
- **Cuentas en 5 monedas:**
  - Efectivo: 150 EUR.
  - BNP Paribas: 5,200 EUR.
  - Broker en Wall Street: $1,500 USD (a 0.925 EUR = 1,387 EUR).
  - Cripto desconocida: Token de lealtad "WINE" (0 decimales).
  - Bitcoin: 0.08 BTC (convertido a EUR).
- **Patrimonio Consolidado:** En EUR unificado.

### Perfil 5: Wei (China - Yuan)
- **Moneda Base:** CNY. Estrategia: Tasa Fija / Frankfurter (1 USD = 7.15 CNY).
- **Cuentas en 5 monedas:**
  - Efectivo: 800 CNY.
  - WeChat / Alipay: 35,000 CNY.
  - Cuenta en Dólares: $1,200 USD (a 7.15 CNY = 8,580 CNY).
  - Cripto desconocida: Token "DRAGON" (6 decimales).
  - Bitcoin: 0.03 BTC (convertido a CNY).
- **Patrimonio Consolidado:** En CNY unificado.

## Capabilities

### New Capabilities
- `currency-configuration`: Configuración de la divisa local, integración de proveedores públicos abiertos (Frankfurter, CoinGecko), catálogo de fuentes recomendadas por país, soporte de monedas personalizadas (0 a 8 decimales) y estrategias de tipo de cambio deterministas, manuales o remotas con respaldo offline.

### Modified Capabilities
<!-- No existen capacidades previas -->

## Impact
- **Arquitectura**: Primer módulo del núcleo de dominio bajo Arquitectura Hexagonal en Kotlin Multiplatform.
- **Red y Dependencias**: Zero-Backend. Peticiones HTTP directas cliente-proveedor público sin requerir servidores intermedios de CoinDen ni claves privadas.
- **Sistemas futuros**: Cimiento para la consolidación patrimonial de carteras (`wallets`) y conversión multimoneda en transacciones.
