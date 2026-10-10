# Tasks: Billeteras y Libro de Movimientos (TDD: primero la prueba)

## 1. Puertos y adaptadores de prueba

- [x] 1.1 Definir `WalletRepositoryPort` (`saveWallet`, `getWallet`, `listWallets`, `appendEvent`, `eventsForWallet`), `IdGeneratorPort`, `ClockPort` y `DeviceIdentityPort` en `port/output/`; verificar que `./gradlew :core:domain:compileKotlinJvm` compila.
- [x] 1.2 Implementar en `commonTest/.../feature/` los fakes `InMemoryWalletRepository` (solo agrega eventos), `SequentialIdGenerator`, `FixedClock` (ajustable) y `FixedDevice`; verificar con una prueba mínima que `appendEvent` nunca reemplaza un evento existente.

## 2. Modelo de billetera

- [x] 2.1 Escribir `WalletTest` (nombre vacío inválido, política de negativos por defecto según `WalletType`: CREDITO sí, el resto no, y transición ACTIVA ↔ ARCHIVADA) y luego implementar `Wallet`, `WalletType` y `WalletStatus` en `model/wallet/` hasta que pase.
- [x] 2.2 Definir `WalletError` y `WalletOperationResult` sellados, sin textos de idioma; verificar con `grep` que `model/wallet/` no contiene strings de usuario.

## 3. Eventos del libro y proyección

- [x] 3.1 Escribir pruebas de `LedgerEvent` (montos estrictamente positivos en GASTO/INGRESO/transferencia, AJUSTE distinto de cero, transferencia con la misma billetera o con montos distintos en la misma moneda rechazada) e implementar la jerarquía sellada hasta que pasen.
- [x] 3.2 Escribir `TransferTest` para `derivedRate()`: 50.00 USD → 594.00 BOB da 11.88 BOB; BTC (8 decimales) → USD con redondeo half-even. Implementar con aritmética entera hasta que pase.
- [x] 3.3 Escribir `LedgerProjectionTest`: saldo como suma de eventos vigentes, transferencia proyectada por pata, anulación neutralizada en la fecha del original, orden (`occurredAt`, `recordedAt`, `id`), mínimo acumulado y primer momento negativo. Implementar `LedgerProjection` hasta que pase.

## 4. Casos de uso

- [x] 4.1 Implementar `CreateWalletUseCase` (billetera + `OpeningBalance` con id, dispositivo y fechas inyectadas) con pruebas de los escenarios de creación de Carlos (BOB) y John (BTC en satoshis).
- [x] 4.2 Implementar `ArchiveWalletUseCase` y `ReactivateWalletUseCase` con pruebas: el historial se conserva, una billetera archivada rechaza movimientos y una reactivada mantiene su saldo.
- [x] 4.3 Implementar `RegisterMovementUseCase` (GASTO/INGRESO/AJUSTE) con validación de moneda, monto, estado y control cronológico (`InsufficientFunds` con faltante y fecha); verificar los escenarios "olvidó el cajero" y "fecha pasada con hueco negativo".
- [x] 4.4 Implementar `RegisterTransferUseCase` validando el control cronológico en la billetera de origen; verificar los escenarios de venta al paralelo y retiro de cajero.
- [x] 4.5 Implementar `AnnulMovementUseCase` (doble anulación, anular una anulación, transferencia anulada en ambos lados, anulación de un ingreso que deja hueco rechazada); verificar con sus escenarios.
- [x] 4.6 Implementar `GetWalletStatusUseCase` (saldo + ACTIVA/ARCHIVADA + DESCUADRADA con faltante) y `GetActiveWalletBalancesUseCase`; verificar el escenario de fusión desde otro dispositivo insertando eventos directamente en el repositorio.

## 5. Pruebas de feature por persona

- [x] 5.1 `CarlosWalletsFeatureTest`: Billetera, BNB Bs, Dólares colchón; gasto rechazado → retiro de cajero → gasto aceptado; venta de 50 USD a 594 Bs; corrección por anulación; saldos activos usados como `holdings` en `GetConsolidatedPortfolioUseCase`. Verificar que pasa.
- [x] 5.2 `JohnSatoshiWalletFeatureTest`: billetera BTC, transferencia de 100,000 satoshis exactos entre dos billeteras CRIPTO y venta de BTC a USD con tasa derivada. Verificar que pasa.
- [x] 5.3 `FacundoCreditCardFeatureTest`: tarjeta de crédito en ARS con saldo negativo aceptado y pago de la tarjeta desde Banco Galicia por transferencia. Verificar que pasa.

## 6. Verificación integral

- [x] 6.1 Ejecutar `npm test` y `npm run lint` con todo en verde, y comprobar que las 33 pruebas previas de currency-hub siguen pasando.

## Workflow follow-up

- Archivar `wallet-management` con `opsx-archive` tras la revisión.
- Siguientes cambios del roadmap: `domain-i18n-cleanup` y luego `draft-inbox`.
