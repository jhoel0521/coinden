# Diseño

## Contexto

Ver la motivación en `proposal.md` y el contrato en `specs/spending-insights/spec.md`.

- `SpendingByCategoryUseCase` ya filtra eventos vigentes por intervalo semiabierto y devuelve `SpendingReport`, con `CategoryTotal.byCurrency`, `consolidated` y `hasUnconvertedAmounts`. Incluye billeteras archivadas y excluye anulaciones y transferencias.
- `AppContainer` aún no expone ese caso de uso. `App.kt` concentra la navegación; `HomeScreen` presenta saldos y accesos a bandeja e historial.
- `MoneyFormatter`, los recursos de categorías de `CaptureTexts` y las pruebas de presentadores son reutilizables. `customCategoriesOf` permite resolver nombres incluso de categorías archivadas.
- El puerto regional entrega un desfase en minutos, no una zona IANA. La app actual es Android y sus pruebas corren en JVM; la presentación ya usa utilidades de JVM.
- `ProfileRates.DEFAULT_CRYPTO_RATES` contiene un precio de ejemplo de BTC. No se debe presentar como referencia real en el nuevo resumen.

## Objetivos y límites técnicos

- Mantener el cálculo monetario en el caso de uso existente y el calendario/formato en presentación.
- Incorporar pantalla, presentador, navegación y pruebas sin migrar la base ni agregar dependencias.
- Aislar la conexión del reporte para probar cargas fallidas y respuestas fuera de orden.
- No modificar el comportamiento del patrimonio ni corregir globalmente sus referencias cripto dentro de este cambio.

## Decisiones

### 1. Reutilizar el reporte y exigir referencias reales configuradas

Exponer `spendingByCategory` desde `AppContainer`, construido con su contexto de categorización, repositorio de perfiles y `cryptoRates = emptyList()`. Las tasas explícitas de `referenceRates` siguen disponibles; BTC sin referencia queda sin convertir. Así no se duplica el cálculo ni se introduce un precio de ejemplo en esta capacidad. La alternativa de usar los valores cripto por defecto produciría una estimación engañosa; cambiar globalmente `ProfileRates` ampliaría el alcance.

`SpendingInsightsPresenter` transforma el reporte en un estado con período, filas, total, indicador de parcialidad y nombres propios resueltos por id. Nunca lee la representación interna de `Money`. Las sumas adicionales, si se requieren para presentación, usan exclusivamente sus operaciones públicas.

### 2. Calendario civil en presentación

Crear una utilidad `SpendingMonth` (año/mes) en presentación con `java.time.YearMonth`, `Instant` y `ZoneOffset`, sin librerías nuevas. Produce `[inicio, siguienteInicio)` en milisegundos para el caso de uso. El mes inicial sale de `ClockPort` y del desfase regional; no se fija octubre en código. Navegar conserva el período y limita el avance al mes actual.

Se usa el mismo desfase que ofrece hoy la app, también para meses pasados. No se inventan reglas de horario de verano: resolver zonas históricas exigiría ampliar el puerto y queda fuera de este cambio. Mantener la utilidad en presentación facilita reemplazarla cuando haya otros targets. Se descarta contar meses como bloques de 30 días y duplicar un algoritmo de calendario propio.

### 3. Estado de consulta consistente

Usar `StateFlow` como los otros presentadores, con estados de carga, resultado y error tipado. Al cargar un período se retiran los importes anteriores. Cada solicitud tiene una generación; solo la última publica resultado o error. La cancelación de corrutinas se propaga y no se convierte en fallo de lectura. Reintentar consulta el mes seleccionado; al entrar otra vez se releen movimientos, categorías y tasas.

Un puerto funcional local de consulta suspendida o una función inyectada permite controlar demoras y errores en pruebas sin cambiar los puertos del dominio. Se prefiere esa separación a acoplar el presentador a Compose o agregar un framework de inyección.

### 4. Lista legible con total y desglose

Agregar la ruta del resumen y un acceso en Inicio. La pantalla usa el tema Cobre, encabezado con volver, mes/año con anterior/siguiente y lista de categorías. Cada fila muestra importe consolidado y montos nativos diferenciados, sin sugerir que deban sumarse entre sí. No hay gráficos ni porcentajes en esta entrega.

Si alguna fila tiene `hasUnconvertedAmounts`, el total se rotula parcial y las filas afectadas indican que falta tasa. Si todas las filas carecen de equivalencia, mostrar «Sin importe convertido» junto al aviso, en vez de un cero que parezca un mes vacío. Solo `rows.isEmpty()` activa el estado «Sin gastos». Con conversiones válidas cuyo resultado redondea a cero se conserva el cero calculado, sin confundirlo con ausencia de tasa.

Las categorías base se traducen con los recursos existentes y las propias se resuelven desde el repositorio incluyendo archivadas; un id ausente usa una etiqueta localizada de categoría no disponible. El mes se representa con año numérico y recurso de nombre de mes para el idioma de la app. Los textos nuevos viven en los tres archivos `strings.xml` y los montos pasan por `MoneyFormatter`.

### 5. Verificación por comportamiento

Primero pruebas rojas de calendario y presentador, luego implementación. Reutilizar los escenarios de Carlos, John y Sophie y el contenedor en memoria; probar el recorrido real de confirmar/corregir al consultar para verificar integración, no reimplementar las reglas del dominio en el presentador. Cubrir cada escenario del nuevo spec en pruebas automatizadas y complementar navegación, accesibilidad, textos y modo avión con comprobación Android.

## Riesgos y compromisos

- Desfase fijo para meses con horario de verano histórico: se documenta el contrato actual y se prueban sus límites; no se promete una zona histórica que el puerto no ofrece.
- Totales pasados cambian al cambiar tasas: texto visible de estimación con referencias actuales; los montos originales permanecen visibles.
- Lectura del libro completo en cada consulta: reutilizar el caso de uso existente y cargar fuera del hilo de UI mediante la capa de presentación; optimizar o paginar solo con evidencia de lentitud.
- Lecturas de movimientos y metadatos no forman una instantánea transaccional: recargar al entrar y evitar resultados fuera de orden. No añadir caché monetaria persistida.
- Sin dispositivo/emulador disponible no se puede dar por validada la experiencia Android: registrar qué verificaciones manuales quedaron pendientes, sin marcarlas como realizadas.

## Despliegue y reversión

No hay migración de datos. Tras pruebas, lint y build, instalar el APK en dispositivo/emulador y recorrer Inicio → resumen → mes anterior → volver; confirmar/corregir un gasto y comprobar la recarga. Revertir los archivos de esta funcionalidad elimina el acceso sin modificar los movimientos guardados. No archivar el cambio ni hacer commit sin la petición correspondiente.
