# expense-categorization Specification

## Purpose
Responde "¿en qué se me fue el dinero?": clasifica cada gasto en una categoría, recomienda categorías desde el primer día con palabras del paquete de idioma, aprende de forma determinista de las correcciones del usuario y reporta el gasto de un período por categoría, sin tocar nunca el libro de dinero.

## Requirements

### Requirement: Categorías Base y Propias
El sistema DEBE (MUST) ofrecer 12 categorías base identificadas sin idioma (comida, mercado, transporte, salud, suscripciones, servicios, hogar, salidas, compras, educación, personas y comisiones) y permitir que cada usuario cree categorías propias con el nombre que escriba, las renombre, las archive sin borrarlas y las reactive. Un gasto sin categoría queda como "Sin categoría".

#### Scenario: Categoría propia
- **WHEN** Carlos crea la categoría "Semillitas"
- **THEN** puede asignarla a sus gastos igual que una categoría base, y se guarda con el nombre tal como lo escribió

#### Scenario: Archivar una categoría propia
- **WHEN** Carlos archiva "Semillitas"
- **THEN** ya no se sugiere ni se ofrece para gastos nuevos, pero los gastos que ya la tenían la conservan en los reportes

#### Scenario: Renombrar una categoría propia
- **WHEN** Carlos tiene dos gastos en "Mascota" y la renombra a "Mascotas"
- **THEN** los dos gastos, el historial y el reporte muestran "Mascotas"

#### Scenario: Reactivar una categoría propia
- **WHEN** Carlos reactiva "Semillitas", que estaba archivada
- **THEN** vuelve a ofrecerse para gastos nuevos

#### Scenario: Nombre vacío
- **WHEN** Carlos intenta crear o renombrar una categoría con el nombre vacío
- **THEN** la app no la guarda y le pide un nombre

### Requirement: Solo los Gastos se Categorizan
El sistema DEBE (MUST) clasificar únicamente movimientos de gasto. Las transferencias, los retiros de efectivo y los cambios de moneda mueven dinero entre billeteras propias y nunca reciben categoría ni aparecen en el reporte de gasto.

#### Scenario: Retiro del cajero
- **WHEN** Carlos confirma un retiro de 200.00 Bs del banco al efectivo
- **THEN** el retiro no recibe categoría y no suma como gasto en ninguna categoría

#### Scenario: Intentar categorizar una transferencia
- **WHEN** el usuario intenta asignar una categoría a una transferencia
- **THEN** el sistema rechaza la operación porque solo los gastos se categorizan

### Requirement: Recomendación de Categoría en Orden de Respaldo
El sistema DEBE (MUST) recomendar la categoría de un gasto a partir de su descripción en este orden: una regla aprendida del usuario (confianza ALTA), una palabra típica de los paquetes de idioma activos (confianza MEDIA) o ninguna (Sin categoría). La regla del usuario siempre gana sobre el paquete. La recomendación indica por qué se eligió.

#### Scenario: Palabra típica del paquete
- **WHEN** Carlos, con el paquete "es", tiene un gasto con la descripción "almuerzo"
- **THEN** el sistema recomienda "comida" con confianza MEDIA porque "almuerzo" es una palabra típica de esa categoría

#### Scenario: La regla del usuario gana
- **WHEN** Carlos tiene la regla "salteña → salidas" y un gasto con la descripción "salteña"
- **THEN** el sistema recomienda "salidas" con confianza ALTA aunque el paquete asocie "salteña" con comida

#### Scenario: Descripción desconocida
- **WHEN** un gasto tiene la descripción "Mariela Quispe Rojas" y no hay regla ni palabra típica
- **THEN** el sistema no recomienda categoría y el gasto queda como Sin categoría

#### Scenario: Recomendación en inglés y portugués
- **WHEN** John tiene un gasto "lunch" y Ana, con el paquete "pt", uno "almoço"
- **THEN** a ambos se les recomienda "comida"

### Requirement: Aprendizaje por Corrección
Cuando el usuario elige o cambia la categoría de un gasto que tiene descripción, el sistema DEBE (MUST) crear o actualizar una regla local de forma determinista: la descripción normalizada pasa a recomendar esa categoría. Las reglas se aplican a descripciones iguales o que contienen la regla como palabras completas, y gana la regla más específica.

#### Scenario: Aprende un comercio
- **WHEN** Carlos elige "comida" para un gasto "POLLOS DEL VALLE" que no tenía recomendación
- **THEN** el próximo gasto "POLLOS DEL VALLE" se recomienda como "comida" con confianza ALTA

#### Scenario: Aprende su forma de escribir
- **WHEN** Carlos elige "transporte" para "trasmporte", que el paquete no reconoce por el error de tipeo
- **THEN** el próximo "page 3 bs para trasmporte" se recomienda como "transporte"

#### Scenario: Cambiar de opinión
- **WHEN** Carlos tiene la regla "salteña → comida" y reclasifica un gasto "salteña" como "salidas"
- **THEN** la regla pasa a recomendar "salidas"

### Requirement: Clasificación Separada del Dinero
El sistema DEBE (MUST) guardar la descripción y la categoría de cada gasto en un registro append-only propio, enlazado al id del movimiento y separado del libro de dinero. Cambiar la categoría agrega una nueva clasificación; la vigente es la más reciente en orden cronológico. Clasificar nunca modifica, anula ni registra movimientos de dinero.

#### Scenario: Reclasificar un gasto confirmado
- **WHEN** Carlos cambia un gasto confirmado de "comida" a "salidas"
- **THEN** el libro de dinero no cambia, el historial conserva ambas clasificaciones y la vigente es "salidas"

#### Scenario: Gasto registrado sin borrador
- **WHEN** un gasto se registró directamente en el libro, sin descripción
- **THEN** el usuario puede clasificarlo indicando la categoría y, opcionalmente, una descripción

### Requirement: Reporte de Gasto por Categoría
El sistema DEBE (MUST) informar, para un usuario y un período, el total gastado por categoría (incluida Sin categoría), por moneda y consolidado en la moneda local con las tasas configuradas, ordenado de mayor a menor. Solo cuentan los gastos vigentes, sin los anulados, de billeteras activas o archivadas.

#### Scenario: El mes de Carlos
- **WHEN** Carlos consulta octubre con gastos de comida por 72.00 Bs, suscripciones por 118.71 Bs, salud por 31.70 Bs y transporte por 3.00 Bs
- **THEN** el reporte muestra suscripciones, comida, salud y transporte en ese orden, con un total de 225.41 Bs

#### Scenario: Gasto anulado
- **WHEN** Carlos anuló un gasto de 54.00 Bs y registró el correcto de 45.00 Bs
- **THEN** el reporte solo cuenta los 45.00 Bs

#### Scenario: Gastos en otra moneda
- **WHEN** Carlos tiene un gasto de 12.00 USD en suscripciones y su tasa es 1 USD = 11.20 BOB
- **THEN** el reporte muestra 12.00 USD en suscripciones y lo suma como 134.40 Bs en el total consolidado

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
