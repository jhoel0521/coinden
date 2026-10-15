# capture-matching Specification

## Purpose
Evita contar dos veces el mismo pago cuando llega por varios caminos (dictado, comprobante, archivo repetido) sin decidir por la persona: cada captura se guarda, la app pregunta si dos son el mismo movimiento y aplica lo que la persona elige.

## Requirements

### Requirement: Pregunta de Posible Mismo Movimiento
El sistema DEBE (MUST) preguntar si dos borradores del mismo usuario son el mismo movimiento cuando son del mismo tipo, con el mismo monto y moneda y del mismo día local. Se comparan borradores pendientes y confirmados, nunca descartados. La pregunta no cambia saldos ni estados hasta que la persona responde.

#### Scenario: Taxi y power del mismo precio
- **WHEN** Carlos captura "gasté 10 bs por qr" por el taxi y, más tarde el mismo día, otra vez "gasté 10 bs por qr" por un power
- **THEN** los dos borradores se guardan y el segundo pregunta si es el mismo gasto que el primero

#### Scenario: Audio y comprobante del mismo pago
- **WHEN** Carlos dicta "gasté 10 en power" a las 10:40 y a las 11:30 comparte el comprobante QR de 10.00 Bs de las 10:42 con referencia "400000123"
- **THEN** el borrador del comprobante pregunta si es el mismo gasto que "power"

#### Scenario: Ya confirmado
- **WHEN** Carlos confirmó en la mañana el dictado "pagué 40 de taxi por qr" en "BNB Bs" y a la tarde comparte el comprobante de 40.00 Bs de ese día
- **THEN** el borrador del comprobante pregunta si es el mismo gasto que el ya confirmado

#### Scenario: Otro día o otro monto
- **WHEN** Carlos gastó 10.00 Bs el lunes y 10.00 Bs el martes, o 10.00 Bs y 12.00 Bs el mismo día
- **THEN** no se pregunta nada

### Requirement: Aviso de Captura Repetida
Cuando dos borradores comparten el mismo archivo, el mismo texto de un comprobante (de varias líneas) o la misma referencia con el mismo monto, el sistema DEBE (MUST) avisar que la captura está repetida, aunque falte el monto o sean de días distintos, con las mismas respuestas que la pregunta.

#### Scenario: Mismo comprobante enviado dos veces
- **WHEN** Carlos comparte dos veces el comprobante con referencia "400000001" de 25.00 Bs
- **THEN** el segundo borrador avisa que es el mismo comprobante que el primero

#### Scenario: Una frase corta repetida no es un comprobante repetido
- **WHEN** Carlos dicta dos veces "gasté 10 bs por qr" el mismo día
- **THEN** el segundo pregunta si es el mismo gasto, no avisa que la captura está repetida

#### Scenario: Mismo archivo enviado dos veces
- **WHEN** Carlos comparte dos veces la misma nota de voz, sin transcribir todavía
- **THEN** el segundo borrador avisa que es el mismo archivo que el primero

### Requirement: Son Distintos
Cuando la persona responde que dos borradores son distintos, el sistema DEBE (MUST) mantener ambos como estaban, registrar la decisión como un evento y no volver a preguntar por ese par, también en sus otros dispositivos.

#### Scenario: Taxi y power separados
- **WHEN** Carlos responde "Son distintos" a la pregunta del taxi y el power de 10.00 Bs
- **THEN** quedan los dos borradores, al confirmarlos el saldo baja 20.00 Bs y la pregunta no vuelve a aparecer

### Requirement: Son el Mismo
Cuando la persona responde que dos borradores son el mismo movimiento, el sistema DEBE (MUST) dejar uno solo con lo que sabían ambos y descartar el otro enlazado al que queda, sin perder su evidencia. Queda el confirmado si uno lo está; si no, el capturado primero.

#### Scenario: Comprobante repetido
- **WHEN** Carlos responde "Son el mismo" al comprobante compartido dos veces
- **THEN** queda uno solo y al confirmarlo el saldo baja una sola vez

#### Scenario: La evidencia no se pierde
- **WHEN** Carlos une el dictado "gasté 10 en power" con su comprobante
- **THEN** el borrador que queda muestra el audio y el comprobante como evidencia, y el otro queda Descartado enlazado a él

### Requirement: Qué Dato Gana al Unir
Al unir dos borradores el sistema DEBE (MUST) tomar el monto, la moneda, la billetera, el momento y la referencia leídos de un comprobante antes que los dichos o sugeridos, y la nota dicha por la persona antes que la del comprobante. El medio de pago y la referencia se toman de quien los tenga.

#### Scenario: Power con su comprobante
- **WHEN** Carlos une el dictado "gasté 10 en power" (billetera sugerida "Billetera") con el comprobante QR de 10.00 Bs de "BNB Bs" de las 10:42 con referencia "400000123"
- **THEN** queda un borrador con la nota "power", 10.00 Bs, "BNB Bs", las 10:42 y la referencia "400000123"

### Requirement: Unir con un Movimiento Confirmado
Si el borrador que queda ya está confirmado, el sistema DEBE (MUST) agregarle solo la referencia y la evidencia, sin registrar otro movimiento. Si el comprobante indica otra billetera, no se unen y se avisa para corregirlo en la revisión.

#### Scenario: Unir con el taxi confirmado
- **WHEN** Carlos une el gasto confirmado "taxi" de 40.00 Bs en "BNB Bs" con el comprobante de 40.00 Bs de "BNB Bs" con referencia "400000777"
- **THEN** el gasto confirmado gana la referencia "400000777" y el comprobante como evidencia, y el saldo no cambia

#### Scenario: Billetera distinta
- **WHEN** el gasto confirmado de 40.00 Bs está en "Billetera" y el comprobante dice "BNB Bs"
- **THEN** no se unen, el comprobante sigue pendiente y se avisa que la billetera no coincide
