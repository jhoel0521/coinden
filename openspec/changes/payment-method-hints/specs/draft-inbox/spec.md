# Spec Delta

## MODIFIED Requirements

### Requirement: Resolución de Billetera por Id o Alias
El sistema DEBE (MUST) aceptar la billetera de una propuesta por su id o por una pista de texto, y resolver las pistas con alias definidos por el usuario para cada billetera, sin distinguir mayúsculas. Una pista sin coincidencia deja la billetera como faltante.

#### Scenario: Alias definido por el usuario
- **WHEN** Carlos definió el alias "bnb" para "BNB Bs" y una propuesta trae la pista "BNB"
- **THEN** el borrador usa la billetera "BNB Bs"

#### Scenario: Pista desconocida
- **WHEN** una propuesta trae la pista "tigo" y ninguna billetera tiene ese alias
- **THEN** el borrador queda Incompleto con la billetera como faltante y la pista sin resolver indicada

### Requirement: Confirmación con las Reglas del Libro
Confirmar un borrador Listo DEBE (MUST) registrar su movimiento en el libro aplicando todas las reglas de `wallet-management`, con el momento de ocurrencia de la propuesta o, si falta, el de la captura. Si el libro rechaza el movimiento, el borrador sigue Listo con el motivo del rechazo. Solo un borrador Listo puede confirmarse. Al confirmar un gasto, el usuario puede indicar una categoría; el sistema registra la clasificación con la nota del borrador como descripción y, si no se indicó categoría, el gasto queda Sin categoría. La categoría nunca bloquea la confirmación.

#### Scenario: Confirmar el almuerzo
- **WHEN** Carlos confirma a las 21:00 el borrador "almuerzo 45 bnb" capturado a las 12:58
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
