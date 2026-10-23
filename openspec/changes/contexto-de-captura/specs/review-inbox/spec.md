## MODIFIED Requirements

### Requirement: Ver la Evidencia
Desde la bandeja, el formulario y el historial, la app DEBE (MUST) mostrar la evidencia de un borrador o movimiento: todas las evidencias de su paquete en el orden en que llegaron, cada una con lo que se leyó de ella: la foto y el PDF a pantalla completa, cada audio con reproducción y su transcripción, y cada nota. Las quitadas se muestran como quitadas. Se descifran solo en memoria y no quedan copias legibles fuera de la app.

#### Scenario: Ver el comprobante
- **WHEN** Carlos toca "Ver comprobante" en el formulario del almuerzo
- **THEN** ve la foto del comprobante a pantalla completa

#### Scenario: Escuchar la nota de voz
- **WHEN** Carlos toca la evidencia de audio de "gasté 40 en taxi"
- **THEN** escucha el audio dentro de la app

#### Scenario: Sin copias legibles
- **WHEN** Carlos cierra el visor de un PDF
- **THEN** no queda ningún archivo descifrado del PDF en el almacenamiento del teléfono

#### Scenario: Comprobante con su audio
- **WHEN** Carlos abre la evidencia del borrador del comprobante QR al que le agregó un audio
- **THEN** ve la imagen con el texto leído y el audio con su reproducción y la transcripción "esto es de mi comida"

#### Scenario: Audio todavía sin transcribir
- **WHEN** Carlos abre la evidencia antes de que la cola transcriba un audio del paquete
- **THEN** el audio se escucha y dice que su texto todavía no se leyó

## ADDED Requirements

### Requirement: Motivo de Revisión del Paquete
Un borrador que el intérprete del paquete dejó para revisar DEBE (MUST) mostrar su motivo en la bandeja y en el formulario: "asignado por orden", "el contexto puede ser de cualquiera de los pagos" o la contradicción con las dos versiones ("dijiste 1500.00 Bs, el comprobante dice 25.00 Bs"). Una contradicción ofrece elegir el dato ("Usar 25.00 Bs" o "Usar 1500.00 Bs"), y elegir lo deja en el borrador como corregido a mano.

#### Scenario: Elegir el monto
- **WHEN** Carlos abre el borrador con el motivo "dijiste 1500.00 Bs, el comprobante dice 25.00 Bs" y toca "Usar 25.00 Bs"
- **THEN** el borrador queda con 25.00 Bs, sin el motivo, y no cambia al reinterpretar

#### Scenario: Asignado por orden
- **WHEN** los dos comprobantes de 10.00 Bs quedaron con "comida" y "somó" asignados por orden
- **THEN** los dos aparecen en "Revisar" con el motivo "asignado por orden"

### Requirement: Quitar y Separar al Revisar
Desde la evidencia de un borrador pendiente la persona DEBE (MUST) poder quitar una evidencia del paquete o separarla en un borrador propio. Separar crea un paquete nuevo con esa evidencia y lo cierra; los dos paquetes se vuelven a interpretar. Nada cambia en el libro.

#### Scenario: Separar otro pago
- **WHEN** el paquete tiene el comprobante de 25.00 Bs de una comida y el de 1500.00 Bs de otro pago, y Carlos separa el de 1500.00 Bs
- **THEN** quedan dos borradores, uno de 25.00 Bs y otro de 1500.00 Bs, con su propia evidencia

#### Scenario: Quitar al revisar
- **WHEN** Carlos quita en Revisar una nota que no era de ese pago
- **THEN** el borrador se vuelve a interpretar sin esa nota
