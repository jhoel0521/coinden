# Design: Arquitectura del Hub de Monedas y Estrategias Cambiarias

## Context
CoinDen es una aplicación financiera Local-First y Zero-Backend que debe ejecutarse en Android, iOS y WebAssembly. Los sistemas tradicionales centralizan las cotizaciones en servidores propios o asumen que todos los usuarios operan bajo una moneda hegemónica (como el dólar). En la práctica mundial, conviven usuarios que necesitan cero fricción cambiaria (EE.UU./Europa) con usuarios sujetos a brechas cambiarias, monedas locales con alta inflación o restricciones de acceso a divisas (Bolivia, Argentina).

Este diseño desacopla la gestión de monedas y tipos de cambio en un módulo de dominio puro (`currency-configuration`), separando estrictamente la lógica de negocio y cálculo financiero de la infraestructura de red, y formalizando una arquitectura de pruebas orientada a TDD con Tests Unitarios y Tests de Feature.

## Goals / Non-Goals

**Goals:**
- **Modelo de Dominio Puro KMP:** Entidades y Value Objects independientes de cualquier sistema operativo o framework de red/base de datos.
- **Monedas Ancla Mundiales:** Soporte nativo para USD, EUR, CNY y Bitcoin (BTC con 8 decimales / Satoshis) como pivotes internacionales de conversión.
- **Lista Blanca de Fuentes Verificadas (Whitelist):** Catálogo integrado de fuentes comunitarias/oficiales listas para usar en 1 toque (Frankfurter para divisas globales, CoinGecko para Bitcoin, DolarApi para Argentina, DolarBlueBolivia para Bolivia).
- **Ciclo de Vida de Selectores Personalizados:** Soporte para que el usuario confirme selectores sobre URLs externas (JSON o HTML de páginas web), los persista para consultas automáticas directas y cuente con auto-recuperación ante cambios de maquetación (fallback a `lastKnownRate` + alerta para re-escanear).
- **Estrategias Cambiarias Polimórficas:** Patrón Strategy tipado para soportar tasa Identidad (1.0), Tasa Fija, Tasa Manual Periódica, Fuente Verificada y Selector Web Personalizado.
- **Proyección de Cartera Dual:** Modelo de lectura para visualización unificada (patrimonio consolidado con etiqueta de proveedor y fecha) o desglosada (lista individual por divisa con valor estimado).
- **Estrategia de Testing Clean Code:** Batería completa de pruebas dividida en Tests Unitarios (lógica pura) y Tests de Feature (los 5 perfiles mundiales simulados de extremo a extremo sin internet).

**Non-Goals:**
- Implementación de interfaz visual en Jetpack Compose (se abordará en fases posteriores de UI).
- Persistencia física en SQLite/SQLCipher (se conectará en la fase de persistencia mediante los puertos SPI definidos aquí).
- Ejecución de modelos pesados de IA en este cambio (el contrato de extracción y re-aprendizaje queda preparado para conectarse a un agente de IA en la siguiente etapa).

## Decisions

### Decisión 1: Conversión por Transitividad sobre Moneda Ancla (USD Pivot)
* **Elección:** Utilizar el Dólar (USD) como pivote universal de conversión entre monedas locales y activos globales.
* **Fórmula de tránsito:** Para convertir de Moneda $A$ a Moneda $B$:
  $$\text{Valor en } B = \left( \frac{\text{Monto en } A}{\text{Tasa } A/\text{USD}} \right) \times \text{Tasa } B/\text{USD}$$

### Decisión 2: Extractores Polimórficos de Respuesta (`RateResponseExtractor`)
* **Elección:** Modelar la extracción de cotizaciones de fuentes externas mediante interfaces agnósticas de transporte:
  - `JsonDotNotationExtractor`: Extrae valores usando notación de puntos (ej. `"data.blue.sell"` o `"venta"`).
  - `HtmlRegexExtractor`: Extrae valores numéricos de páginas web complejas (ej. `id="usdRate">Bs\s*([0-9.,]+)`).
* **Beneficio:** Permite que tanto APIs limpias como páginas web de periódicos o casas de cambio sean consumidas directamente por el cliente sin servidores proxy.

### Decisión 3: Ciclo de Vida del Selector y Auto-Recuperación (`SavedRateSelector`)
* **Elección:** Un selector confirmado por el usuario se modela con estados:
  - `ACTIVE`: La regla funciona y se ejecuta directamente en cada actualización sin gastar CPU en re-analizar la web.
  - `NEEDS_RECONFIRMATION`: Si la página web cambió su HTML y la regla no extrae el número, el sistema no falla: hace fallback a `lastKnownRate` y marca el selector para que la app notifique al usuario y ofrezca re-escanear.

### Decisión 4: Lista Blanca de Fuentes Verificadas (`VerifiedSourceDirectory`)
* **Elección:** Mantener un catálogo de solo lectura en el dominio con las fuentes públicas más populares y seguras por país, permitiendo configuración de 1 toque para el usuario final y facilitando añadir nuevos países en futuras actualizaciones de la app.

### Decisión 5: Puertos Hexagonales (Inbound & Outbound)
* **Puertos de Salida (SPI):**
  - `CurrencyProfileRepositoryPort`: Lectura y escritura del perfil del usuario y selectores guardados.
  - `RemoteWebFetcherPort`: Abstracción para realizar la petición HTTP (GET) y devolver el string crudo (JSON o HTML).
* **Puertos de Entrada (Casos de Uso):**
  - `ConfigureAnchorCurrencyUseCase`
  - `ConfigureLocalCurrencyUseCase`
  - `ConfirmWebSelectorUseCase`
  - `UpdateManualExchangeRateUseCase`
  - `GetConsolidatedPortfolioUseCase` (genera proyecciones unificadas y desglosadas)

## Risks / Trade-offs

- **[Riesgo: Rediseño inesperado de una web de cotización externa]**
  - *Mitigación:* Nunca romper la ejecución de la app. Activar fallback automático a `lastKnownRate` y transicionar el selector a `NEEDS_RECONFIRMATION`.
- **[Riesgo: Sobrecarga en batería por re-analizar webs en cada apertura]**
  - *Mitigación:* Almacenar la regla extraída en la primera confirmación para que las llamadas subsecuentes apliquen directamente el selector precalculado.
