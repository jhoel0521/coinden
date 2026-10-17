# Diseño

## Contexto

Los casos de uso de creación, archivado, reactivación y predeterminada ya existen. Los perfiles guardan referencias exactas y el almacenamiento local tiene contratos comunes con memoria. Falta su acceso después del onboarding. La bienvenida referencia tres claves de saludo deliberadamente; no es una traducción ausente. `ProvideAppLocale` además reinicia todo el árbol con `key(language)` y modifica recursos globales.

## Decisiones

- `WalletSettingsPresenter` usa `AppContainer` y casos de uso existentes, con estados traducibles y exclusión de acciones concurrentes mediante una bandera ocupada antes de suspender. Valida propiedad y evita archivar la última activa; la predeterminada se reasigna con el criterio existente.
- Las referencias usan `ExchangeRate.parse` desde `AmountReader`, nunca flotantes. Quitar una referencia elimina también su respaldo legado si corresponde. El perfil conserva moneda principal, monedas propias y demás referencias. No se edita el libro.
- Pantalla desplazable con bloques de billeteras, formulario de creación y referencias; botones explícitos, aviso de que las tasas solo valoran totales, confirmación antes de archivar. Se reutilizan etiquetas de tipo y campos del onboarding; controles de al menos 48 dp.
- Corregir recursos Android mediante un contexto configurado para el idioma elegido y `LocalConfiguration`, conservando el árbol de navegación. El saludo usa una sola clave traducida. Los nombres escritos por la persona permanecen intactos.
- Icono vectorial desde las rutas del isotipo existente, con fondo Cobre y margen seguro de iconos adaptables; no se genera una nueva imagen ni marca. Una prueba verifica manifiesto, recursos y empaquetado.
- Auditoría estática de dinero, estados, navegación, recursos, accesibilidad y empaquetado; corregir hallazgos de este alcance y registrar los demás. Las pruebas de integración cubren repositorios en memoria y persistencia real con cierre/reapertura en JVM donde aplique.

## Riesgos y despliegue

Hallazgos de auditoría incorporados: las escrituras iniciadas se completan en `NonCancellable` aunque se abandone la pantalla; Inicio recibe `cryptoRates = emptyList()` para no revivir un precio BTC de ejemplo después de quitar su referencia. El reporte distingue ausencia de tasa con `canConvert` y deja propagar desbordamientos monetarios como exige el contrato existente; una referencia extrema se presenta con aviso si no cabe su cotización. Cada caso tiene prueba de regresión. No se modifica el formato de los eventos ni las fórmulas del dominio.

No hay migraciones. Varias escrituras de metadatos no son una transacción global: el dominio filtra predeterminadas archivadas y la UI las repara al cargar. Los fallos se informan sin borrar lo guardado. No se promete atomicidad entre operaciones separadas ni verificación visual en teléfono; se registra en la issue #2 y el informe de auditoría.

## Fuera de alcance técnico

Sin cambios de esquema, dependencias nuevas, llamadas de red, reimplementación del cálculo monetario ni cambios de política del dominio.
