# Propuesta: cierre de UX/UI del MVP

## Por qué

La auditoría estática detectó riesgo de capturas duplicadas, pérdida silenciosa de edición, ausencia de recuperación visible y nombres o totales ambiguos. Antes de declarar listo el MVP hay que corregir estos recorridos y comprobarlos en Android, conservando la identidad Cobre.

## Qué cambia

- Proteger las acciones del formulario frente a toques repetidos y mostrar progreso y errores recuperables.
- Advertir al salir de un formulario con cambios, sin guardar ni confirmar automáticamente.
- Diferenciar carga, vacío y fallo en Inicio, historial y bandeja, con reintento.
- Mostrar nombres de categorías propias consistentemente en tarjetas, historial y formulario.
- Identificar explícitamente el patrimonio parcial cuando falten referencias.
- Validar la interfaz en emulador Android y dejar evidencia, limitaciones y tareas retomables.
- Documentar que las cotizaciones automáticas se posponen hasta después del MVP; permanecen las tasas manuales.

## Capacidades

### Capacidades nuevas

Ninguna: se extienden los contratos existentes.

### Capacidades modificadas

- `capture-ui`: guardado único por formulario, progreso, recuperación y salida consciente.
- `review-inbox`: carga recuperable de bandeja/historial y nombres de categorías propias.
- `app-onboarding`: carga recuperable de Inicio y etiqueta de patrimonio parcial.

## Impacto

Presentadores y pantallas de captura, Inicio, historial y bandeja; recursos es/en/pt; pruebas de presentadores y recursos; documentación de alcance y recorrido Android. No se requieren nuevos servicios ni migraciones de datos. Cada bloque terminado se verifica, se commitea y se pushea en `codex/rumbo-al-mvp` junto con su estado de continuidad.

## Fuera de alcance

Cotizaciones automáticas, nuevas funciones financieras, rediseño de marca, sincronización, respaldo, transacciones nuevas de almacenamiento, garantía frente a muerte del proceso, y certificación del hardware de cámara/micrófono real mediante un emulador. Un hallazgo visual fuera de estos recorridos se registra y se consulta antes de ampliar el alcance.
