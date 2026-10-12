# Design: Dinero y Tasas Exactas

## Context
Ver proposal.md (Why) y los requisitos en `specs/currency-configuration/spec.md`. Hoy `Money` es un `data class` con `amountInMinorUnits: Long` público. `CurrencyConverter` pasa por USD con `Double` (`toDecimalDouble`, `fromDecimal`). Las tasas son `Double` en `ExchangeRateStrategy`, `SavedRateSelector`, `ProviderStamp`, `PortfolioHoldingItem` y en el mapa `cryptoToUsdRates`. Los extractores devuelven `Double?`. En `wallet`, `LedgerProjection` y `LedgerGuard` suman `Long` directamente, y `Transfer.derivedRate()` devuelve un `Money`.

## Goals / Non-Goals

**Goals:**
- Ningún `Double` en la ruta del dinero ni de las tasas dentro de `commonMain`.
- Ningún acceso a la representación de `Money` fuera de `Money` y `ExchangeRate`.
- Conservar exactamente los valores esperados de las pruebas de las 5 personas.

**Non-Goals:**
- Precisión arbitraria (ver D6).
- Cambiar la semántica de las estrategias: siguen expresando "moneda local por 1 USD".

## Decisions

### D1. `Money` como clase con representación privada
`class Money private constructor(private val minor: Long, val currency: Currency)`, con `equals`/`hashCode` escritos a mano. Se deja de usar `data class` porque generaría `copy()` y `componentN()`, que filtran la representación.

API pública:
- Creación: `Money.ofMinorUnits(units: Long, currency)`, `Money.zero(currency)` y `Money.parseCanonical(text, currency)`. Crear un `Money` desde unidades mínimas es una *entrada*; lo que se evita es *leerlas*.
- Operaciones: `+`, `-` (misma moneda), `unaryMinus`, `compareTo` (misma moneda), `isZero`, `isPositive`, `isNegative` y `times(rate: ExchangeRate)`.
- Salida: `toString()` y `toNumericString()` canónicos.

Se eliminan `amountInMinorUnits`, `amountInCents`, `times(Int/Long)`, `fromMajorUnits`, `fromDecimal`, `toDecimalDouble` y `toUnsignedNumericString`.
- *Alternativa descartada:* un `@JvmInline value class`. No puede llevar la moneda junto al monto.

### D2. `ExchangeRate` como fracción exacta entre unidades mínimas
`ExchangeRate.of(base: Money, quote: Money)` exige monedas distintas y montos positivos. Internamente guarda `from`, `to` y la fracción `numerator / denominator = quote.minor / base.minor`, reducida por MCD. Es decir: cuántas unidades mínimas de `to` vale una unidad mínima de `from`.
- `inverse()` intercambia monedas y fracción.
- `times(other)` (`rate1 * rate2`) exige `this.to == other.from` y multiplica las fracciones, reduciendo por MCD antes de multiplicar para alejar el desbordamiento.
- `ExchangeRate.parse(from, to, perUnit: String)` lee un decimal canónico ("1420.50", "0.923456"), sin límite de decimales, como `quote = perUnit × 10^to.decimals` por `base = 1 × 10^from.decimals`, todo en enteros.
- `Money.times(rate)` exige `this.currency == rate.from` y calcula `minor × num / den` con redondeo half-even. Es el único punto de redondeo.
- `quoteFor(base: Money)` (equivale a `base * this`) permite a la presentación mostrar "1 USD = 11.88 BOB".
- *Alternativa descartada:* un decimal de escala fija (por ejemplo 10⁻¹²). La composición BTC→USD→BOB acumularía error de truncamiento; la fracción es exacta.

### D3. Aritmética entera verificada
Un helper interno (`internal object ExactMath`) ofrece `addExact`, `multiplyExact` (detección de desbordamiento sin `Math`, porque `java.lang.Math` no existe en `commonMain`), `gcd` y `divideHalfEven`, que hoy vive dentro de `LedgerEvent`. Un desbordamiento lanza `ArithmeticException` (D5).

### D4. Conversor por composición de tasas
`CurrencyConverter(ratesToUsd: Map<String, ExchangeRate>)` recibe, por cada moneda distinta de USD, su tasa *desde* USD (`1 USD = X`). Para convertir A→B arma `rateA⁻¹ * rateB` (pasando por USD si hace falta), y aplica `money * composed` una sola vez. `consolidate` suma `Money` y entrega por cada posición la tasa aplicada (`appliedRate: ExchangeRate?`, null si la moneda ya es la de destino). Así el total es por construcción la suma del desglose.

`GetConsolidatedPortfolioUseCase` arma ese mapa a partir de la estrategia del perfil (ya `ExchangeRate`) y de los precios cripto inyectados (`cryptoRatesFromUsd: List<ExchangeRate>`; el default de BTC se conserva tipado). `ProviderStamp.exchangeRateToUsd: Double` pasa a ser `rate: ExchangeRate`.

### D5. Desbordamiento como excepción, no como resultado
Un desbordamiento es un límite técnico, no un caso de negocio que el usuario pueda corregir. Por eso se lanza `ArithmeticException` desde `ExactMath`, igual que los invariantes de `Money`, en lugar de agregar un `Result` a cada suma. Los casos de uso no lo capturan: si ocurre, es un bug de datos que tiene que verse.

### D6. Mantener `Long` por dentro
Con `Long`, los saldos llegan hasta unos 9.2 × 10¹⁸ unidades mínimas (unos 92 mil millones de BTC en satoshis), y la conversión BTC→USD→BOB de las pruebas usa productos del orden de 10¹⁷. Si un caso real lo supera, D1 y D2 permiten cambiar la representación (por ejemplo, a enteros de precisión arbitraria con dígitos y acarreo) solo dentro de `Money`, `ExchangeRate` y `ExactMath`.

### D7. Extractores sin `Double`
`RateResponseExtractor.extract` devuelve `String?` canónico ("11.88"): sigue normalizando la coma de la web y valida con el mismo patrón que `parseCanonical`, sin límite de decimales. `ConfirmWebSelectorUseCase` construye `ExchangeRate.parse(USD, targetCurrency, text)`; un texto que no es positivo da `NO_VALID_RATE`.

### D8. Persistencia por forma canónica
Cuando exista el adaptador SQLite, guardará `Money` como su forma canónica (o como el par moneda + texto numérico) y `ExchangeRate` como el par canónico `base`/`quote`. El `Long` no sale de `Money`. Este cambio solo lo deja documentado; no hay persistencia todavía.

## Risks / Trade-offs
- [Muchas pruebas cambian de forma] → Los valores esperados no cambian. Se migra archivo por archivo y la suite completa debe quedar en verde en cada grupo de tareas.
- [Las fracciones de tasas con muchos decimales crecen al componerse] → Se reduce por MCD antes de multiplicar y, si aun así se desborda, falla explícitamente (D5). Las pruebas cubren el peor caso actual (BTC→USD→BOB).
- [Igualdad de `Money` a mano] → Se prueba explícitamente: misma moneda y monto son iguales, monedas distintas no lo son, y `hashCode` es consistente.

## Migration Plan
No aplica: no hay datos persistidos ni consumidores externos.
