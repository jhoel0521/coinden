# Spec Delta

## MODIFIED Requirements

### Requirement: Formulario Completo
Escribir DEBE (MUST) abrir una página con todos los campos del movimiento: tipo, monto, moneda, billetera, medio de pago, categoría, fecha, nota y referencia (número de transacción, opcional), y un campo de frase libre que, al escribirla, llena los campos con el intérprete. Guardar crea un borrador con lo indicado en el formulario como dicho por el usuario.

#### Scenario: Llenar con una frase
- **WHEN** Carlos escribe "almuerzo 45 bnb" en la frase del formulario
- **THEN** el formulario propone un gasto de 45 en "BNB Bs" con la categoría comida, y Carlos puede cambiar cualquier campo antes de guardar

#### Scenario: Formulario completo a mano
- **WHEN** Carlos elige gasto, 120 Bs, "BNB Bs", tarjeta, supermercado y la fecha de ayer, y guarda
- **THEN** se guarda un borrador Listo con esos datos y confianza alta

#### Scenario: Faltan datos
- **WHEN** Carlos guarda el formulario sin monto
- **THEN** se guarda igual como borrador Incompleto, sin perder lo que escribió

#### Scenario: Referencia escrita a mano
- **WHEN** Carlos llena un gasto de 120 Bs en "BNB Bs" y escribe la referencia "77001234"
- **THEN** el borrador guardado trae la referencia "77001234"

#### Scenario: Pegar un comprobante en la frase
- **WHEN** Carlos pega en la frase el texto de un pago QR con "Nro. de transacción: 400000001" y toca Llenar
- **THEN** el campo referencia queda con "400000001"

### Requirement: Resultado de la Captura y Confirmación Inmediata
Después de cada captura la app DEBE (MUST) indicar que se guardó como borrador, mostrar su tarjeta (con la referencia, si la hay) y ofrecer confirmarlo en ese momento, con la categoría sugerida si la hay, o dejarlo para la revisión. Confirmar aplica las reglas del libro; si el libro lo rechaza, el borrador sigue pendiente y se muestra el motivo. Si la captura puede ser el mismo movimiento que otra, la tarjeta pregunta "¿Es el mismo gasto?" mostrando la otra (nota, monto, hora y billetera) con los botones "Son el mismo" y "Son distintos".

#### Scenario: Confirmar ahora
- **WHEN** Carlos captura "almuerzo 45 bnb" y toca Confirmar
- **THEN** el gasto de 45 Bs se registra en "BNB Bs" clasificado como comida y el saldo de Inicio baja 45 Bs

#### Scenario: Dejar para la noche
- **WHEN** Carlos captura un gasto y vuelve a Inicio sin confirmar
- **THEN** el borrador queda pendiente y los saldos no cambian

#### Scenario: Confirmación rechazada
- **WHEN** Carlos confirma un gasto de 45 Bs en "Billetera", que solo tiene 10 Bs
- **THEN** el borrador sigue pendiente y la tarjeta muestra que faltan 35 Bs

#### Scenario: Preguntar en el momento
- **WHEN** Carlos comparte el comprobante de 10.00 Bs después de haber dictado "gasté 10 por power" ese día
- **THEN** la tarjeta pregunta si es el mismo gasto que "power", con "Son el mismo" y "Son distintos"

#### Scenario: Responder Son el mismo
- **WHEN** Carlos toca "Son el mismo"
- **THEN** la tarjeta pasa a mostrar el borrador unido y Inicio cuenta un borrador pendiente menos

#### Scenario: Responder Son distintos
- **WHEN** Carlos toca "Son distintos"
- **THEN** la pregunta desaparece y quedan los dos borradores
