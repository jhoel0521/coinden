# Proposal

## Why

Las specs vigentes ya exigen que las palabras de cada idioma y los formatos de cada banco sean **datos**: `text-interpreter` dice "solo a través de paquetes de idioma definidos como datos... Agregar un idioma no requiere cambiar código" y "plantillas... definida como datos". El código no cumple ninguna de las dos: `LanguagePack.kt` tiene 18 listas con cientos de palabras de es, en y pt escritas en Kotlin, y `ReceiptTemplate.kt` tiene 5 plantillas de bancos bolivianos también en Kotlin.

Hoy eso significa que sumar un idioma, corregir una palabra o soportar el comprobante de un banco nuevo **obliga a publicar una versión de la app**. Para alguien en Perú o Argentina que captura comprobantes de sus bancos, eso es esperar semanas por algo que es un archivo de texto.

El bug está en el código, no en el spec (Manifiesto §1): se corrige el código.

## What Changes

- **Los paquetes de idioma pasan a archivos JSON.** Un paquete es un archivo con su código de idioma y sus listas (etiquetas de comprobante, verbos, meses, monedas, preposiciones, conectores, medios de pago, palabras de categoría). El dominio deja de conocer palabras: las recibe por `InstalledPacksPort`, que ya existe.
- **Las plantillas de comprobantes pasan a archivos JSON.** Mismo camino, mismo puerto.
- **El paquete inglés y las plantillas base viajan dentro de la app** como recursos, para que la app funcione completa sin descargar nada (Manifiesto, principio 1).
- **Los demás paquetes y plantillas se pueden descargar**, con el mismo trato que los modelos de voz: la persona los pide, se muestra el tamaño, se verifica la huella y un archivo que no coincide se borra. Nunca se envía nada de la persona.
- **Un paquete inválido no rompe la app:** se descarta y se sigue con los que sí cargaron.
- **Sin cambios de comportamiento.** Las 408 pruebas del dominio deben pasar igual, con los mismos datos, ahora leídos de archivos. Es la red de seguridad de este cambio.
- **BREAKING interno:** `LanguagePacks.ENGLISH/SPANISH/PORTUGUESE` y `ReceiptTemplates.all` desaparecen como constantes de Kotlin. Nada fuera del repo depende de ellas.

## Capabilities

### New Capabilities
- `language-packs`: los paquetes de idioma y las plantillas de comprobantes como datos. Cubre el formato de los archivos, cuáles viajan con la app, cómo se instalan, se descargan y se verifican, qué pasa con uno inválido y cómo se combinan varios activos.

### Modified Capabilities
- `text-interpreter`: los requisitos "Paquetes de Idioma como Datos" y "Plantillas Conocidas como Datos" dejan de ser una intención y pasan a decir de dónde salen los datos; el intérprete no trae palabras propias.

## Meta de la 0.2.0

Este cambio cumple el **criterio 1** ("ninguna palabra de idioma ni formato de banco en Kotlin; todo vive en archivos de datos que se pueden descargar") y es la base de los criterios 2 y 3: hasta que las palabras no estén en archivos, no se puede decidir cuáles reemplaza el modelo y cuáles quedan.

Aporta además al **criterio 5** ("actualizar nunca borra datos"): las palabras nunca estuvieron en la base, y la tarea 6.2 lo comprueba de punta a punta actualizando sobre una 0.1.0 con datos.

## Impact

- **`core/domain`:** `LanguagePack.kt` y `ReceiptTemplate.kt` se quedan solo con los tipos (las `data class`), sin ninguna constante con palabras. `Readers.kt`, `WalletNameHints.kt` y `Categorizer.kt` no cambian: ya reciben el paquete por parámetro.
- **Serialización:** hace falta leer JSON en `commonMain`. Se declara en `gradle/libs.versions.toml`.
- **`composeApp`:** `AppLanguage.kt` deja de devolver constantes y pasa a cargar los archivos; los paquetes base van en recursos.
- **Pruebas:** las que hoy usan `LanguagePacks.SPANISH` cargan el mismo paquete desde su archivo. Los datos no cambian, así que las aserciones tampoco.
- **Nada toca la base de datos:** las palabras nunca se guardaron en ella, así que `UpdateFromReleaseTest` y `verifySqlDelightMigration` no se ven afectados.
