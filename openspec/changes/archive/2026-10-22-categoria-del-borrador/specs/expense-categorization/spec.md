## ADDED Requirements

### Requirement: Categoría Elegida en un Borrador
Cuando la persona elige una categoría (base o propia) en un borrador pendiente y lo guarda sin confirmar, el sistema DEBE (MUST) conservar esa categoría en la revisión del borrador. Al reabrirlo se muestra la categoría elegida antes que cualquier sugerencia, y al confirmarlo sin elegir otra el gasto se clasifica con ella. Elegir otra y guardar la reemplaza. La categoría elegida viaja con el historial del borrador a los otros dispositivos.

#### Scenario: Guardar y reabrir
- **WHEN** Carlos abre el borrador "veterinario 30 bnb", elige "Mascotas" y toca Guardar
- **THEN** al reabrirlo la categoría marcada es "Mascotas"

#### Scenario: Guardar y después confirmar
- **WHEN** Carlos eligió "comida" para "croquetas 40 bnb", lo guardó y más tarde lo confirma sin cambiar la categoría
- **THEN** el gasto confirmado queda clasificado como "comida"

#### Scenario: Confirmar los listos de una vez
- **WHEN** Carlos guardó "almuerzo 25 bnb" con "salidas" y toca "Confirmar los listos" en la bandeja
- **THEN** el gasto queda clasificado como "salidas", no como la sugerencia "comida"

#### Scenario: Cambiar de idea
- **WHEN** Carlos guardó "veterinario" con "Mascotas" y después lo guarda con "salud"
- **THEN** al reabrirlo la categoría marcada es "salud"

#### Scenario: Sin categoría elegida
- **WHEN** Carlos guarda un borrador "almuerzo 25 bnb" sin elegir categoría
- **THEN** al reabrirlo se muestra la sugerencia "comida", como antes

#### Scenario: Borradores guardados antes
- **WHEN** la app lee un borrador revisado antes de este cambio
- **THEN** se lee igual, sin categoría elegida
