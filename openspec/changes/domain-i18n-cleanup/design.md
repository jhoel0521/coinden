# Design: Dominio sin Idioma

## Context
Ver proposal.md (Why). Hoy hay textos de idioma en cuatro lugares: `ProviderStamp.sourceName`, que `GetConsolidatedPortfolioUseCase` arma en español; `Currency.name` y `Currency.symbol` en el catálogo predefinido; `RateSourceDefinition.name` y `description` en `VerifiedSourceDirectory`; y `Money.formatted()`, junto con `Money.fromFormatted()`, que acepta coma o punto. `wallet-management` ya nació sin textos y sirve de referencia (enums y errores tipados).

## Goals / Non-Goals

**Goals:**
- Ningún string pensado para el usuario en `core/domain/src/commonMain`, salvo los datos que escribe el usuario.
- Que cada cambio incompatible se refleje en una prueba que lo fije.

**Non-Goals:**
- Tasas exactas (`Double` → aritmética entera): cambio aparte, `exact-rates`.
- Utilidades de formato por locale: pertenecen a la futura capa de presentación.

## Decisions

### D1. `ProviderStamp` estructurado
```
enum RateSourceKind { BASE, FIXED, MANUAL, VERIFIED, WEB_SELECTOR }
ProviderStamp(kind, reference: String?, exchangeRateToUsd, asOfEpochMs, isDegradedFallback)
```
`reference` lleva el `sourceId` en VERIFIED y la URL en WEB_SELECTOR, y queda `null` en los demás. La moneda local de FIXED ya se conoce por el `targetCurrency` del consolidado, así que no se repite.
- *Alternativa descartada:* una clave de traducción (`"stamp.fixed"`). Mete una convención de UI dentro del dominio; un enum tipado es más seguro y exhaustivo en `when`.

### D2. `Currency`: nombre y símbolo opcionales, solo para monedas personalizadas
`symbol: String? = null` y `name: String? = null`. El catálogo predefinido pasa a ser solo `Currency(code, decimals)`. `Currency.custom(...)` sigue exigiendo símbolo y nombre (datos del usuario). En `Currency.of(...)` los parámetros `symbol` y `name` se mantienen para crear monedas ad hoc.
- *Alternativa descartada:* conservar los símbolos ISO ("$", "Bs") porque "no son idioma". Sí son regionales ("$" es USD, ARS, CLP o COP según el país, y "US$" en Bolivia). La presentación los obtiene del locale (datos CLDR).

### D3. Catálogo de fuentes con nombre propio y sin descripción
`RateSourceDefinition` pierde `description`, y `name` pasa a ser el nombre propio del proveedor ("DolarApi", "DolarBlueBolivia", "Frankfurter", "CoinGecko"), que no se traduce. Lo que la fuente cotiza (blue, paralelo, oficial) ya está en datos: `countryIso`, `targetCurrency` y `id`.

### D4. `Money`: solo representación canónica
- Se elimina `formatted()`.
- `toString()` y `toNumericString()` se mantienen y se documentan como **canónicos** (punto decimal, sin separadores de miles, código ISO).
- Se elimina `toFormattedString()`: era un alias de `toString()`, y las pruebas pasan a usar `toString()`.
- `fromFormatted()` se renombra a `parseCanonical()`: acepta un signo opcional, dígitos y, opcionalmente, un punto con hasta `decimals` dígitos; cualquier otro carácter, incluida la coma, lanza `IllegalArgumentException`. Antes truncaba decimales sobrantes; ahora un exceso de decimales también se rechaza, porque truncar dinero en silencio es un error.
- `fromDecimal(Double)` se mantiene porque lo usa el conversor; desaparece en `exact-rates`.

### D5. Mensajes de `require` fuera del alcance
Son mensajes para desarrolladores y nunca llegan a la UI. Los errores de negocio ya son tipados (wallet) o lo serán al tocar cada caso de uso.

### D6. Fallos del selector web tipados
`SelectorConfirmationResult.Failure` deja de llevar una frase en español y pasa a llevar `SelectorFailure { FETCH_FAILED, NO_VALID_RATE }`, siguiendo el patrón de `WalletError`. El detalle técnico de la excepción de red no se expone: no es para el usuario y solo serviría para diagnóstico.

## Risks / Trade-offs
- [Muchos cambios incompatibles a la vez] → No hay consumidores externos. La suite completa (80 pruebas) actúa como red: cada prueba que comparaba textos se reescribe para comparar datos.
- [Rechazar decimales de más puede romper flujos futuros] → Es intencional. El parser de borradores (draft-inbox) normalizará la entrada del usuario antes de crear un `Money`.

## Migration Plan
No aplica: no hay datos persistidos ni UI.
