# Tasks: Dinero y Tasas Exactas (TDD: primero la prueba)

## 1. Aritmética exacta

- [ ] 1.1 Escribir `ExactMathTest` (suma y multiplicación que desbordan lanzan `ArithmeticException`; MCD; `divideHalfEven` con empates a par, negativos y resto cero). Luego implementar `internal object ExactMath` y hacer que `LedgerEvent` deje de tener su propio `divideHalfEven`, hasta que pase.

## 2. Money encapsulado

- [ ] 2.1 Reescribir `MoneyTest` sin leer unidades mínimas (comparar `Money` con `Money`, igualdad y `hashCode`, desbordamiento al sumar, 0.10 + 0.20 = 0.30). Luego convertir `Money` en clase con representación privada y `Money.ofMinorUnits`, quitando `amountInMinorUnits`, `amountInCents`, `times(Int/Long)`, `fromMajorUnits` y `toUnsignedNumericString`, hasta que pase.
- [ ] 2.2 Migrar `LedgerProjection`, `LedgerGuard`, `LedgerEvent` y `WalletUseCases` a operaciones de `Money` (sumas, comparaciones y signo); verificar que las pruebas de wallet (unitarias y de Carlos, John y Facundo) pasan con los mismos valores.

## 3. ExchangeRate

- [ ] 3.1 Escribir `ExchangeRateTest` con los escenarios del spec: paralelo (50 USD → 594 Bs), composición BTC→USD→BOB (0.15 BTC → 110,840.40 Bs con un solo redondeo), half-even (0.05 USD → 0.01 EUR), tasa de una operación real, inversa (1,120 Bs → 100 USD), `parse("1420.50")`, monedas iguales o montos no positivos rechazados, y composición incompatible rechazada. Luego implementar `ExchangeRate` y `Money.times(rate)` hasta que pase.
- [ ] 3.2 Cambiar `Transfer.derivedRate()` para que devuelva `ExchangeRate`; actualizar `TransferTest` y las pruebas de feature que lo usan (11.88 BOB por USD y 65,000 USD por BTC, verificados con `quoteFor`).

## 4. Tasas del dominio sin Double

- [ ] 4.1 Cambiar los extractores para que devuelvan `String?` canónico; actualizar `JsonDotNotationExtractorTest` y `HtmlRegexExtractorTest` ("11.88", "1420.50"; vacío o inválido da null) y verificar que pasan.
- [ ] 4.2 Migrar `ExchangeRateStrategy` (`Fixed`, `ManualDaily`, `Verified`), `SavedRateSelector` y `ConfirmWebSelectorUseCase` a `ExchangeRate`; verificar con `CarlosBoliviaFeatureTest` (selector, fallback degradado y fallos tipados) y `FacundoArgentinaFeatureTest`.
- [ ] 4.3 Reescribir `CurrencyConverter` por composición de tasas, con `PortfolioHoldingItem.appliedRate` y `ProviderStamp.rate` como `ExchangeRate`, y adaptar `GetConsolidatedPortfolioUseCase`. Actualizar `CurrencyConverterTest` con el escenario "consolidado igual a la suma del desglose" y verificar que los totales de John (9,330.00 USD), Carlos (112,028.40 Bs), Facundo (5,126,200.00 ARS), Sophie (920.00 EUR) y Wei (58,773.00 CNY) se mantienen.
- [ ] 4.4 Eliminar `Money.fromDecimal` y `toDecimalDouble` (las pruebas crean montos con `parseCanonical` u `ofMinorUnits`); verificar con `grep` que `commonMain` no tiene `Double` en la ruta de dinero y tasas, y que ningún archivo fuera de `Money`/`ExchangeRate`/`ExactMath` accede a la representación interna.

## 5. Verificación integral

- [ ] 5.1 Ejecutar `npm test` y `npm run lint` con todo en verde y actualizar la sección "Dinero en enteros" de `AGENTS.md` para indicar que se cruza de moneda solo con `Money * ExchangeRate`.

## Workflow follow-up

- Archivar `money-precision` con `opsx-archive` tras la revisión.
- Siguiente cambio del roadmap: `draft-inbox`.
