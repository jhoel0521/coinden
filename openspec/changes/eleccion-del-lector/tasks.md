# Tasks

## 1. El presentador distingue las tres situaciones

- [x] 1.1 Prueba del presentador: con `privacyNoticeSeen` en falso el estado dice que todavía no hay elección; con la bandera en verdadero el estado trae la elección guardada (con y sin lector). Corre en `:composeApp:testDebugUnitTest`.
- [x] 1.2 El estado de la pantalla deja de ser un único interruptor y expresa las tres situaciones de la tabla del design, sin tocar `PreferencesRepositoryPort` ni sus adaptadores.
- [x] 1.3 Prueba: elegir usar el lector escribe las dos banderas juntas (`setGoogleReaderEnabled(true)` y `setPrivacyNoticeSeen()`); elegir seguir sin él escribe `false` y la bandera de visto.
- [x] 1.4 Prueba: cambiar la elección desde Perfil guarda la nueva y no vuelve a pedir elegir.

## 2. La pantalla pide elegir

- [x] 2.1 La pantalla de privacidad, cuando todavía no hay elección, muestra las dos acciones del lector de Google sin ninguna preseleccionada y sin botón de continuar. Se comprueba en el emulador: la pantalla aparece entre la bienvenida y las monedas.
- [x] 2.2 No se avanza sin elegir: sin tocar ninguna de las dos acciones, la app se queda en la pantalla. Se comprueba en el emulador intentando volver y continuar.
- [x] 2.3 Cuando ya hay elección (se abre desde Perfil), la pantalla muestra el lector como interruptor con su valor actual, como hoy.
- [ ] 2.4 El enlace a la página de datos de Google se abre desde la pantalla y lleva a
  `https://developers.google.com/ml-kit/android-data-disclosure`. El botón **está y se ve** en la
  pantalla, en los dos modos, y usa el mismo `LocalUriHandler` que ya abre enlaces en Licencias y
  Perfil; **falta abrirlo de verdad en el emulador**, que quedó trabado tras una prueba con un
  usuario secundario.

## 3. Los textos

- [x] 3.1 Textos nuevos en `values/strings.xml` (es): las dos acciones, el enlace y su etiqueta.
- [x] 3.2 Los mismos textos en inglés y portugués, sin dejar ninguna clave sin traducir. Se verifica con la guardia de textos que ya corre en las pruebas.

## 4. La navegación

- [x] 4.1 Prueba: desde la pantalla de privacidad, elegir cualquiera de las dos opciones lleva a monedas si falta el onboarding, y a Inicio si ya estaba hecho.
- [x] 4.2 `App.kt` no necesitó cambios: el presentador guarda la elección y `onContinue` sigue sirviendo
  solo para navegar, que es menos superficie que pasarle las dos elecciones.

## 5. Cierre

- [x] 5.1 Emulador sobre la 0.1.0, **sin desinstalar**: hecho el 26-oct-2026. Quien ya eligió (usuario
  principal, saldos 2.690,00 BOB intactos) ve el interruptor y no se le vuelve a pedir; en un usuario
  Android limpio aparece Bienvenida → Privacidad con las dos acciones, volver no deja avanzar, y al
  elegir "Seguir sin él" pasa al paso de monedas.
- [ ] 5.2 Emulador: elegir seguir sin el lector y comprobar que, al escanear un comprobante, no se encola ningún evento nuevo en `com.google.android.datatransport.events` (hoy hay 20 con el lector activo).
- [x] 5.3 `npm test` (864 pruebas, 0 fallos), `npm run lint`, `npm run build:android`, `bundleRelease` y
  `openspec validate --all` (25/25) en verde el 26-oct-2026.

## Workflow follow-up

- Las tres páginas de privacidad del sitio (es, en y pt) ya llevan el enlace y la fecha nueva; se publican al hacer push.
- Al archivar, los deltas de `app-onboarding` y `evidence-perception` pasan al spec principal.
