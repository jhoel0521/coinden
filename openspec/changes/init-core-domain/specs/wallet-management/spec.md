# Spec Delta

## Purpose
Proporciona la gestión autónoma y local de múltiples carteras de dinero (efectivo, cuentas bancarias y billeteras digitales) con sus respectivas divisas sin depender de servidores externos.

## ADDED Requirements

### Requirement: Wallet Creation
The system MUST allow users to create a new wallet specifying name, type, and default currency.

#### Scenario: Creación exitosa de cartera de efectivo
- **WHEN** el usuario crea una cartera con nombre "Bolsillo Diario", tipo "Efectivo" y moneda "BOB"
- **THEN** la cartera se guarda localmente con saldo inicial de 0 y un identificador único

### Requirement: Multi-Currency Support
The system MUST support monetary amounts in ISO currencies (BOB, USD, EUR) with exact decimal precision.

#### Scenario: Asignación de divisa a una cuenta
- **WHEN** se crea una cuenta con código de moneda "USD"
- **THEN** todas las operaciones de saldo de esa cuenta preservan la divisa "USD"
