# Tasks

## 1. Modelado de Dominio y Value Objects

- [ ] 1.1 Definir Value Objects inmutables `Currency` (códigos ISO como BOB, USD) y `Money` (cálculos seguros en céntimos sin coma flotante) y verificar con pruebas unitarias de suma y formato.
- [ ] 1.2 Definir Entidades del Dominio `Wallet` (cuenta/cartera) y `Expense` (gasto con estados borrador y cálculo de fingerprint de deduplicación) y verificar su inmutabilidad.

## 2. Puertos Hexagonales (Inbound & Outbound Ports)

- [ ] 2.1 Definir interfaces de puertos de salida (SPI): `WalletRepositoryPort` y `ExpenseRepositoryPort` con operaciones reactivas y asíncronas puras.
- [ ] 2.2 Definir contratos de casos de uso (puertos de entrada): `CreateWalletUseCase` y `SaveDraftExpenseUseCase`.

## 3. Implementación de Casos de Uso y Pruebas Unitarias

- [ ] 3.1 Implementar `CreateWalletInteractor` validando nombres válidos y saldos iniciales coherentes.
- [ ] 3.2 Implementar `SaveDraftExpenseInteractor` asegurando que transacciones duplicadas sean rechazadas antes del guardado.
- [ ] 3.3 Crear repositorio en memoria (`FakeExpenseRepository`, `FakeWalletRepository`) y verificar que todos los escenarios descritos en los specs pasan limpiamente.

## Workflow follow-up

- Archivar el cambio `init-core-domain` tras la aprobación y verificación completa.
- Proceder al cambio `room-sqlcipher-persistence` en la siguiente fase.
