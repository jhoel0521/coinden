## ADDED Requirements

### Requirement: Interpretación del Paquete
El intérprete DEBE (MUST) interpretar un paquete en una sola pasada. Recibe los textos de sus evidencias no quitadas, cada uno marcado como **comprobante** (texto de un comprobante, foto o PDF leídos) o **contexto** (audios transcritos y notas), y devuelve los pagos del paquete. Cada comprobante es un pago con sus hechos: monto, moneda, medio de pago, billetera, hora del pago y referencia.

#### Scenario: Comprobante sin glosa y su audio
- **WHEN** el paquete tiene el comprobante QR de 25.00 Bs de "BNB Bs" de las 14:22, con referencia "400000001" y sin glosa, y el audio "esto es de mi comida"
- **THEN** el intérprete devuelve un pago de 25.00 Bs en "BNB Bs" a las 14:22, con la referencia "400000001", la nota "comida", la categoría Comida y confianza alta

#### Scenario: Paquete sin comprobante
- **WHEN** el paquete solo tiene el audio "gasté 45 en almuerzo con bnb"
- **THEN** el intérprete lo entiende como hoy entiende esa frase

### Requirement: Modelo de Lenguaje con el Mismo Paquete
Si hay un modelo de lenguaje en el teléfono, DEBE (MUST) recibir el mismo paquete con el mismo contrato que el intérprete de reglas, y las reglas validan su propuesta como validan cualquier otra. Sin modelo, el paquete se interpreta igual con las reglas.

#### Scenario: Modelo y reglas no coinciden
- **WHEN** el modelo propone 45.00 Bs para un paquete cuyo comprobante dice 25.00 Bs
- **THEN** las reglas no aceptan el monto y el pago queda para revisar

### Requirement: Frases de Contexto
El intérprete DEBE (MUST) partir el contexto en frases, en el orden en que se dijeron o escribieron, y entenderlas en modo contexto: una frase no necesita monto ni verbo para aportar. Una frase con monto habla de un pago; una frase sin monto lo explica (nota, categoría, billetera, para quién). Las palabras que unen o separan frases ("y luego", "después", "le di", "en efectivo", "acá te paso el comprobante", "esto es de") vienen de los paquetes de idioma, nunca del código.

#### Scenario: Frase sin monto
- **WHEN** el contexto es "esto es de mi comida"
- **THEN** es una frase explicativa con la nota "comida" y la categoría Comida

#### Scenario: Varias frases en un audio
- **WHEN** el contexto es "estaba comiendo y luego un ambulante pasó y le di 25 en efectivo"
- **THEN** hay dos frases: "estaba comiendo", explicativa, y "le di 25 en efectivo", con monto 25.00 Bs y medio efectivo

### Requirement: Emparejar Contexto con Comprobantes
Cada frase con monto DEBE (MUST) emparejarse con los comprobantes, usando cada uno una sola vez: si coincide en monto y medio de pago, explica ese pago; si coincide el monto pero el medio es otro, es otro pago. Con varios comprobantes del mismo monto, se asignan por la hora del pago en el orden en que se dijeron, para revisar con el motivo "asignado por orden". Repetir el monto de un pago ya explicado es mencionarlo dos veces.

#### Scenario: El ambulante en efectivo
- **WHEN** el paquete tiene los comprobantes QR de 25.00 Bs y 60.00 Bs y el audio "estaba comiendo y luego un ambulante pasó y le di 25 en efectivo"
- **THEN** hay tres pagos: QR 25.00 Bs, QR 60.00 Bs y 25.00 Bs en efectivo con la nota "ambulante"

#### Scenario: Comida y somó del mismo monto
- **WHEN** el paquete tiene comprobantes de 60.00 Bs y 10.00 Bs de las 12:05 y otro de 10.00 Bs de las 12:25, y el audio "estaba comiendo 60 y 10, pasó el somosero y le compré somó 10"
- **THEN** el de 60.00 Bs y el de 10.00 Bs de las 12:05 tienen la nota "comida", el de 10.00 Bs de las 12:25 tiene la nota "somó", y los dos de 10.00 Bs quedan para revisar con el motivo "asignado por orden"

#### Scenario: El mismo pago dicho dos veces
- **WHEN** el paquete tiene un solo comprobante de 60.00 Bs y el audio "60 de mi comida, sí, los 60 de la comida"
- **THEN** hay un solo pago de 60.00 Bs con la nota "comida"

### Requirement: Frase con Monto sin su Comprobante
Una frase con monto que no coincide con ningún comprobante DEBE (MUST) tratarse así: si el paquete tiene un solo comprobante y nada los separa (ni otro medio de pago), es una contradicción y el pago queda para revisar con las dos versiones; si tiene varios comprobantes o ninguno, es otro pago, para revisar.

#### Scenario: Comprobante equivocado
- **WHEN** el paquete tiene el comprobante de 25.00 Bs de una comida y el audio "gasté 1500, acá te paso el comprobante"
- **THEN** hay un pago para revisar con el motivo "dijiste 1500.00 Bs, el comprobante dice 25.00 Bs"

#### Scenario: Otro pago entre varios comprobantes
- **WHEN** el paquete tiene los comprobantes de 25.00 Bs y 60.00 Bs y el audio "y le pagué 15 al taxi"
- **THEN** hay un tercer pago de 15.00 Bs con la nota "taxi", para revisar

### Requirement: Contexto Explicativo
Una frase sin monto DEBE (MUST) aplicarse al pago que queda sin explicar si es uno solo. Si quedan varios pagos sin explicar, se sugiere en todos y todos quedan para revisar con el motivo "el contexto puede ser de cualquiera de los pagos". Sin pagos sin explicar, la frase completa el pago al que sigue en el orden de lo dicho.

#### Scenario: Dos pagos y una frase
- **WHEN** el paquete tiene los comprobantes de 40.00 Bs y 78.00 Bs y el audio "estaba comiendo"
- **THEN** hay dos pagos con la categoría Comida sugerida, los dos para revisar con el motivo "el contexto puede ser de cualquiera de los pagos"

#### Scenario: La frase explica el único pago
- **WHEN** el paquete tiene un solo comprobante y la nota "útiles del colegio"
- **THEN** el pago tiene la nota "útiles del colegio", sin motivo de revisión

### Requirement: Hechos del Comprobante Protegidos
El contexto NUNCA DEBE (MUST NOT) cambiar en silencio un hecho que un comprobante sí trae (monto, moneda, hora, referencia o billetera). Si lo contradice, el pago queda para revisar mostrando las dos versiones. El contexto sí completa lo que el comprobante no trae: la nota, la categoría, la billetera si el comprobante no la identifica y el medio de pago.

#### Scenario: La billetera la dice el contexto
- **WHEN** el comprobante no identifica la billetera y el audio dice "lo pagué con bnb"
- **THEN** el pago queda en "BNB Bs" con origen dicho

#### Scenario: El contexto completa una foto sin leer
- **WHEN** la foto del comprobante no se pudo leer y la nota dice "farmacia 33 bnb"
- **THEN** hay un pago de 33.00 Bs en "BNB Bs" con la nota "farmacia"
