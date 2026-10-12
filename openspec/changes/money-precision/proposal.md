# Proposal: Dinero y Tasas Exactas (Money Precision)

## Why
`Money` guarda montos exactos, pero la conversión entre monedas todavía pasa por `Double`: las tasas (`Fixed`, `ManualDaily`, `Verified`, el selector web y el precio de BTC) son `Double`, el conversor divide y multiplica en coma flotante, y el redondeo ocurre más de una vez al pasar por USD. Además, la representación interna (`amountInMinorUnits: Long`) es pública y la usan directamente el libro de movimientos, el conversor y las pruebas, así que cambiarla obligaría a tocar todo el dominio. Antes de construir `draft-inbox` y las metas, que suman dinero de muchas billeteras, el dinero tiene que ser exacto de punta a punta y estar encapsulado.

## What Changes
- **BREAKING** `Money` encapsula su representación: deja de exponer `amountInMinorUnits`/`amountInCents`. Las únicas formas de "sacar" un monto son las operaciones de dominio y su forma canónica (`"1188.00 BOB"`), que también usará la persistencia.
- **BREAKING** `Money` solo admite operaciones de dominio: `Money + Money` y `Money - Money` en la misma moneda, negación, comparación y signo. Desaparecen la multiplicación por enteros y las conversiones desde y hacia `Double` (`fromDecimal`, `toDecimalDouble`, `fromMajorUnits`).
- **Nuevo** `ExchangeRate`: "X de una moneda equivale a Y de otra", definido por dos montos (`1.00 USD = 11.88 BOB`, o `50.00 USD = 594.00 BOB`). Permite `inverse()` y la composición exacta `rate1 * rate2` (BTC→USD por USD→BOB = BTC→BOB).
- **Nuevo** `Money * ExchangeRate`: la única forma de cruzar monedas. Es exacta hasta el final y redondea una sola vez, al centavo (o satoshi) de la moneda destino, con half-even.
- **Desbordamiento explícito**: si una suma, una conversión o una composición no cabe en la representación, la operación falla con un error en lugar de devolver un número equivocado.
- **BREAKING** Las tasas del dominio dejan de ser `Double`: las estrategias, el selector guardado, el sello del proveedor y el desglose del consolidado usan `ExchangeRate`. Los extractores JSON y HTML devuelven la cotización como texto decimal canónico y se convierte a `ExchangeRate` sin pasar por `Double`.
- La tasa derivada de una transferencia (`Transfer.derivedRate()`) pasa a ser un `ExchangeRate` en lugar de un `Money`.

### Non-Goals
- Enteros de precisión arbitraria: la representación sigue siendo `Long` con desbordamiento explícito. Al quedar encapsulada, se podrá reemplazar más adelante sin tocar al resto del dominio.
- Obtener precios en vivo de BTC (CoinGecko) o de Frankfurter: siguen inyectados como hoy, solo cambia su tipo.
- Repartir un monto en cuotas (`allocate`): llegará con `commitments`.
- Formato regional de montos y tasas (presentación).

## Capabilities

### New Capabilities
<!-- Ninguna: la aritmética monetaria y la conversión pertenecen a currency-configuration. -->

### Modified Capabilities
- `currency-configuration`: se agregan los requisitos de aritmética monetaria segura (solo misma moneda, desbordamiento explícito) y de conversión exacta mediante tasas de cambio con redondeo único half-even.

## Impact
- **Código**: `Money`, `ExchangeRate` (nuevo), `ExchangeRateStrategy`, `SavedRateSelector`, `CurrencyConverter`, `ProviderStamp`, `PortfolioHoldingItem`, los extractores, `GetConsolidatedPortfolioUseCase`, `ConfirmWebSelectorUseCase`, `LedgerEvent.Transfer`, `LedgerProjection` y `LedgerGuard`.
- **Pruebas**: casi todas cambian su forma (montos creados con unidades mínimas o forma canónica y comparados como `Money`), pero los valores esperados de las 5 personas se conservan.
- **Sin dependencias nuevas**: solo stdlib.
