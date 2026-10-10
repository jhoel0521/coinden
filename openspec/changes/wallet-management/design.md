# Design: Billeteras y Libro de Movimientos

## Context
`core/domain` ya tiene `Money` (unidades mínimas `Long`, con operaciones que exigen la misma moneda) y `Currency` (0 a 8 decimales). La vista consolidada (`GetConsolidatedPortfolioUseCase`) recibe un `List<Money>` armado a mano. No hay identificadores, reloj ni concepto de dispositivo en el dominio, y los resultados de negocio ya usan tipos sellados (`SelectorConfirmationResult`). La motivación está en proposal.md y los requisitos en `specs/wallet-management/spec.md`.

## Goals / Non-Goals

**Goals:**
- Un único modelo de eventos de libro del que se proyectan saldo, historial, validación cronológica y estado descuadrado.
- Pruebas 100 % deterministas: identificadores, hora y dispositivo se inyectan.
- Errores de negocio como tipos, sin textos de idioma.

**Non-Goals:**
- Resolver conflictos de metadatos de billetera entre dispositivos (nombre, archivado): eso queda para `device-sync`.
- Cambiar la firma de `GetConsolidatedPortfolioUseCase`.

## Decisions

### D1. El libro es una secuencia de eventos; el saldo es una proyección
`LedgerEvent` es una jerarquía sellada: `OpeningBalance`, `Adjustment`, `Expense`, `Income`, `Transfer` y `Annulment`. Todos llevan `id`, `deviceId`, `occurredAtEpochMs` y `recordedAtEpochMs`. Saldo, historial y validación salen de una sola función pura, `LedgerProjection`, que recibe los eventos que tocan una billetera.
- *Alternativa descartada:* guardar `balance` en `Wallet` y actualizarlo. Se puede desincronizar, no sirve para fusionar dispositivos y contradice "el saldo no se almacena".

### D2. La transferencia es UN evento con dos patas
`Transfer(fromWalletId, toWalletId, amountOut: Money, amountIn: Money)`. Cada billetera proyecta su pata (−`amountOut` en el origen, +`amountIn` en el destino). Anular la transferencia es un solo `Annulment(targetId)` que neutraliza ambas patas a la vez.
- *Alternativa descartada:* dos movimientos enlazados por `transferId`. Se podría anular un lado sin el otro y la fusión podría traer uno solo.

### D3. La tasa derivada es aritmética entera, no `Double`
`Transfer.derivedRate()` devuelve un `Money` en la moneda de destino por **una unidad mayor** de la moneda de origen: `amountIn.minor × 10^decimalsOut / amountOut.minor`, con redondeo half-even. Ejemplo: 59400 × 100 / 5000 = 1188, es decir 11.88 Bs por USD. Se calcula a demanda y nunca se persiste.

### D4. La anulación actúa en la fecha del original
Un movimiento anulado deja de contar en **su propio** punto cronológico, como si no hubiera ocurrido. Así la validación cronológica detecta que anular un ingreso del que dependen gastos posteriores deja un hueco negativo. El evento `Annulment` conserva sus propias fechas para auditoría.
- *Alternativa descartada:* que la anulación aplique un contra-movimiento en su propia fecha. Esconde huecos históricos.

### D5. Validación cronológica: proyectar el libro candidato
Para registrar un evento nuevo, el caso de uso carga los eventos de las billeteras afectadas, agrega el candidato, ordena por (`occurredAt`, `recordedAt`, `id`) y calcula el saldo acumulado. Si la billetera no admite negativos y el candidato **empeora** la proyección (el mínimo baja o el primer negativo se adelanta respecto del libro sin candidato), devuelve `Rejected(InsufficientFunds(walletId, shortfall: Money, firstNegativeAtEpochMs))` y no persiste nada. La misma proyección sobre el libro real, sin candidato, da el estado `DESCUADRADA`. Comparar contra el libro sin candidato evita que un hueco previo (por ejemplo, de una fusión) bloquee todos los movimientos, incluido el ajuste que lo corrige.
- Costo: O(n log n) por billetera en cada registro. Es aceptable en un dispositivo; si crece, en el futuro se pueden agregar snapshots por periodo sin cambiar la regla.

