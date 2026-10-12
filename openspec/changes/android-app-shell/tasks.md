# Tasks: Primera App Instalable

## 1. Build y dominio para Android

- [ ] 1.1 Agregar al catálogo de versiones el Android Gradle Plugin, los plugins de Compose Multiplatform y del compilador de Compose y AndroidX Activity Compose; crear `local.properties` con `sdk.dir` y agregarlo a `.gitignore`; verificar que Gradle resuelve los plugins.
- [ ] 1.2 Agregar `androidTarget` a `:core:domain` (namespace `com.coinden.core.domain`, minSdk 26, compileSdk 34) y verificar que compila para Android, que las 234 pruebas siguen pasando con `:core:domain:jvmTest` y que el código común no importa Android.

## 2. Adaptadores en memoria

- [ ] 2.1 Crear `:core:data-memory` (jvm + android) con los repositorios en memoria con `Mutex`, `UuidIdGenerator`, `SystemClock`, `StaticInstalledPacks` y `FixedDeviceIdentity`; escribir pruebas en `jvmTest` (libro de solo agregar, ids UUID únicos, paquetes y plantillas del catálogo) y verificar que pasan.

## 3. App Compose Multiplatform

- [ ] 3.1 Crear `:composeApp` (androidTarget, applicationId `com.coinden.app`), `MainActivity`, `.editorconfig` con la regla de funciones `@Composable` y una pantalla mínima; verificar que `:composeApp:assembleDebug` genera el APK y que `npm run lint` pasa.
- [ ] 3.2 Implementar `MoneyFormatter` con pruebas (coma y punto decimal según el locale, separadores de miles, los dígitos no cambian, monedas con 0 y 8 decimales) y verificar que pasan con `:composeApp:testDebugUnitTest`.
- [ ] 3.3 Implementar `AppContainer` y `DemoSeed`; probar que al iniciar existen la billetera de efectivo por defecto y la de banco con el alias "qr".
- [ ] 3.4 Escribir `CaptureDemoPresenterTest` con los escenarios ("page 3 bs para trasmporte" → gasto 3 Bs, efectivo sugerida, confianza baja, sin categoría; "almuerzo 45 qr" → 45 Bs, banco, confianza media, comida por "almuerzo"; "hola" → capturado sin propuesta) e implementar `CaptureDemoPresenter` y `CaptureDemoState` hasta que pase.
- [ ] 3.5 Implementar la pantalla Compose de la demo con textos en `strings.xml` (español), incluidos los nombres de las 12 categorías base con un `when` exhaustivo; verificar que compila y que no hay textos fijos en el código de la pantalla.

## 4. Comandos y CI

- [ ] 4.1 Actualizar `package.json` (`test` con los tres módulos y `build:android`), agregar el job `android` al CI que compila y sube el APK, y actualizar la sección "Desarrollo" del README y los comandos de `AGENTS.md`; verificar que `npm test`, `npm run lint` y `npm run build:android` pasan localmente.

## 5. Verificación en el dispositivo

- [ ] 5.1 Instalar el APK en el emulador `Android14_API34`, abrir la app, capturar "page 3 bs para trasmporte" y "almuerzo 45 qr", verificar lo que muestra la pantalla contra los escenarios y guardar una captura como evidencia.

## Workflow follow-up

- Archivar `android-app-shell` con `opsx-archive` tras la revisión.
- Siguiente del MVP: `local-persistence`.
