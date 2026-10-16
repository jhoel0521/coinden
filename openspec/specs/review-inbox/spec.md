# review-inbox Specification

## Purpose
Da a la persona un lugar tranquilo para revisar en la noche lo que capturó en el día: ver lo pendiente y la evidencia, corregir en un formulario editable, confirmar de una vez lo que ya está seguro y corregir lo ya confirmado, siempre con su aprobación.

## Requirements

### Requirement: Bandeja de Revisión
La app DEBE (MUST) ofrecer desde Inicio una bandeja con los saldos proyectados de las billeteras afectadas, avisando cuándo una quedaría por debajo de cero. Debajo muestra dos secciones: "Revisar" y "Listos". Cada borrador muestra su nota, monto, billetera, hora, categoría sugerida, confianza y si tiene evidencia. Sin pendientes, la bandeja lo dice.

#### Scenario: Abrir la bandeja
- **WHEN** Carlos tiene 3 borradores pendientes y toca "Revisar" en Inicio
- **THEN** ve la bandeja con los 3 borradores repartidos entre "Revisar" y "Listos"

#### Scenario: Faltante proyectado
- **WHEN** "Billetera" tiene 10.00 Bs y hay borradores Listos por 3.00 Bs y 45.00 Bs en ella
- **THEN** la bandeja muestra "Billetera" con -38.00 Bs y un aviso de faltante

#### Scenario: Nada pendiente
- **WHEN** Carlos abre la bandeja sin borradores pendientes
- **THEN** la bandeja indica que no hay nada para revisar

### Requirement: Qué Va en Revisar y qué en Listos
La bandeja DEBE (MUST) poner en "Listos" solo los borradores Listos de confianza ALTA sin preguntas de mismo gasto ni cambios pendientes. Todo lo demás va en "Revisar": las preguntas de mismo gasto, los cambios pendientes, los borradores Capturados, Incompletos o No soportados y los Listos de confianza MEDIA o BAJA, ordenados de menor a mayor confianza.

#### Scenario: Comprobante seguro
- **WHEN** Carlos tiene el borrador de un comprobante QR Listo con confianza ALTA y sin preguntas
- **THEN** aparece en "Listos"

#### Scenario: Frase sin billetera
- **WHEN** Carlos tiene "page 3 bs para trasmporte" con la billetera sugerida y confianza BAJA
- **THEN** aparece en "Revisar"

#### Scenario: Pregunta pendiente
- **WHEN** un borrador Listo de confianza ALTA pregunta si es el mismo gasto que otro
- **THEN** aparece en "Revisar" con la pregunta

### Requirement: Revisar en el Formulario Editable
Tocar un borrador de gasto o ingreso DEBE (MUST) abrir el formulario completo con todo lo propuesto y la evidencia a mano. Ahí la persona cambia cualquier campo y toca Confirmar (guarda y confirma), Guardar (guarda sin confirmar) o Descartar. Si el libro rechaza la confirmación, el formulario sigue abierto con el motivo.

#### Scenario: La IA leyó 320
- **WHEN** Carlos abre el borrador de una foto leída como 320.00 Bs, cambia el monto a 32.00 Bs y toca Confirmar
- **THEN** el gasto se registra por 32.00 Bs y el borrador sale de la bandeja

#### Scenario: Completar una foto sin leer
- **WHEN** Carlos abre un borrador Capturado de una foto que no se pudo leer, llena 18.50 Bs en "Billetera" y toca Guardar
- **THEN** el borrador queda Listo y sigue en la bandeja sin afectar el saldo

#### Scenario: Descartar desde el formulario
- **WHEN** Carlos abre un borrador capturado por error y toca Descartar
- **THEN** el borrador queda Descartado con su evidencia y sale de la bandeja

#### Scenario: Confirmación rechazada en el formulario
- **WHEN** Carlos confirma desde el formulario un gasto de 45.00 Bs en "Billetera", que solo tiene 10.00 Bs
- **THEN** el formulario sigue abierto y muestra que faltan 35.00 Bs

