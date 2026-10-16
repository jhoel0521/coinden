# Spec Delta

## MODIFIED Requirements

### Requirement: Pregunta de Mismo Gasto en el Resultado
Si la captura puede ser el mismo movimiento que otra, la tarjeta del resultado DEBE (MUST) preguntar "¿Es el mismo gasto?" mostrando la otra captura (nota, monto, hora y billetera) con los botones "Son el mismo" y "Son distintos". Si la otra captura ya está confirmada y la nueva es un comprobante real, "Son el mismo" abre el formulario de corrección con los cambios resaltados.

#### Scenario: Preguntar en el momento
- **WHEN** Carlos comparte el comprobante de 10.00 Bs después de haber dictado "gasté 10 en power" ese día
- **THEN** la tarjeta pregunta si es el mismo gasto que "power", con "Son el mismo" y "Son distintos"

#### Scenario: Responder Son el mismo
- **WHEN** Carlos toca "Son el mismo"
- **THEN** la tarjeta pasa a mostrar el borrador unido y Inicio cuenta un borrador pendiente menos

#### Scenario: Responder Son distintos
- **WHEN** Carlos toca "Son distintos"
- **THEN** la pregunta desaparece y quedan los dos borradores

#### Scenario: Son el mismo que un gasto confirmado
- **WHEN** Carlos confirmó en la mañana el taxi de 40.00 Bs, en la tarde toma la foto de su comprobante y toca "Son el mismo"
- **THEN** se abre el formulario de corrección del taxi con la referencia del comprobante resaltada y nada cambia hasta que Carlos confirma
