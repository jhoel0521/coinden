## MODIFIED Requirements

### Requirement: Bandeja de Revisión
La app DEBE (MUST) ofrecer en la pestaña Revisar una bandeja con los saldos proyectados de las billeteras afectadas, avisando cuándo una quedaría por debajo de cero. Debajo muestra dos secciones: "Revisar" y "Listos". Cada borrador muestra su nota, monto, billetera, hora, categoría sugerida, confianza y si tiene evidencia. Sin pendientes, la bandeja lo dice.

#### Scenario: Abrir la bandeja
- **WHEN** Carlos tiene 3 borradores pendientes y toca la pestaña Revisar
- **THEN** ve la bandeja con los 3 borradores repartidos entre "Revisar" y "Listos"

#### Scenario: Faltante proyectado
- **WHEN** "Billetera" tiene 10.00 Bs y hay borradores Listos por 3.00 Bs y 45.00 Bs en ella
- **THEN** la bandeja muestra "Billetera" con -38.00 Bs y un aviso de faltante

#### Scenario: Nada pendiente
- **WHEN** Carlos abre la bandeja sin borradores pendientes
- **THEN** la bandeja indica que no hay nada para revisar

### Requirement: Historial de Movimientos con Corrección
La app DEBE (MUST) ofrecer en la pestaña Movimientos un historial de los movimientos confirmados, del más reciente al más antiguo, con su nota, monto, billetera, fecha, categoría, referencia y evidencia. Desde cada uno, Corregir abre el formulario con sus datos y los cambios se resaltan antes de confirmar. Los movimientos anulados no aparecen como vigentes.

#### Scenario: Corregir a mano
- **WHEN** Carlos abre el almuerzo de 35.00 Bs en el historial, toca Corregir, cambia el monto a 32.00 Bs y confirma
- **THEN** el historial muestra el almuerzo de 32.00 Bs y el saldo de "BNB Bs" sube 3.00 Bs

#### Scenario: Salir sin confirmar
- **WHEN** Carlos toca Corregir en el taxi, cambia la nota y sale sin confirmar
- **THEN** el taxi no cambia

#### Scenario: Movimiento anulado
- **WHEN** el almuerzo de 35.00 Bs fue corregido a 32.00 Bs
- **THEN** el historial muestra solo el de 32.00 Bs como vigente
