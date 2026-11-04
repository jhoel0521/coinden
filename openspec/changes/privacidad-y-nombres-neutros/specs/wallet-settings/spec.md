# Spec Delta

## ADDED Requirements

### Requirement: Nombres Neutros para los Tipos de Billetera
La app DEBE (MUST) mostrar los tipos de billetera con nombres de registro y no de producto financiero: Efectivo, Cuenta, Digital, Personalizada y Tarjeta (en español), con su equivalente en inglés y portugués. Cambiar los nombres no cambia el comportamiento: Tarjeta sigue admitiendo saldo inicial negativo, y la moneda y el saldo siguen siendo los mismos.

#### Scenario: Tipos al crear una billetera
- **WHEN** Carlos abre el formulario para agregar una billetera
- **THEN** ve los tipos Efectivo, Cuenta, Digital, Personalizada y Tarjeta, y no ve "Banco", "Cripto" ni "Tarjeta de crédito"

#### Scenario: Una billetera existente conserva su tipo
- **WHEN** Carlos actualiza la app y tenía una billetera de tipo banco
- **THEN** su billetera aparece como Cuenta con el mismo saldo y los mismos movimientos

#### Scenario: Tarjeta admite deuda inicial
- **WHEN** Facundo agrega una tarjeta con saldo inicial -25.00 ARS
- **THEN** se conserva la deuda inicial exacta, igual que antes del cambio de nombre

#### Scenario: Nombres en los tres idiomas
- **WHEN** Ana cambia la app a portugués y abre el formulario de billetera
- **THEN** los tipos aparecen como Dinheiro, Conta, Digital, Personalizada y Cartão

### Requirement: Movimiento entre Billeteras con Nombre Neutro
El movimiento que lleva dinero de una billetera a otra de la persona DEBE (MUST) mostrarse como "Traspaso" (en inglés "Move", en portugués "Repasse") y no como "Transferencia", sin cambiar sus montos, su tasa ni su efecto en los saldos.

#### Scenario: Traspaso entre monedas
- **WHEN** Carlos pasa 50.00 USD de "Meru" a "BNB Bs" y recibe 594.00 Bs
- **THEN** el historial lo muestra como Traspaso, con los mismos 50.00 USD y 594.00 Bs, y los saldos cambian como antes
