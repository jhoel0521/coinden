# Spec Delta

## REMOVED Requirements

### Requirement: Resultado de la Captura y Confirmación Inmediata
**Reason**: El requisito creció al sumarle la pregunta "¿Es el mismo gasto?" y superó el largo recomendado. Se divide en dos requisitos sin cambiar el comportamiento.
**Migration**: Ver "Resultado de la Captura" (guardado, tarjeta y confirmación inmediata) y "Pregunta de Mismo Gasto en el Resultado" en `capture-ui`; conservan los seis escenarios.

## ADDED Requirements

### Requirement: Resultado de la Captura
Después de cada captura la app DEBE (MUST) indicar que se guardó como borrador, mostrar su tarjeta (con la referencia, si la hay) y ofrecer confirmarlo en ese momento, con la categoría sugerida si la hay, o dejarlo para la revisión. Confirmar aplica las reglas del libro; si el libro lo rechaza, el borrador sigue pendiente y se muestra el motivo.

#### Scenario: Confirmar ahora
- **WHEN** Carlos captura "almuerzo 45 bnb" y toca Confirmar
- **THEN** el gasto de 45 Bs se registra en "BNB Bs" clasificado como comida y el saldo de Inicio baja 45 Bs

#### Scenario: Dejar para la noche
- **WHEN** Carlos captura un gasto y vuelve a Inicio sin confirmar
- **THEN** el borrador queda pendiente y los saldos no cambian

#### Scenario: Confirmación rechazada
- **WHEN** Carlos confirma un gasto de 45 Bs en "Billetera", que solo tiene 10 Bs
- **THEN** el borrador sigue pendiente y la tarjeta muestra que faltan 35 Bs

### Requirement: Pregunta de Mismo Gasto en el Resultado
Si la captura puede ser el mismo movimiento que otra, la tarjeta del resultado DEBE (MUST) preguntar "¿Es el mismo gasto?" mostrando la otra captura (nota, monto, hora y billetera) con los botones "Son el mismo" y "Son distintos".

#### Scenario: Preguntar en el momento
- **WHEN** Carlos comparte el comprobante de 10.00 Bs después de haber dictado "gasté 10 en power" ese día
- **THEN** la tarjeta pregunta si es el mismo gasto que "power", con "Son el mismo" y "Son distintos"

#### Scenario: Responder Son el mismo
- **WHEN** Carlos toca "Son el mismo"
- **THEN** la tarjeta pasa a mostrar el borrador unido y Inicio cuenta un borrador pendiente menos

#### Scenario: Responder Son distintos
- **WHEN** Carlos toca "Son distintos"
- **THEN** la pregunta desaparece y quedan los dos borradores
