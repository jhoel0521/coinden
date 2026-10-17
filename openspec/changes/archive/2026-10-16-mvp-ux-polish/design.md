# Diseño: cierre de UX/UI del MVP

## Contexto

Ver `proposal.md` para motivación y alcance. `DraftFormPresenter.save()` crea una captura por llamada y los botones lanzan corutinas sin bloqueo; no hay estado de escritura ni intercepción de salida. Inicio, historial y bandeja cargan sin estados completos de error. `SpendingInsightsPresenter` ya ofrece un patrón de generación de consultas y cancelación. `CategoryView` transporta la referencia pero no el nombre; varias pantallas usan `category_custom` de forma incondicional. Inicio muestra `home_total` aunque `unconverted` no esté vacío.

## Objetivos y no objetivos

Separar presentación y persistencia, mantener UUID y dinero exacto, y cubrir la lógica con pruebas sin Compose más comprobación Android. No añadir frameworks, migraciones ni lógica monetaria a las vistas. La edición de Ajustes y su recuperación frente a muerte del proceso no se amplían en este cambio.

## Decisiones

### Escrituras del formulario

Añadir estado de carga/escritura/error y un guardián previo a suspender en el presentador; deshabilitar acciones incompatibles y edición durante la escritura. Guardar conserva el ID de la captura una vez creada y el resultado exitoso para impedir duplicados por llamadas concurrentes o reintentos. Tras un fallo posterior se retoma revisión/preparación sobre ese mismo ID. La navegación solo ocurre por resultado exitoso y una vez. No basta con deshabilitar el botón: el presentador también protege su contrato. La firma de `save` puede pasar a resultado nullable o sellado para distinguir ocupado/fallo sin simular éxito; adaptar sus consumidores y pruebas.

Las excepciones recuperables se muestran como error localizado; `CancellationException` se propaga. No convertir errores monetarios en tasas faltantes. No afirmar idempotencia ante una escritura que se confirma en disco y falla antes de devolver su ID: ese caso exige otro contrato de persistencia, fuera de este cambio.

### Edición y salida

Comparar un snapshot de campos editables con el baseline obtenido tras cargar; excluir progreso, directorio y errores. Cambios vueltos al valor original dejan de estar sucios. El aviso ofrece permanecer o salir sin guardar. Interceptar control superior y Atrás Android con un puerto de presentación si `commonMain` no tiene un API compatible; no instalar una librería solo para ello. Mientras se guarda, interceptar la salida sin cancelar la operación. Ir a evidencia no abandona el formulario: conservar el presentador en la entrada de navegación o elevarlo a su alcance para no recargar y pisar edición al volver. No autosalvar ni confirmar por navegar.

### Cargas de Inicio, historial y bandeja

Estado explícito de carga, éxito y error; resultados anteriores opcionales con señal de desactualización. Usar contador de generación como el reporte mensual para ignorar respuestas tardías, sin capturar cancelaciones como fallos. Deshabilitar confirmaciones basadas en una bandeja no disponible o en recarga. Añadir puntos de inyección mínimos o repositorios de prueba suspendibles para reproducir errores y carreras, sin cambiar el dominio. Reutilizar una vista pequeña de carga/error/reintento con textos es/en/pt si evita duplicación, no un framework de estado global.

### Categorías y patrimonio

Resolver nombres propios en presentación mediante el repositorio existente, incluyendo archivadas y respetando propietario. Transportarlos junto a referencias en tarjetas, historial y formulario, incluido el valor anterior. Conservar la categoría propia seleccionada aunque no aparezca entre las categorías base; no agregar una pantalla de CRUD. Fallback localizado si no se puede resolver; nunca IDs. El reporte ya resuelve nombres y puede servir de referencia.

El encabezado de Inicio elige clave total/parcial a partir de `unconverted`; mantiene cada importe original y una explicación explícita. Sin conversiones nuevas ni acceso a la representación de `Money`.

### Verificación y entrega incremental

TDD por bloque: primero una regresión que falle, después código, prueba dirigida, lint y commit con push. Usar la rama existente `codex/rumbo-al-mvp`. Actualizar `tasks.md` y `continuidad.md` con comandos y resultados en el mismo bloque; no marcar validaciones pendientes como hechas. Al finalizar ejecutar suite completa y APK.

Preparar un AVD Android API 36 x86_64 con la imagen oficial del SDK y arrancarlo sin ventana visible si es posible. Primero comprobar aceleración; no activar características Windows ni reiniciar el equipo sin consultar. Si solo arranca sin aceleración, registrar menor rendimiento. Instalar el APK existente para línea base y luego el corregido, sin borrar datos ajenos. Validar onboarding, captura, doble toque, salida, bandeja, historial, tasas manuales y resumen. Repetir vistas clave con es/en/pt, tema oscuro, ancho reducido y fuente grande. Registrar capturas locales y resultados reproducibles; el emulador no demuestra funcionamiento de sensores físicos ni revisión completa con TalkBack.

## Riesgos y compromisos

- Cancelación o cierre durante escritura → bloqueo de salida y prueba suspendida; no se promete atomicidad ante apagado.
- Nombre de categoría ausente → fallback explícito y prueba de archivadas/desconocidas.
- Recursos fuera del viewport → errores visibles junto a acciones, controles de al menos 48 dp y revisión con teclado/fuente grande.
- Emulador sin aceleración → diagnosticar y registrar límite, sin cambiar opciones de seguridad del equipo.
- Hallazgos fuera del alcance → registrarlos y pedir decisión, no añadir nuevas funciones de forma silenciosa.

## Despliegue y reversión

Sin migración de datos. Commits independientes por bloque; cualquier reversión debe ser un nuevo commit solicitado, nunca reescribir historial publicado. El cambio sigue activo hasta completar los escenarios y la verificación o acordar explícitamente excepciones.