#### Scenario: Transferencia en la bandeja
- **WHEN** Carlos tiene un borrador de cambio de 50.00 USD a 594.00 Bs
- **THEN** la bandeja lo muestra con Confirmar y Descartar en su tarjeta, sin abrir el formulario

### Requirement: Confirmar los Listos de una Vez
La sección "Listos" DEBE (MUST) ofrecer "Confirmar los N listos". Antes de confirmar muestra un resumen con la cantidad y el total por billetera, y después confirma cada borrador con las reglas del libro y su categoría sugerida. Los que el libro rechace quedan en la bandeja con su motivo.

#### Scenario: Tres listos
- **WHEN** Carlos tiene 3 borradores en "Listos" por 25.00 Bs, 10.00 Bs y 12.00 Bs en "BNB Bs" y acepta el resumen de 3 gastos por 47.00 Bs
- **THEN** los 3 quedan confirmados y el saldo de "BNB Bs" baja 47.00 Bs

#### Scenario: Uno rechazado
- **WHEN** de 3 borradores listos, uno no entra en el saldo de "Billetera"
- **THEN** se confirman los otros 2 y el tercero queda en la bandeja con el faltante

#### Scenario: Cancelar el resumen
- **WHEN** Carlos ve el resumen y lo cancela
- **THEN** no se confirma nada

### Requirement: Ver la Evidencia
Desde la bandeja, el formulario y el historial, la app DEBE (MUST) mostrar la evidencia de un borrador o movimiento: la foto y el PDF a pantalla completa y el audio con reproducción. Se descifran solo en memoria y no quedan copias legibles fuera de la app. El texto capturado o leído también se puede ver.

#### Scenario: Ver el comprobante
- **WHEN** Carlos toca "Ver comprobante" en el formulario del almuerzo
- **THEN** ve la foto del comprobante a pantalla completa

#### Scenario: Escuchar la nota de voz
- **WHEN** Carlos toca la evidencia de audio de "gasté 40 en taxi"
- **THEN** escucha el audio dentro de la app

#### Scenario: Sin copias legibles
- **WHEN** Carlos cierra el visor de un PDF
- **THEN** no queda ningún archivo descifrado del PDF en el almacenamiento del teléfono

### Requirement: Cambios Pendientes en el Formulario
Tocar un cambio pendiente DEBE (MUST) abrir el mismo formulario editable con los datos propuestos. Cada campo que cambia se resalta con su valor anterior al lado, y una diferencia grande se marca con un aviso. La persona puede editar cualquier campo antes de tocar Confirmar, Descartar o "Son distintos".

#### Scenario: Revisar el almuerzo
- **WHEN** Carlos toca el cambio pendiente del almuerzo leído como 320.00 Bs
- **THEN** el formulario muestra 320.00 resaltado con "antes: 35.00" y el aviso de diferencia grande, y la referencia "400000001" con "antes: vacío"

#### Scenario: Corregir lo leído y aplicar
- **WHEN** Carlos cambia 320.00 por 32.00 en ese formulario y toca Confirmar
- **THEN** el almuerzo queda en 32.00 Bs con la referencia "400000001" y el cambio sale de la bandeja

### Requirement: Historial de Movimientos con Corrección
La app DEBE (MUST) ofrecer desde Inicio un historial de los movimientos confirmados, del más reciente al más antiguo, con su nota, monto, billetera, fecha, categoría, referencia y evidencia. Desde cada uno, Corregir abre el formulario con sus datos y los cambios se resaltan antes de confirmar. Los movimientos anulados no aparecen como vigentes.

#### Scenario: Corregir a mano
- **WHEN** Carlos abre el almuerzo de 35.00 Bs en el historial, toca Corregir, cambia el monto a 32.00 Bs y confirma
- **THEN** el historial muestra el almuerzo de 32.00 Bs y el saldo de "BNB Bs" sube 3.00 Bs

#### Scenario: Salir sin confirmar
- **WHEN** Carlos toca Corregir en el taxi, cambia la nota y sale sin confirmar
- **THEN** el taxi no cambia

#### Scenario: Movimiento anulado
- **WHEN** el almuerzo de 35.00 Bs fue corregido a 32.00 Bs
- **THEN** el historial muestra solo el de 32.00 Bs como vigente
