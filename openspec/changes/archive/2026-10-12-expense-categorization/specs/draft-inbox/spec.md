# Spec Delta

## MODIFIED Requirements

### Requirement: Confirmación con las Reglas del Libro
Confirmar un borrador Listo DEBE (MUST) registrar su movimiento en el libro aplicando todas las reglas de `wallet-management`, con el momento de ocurrencia de la propuesta o, si falta, el de la captura. Si el libro rechaza el movimiento, el borrador sigue Listo con el motivo del rechazo. Solo un borrador Listo puede confirmarse. Al confirmar un gasto, el usuario puede indicar una categoría; el sistema registra la clasificación con la nota del borrador como descripción y, si no se indicó categoría, el gasto queda Sin categoría. La categoría nunca bloquea la confirmación.

#### Scenario: Confirmar el almuerzo
- **WHEN** Carlos confirma a las 21:00 el borrador "almuerzo 45 qr" capturado a las 12:58
- **THEN** el libro registra un gasto de 45.00 Bs en "BNB Bs" ocurrido a las 12:58 y el borrador queda Confirmado con el id del movimiento

#### Scenario: Rechazo por saldo
- **WHEN** Carlos confirma un gasto de 45.00 Bs en "Billetera", que solo tiene 10.00 Bs
- **THEN** el borrador sigue Listo con el motivo de saldo insuficiente (faltan 35.00 Bs) y el libro no cambia

#### Scenario: Confirmar un borrador incompleto
- **WHEN** el usuario intenta confirmar un borrador Incompleto
- **THEN** el sistema rechaza la confirmación indicando los datos faltantes

#### Scenario: Confirmar con categoría
- **WHEN** Carlos confirma el borrador "POLLOS DEL VALLE 25 Bs" eligiendo "comida"
- **THEN** el gasto queda clasificado como "comida" con la descripción "POLLOS DEL VALLE" y nace la regla correspondiente

#### Scenario: Confirmar sin categoría
- **WHEN** Carlos confirma un gasto sin indicar categoría
- **THEN** el gasto se registra igual y queda como Sin categoría

### Requirement: Bandeja de Revisión con Saldo Proyectado
El sistema DEBE (MUST) listar los borradores pendientes de un usuario (Capturado, Incompleto, Listo y No soportado), ordenados de menor a mayor confianza y, a igual confianza, por momento de captura, y, para cada billetera afectada por borradores Listos, el saldo que quedaría al confirmarlos, avisando sin bloquear cuando una billetera que no admite negativos quedaría por debajo de cero. Para cada borrador de gasto la bandeja muestra además la categoría sugerida con su confianza y su motivo, sin que la categoría afecte la confianza del borrador.

#### Scenario: Aviso de faltante proyectado
- **WHEN** "Billetera" tiene 10.00 Bs y hay borradores Listos por 3.00 Bs y 45.00 Bs en ella
- **THEN** la bandeja muestra un saldo proyectado de -38.00 Bs con un aviso de faltante, y los borradores siguen pendientes

#### Scenario: Terminales fuera de la bandeja
- **WHEN** Carlos tiene borradores Confirmados y Descartados
- **THEN** la bandeja no los incluye entre los pendientes

#### Scenario: Lo dudoso primero
- **WHEN** Carlos tiene un borrador de un comprobante con confianza ALTA capturado a las 10:00 y "gaste 2 para un semillita" con confianza BAJA capturado a las 15:00
- **THEN** la bandeja muestra primero el borrador de confianza BAJA

#### Scenario: Categoría sugerida en la bandeja
- **WHEN** Carlos tiene pendiente el borrador "almuerzo 45 qr"
- **THEN** la bandeja lo muestra con la categoría sugerida "comida", confianza MEDIA y el motivo "almuerzo"
