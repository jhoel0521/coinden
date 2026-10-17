# Tareas: cierre de UX/UI del MVP

Cada bloque funcional se entrega con pruebas, recursos es/en/pt, actualización de continuidad, commit convencional en español y push. No marcar un bloque terminado si solo está parcialmente implementado.

## 1. Entorno Android y línea base

- [x] 1.1 Instalar emulador e imagen oficial API 36 x86_64, comprobar aceleración y crear AVD propio `CoinDen_MVP_API36`; verificar arranque con `adb devices` y `sys.boot_completed`, sin cambiar características Windows ni borrar AVD ajenos.
- [x] 1.2 Instalar el APK de línea base y registrar versión de imagen, dispositivo, capturas y recorridos alcanzados en `continuidad.md`; verificar apertura y ausencia de crash en logcat.

## 2. Guardado único y errores del formulario

- [ ] 2.1 Cubrir con pruebas rojas los cuatro escenarios de «Acciones del Formulario sin Duplicados» en `capture-ui`; implementar bloqueo, progreso, recuperación y resultado único en presentador/vista. Verificar pruebas dirigidas de formulario y que los saldos no cambian al guardar.
- [ ] 2.2 Registrar resultados y comandos de 2.1 en `continuidad.md`, verificar lint y traducciones, commitear y pushear el bloque; comprobar igualdad de HEAD y rama remota.

## 3. Salida consciente y evidencia

- [ ] 3.1 Cubrir primero los cinco escenarios de «Salida Consciente del Formulario» en `capture-ui`; implementar baseline, diálogo y Atrás del sistema, conservar edición al volver de evidencia y bloquear salida durante escritura. Verificar lógica con pruebas de presentador y navegación/control superior/sistema en Android.
- [ ] 3.2 Registrar resultados y comandos de 3.1, verificar lint y recursos es/en/pt, commitear y pushear el bloque; comprobar igualdad de HEAD y rama remota.

## 4. Carga y recuperación de pantallas

- [ ] 4.1 Cubrir primero los cuatro escenarios de «Carga Recuperable de Inicio» y los cinco de «Carga Recuperable de Bandeja e Historial», incluyendo cada pantalla, error/reintento y carreras con lecturas suspendidas. Implementar estados, señal de datos anteriores y deshabilitar acciones incompatibles; verificar pruebas dirigidas y cancelación propagada.
- [ ] 4.2 Registrar resultados y comandos de 4.1, verificar lint, recursos es/en/pt y presentación de estados, commitear y pushear el bloque; comprobar igualdad de HEAD y rama remota.

## 5. Nombres de categorías propias

- [ ] 5.1 Cubrir primero los cuatro escenarios de «Nombre de Categorías Propias en el Recorrido»; resolver nombres activos/archivados, fallback sin IDs y selección/comparación del formulario. Verificar tarjetas, historial y correcciones con las pruebas de Carlos, John y Sophie.
- [ ] 5.2 Registrar resultados y comandos de 5.1, verificar lint y recursos, commitear y pushear el bloque; comprobar igualdad de HEAD y rama remota.

## 6. Claridad del patrimonio y alcance del MVP

- [ ] 6.1 Cubrir primero los tres escenarios de «Patrimonio Parcial Explícito»; implementar etiquetas y explicación es/en/pt sin cambiar importes. Mover cotizaciones automáticas a pos-MVP en README y documentar tasas manuales. Verificar pruebas de Inicio, recursos y diff monetario sin cambios de dominio.
- [ ] 6.2 Registrar resultados y comandos de 6.1, verificar lint, commitear y pushear el bloque; comprobar igualdad de HEAD y rama remota.

## 7. Integración y auditoría visual de cierre

- [ ] 7.1 Ejecutar `npm test`, `npm run lint`, `npm run build:android`, `openspec validate mvp-ux-polish --strict` y pruebas de cifrado Android en el emulador; registrar comandos, resultados y fallos reales en `continuidad.md`.
- [ ] 7.2 Instalar APK corregido y recorrer onboarding, captura/revisión/confirmación, doble toque, salir/cancelar salida, historial, billeteras/tasas y resumen. Repetir pantallas clave en es/en/pt, claro/oscuro, ancho reducido y fuente grande con teclado; registrar capturas y limitaciones, sin atribuir pruebas de hardware físico al emulador.
- [ ] 7.3 Actualizar el informe de auditoría y seguimiento de issue #2 con evidencia de lo ejecutado y pendientes reales; commitear y pushear el cierre verificado, comprobar árbol limpio y HEAD remoto idéntico.

## Seguimiento del flujo

- Solicitar revisión del resultado y archivar solo cuando se cumplan los requisitos o la persona acuerde explícitamente las excepciones.
- No añadir cotizaciones automáticas: la persona las dejó para después del MVP.
- Al interrumpirse, retomar el primer checkbox pendiente, leer `continuidad.md` y consultar las instrucciones apply del cambio antes de editar.
