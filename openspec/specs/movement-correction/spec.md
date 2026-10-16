# movement-correction Specification

## Purpose
Permite corregir un movimiento ya confirmado sin que la app cambie nada por su cuenta: cada cambio, venga de la persona o de un comprobante, se aprueba completo en el formulario y queda en el historial como un evento nuevo.

## Requirements

### Requirement: Ningún Cambio sin Aprobación
El sistema DEBE (MUST) cambiar los datos de un movimiento confirmado solo cuando la persona aprueba la corrección completa. Esto incluye llenar un dato vacío, como el número de transacción o la hora. Mientras no la apruebe, el movimiento, su clasificación y los saldos no cambian.

#### Scenario: Comprobante con número de transacción
- **WHEN** Carlos confirmó "pagué 40 de taxi por qr" sin referencia en "BNB Bs" y luego llega su comprobante con la referencia "400000777"
- **THEN** el gasto confirmado sigue sin referencia hasta que Carlos aprueba el cambio

#### Scenario: Lectura que nadie aprobó
- **WHEN** hay un cambio pendiente de 35.00 Bs a 32.00 Bs sobre el almuerzo de Carlos y Carlos no lo resolvió
- **THEN** el saldo de "BNB Bs" sigue contando 35.00 Bs

### Requirement: Corrección Completa de una Vez
Al aprobar, el sistema DEBE (MUST) aplicar juntos todos los datos del formulario, nunca campo por campo. Si el libro rechaza la corrección, no se aplica nada y se muestra el motivo.

#### Scenario: Monto y referencia juntos
- **WHEN** Carlos aprueba el formulario del almuerzo con 32.00 Bs y la referencia "400000001"
- **THEN** el gasto queda con 32.00 Bs y la referencia "400000001" en una sola corrección

#### Scenario: Corrección rechazada por saldo
- **WHEN** Carlos corrige un gasto de "Billetera" de 10.00 Bs a 90.00 Bs y la billetera solo tiene 50.00 Bs disponibles
- **THEN** el gasto sigue en 10.00 Bs, la referencia y la nota no cambian y se muestra que faltan 30.00 Bs

### Requirement: Corregir el Dinero con Anulación
Si la corrección cambia el monto, la moneda, la billetera, el tipo o el momento, el sistema DEBE (MUST) anular el movimiento anterior y registrar uno nuevo con los datos aprobados, sin editar ni borrar el original. La categoría y la descripción pasan al movimiento nuevo.

#### Scenario: El almuerzo era de 32
- **WHEN** Carlos corrige el almuerzo confirmado de 35.00 Bs a 32.00 Bs
- **THEN** el libro contiene el gasto de 35.00 Bs, su anulación y un gasto de 32.00 Bs clasificado como "comida", y el saldo refleja solo los 32.00 Bs

#### Scenario: Otra billetera
- **WHEN** Carlos corrige el taxi de 40.00 Bs de "Billetera" a "BNB Bs"
- **THEN** "Billetera" recupera los 40.00 Bs y "BNB Bs" baja 40.00 Bs

#### Scenario: Corregir en dos dispositivos
- **WHEN** Carlos corrige el almuerzo en el celular y el historial se sincroniza con la PC
- **THEN** la PC muestra el mismo gasto de 32.00 Bs sin registrar otra anulación ni otro gasto

### Requirement: Corregir sin Tocar el Dinero
Si la corrección solo cambia la nota, la categoría o la referencia, el sistema DEBE (MUST) registrarla sin anular ni registrar movimientos en el libro. La categoría nueva se guarda como una nueva clasificación.

#### Scenario: Solo la referencia
- **WHEN** Carlos aprueba agregar la referencia "400000777" al taxi confirmado sin cambiar nada más
- **THEN** el taxi muestra la referencia "400000777" y el libro de dinero no cambia

#### Scenario: Solo la categoría
- **WHEN** Carlos cambia el gasto "power" de "comida" a "salidas"
- **THEN** el libro de dinero no cambia y la clasificación vigente es "salidas"

### Requirement: Cambio Pendiente desde un Comprobante
Cuando la persona indica que un comprobante real (foto, PDF o texto del banco) es el mismo movimiento que uno confirmado, el sistema DEBE (MUST) crear un cambio pendiente con lo que diría el movimiento al unirlos. Se resuelve con tres respuestas: Confirmar (aplica la corrección y guarda el comprobante como evidencia), Descartar (todo queda como estaba y el comprobante queda como evidencia) o "Son distintos" (el comprobante sigue como borrador propio).

