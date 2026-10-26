# Tasks

## 1. Red de seguridad antes de mover nada

- [x] 1.1 Prueba que recorre los archivos de `core/domain` y falla si alguno declara una lista de palabras de idioma (la deuda no vuelve a entrar).
- [x] 1.2 Prueba que congela los datos de hoy: para cada paquete (en, es, pt) y cada plantilla, el conjunto exacto de palabras y etiquetas que tiene antes de la mudanza.

## 2. Formato de los archivos (language-packs)

- [x] 2.1 Pruebas del formato: un paquete válido se lee entero; uno sin `formatVersion` se descarta; uno con `formatVersion` mayor se ignora sin fallar; uno dañado se descarta y los demás siguen.
- [x] 2.2 Tipos de serialización del paquete y de la plantilla, con `formatVersion`, y la dependencia declarada en `gradle/libs.versions.toml`.
- [x] 2.3 Prueba: todas las palabras de todos los archivos están normalizadas (sin tildes, minúsculas).

## 3. Mudanza de los datos

- [x] 3.1 `en.json` con el contenido exacto de `LanguagePacks.ENGLISH`.
- [x] 3.2 `es.json` y `pt.json` con el contenido exacto de `LanguagePacks.SPANISH` y `PORTUGUESE`.
- [x] 3.3 Las 4 plantillas de `ReceiptTemplates` a sus archivos.
- [x] 3.4 Borrar `object LanguagePacks` y `object ReceiptTemplates`; `LanguagePack.kt` y `ReceiptTemplate.kt` se quedan solo con los tipos.
- [x] 3.5 Las pruebas que usaban las constantes cargan los archivos, **sin cambiar ninguna aserción**. La prueba 1.2 confirma que no se perdió nada.

## 4. Carga en la app

- [x] 4.1 Pruebas del cargador: junta los de recursos con los descargados; ante la misma palabra en dos paquetes gana el del idioma de la app; un archivo inválido no impide cargar los demás.
- [x] 4.2 `PackLoader` y el adaptador de `InstalledPacksPort` que lo usa; `en.json` y las plantillas base como recursos de la app.
- [x] 4.3 `AppLanguage.kt` deja de devolver constantes y pasa por el cargador.
- [x] 4.4 Prueba de primer arranque: sin red y sin nada descargado, "paid 12 usd for lunch" propone 12.00 USD con la nota "lunch".

## 5. Descarga y verificación

- [x] 5.1 Pruebas: se muestra el tamaño antes; se verifica la huella; un archivo que no coincide se borra y no se usa; la petición no lleva datos de la persona; se puede borrar lo descargado.
- [x] 5.2 Descarga de paquetes y plantillas sobre el mismo mecanismo que los modelos de voz, sin duplicarlo.

## 6. Cierre

- [x] 6.1 `npm test`, `npm run lint`, `npm run build:android`, `bundleRelease` y `openspec validate --all` en verde.
- [x] 6.2 Emulador: instalar la 0.1.0 con datos, **actualizar sin desinstalar** y comprobar que las capturas siguen interpretándose igual (las palabras no están en la base, pero se verifica de punta a punta). Hecho el 26-oct-2026 en Android 14: dos actualizaciones sin desinstalar, saldos intactos (2.690,00 BOB) y los comprobantes compartidos se interpretaron con las plantillas del paquete (25,00 / 60,00 / 10,00 BOB). La carpeta `files/packs` del teléfono está vacía: se usaron los paquetes incluidos en el APK.

## Workflow follow-up

- Un solo PR.
- Después de este cambio sigue `interprete-por-modelo`, que decide qué listas sobreviven y cuáles las reemplaza el modelo.
