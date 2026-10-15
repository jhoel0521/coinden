# Spec Delta

## ADDED Requirements

### Requirement: Motivo de la Billetera Sugerida
Cuando el intérprete sugiere una billetera que la frase no nombró, DEBE (MUST) indicar el motivo como dato, sin texto: es la billetera por defecto, es la única que acepta el medio de pago o la moneda dichos, o es una recomendada entre varias candidatas (con cuántas había).

#### Scenario: Única cuenta que acepta QR
- **WHEN** Carlos tiene "Billetera" en efectivo (por defecto) y solo "BNB Bs" como banco, y escribe "almuerzo 45 qr"
- **THEN** la propuesta sugiere "BNB Bs" con el motivo "única que acepta el medio de pago"

#### Scenario: Varias cuentas que aceptan QR
- **WHEN** Carlos tiene "BNB Bs", "BE Bs" y "Yape", y escribe "almuerzo 45 qr"
- **THEN** la propuesta sugiere una de ellas con el motivo "recomendada entre 3 candidatas"

#### Scenario: Billetera por defecto
- **WHEN** Carlos escribe "page 3 bs para trasmporte" sin nombrar billetera ni medio
- **THEN** la propuesta sugiere su billetera por defecto con el motivo "billetera por defecto"

#### Scenario: Billetera dicha sin motivo
- **WHEN** Carlos escribe "almuerzo 45 bnb"
- **THEN** la billetera es la dicha por el usuario y la propuesta no trae motivo de sugerencia
