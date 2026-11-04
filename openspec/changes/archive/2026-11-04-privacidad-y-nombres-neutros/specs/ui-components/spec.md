# Spec Delta

## ADDED Requirements

### Requirement: Acciones con el Texto Completo en Todos los Idiomas
Los botones de acción DEBEN (MUST) mostrar su texto completo, sin cortarlo con puntos suspensivos ni partir una palabra en dos líneas, en español, inglés y portugués. La barra de captura NO DEBE (MUST NOT) repartir más de tres botones en una misma fila: una acción adicional, como "Agregar al último", va en su propia fila debajo.

#### Scenario: Barra de captura con acción adicional
- **WHEN** Ana usa la app en portugués y tiene un último borrador al que puede agregar contexto
- **THEN** ve "Ditar", "Foto" y "Escrever" completos en una fila y "Adicionar ao último" en una fila aparte

#### Scenario: Quitar en la pantalla de contexto
- **WHEN** John abre "What you sent" con una nota ya agregada
- **THEN** el botón "Remove" se lee en una sola línea, junto a la nota

#### Scenario: Textos con apóstrofe
- **WHEN** John usa la app en inglés y abre el paso de fuentes
- **THEN** el campo dice "Today's total", sin barra invertida delante del apóstrofe
