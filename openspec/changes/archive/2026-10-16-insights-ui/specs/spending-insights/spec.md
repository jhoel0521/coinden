## Purpose

Permite consultar desde CoinDen el gasto confirmado de cada mes por categoría y moneda, con equivalencias estimadas y avisos claros cuando faltan tasas de referencia.

## ADDED Requirements

### Requirement: Acceso al Resumen Mensual

La app DEBE (MUST) ofrecer «Gastos del mes» desde Inicio. Al entrar muestra el mes actual del teléfono y permite volver a Inicio sin alterar movimientos ni borradores.

#### Scenario: Abrir desde Inicio
- **WHEN** Carlos toca «Gastos del mes» el 16 de octubre de 2026
- **THEN** ve el resumen de octubre de 2026 y puede volver a Inicio

### Requirement: Selección de Mes Civil

El resumen DEBE (MUST) permitir retroceder y avanzar por meses, mostrando mes y año. Cada consulta incluye desde el inicio local del primer día hasta antes del inicio del mes siguiente, usando el desfase horario del teléfono al consultar y la fecha de ocurrencia del gasto. No permite avanzar más allá del mes actual.

#### Scenario: Cambiar de año
- **WHEN** Carlos retrocede desde enero de 2026
- **THEN** consulta diciembre de 2025 y puede avanzar nuevamente a enero de 2026

#### Scenario: Límites locales
- **WHEN** Carlos consulta octubre con desfase -04:00 y tiene gastos el 1 de octubre a las 00:00 y el 1 de noviembre a las 00:00 locales
- **THEN** solo el primero pertenece a octubre

#### Scenario: Febrero bisiesto
- **WHEN** Sophie consulta febrero de 2024 con un gasto ocurrido el 29 de febrero
- **THEN** ese gasto está incluido y uno ocurrido el 1 de marzo está excluido

#### Scenario: Límite del mes actual
- **WHEN** Carlos consulta octubre de 2026 y el teléfono está en ese mismo mes
- **THEN** avanzar está deshabilitado y retroceder sigue disponible

### Requirement: Gastos Confirmados por Categoría

El resumen DEBE (MUST) mostrar total y categorías de mayor a menor importe consolidado, incluyendo «Sin categoría» y nombres propios de categorías tal como se guardaron. Solo incluye gastos vigentes, también de billeteras archivadas; excluye borradores, ingresos, transferencias y gastos anulados.

#### Scenario: El mes de Carlos
- **WHEN** Carlos consulta octubre con gastos confirmados de comida por 72.00 BOB, suscripciones por 118.71 BOB, salud por 31.70 BOB y transporte por 3.00 BOB
- **THEN** ve total 225.41 BOB y las categorías en orden suscripciones, comida, salud y transporte

#### Scenario: Pendientes y movimientos que no son gasto
- **WHEN** Carlos tiene un gasto confirmado de 20.00 BOB, un borrador de 50.00 BOB, un ingreso y un cambio de moneda en octubre
- **THEN** el resumen del mes suma solo 20.00 BOB

#### Scenario: Corrección y recarga
- **WHEN** Carlos corrige el almuerzo confirmado de 35.00 a 32.00 BOB y vuelve a entrar al resumen del mismo mes
- **THEN** el reporte cuenta 32.00 BOB una sola vez

#### Scenario: Billetera archivada y categoría propia
- **WHEN** Carlos tiene un gasto confirmado de 8.00 BOB en «Semillitas» y luego archiva esa categoría y su billetera
- **THEN** el resumen del mes conserva los 8.00 BOB bajo el nombre «Semillitas»

#### Scenario: Sin categoría
- **WHEN** Carlos confirma un gasto de 5.00 BOB sin asignarle categoría
- **THEN** aparece bajo «Sin categoría» y suma en el total

### Requirement: Equivalencias Transparentes

El resumen DEBE (MUST) mostrar montos originales por moneda y equivalencias en la moneda principal con las referencias configuradas al consultar. Debe identificar el consolidado como estimado con esas tasas actuales, no como valoración histórica, sin usar precios de ejemplo ni consultar la red. Consultar nunca modifica los movimientos.

#### Scenario: Gasto en dólares
- **WHEN** Carlos tiene 12.00 USD en suscripciones y referencia configurada de 1 USD = 11.20 BOB
- **THEN** ve 12.00 USD y su equivalente de 134.40 BOB dentro del total estimado

#### Scenario: Cambio de referencia
- **WHEN** Carlos vuelve a consultar ese mes después de que su referencia cambia a 10.00 BOB por USD
- **THEN** ve el mismo gasto original de 12.00 USD y una equivalencia de 120.00 BOB, identificada como estimación con las tasas actuales

### Requirement: Monedas sin Tasa Visibles

Si falta conversión para algún gasto, el resumen DEBE (MUST) conservar el importe original e identificar la categoría afectada y el total como parciales. Un mes con gastos sin convertir no se presenta como un mes sin gastos, ni su cero convertible como gasto total completo.

#### Scenario: Reales sin referencia
- **WHEN** Carlos consulta un mes con 20.00 BOB de comida y 50.00 BRL de compras sin referencia para BRL
- **THEN** ve los 50.00 BRL en compras, aviso de conversión pendiente y total parcial de 20.00 BOB

#### Scenario: Todo sin convertir
- **WHEN** Carlos solo tiene gastos de 50.00 BRL sin referencia en un mes
- **THEN** ve esos gastos y un total parcial sin importe convertido, sin mostrar «Sin gastos»

### Requirement: Estados de Consulta Local

La app DEBE (MUST) indicar carga, ausencia de gastos y fallo recuperable con Reintentar, conservando el mes elegido. Cambiar rápido de mes no debe mostrar datos de un mes anterior bajo la nueva etiqueta. El reporte debe funcionar sin conexión.

#### Scenario: Mes vacío
- **WHEN** John consulta un mes sin gastos confirmados
- **THEN** ve total cero en su moneda principal y un mensaje de que no hay gastos ese mes

#### Scenario: Error y reintento
- **WHEN** falla la lectura local de octubre y Carlos toca Reintentar
- **THEN** ve un aviso sin datos obsoletos y se vuelve a consultar octubre sin perder el período elegido

#### Scenario: Cambio rápido de mes
- **WHEN** Carlos solicita septiembre y luego agosto, y la consulta de septiembre termina después
- **THEN** la pantalla conserva agosto con los importes de agosto

#### Scenario: Sin conexión
- **WHEN** Carlos abre el resumen en modo avión con gastos y referencias guardados
- **THEN** consulta los mismos importes que con conexión sin requerir red

### Requirement: Presentación Localizada y Accesible

La app DEBE (MUST) traducir etiquetas, meses, estados y categorías base al idioma elegido (español, inglés o portugués), y formatear montos según el contexto regional existente. Los controles deben tener descripción comprensible y área táctil de al menos 48 dp; los avisos no deben depender solo del color.

#### Scenario: Idioma de John
- **WHEN** John usa la app en inglés y consulta octubre de 2026
- **THEN** ve mes, etiquetas y categorías base en inglés y los montos con el formato regional de su configuración

#### Scenario: Idioma portugués
- **WHEN** Carlos cambia el idioma a portugués y abre el reporte
- **THEN** etiquetas, mes y estados aparecen en portugués y los nombres propios de categorías se conservan

#### Scenario: Controles comprensibles
- **WHEN** Sophie consulta el resumen con lector de pantalla y hay un total parcial
- **THEN** puede identificar los controles de mes y volver, y el aviso de total parcial se expresa con texto accesible
