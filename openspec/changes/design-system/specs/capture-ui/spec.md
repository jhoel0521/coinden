# Spec Delta

## MODIFIED Requirements

### Requirement: Formulario Completo
Escribir DEBE (MUST) abrir una página con todos los campos del movimiento: tipo, monto, billetera, medio de pago, categoría, fecha, nota y referencia (número de transacción, opcional), y un campo de frase libre que, al escribirla, llena los campos con el intérprete. La moneda no se elige: es la de la billetera y se muestra junto al monto. La nota y la referencia van en "Más detalles", que se abre solo si se toca o si ya traen un valor. Guardar crea un borrador con lo indicado en el formulario como dicho por el usuario.

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

#### Scenario: Moneda de la billetera
- **WHEN** Carlos elige "Dolares" en el formulario
- **THEN** junto al monto se muestra USD y no hay un campo para elegir otra moneda

#### Scenario: Más detalles con referencia
- **WHEN** Carlos llena el formulario con un comprobante que trae referencia
- **THEN** "Más detalles" aparece abierto y muestra la referencia
