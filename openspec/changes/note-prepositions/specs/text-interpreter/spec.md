# Spec Delta

## ADDED Requirements

### Requirement: Nota sin Preposiciones Sueltas
La nota de una frase NO DEBE (MUST NOT) contener las preposiciones de nota del paquete ("por", "para", "de", "en", "pelo", "by", "for"...). La nota es lo que sigue a la preposición; si después de ella no queda ninguna palabra (porque era el medio de pago, la moneda o la billetera), la nota son las demás palabras de la frase sin preposiciones; si tampoco queda ninguna, la frase no trae nota.

#### Scenario: Gasto con "por"
- **WHEN** Carlos dice "gasté 10 por power"
- **THEN** la nota es "power"

#### Scenario: Solo el medio después de "por"
- **WHEN** Carlos dice "gaste 10 bs por qr"
- **THEN** la propuesta no trae nota

#### Scenario: La nota va antes del monto
- **WHEN** Carlos escribe "almuerzo 45 por qr"
- **THEN** la nota es "almuerzo"

#### Scenario: Portugués e inglés
- **WHEN** Ana dice "paguei 30 pelo pix" y John dice "paid 12 by card"
- **THEN** ninguna de las dos propuestas trae nota

#### Scenario: Las notas que ya funcionaban
- **WHEN** Carlos escribe "page 3 bs para trasmporte"
- **THEN** la nota sigue siendo "trasmporte"