### D6. Puertos nuevos para el mundo exterior
- `WalletRepositoryPort`: `saveWallet`, `getWallet`, `listWallets(ownerId)`, `appendEvent` (solo agrega, nunca actualiza ni borra) y `eventsForWallet(walletId)`.
- `IdGeneratorPort`: `newId(): String` (UUID en producción y secuencial en pruebas).
- `ClockPort`: `nowEpochMs()`.
- `DeviceIdentityPort`: `deviceId(): String`.

Se usan `Long` epoch ms, como `asOfEpochMs` en currency-hub, para no sumar `kotlinx-datetime` en este cambio.
- *Alternativa descartada:* `kotlin.uuid.Uuid`. Es experimental en Kotlin 2.0 y además ataría las pruebas a valores aleatorios.

### D7. Resultados tipados sin texto
Los casos de uso devuelven `WalletOperationResult` sellado: `Accepted(event)` o `Rejected(reason: WalletError)`. `WalletError` es un sellado sin mensajes: `InvalidName`, `WalletArchived`, `CurrencyMismatch`, `InvalidAmount`, `SameWallet`, `InconsistentTransferAmounts`, `AlreadyAnnulled`, `CannotAnnulAnnulment`, `InsufficientFunds(...)` y `NotFound`. Los invariantes estructurales de los value objects siguen usando `require`, como `Money` y `Currency`.

### D8. Metadatos de billetera como estado, no como eventos
`Wallet(id, ownerId, name, currency, type, allowsNegative, status)` se guarda como estado actual (`saveWallet`). Archivar o reactivar reemplaza el estado. Solo el dinero es append-only; los metadatos no mueven dinero, y su fusión entre dispositivos se resolverá en `device-sync`.

### D9. Integración con la vista consolidada sin romperla
`GetActiveWalletBalancesUseCase(ownerId)` devuelve `List<WalletBalance(walletId, balance: Money)>` de las billeteras activas. La prueba de feature compone `balances.map { it.balance }` como `holdings` de `GetConsolidatedPortfolioUseCase`, que no cambia.

### Organización del código
```
model/wallet/      Wallet, WalletType, WalletStatus, LedgerEvent, LedgerProjection,
                   WalletBalance, WalletError, WalletOperationResult
port/output/       WalletRepositoryPort, IdGeneratorPort, ClockPort, DeviceIdentityPort
usecase/wallet/    CreateWallet, ArchiveWallet, ReactivateWallet, RegisterMovement,
                   RegisterTransfer, AnnulMovement, GetWalletStatus,
                   GetActiveWalletBalances
test unit/         LedgerProjectionTest, TransferTest, WalletTest
test feature/      CarlosWalletsFeatureTest, JohnSatoshiWalletFeatureTest,
                   FacundoCreditCardFeatureTest + InMemoryWalletRepository y fakes
```

## Risks / Trade-offs
- [Validación sobre todo el historial en cada registro] → En un dispositivo personal son cientos o miles de eventos. Se mide en las pruebas y, si hace falta, se agregan snapshots sin cambiar la regla.
- [Un registro con fecha pasada puede rechazarse por huecos que el usuario no ve] → El rechazo trae `firstNegativeAtEpochMs` y `shortfall` para que la UI muestre exactamente dónde falta dinero.
- [Metadatos con último-gana en el futuro] → Un archivado en la PC y un renombrado en el celular podrían pisarse. No afecta al dinero; se resuelve en `device-sync`.
- [La tasa derivada redondea] → Solo afecta lo que se *muestra*. Los montos guardados son exactos y la tasa siempre se puede recalcular.

## Migration Plan
No aplica: no hay datos persistidos ni consumidores externos. El cambio es aditivo y `currency-configuration` no se modifica.

## Open Questions
- ¿Se podrá cambiar la política de saldo negativo de una billetera después de crearla? Por ahora se fija al crearla. Permitir cambiarla solo exigiría revalidar el libro, sin alterar estos specs.
