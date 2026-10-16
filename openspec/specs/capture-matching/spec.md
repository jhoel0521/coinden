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
Si el borrador que queda ya está confirmado, el sistema DEBE (MUST) no registrar otro movimiento ni cambiar sus datos por su cuenta. Si el otro borrador viene de un comprobante real (foto, PDF o texto del banco), "Son el mismo" crea un cambio pendiente con lo que diría el movimiento al unirlos, que la persona aprueba o descarta en el formulario de corrección, incluso si el comprobante indica otra billetera. Si viene de texto escrito, dictado o audio, solo se agrega su evidencia.

#### Scenario: Unir con el taxi confirmado
- **WHEN** Carlos responde "Son el mismo" entre el gasto confirmado "taxi" de 40.00 Bs en "BNB Bs" y el comprobante de 40.00 Bs de "BNB Bs" con referencia "400000777"
- **THEN** se crea un cambio pendiente que agrega la referencia "400000777", y el taxi sigue sin referencia y el saldo no cambia hasta que Carlos lo aprueba

#### Scenario: Billetera distinta
- **WHEN** el gasto confirmado de 40.00 Bs está en "Billetera", el comprobante dice "BNB Bs" y Carlos responde "Son el mismo"
- **THEN** el cambio pendiente muestra la billetera "Billetera" → "BNB Bs" para aprobarla, y nada cambia hasta entonces

#### Scenario: Dictado unido a un confirmado
- **WHEN** Carlos responde "Son el mismo" entre el taxi confirmado y su dictado "gasté 40 en taxi"
- **THEN** el taxi suma el audio como evidencia, sus datos no cambian y no se crea un cambio pendiente