#### Scenario: Confirmar el cambio del almuerzo
- **WHEN** el comprobante del almuerzo trae 32.00 Bs y la referencia "400000001", y Carlos confirma el cambio pendiente
- **THEN** el gasto queda en 32.00 Bs con la referencia "400000001", el comprobante aparece como su evidencia y el borrador del comprobante queda Descartado enlazado al gasto

#### Scenario: Descartar el cambio
- **WHEN** Carlos descarta el cambio pendiente del almuerzo
- **THEN** el gasto sigue en 35.00 Bs sin referencia, el comprobante aparece como su evidencia y no queda nada pendiente

#### Scenario: Son distintos
- **WHEN** Carlos responde "Son distintos" al cambio pendiente
- **THEN** el gasto confirmado no cambia, el comprobante sigue como un borrador pendiente y no se vuelve a preguntar por ese par

### Requirement: Comprobante Idéntico
Si el comprobante tiene la misma referencia que el movimiento confirmado y nada que cambiar, el sistema DEBE (MUST) guardarlo solo como evidencia de ese movimiento, sin cambio pendiente ni pregunta, y sin tocar sus datos ni los saldos.

#### Scenario: El mismo comprobante otra vez
- **WHEN** el taxi confirmado tiene 40.00 Bs en "BNB Bs" con la referencia "400000777" y Carlos comparte otra vez ese comprobante
- **THEN** el comprobante queda como evidencia del taxi, el taxi no cambia y no aparece nada para revisar

#### Scenario: Sin referencia no es idéntico
- **WHEN** el taxi confirmado no tiene referencia y llega un comprobante de 40.00 Bs en "BNB Bs" con la referencia "400000777"
- **THEN** el sistema crea un cambio pendiente para agregar la referencia, no lo aplica solo

### Requirement: El Texto y el Audio No Corrigen
Una captura de texto escrito, dictado o audio NUNCA DEBE (MUST NOT) generar un cambio pendiente sobre un movimiento confirmado. Si coincide con uno, se pregunta si es el mismo gasto como cualquier borrador, y "Son el mismo" solo une la evidencia.

#### Scenario: Dictado después de confirmar
- **WHEN** Carlos confirmó el taxi de 40.00 Bs y más tarde dicta "gasté 40 en taxi"
- **THEN** no se crea un cambio pendiente y, si Carlos responde "Son el mismo", el taxi suma el audio como evidencia sin cambiar sus datos

### Requirement: Diferencias Visibles con su Valor Anterior
Para cada corrección, el sistema DEBE (MUST) informar qué campos cambian (monto, moneda, billetera, tipo, momento, nota, categoría y referencia) con su valor anterior y el nuevo, incluso cuando el anterior estaba vacío.

#### Scenario: Diferencias del almuerzo
- **WHEN** el cambio pendiente del almuerzo trae 32.00 Bs, la referencia "400000001" y la hora 12:41
- **THEN** las diferencias son monto 35.00 → 32.00, referencia vacía → "400000001" y hora 12:40 → 12:41, y la nota "almuerzo" no figura como cambio

### Requirement: Aviso de Diferencia Grande
El sistema DEBE (MUST) marcar como diferencia grande un monto nuevo que sea el doble o más, o la mitad o menos, del anterior en la misma moneda. El aviso nunca bloquea la corrección.

#### Scenario: El OCR perdió la coma
- **WHEN** el comprobante del almuerzo de 35.00 Bs se leyó como 320.00 Bs
- **THEN** el cambio de monto se marca como diferencia grande

#### Scenario: Diferencia chica
- **WHEN** el monto cambia de 35.00 Bs a 32.00 Bs
- **THEN** el cambio no se marca como diferencia grande

### Requirement: Historial de la Corrección
Cada corrección DEBE (MUST) quedar en el historial del movimiento como un evento append-only con su origen (la persona a mano o un comprobante, con su borrador), el momento y el dispositivo. Los datos anteriores siguen consultables.

#### Scenario: Lectura corregida por la persona
- **WHEN** el comprobante del almuerzo se leyó como 320.00 Bs y Carlos lo corrige a 32.00 Bs en el formulario antes de confirmar
- **THEN** el historial muestra que el comprobante leyó 320.00 Bs, que el gasto pasó de 35.00 Bs a 32.00 Bs y que lo aprobó Carlos

#### Scenario: Corrección a mano
- **WHEN** Carlos corrige a mano la nota del taxi a "taxi a la u"
- **THEN** el historial guarda la corrección con origen manual y la nota anterior "taxi"
