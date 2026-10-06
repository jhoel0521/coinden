# Design: Núcleo Hexagonal de Dominio CoinDen

## Context
Véase `proposal.md` para la motivación. CoinDen arranca como un proyecto Kotlin Multiplatform (KMP). La regla de oro arquitectónica es que el núcleo de negocio (`core/domain`) debe compilarse en cualquier plataforma (JVM, Android, iOS, Wasm) sin depender de AndroidX, Room, UIKit ni librerías externas que aten las entidades a un sistema operativo.

## Goals / Non-Goals

**Goals:**
- Definir Value Objects inmutables: `Money` (monto y moneda segura), `Currency`, `WalletId`, `ExpenseId`.
- Definir Entidades de dominio: `Wallet` (cartera) y `Expense` (gasto con estados Draft y Confirmed).
- Definir Puertos de Entrada (Casos de Uso): `CreateWalletUseCase`, `SaveDraftExpenseUseCase`.
- Definir Puertos de Salida: `WalletRepositoryPort`, `ExpenseRepositoryPort`.
- Pruebas unitarias de dominio puras que verifiquen reglas como deduplicación y montos válidos.

**Non-Goals:**
- Implementación de adaptadores de base de datos Room/SQLCipher (se abordará en el siguiente cambio).
- Implementación de UI Jetpack Compose (se abordará en cambios posteriores).
- Integración de modelos de IA u OCR en este paso.

## Decisions

### Decisión 1: Representación Monetaria (Long en céntimos vs Double)
* **Elección:** Representar el dinero internamente con un valor entero (`Long amountInCents`) y un divisor de escala (`decimals`), empaquetado en el Value Object `Money`.
* **Alternativa descartada:** Usar `Double` o `Float`. Rechazado por los clásicos errores de redondeo de punto flotante en cálculos contables.
* **Alternativa descartada:** `java.math.BigDecimal`. Rechazado en el dominio base porque no es 100% nativo multiplataforma en todos los targets sin dependencias externas.

### Decisión 2: Arquitectura Hexagonal Pura (Puertos y Adaptadores)
* **Elección:** El caso de uso (`SaveDraftExpenseUseCase`) es una interfaz de entrada (Inbound Port) implementada por un `Interactor` que solo interactúa con el dominio y con interfaces de salida (Outbound Ports / SPI).
* **Beneficio para desarrollador PHP:** Equivale exactamente a la separación de capas en Clean Architecture / DDD (Entities -> Use Cases -> Repositories). Facilita pruebas con dobles de prueba (`fakes` o `mocks`) sin levantar ningún emulador.

### Decisión 3: Huella Única para Deduplicación de Transacciones
* **Elección:** Cada gasto o movimiento calcula una propiedad calculada `deduplicationFingerprint = sha256(walletId + date + amount + referenceCode)`.
* **Razón:** Permite que cualquier adaptador (cámara, OCR, portapapeles o ingreso manual) verifique en tiempo real si el movimiento ya fue registrado antes de persistirlo.

## Risks / Trade-offs
- [Riesgo: Complejidad inicial de boilerplate al usar Hexagonal estricto] → Mitigación: Mantiene el código 100% blindado contra cambios de base de datos o interfaces futuras (Android, iOS y Web consumen exactamente el mismo caso de uso).
