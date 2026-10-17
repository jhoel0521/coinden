# Proposal

## Why

La validación de la issue #2 mostró que cada pantalla arma sus botones, chips, campos y tarjetas a mano, con unos 150 usos sueltos de Material repartidos en 11 pantallas. El resultado es desparejo:
- Los grupos de chips se cortan en dos filas y dejan una opción sola abajo ("Visa", "Tarjeta", "BRL").
- El formulario tiene 13 categorías en 4 filas y un campo "Moneda" que sobra.
- Las tarjetas de la bandeja ocupan casi toda la pantalla y una tarjeta de "Para revisar" dice "Listo para confirmar".
- Inicio repite el contador y deja "Billeteras y tasas" suelta.

Carlos y su pareja usan la app de noche y con prisa: los controles tienen que verse ordenados y comportarse igual en todas partes.

## What Changes

- **Sistema de componentes propio:** ninguna pantalla usa directamente botones, chips, campos, superficies ni diálogos de Material. Todo pasa por componentes de CoinDen que fijan la estética (tamaños, formas, colores, densidad) y, además, se acomodan según su contexto: solo, con hermanos, como hijo de un grupo o como padre de otros.
- **Reglas de agrupación:**
  - Acciones: una ocupa todo el ancho; dos van lado a lado y del mismo ancho; tres o más se apilan.
  - Elección única:
    - Hasta 4 opciones cortas: botones segmentados en una sola fila (como el `btn-group` de Bootstrap).
    - Listas abiertas, como las billeteras: una fila que se desliza, con la elegida a la vista.
    - Listas largas, como las categorías: las más usadas y "Más…", que abre una cuadrícula de columnas fijas.
  - Ninguna opción queda sola en una fila incompleta.
- **Formulario más corto:**
  - Tipo y Medio pasan a segmentados; Billetera, a fila deslizable; Categoría, a "las 4 más usadas + Más…".
  - Se quita "Moneda": se muestra junto al monto, porque cada billetera tiene una sola moneda.
  - Nota y Referencia van en "Más detalles", plegable.
- **Bandeja compacta:**
  - Tarjetas de dos líneas (nota, monto, billetera y motivo).
  - En "Para revisar" se muestra el motivo ("Billetera sugerida", "Falta el monto"…) en lugar del estado.
  - El detalle completo queda en el formulario.
- **Inicio más limpio:**
  - Un solo contador, en el botón "Revisar (N)".
  - "Billeteras y tasas" como acción del título "Billeteras".
  - En tema oscuro, botones tonales en lugar de solo contorno.
- **Onboarding y ajustes:** los chips de monedas, tipos de billetera y tasas siguen las mismas reglas.
- **Control automático:** una prueba falla si una pantalla vuelve a usar un control de Material directamente.

## Non-Goals

- Cambiar los colores, las tipografías o el contraste de los temas "Cobre" y "Cobre noche".
- Cambiar qué hace cada pantalla: mismos datos, mismas acciones y mismas reglas del dominio.
- La confianza de "almuerzo 25 bnb" (F5 de la issue #2), que va en su propio cambio.
- Pantallas de la plataforma (cámara de CameraX y hoja de compartir del sistema).
- iOS: los componentes son comunes y servirán allí, pero no se prueba en iOS en este cambio.

## Capabilities

### New Capabilities
- `ui-components`: el catálogo de componentes de CoinDen y sus reglas de composición (acciones, grupos de elección, campos, secciones, tarjetas, avisos y diálogos), y la prohibición de usar controles de Material sueltos en las pantallas.

### Modified Capabilities
- `capture-ui`: "Formulario Completo" deja de pedir la moneda (se muestra la de la billetera junto al monto) y agrupa nota y referencia en "Más detalles".
- `app-theme`: "Presentación de Borradores" admite la tarjeta compacta de la bandeja, que en "Para revisar" muestra el motivo en lugar del estado.

## Impact

- `composeApp/commonMain`: nuevo paquete `ui/components`; se migran las 11 pantallas (formulario, captura, bandeja, historial, inicio, gastos del mes, onboarding, privacidad y modelos, ajustes, evidencia, avisos de carga).
- Textos nuevos en es, en y pt (motivos de revisión, "Más…", "Más detalles").
- Pruebas: reglas de agrupación puras en la JVM, más la prueba que prohíbe los controles sueltos. Capturas de antes y después en claro, oscuro y letra grande en el PR.
- Sin dependencias nuevas: los botones segmentados vienen en Material 3.
