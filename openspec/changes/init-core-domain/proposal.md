# Proposal: Núcleo Hexagonal y Gestión de Gastos Base

## Why
Para garantizar una aplicación de finanzas 100% privada, sin servidores y multiplataforma (Android, iOS, Web), se requiere un núcleo de dominio estricto desacoplado de cualquier framework. Esto permite desarrollar casos de uso aislados, testeables y seguros desde el primer día, evitando la complejidad prematura de integraciones externas (OCR, WebRTC, IA).

## What Changes
- Creación de la estructura base del proyecto Kotlin Multiplatform (KMP).
- Definición de entidades de dominio puras: `Wallet` (Cartera/Cuenta), `Currency` (Moneda), `Expense` (Gasto), `Money` (Monto y Divisa).
- Puertos de entrada (Casos de uso): `CreateWalletUseCase`, `SaveDraftExpenseUseCase`.
- Puertos de salida (Interfaces SPI): `WalletRepositoryPort`, `ExpenseRepositoryPort`.
- Pruebas unitarias de dominio puras (sin emuladores ni dependencias Android).

## Capabilities

### New Capabilities
- `wallet-management`: Creación, consulta y actualización de carteras (cuentas bancarias, efectivo) y monedas locales.
- `expense-tracking`: Registro básico de gastos y movimientos en estado borrador con soporte multimoneda y deduplicación.

### Modified Capabilities
<!-- No existen capacidades previas -->

## Impact
- **Código nuevo**: Estructura de módulos KMP en `core/domain` y `core/application`.
- **Dependencias**: Kotlin Standard Library y Kotlin Coroutines para pruebas unitarias. Cero dependencias de Android en el dominio.
