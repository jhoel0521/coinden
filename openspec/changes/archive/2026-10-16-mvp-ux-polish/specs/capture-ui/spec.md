## ADDED Requirements

### Requirement: Acciones del Formulario sin Duplicados
La app DEBE (MUST) ejecutar una sola acción de escritura a la vez por formulario, mostrar progreso y deshabilitar acciones incompatibles. Guardar repetidamente el mismo formulario nuevo no DEBE crear capturas adicionales. Un fallo recuperable DEBE mantener los campos, explicar el fallo y permitir reintentar sobre la captura ya creada, si existe.

#### Scenario: Doble toque al guardar
- **WHEN** Carlos toca Guardar dos veces mientras se escribe su gasto
- **THEN** se crea un único borrador, se informa el progreso y el saldo no cambia

#### Scenario: Reintento después de crear la captura
- **WHEN** falla la revisión o preparación del resultado después de crear la captura de Carlos y él reintenta
- **THEN** se reutiliza el borrador creado, sin otra captura, y se mantienen los campos

#### Scenario: Fallo antes de crear la captura
- **WHEN** falla el almacenamiento antes de guardar el nuevo borrador
- **THEN** el formulario conserva sus campos, muestra el error y permite reintentar sin anunciar éxito

#### Scenario: Acciones incompatibles durante confirmación
- **WHEN** Sophie confirma desde revisión o corrección y vuelve a tocar una acción de escritura
- **THEN** solo se ejecuta la primera acción y no se navega dos veces

### Requirement: Salida Consciente del Formulario
Al intentar salir mediante el control superior o Atrás del sistema, un formulario con cambios sin guardar DEBE (MUST) ofrecer seguir editando o salir sin guardar. Durante una escritura no DEBE permitir una salida que cancele esa operación. Sin cambios, salir DEBE ser inmediato. Abrir evidencia no cuenta como abandonar la edición.

#### Scenario: Continuar editando
- **WHEN** Carlos modifica un monto e intenta volver y elige seguir editando
- **THEN** permanece en el formulario con todos los campos intactos

#### Scenario: Salir sin guardar
- **WHEN** John modifica una corrección y acepta salir sin guardar
- **THEN** vuelve a la pantalla anterior y el movimiento confirmado no cambia

#### Scenario: Formulario sin cambios
- **WHEN** Carlos abre un formulario y vuelve sin editarlo
- **THEN** vuelve sin diálogo y sin crear un borrador

#### Scenario: Salida durante guardado
- **WHEN** Carlos intenta volver mientras el formulario guarda
- **THEN** permanece visible el progreso hasta que termina la escritura y no se cancela por esa salida

#### Scenario: Consultar evidencia
- **WHEN** Carlos abre la evidencia desde una revisión con campos editados y regresa
- **THEN** conserva la edición sin una advertencia de abandono ni nuevas escrituras
