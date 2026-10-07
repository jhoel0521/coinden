# Tasks: Implementación de Currency Hub (TDD Puro: Unit & Feature Tests)

## 1. Modelado de Dominio y Value Objects

- [x] 1.1 Definir Value Objects inmutables `Currency` (códigos ISO y tokens personalizados con 0 a 8 decimales) y `Money` (cálculos seguros en unidades mínimas enteras sin punto flotante).
- [x] 1.2 Definir catálogo de monedas ancla globales (`AnchorCurrencies`: USD, EUR, CNY, BTC) y la lista blanca de fuentes verificadas por país (`VerifiedSourceDirectory`: DolarApi, DolarBlueBolivia, Frankfurter, CoinGecko).
- [x] 1.3 Definir la jerarquía sellada `ExchangeRateStrategy`, la entidad `SavedRateSelector` (con estados ACTIVE y NEEDS_RECONFIRMATION) y los extractores `JsonDotNotationExtractor` y `HtmlRegexExtractor`.
- [x] 1.4 Definir el conversor transitivo `CurrencyConverter` y los modelos de proyección de cartera (`ConsolidatedPortfolio`: vista unificada con sello de proveedor y vista desglosada por divisa).

## 2. Puertos Hexagonales (Inbound & Outbound)

- [x] 2.1 Definir contratos de puertos de salida (SPI): `CurrencyProfileRepositoryPort` y `RemoteWebFetcherPort`.
- [x] 2.2 Definir contratos de casos de uso (puertos de entrada): `ConfigureAnchorCurrencyUseCase`, `ConfigureLocalCurrencyUseCase`, `ConfirmWebSelectorUseCase`, `UpdateManualExchangeRateUseCase` y `GetConsolidatedPortfolioUseCase`.

## 3. Pruebas Unitarias Puras (unit/)

- [x] 3.1 Implementar `CurrencyTest` y `MoneyTest` verificando invariantes, soporte de tokens custom (0 a 8 decimales) y operaciones aritméticas inmutables.
- [x] 3.2 Implementar `CurrencyConverterTest` verificando la fórmula transitiva universal con pivote USD.
- [x] 3.3 Implementar `JsonDotNotationExtractorTest` verificando extracción de rutas de puntos en payloads JSON anidados.
- [x] 3.4 Implementar `HtmlRegexExtractorTest` verificando extracción directa sobre el HTML real de `dolarbluebolivia.click` (extrayendo 11.88 BOB).

## 4. Pruebas de Feature de Extremo a Extremo (feature/)

- [x] 4.1 Implementar `JohnUsaFeatureTest` (Onboarding express en USD, vista unificada de $9,330 USD y vista desglosada con sello de proveedor).
- [x] 4.2 Implementar `CarlosBoliviaFeatureTest` (Confirmación de selector sobre web de Dólar Blue, ejecución persistida, y simulación de fallback seguro ante rediseño de página web).
- [x] 4.3 Implementar `FacundoArgentinaFeatureTest` (Configuración de un solo toque usando la Lista Blanca comunitaria para Dólar Blue de DolarApi).
- [x] 4.4 Implementar `SophieFranceFeatureTest` y `WeiChinaFeatureTest` (Conversiones multidivisa transitivas para EUR y CNY).
- [x] 4.5 Ejecutar la suite completa (`./gradlew test`) asegurando 100% de cobertura en verde para todos los casos de prueba.

## Workflow follow-up

- Archivar el cambio `currency-hub` tras la verificación completa en verde.
- Pasar al cambio de `wallet-management` en la siguiente fase.
