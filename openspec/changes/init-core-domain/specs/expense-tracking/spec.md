# Spec Delta

## Purpose
Permite el registro, almacenamiento preliminar (borrador) y validación de deduplicación de gastos y transacciones monetarias de forma 100% local en el dispositivo.

## ADDED Requirements

### Requirement: Save Draft Expense
The system MUST allow registering a preliminary draft expense without biometric auth, associated with an existing wallet.

#### Scenario: Registro inmediato de gasto borrador
- **WHEN** el usuario registra un gasto con monto 35.50 BOB en la cartera "Efectivo" con descripción "Almuerzo"
- **THEN** el gasto queda registrado en estado "BORRADOR" con fecha actual y disponible para su clasificación posterior

### Requirement: Transaction Deduplication
The system MUST compute a unique transaction fingerprint to prevent duplicate records.

#### Scenario: Intento de registro duplicado
- **WHEN** se intenta registrar un gasto que contiene la misma referencia bancaria y monto de una transacción existente en la misma cartera
- **THEN** el sistema rechaza la inserción indicando que la transacción ya se encuentra registrada
