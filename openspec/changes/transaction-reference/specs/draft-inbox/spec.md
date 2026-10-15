# Spec Delta

## ADDED Requirements

### Requirement: Referencia de la Transacción en el Borrador
El borrador DEBE (MUST) conservar el número de transacción opcional de su propuesta en cada revisión, y al confirmarse el movimiento confirmado DEBE guardarlo junto con el resto de sus datos. La referencia se guarda tal como se leyó o escribió; sin referencia el borrador funciona igual y la referencia nunca bloquea que esté Listo.

#### Scenario: Comprobante confirmado con su número
- **WHEN** Carlos confirma el borrador del pago QR a "POLLOS DEL VALLE" con la referencia "400000001"
- **THEN** el movimiento confirmado guarda la referencia "400000001"

#### Scenario: Efectivo sin referencia
- **WHEN** Carlos confirma "page 3 bs para trasmporte" pagado en efectivo
- **THEN** el movimiento se confirma sin referencia

#### Scenario: Corregir otro dato no borra la referencia
- **WHEN** Carlos corrige la nota del borrador con referencia "400000001"
- **THEN** el borrador conserva la referencia "400000001"

#### Scenario: Datos guardados antes de la referencia
- **WHEN** la app lee un borrador guardado antes de que existiera la referencia
- **THEN** el borrador se lee igual, sin referencia
