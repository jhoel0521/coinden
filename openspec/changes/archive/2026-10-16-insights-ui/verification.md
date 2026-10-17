# Verificación de insights-ui

## Resultado ejecutado

- `npm test`: correcto. Incluye las pruebas existentes de dominio, adaptadores y presentación y 9 pruebas nuevas del resumen (7 de presentador, 1 de calendario y 1 de recursos).
- `npm run lint`: correcto.
- `npm run build:android`: correcto. APK: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.
- `openspec validate insights-ui --strict`: correcto.
- `git diff --check`: correcto.

Las primeras pruebas fallaron por ausencia del presentador y calendario. Las pruebas de recursos fallaron antes de agregar las traducciones. Las pruebas actuales pasan con la implementación. La confirmación de las pruebas entrega explícitamente la categoría sugerida, igual que la captura de la app.

## Correspondencia con escenarios

Pruebas en `composeApp/src/commonTest/kotlin/com/coinden/app/insights/` y `composeApp/src/androidUnitTest/kotlin/com/coinden/app/insights/`:

| Escenarios del spec | Evidencia automatizada | Complemento Android pendiente |
|---|---|---|
| Abrir desde Inicio | Mes inicial en `SpendingInsightsPresenterTest`; ruta y callback compilados | Tocar el acceso y volver |
| Cambiar de año; límites locales; febrero bisiesto | `SpendingMonthTest` y prueba de límites del presentador usando eventos reales | Navegación visible |
| Límite del mes actual | Presentador: siguiente no cambia el mes actual y se habilita tras retroceder | Control deshabilitado visible |
| El mes de Carlos; pendientes y movimientos que no son gasto | Presentador: 225.41 BOB, orden y exclusión de borrador, ingreso y transferencia | Comparar pantalla y montos |
| Corrección y recarga | Formulario real 35→32, recarga y total sin duplicados | Recorrido de pantallas |
| Billetera archivada y categoría propia; sin categoría | Presentador: categoría archivada «Semillitas», billetera archivada y gasto sin clasificación | Nombres y etiquetas visibles |
| Gasto en dólares; cambio de referencia | Presentador: 134.40→120 BOB conservando 12 USD | Aviso de estimación y desglose |
| Reales sin referencia; todo sin convertir | Presentador: BRL/BTC sin precios de ejemplo, parcialidad y cero convertido distinguible de ausencia de tasa | Avisos y filas visibles |
| Mes vacío | Estado vacío y cero en USD con John | Mensaje visible |
| Error y reintento; cambio rápido de mes | Presentador: conserva período, retira importes obsoletos, ignora consulta tardía y propaga cancelación | Mensajes y controles |
| Sin conexión | Consulta del presentador con repositorios en memoria y sin puerto de red | Modo avión con base persistida y reapertura |
| Idioma de John; idioma portugués | Recursos es/en/pt, mapeo de meses y pruebas existentes de formato de dinero/traducciones | Cambio de idioma en pantalla |
| Controles comprensibles | Recursos de controles/avisos; implementación con texto, encabezados semánticos y tamaño mínimo de 48 dp | Lector de pantalla, texto ampliado, tema claro/oscuro |

La cobertura automatizada verifica estado, calendario y recursos; no sustituye las comprobaciones visuales ni una prueba automatizada de semántica Compose. El escenario de accesibilidad no queda validado por completo hasta ejecutarlo en Android.

## Pendientes de dispositivo

Seguimiento trasladado a https://github.com/jhoel0521/coinden/issues/2 por petición expresa de la persona al archivar el cambio. Las casillas pendientes del archivo son evidencia de validación no ejecutada, no implementación faltante.

`adb devices` no devolvió ningún dispositivo y no hay emulador instalado en el SDK local. Por tanto, siguen pendientes las partes Android de 3.3 y 3.4, y el recorrido de 4.2. No se han marcado como completadas.

Para validarlas: instalar el APK, consultar el mes actual y el anterior, confirmar un gasto, corregirlo, comprobar el nuevo total y que el anterior no se duplique; repetir la consulta en modo avión y tras reabrir. Revisar idiomas, texto ampliado, tema claro/oscuro y navegación con lector de pantalla.
