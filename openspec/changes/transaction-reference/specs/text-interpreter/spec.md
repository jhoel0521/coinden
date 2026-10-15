# Spec Delta

## ADDED Requirements

### Requirement: Número de Transacción del Comprobante
El intérprete DEBE (MUST) proponer, junto al movimiento que lee de un comprobante, su número de transacción tal como aparece, tomado de la etiqueta de la plantilla o, sin plantilla, de las etiquetas de referencia del paquete de idioma. Sin etiqueta reconocible no hay referencia. Una frase escrita o dictada nunca trae referencia.

#### Scenario: Pago QR con plantilla
- **WHEN** Carlos captura un "Pago QR otros bancos" del Banco Económico con "Nro. de transacción: 400000001"
- **THEN** la propuesta de gasto trae la referencia "400000001" como evidencia

#### Scenario: Yape sin dos puntos
- **WHEN** Carlos captura un comprobante de Yape con la línea "Nro. de transacción 900000001"
- **THEN** la propuesta de gasto trae la referencia "900000001"

#### Scenario: Banco desconocido
- **WHEN** Facundo captura un comprobante sin plantilla con "Operación N° 123456"
- **THEN** la propuesta trae la referencia "123456"

#### Scenario: Pix y pago en inglés
- **WHEN** Ana captura un Pix con "ID da transação E00000000000000000001" y John un pago con "Confirmation 000111222"
- **THEN** las propuestas traen las referencias "E00000000000000000001" y "000111222"

#### Scenario: No confundir otros números con la referencia
- **WHEN** un comprobante trae "Nro. de cédula de identidad: 0000000" y "Código de autorización: 100001" además de "Nro. de transacción: 8000001"
- **THEN** la referencia es "8000001"

#### Scenario: Frase sin referencia
- **WHEN** Carlos escribe o dicta "almuerzo 45 bnb"
- **THEN** la propuesta no trae referencia
