## REMOVED Requirements

### Requirement: Recomendación de Categoría en Orden de Respaldo
**Motivo:** su segundo escalón es "una palabra típica de los paquetes de idioma activos", que es justo la lista por idioma que este cambio elimina. Lo reemplaza "Recomendación de Categoría con Modelo y Reglas", que pone al modelo en ese lugar y deja las reglas de la persona como respaldo.

## ADDED Requirements

### Requirement: Recomendación de Categoría con Modelo y Reglas
El sistema DEBE (MUST) recomendar la categoría de un gasto a partir de su descripción en este orden: una regla aprendida del usuario (confianza ALTA), la categoría que propone el modelo de lenguaje cuando está disponible (confianza MEDIA) o ninguna (Sin categoría). La regla del usuario siempre gana sobre el modelo. La recomendación indica por qué se eligió.

#### Scenario: El modelo reconoce el gasto
- **WHEN** Carlos tiene el modelo instalado y un gasto con la descripción "almuerzo"
- **THEN** el sistema recomienda "comida" con confianza MEDIA porque lo propuso el modelo

#### Scenario: La regla del usuario gana
- **WHEN** Carlos tiene la regla "salteña → salidas" y un gasto con la descripción "salteña"
- **THEN** el sistema recomienda "salidas" con confianza ALTA aunque el modelo proponga comida

#### Scenario: Descripción desconocida
- **WHEN** un gasto tiene la descripción "Mariela Quispe Rojas" y no hay regla ni propuesta del modelo
- **THEN** el sistema no recomienda categoría y el gasto queda como Sin categoría

#### Scenario: Sin modelo quedan las reglas de la persona
- **WHEN** Carlos no tiene modelo y un gasto con la descripción "almuerzo", sin ninguna regla suya que coincida
- **THEN** el gasto queda Sin categoría, y al corregirlo Carlos crea la regla que lo categoriza la próxima vez

#### Scenario: Recomendación en cualquier idioma
- **WHEN** John tiene un gasto "lunch" y Ana uno "almoço", ambos con el modelo instalado
- **THEN** a ambos se les recomienda "comida", sin que haga falta una lista de palabras por idioma

### Requirement: El Modelo Solo Propone Categorías que Existen
El modelo NUNCA DEBE (MUST NOT) inventar una categoría: solo puede proponer una de las que la persona ya tiene, base o propia. Una propuesta fuera de esa lista se descarta.

#### Scenario: Una categoría que no existe
- **WHEN** el modelo propone para un gasto una categoría que la persona no tiene
- **THEN** la propuesta se descarta y el gasto queda Sin categoría

#### Scenario: Una categoría propia de la persona
- **WHEN** Carlos tiene la categoría propia "Somó" y el modelo la propone para un gasto
- **THEN** el gasto queda con la categoría "Somó" con confianza MEDIA
