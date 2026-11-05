## ADDED Requirements

### Requirement: Paquete de Captura
Toda captura DEBE (MUST) pertenecer a un paquete, que junta lo que la persona manda para explicar uno o varios pagos: comprobantes, fotos, PDF, audios y notas. Cada evidencia se guarda cifrada en cuanto llega, con su momento y dispositivo, y nunca se pierde, aunque el paquete no llegue a cerrarse. El paquete y sus cambios son eventos del historial con IDs UUID, así que se ven igual en todos los dispositivos de la persona.

#### Scenario: Un comprobante compartido es un paquete
- **WHEN** Carlos comparte el comprobante QR de 25.00 Bs
- **THEN** hay un paquete abierto con ese comprobante como única evidencia

#### Scenario: Varias evidencias en un paquete
- **WHEN** Carlos comparte dos fotos y después agrega un audio y una nota
- **THEN** el paquete tiene las cuatro evidencias, en el orden en que llegaron

#### Scenario: La app se cierra con el paquete abierto
- **WHEN** el sistema cierra CoinDen mientras Carlos tiene un paquete abierto con una foto y un audio
- **THEN** al volver, las dos evidencias siguen guardadas

#### Scenario: Paquete en otro dispositivo
- **WHEN** Carlos arma un paquete en el celular y el historial se sincroniza con la PC
- **THEN** la PC muestra el mismo paquete con las mismas evidencias

### Requirement: Cerrar el Paquete
El paquete DEBE (MUST) cerrarse cuando la persona toca Listo o sale de la página de contexto por cualquier motivo, sin esperar ningún tiempo. Al cerrarse, el paquete pasa a interpretarse. Mientras está abierto no produce borradores y no cuenta como pendiente.

#### Scenario: Compartir y salir
- **WHEN** Carlos comparte el comprobante y vuelve a la app del banco sin tocar nada más
- **THEN** el paquete se cierra al instante y queda para interpretar, como un comprobante compartido hoy

#### Scenario: Tocar Listo
- **WHEN** Carlos agrega un audio y toca Listo
- **THEN** el paquete se cierra con el comprobante y el audio

### Requirement: Quitar sin Borrar
La persona DEBE (MUST) poder quitar una evidencia de un paquete. La evidencia queda guardada y en el historial, pero deja de interpretarse. Quitar es un evento nuevo y no borra nada.

#### Scenario: Foto equivocada
- **WHEN** Carlos agrega por error la foto de otro pago y toca Quitar
- **THEN** la foto sigue guardada, se muestra como quitada y el paquete se interpreta sin ella

### Requirement: Evidencia Repetida en el Paquete
Si llega a un paquete un archivo con la misma huella que otro que ya está en ese paquete, el sistema DEBE (MUST) no agregarlo otra vez y avisar que ya está.

#### Scenario: Guardar en WhatsApp y volver a compartir
- **WHEN** Carlos compartió una foto desde WhatsApp, la guarda y la comparte otra vez desde la galería al mismo paquete
- **THEN** la página dice que esa foto ya está en este pago y el paquete sigue con una sola copia

### Requirement: Reabrir el Paquete
Un paquete cerrado con al menos un borrador pendiente DEBE (MUST) poder reabrirse para agregar o quitar evidencias. Al cerrarse otra vez, se vuelve a interpretar todo el paquete. Un paquete cuyos borradores ya están todos confirmados o descartados no se reabre.

#### Scenario: Volver a entrar después de salir
- **WHEN** Carlos compartió un comprobante, salió y, más tarde, abre "Agregar contexto" en ese borrador y graba "esto es de mi comida"
- **THEN** se reabre el mismo paquete, y al cerrarlo el borrador se vuelve a interpretar con el comprobante y el audio

#### Scenario: Todo confirmado
- **WHEN** todos los borradores de un paquete están confirmados
- **THEN** el paquete no ofrece agregar contexto

### Requirement: Borradores de un Paquete
Interpretar un paquete DEBE (MUST) producir de 1 a N borradores, uno por pago real, cada uno con todas las evidencias del paquete que no se quitaron. Al reinterpretar un paquete reabierto:
- los borradores pendientes se actualizan, sin cambiar lo que la persona corrigió a mano;
- los confirmados no cambian, y sus comprobantes ya no producen otro borrador;
- un pendiente que ya no corresponde a ningún pago se descarta, enlazado al paquete.

#### Scenario: Un comprobante y su audio
- **WHEN** se interpreta un paquete con el comprobante QR de 25.00 Bs y el audio "esto es de mi comida"
- **THEN** hay un solo borrador con las dos evidencias

#### Scenario: Dos pagos en un paquete
- **WHEN** se interpreta un paquete con comprobantes de 25.00 Bs y 60.00 Bs con referencias distintas
- **THEN** hay dos borradores, cada uno con las evidencias del paquete

#### Scenario: Reinterpretar sin pisar lo corregido
- **WHEN** Carlos cambió a mano la nota de un borrador a "regalo", reabre su paquete y agrega un audio que dice "almuerzo"
- **THEN** al reinterpretar, la nota sigue siendo "regalo"

#### Scenario: Reinterpretar con un borrador confirmado
- **WHEN** un paquete tiene los borradores de 25.00 Bs (confirmado) y 60.00 Bs (pendiente), y Carlos lo reabre para agregar una nota
- **THEN** el de 25.00 Bs no cambia y no aparece otro borrador de 25.00 Bs

### Requirement: Paquetes Guardados Antes
Un borrador guardado antes de que existieran los paquetes DEBE (MUST) leerse como un paquete cerrado con su evidencia. Así sus datos no cambian, y se puede reabrir si sigue pendiente.

#### Scenario: Borrador viejo
- **WHEN** la app lee un borrador pendiente guardado antes de este cambio
- **THEN** el borrador se ve igual que antes y ofrece "Agregar contexto"
