## MODIFIED Requirements

### Requirement: Acceso al Resumen Mensual

La app DEBE (MUST) ofrecer «Gastos del mes» en la pestaña Movimientos. Al entrar muestra el mes actual del teléfono y permite volver al historial sin alterar movimientos ni borradores.

#### Scenario: Abrir desde Inicio
- **WHEN** Carlos está en Inicio y quiere ver sus gastos del mes
- **THEN** toca la pestaña Movimientos y elige «Gastos del mes», porque Inicio ya no repite ese acceso

#### Scenario: Abrir desde Movimientos
- **WHEN** Carlos abre Movimientos y elige «Gastos del mes» el 16 de octubre de 2026
- **THEN** ve el resumen de octubre de 2026 y puede volver al historial
