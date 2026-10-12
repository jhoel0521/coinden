# Design: Primera App Instalable

## Context
Ver proposal.md (Why) y `specs/app-shell/spec.md`. Hoy hay un solo módulo, `:core:domain`, Kotlin Multiplatform con destino `jvm` (JDK 21, Kotlin 2.0.21, Gradle 8.14.3, ktlint 12.1.2). Los adaptadores en memoria existen solo como fakes en `commonTest`. La máquina de desarrollo tiene el Android SDK en `%LOCALAPPDATA%\Android\Sdk` (plataformas 34, 36 y 36.1; build-tools 35 y 36), los emuladores `Android14_API34` y `Small_Phone`, y `ANDROID_HOME` no está definido.

## Goals / Non-Goals

**Goals:**
- Un APK de depuración reproducible localmente y en CI.
- La UI en `commonMain` de Compose Multiplatform, lista para agregar Desktop sin moverla.
- Lógica de pantalla probada en la JVM, sin emulador; el emulador solo para la verificación final.

**Non-Goals:**
- Arquitectura de navegación, temas visuales definitivos y diseño de pantallas del MVP.
- Rendimiento de los adaptadores en memoria con muchos datos.

## Decisions

### D1. Versiones
| Pieza | Versión | Motivo |
|---|---|---|
| Kotlin | 2.0.21 (sin cambio) | evita migrar el dominio |
| Android Gradle Plugin | 8.7.3 | compatible con Gradle 8.14 y Kotlin 2.0 |
| Compose Multiplatform | 1.7.3 | compatible con Kotlin 2.0.21 |
| Compilador de Compose | `org.jetbrains.kotlin.plugin.compose` 2.0.21 | obligatorio desde Kotlin 2.0 |
| AndroidX Activity Compose | 1.9.3 | `setContent` en la Activity |
| compileSdk / targetSdk | 34 | plataforma instalada y soportada por AGP 8.7 |
| minSdk | 26 | Android 8.0 |

Todo entra por `gradle/libs.versions.toml`. Si al compilar alguna combinación falla, se ajusta la versión menor y se documenta en este design.

### D2. Módulos
```
:core:domain       KMP: jvm + androidTarget (com.android.library, namespace com.coinden.core.domain)
:core:data-memory  KMP: jvm + androidTarget; depende de :core:domain
:composeApp        KMP: androidTarget (com.android.application, applicationId com.coinden.app)
                   commonMain = UI y presentador; androidMain = MainActivity y adaptadores de plataforma
```
El dominio necesita `androidTarget` porque un destino Android de KMP no puede consumir un módulo que solo publica `jvm`. El código común del dominio no cambia.

### D3. Adaptadores en memoria de producción
`:core:data-memory` contiene `InMemoryWalletRepository`, `InMemoryDraftRepository`, `InMemoryWalletAliasRepository`, `InMemoryCategorizationRepository`, `InMemoryCurrencyProfileRepository`, `UuidIdGenerator` (`kotlin.uuid.Uuid` con `@OptIn(ExperimentalUuidApi)`), `SystemClock`, `StaticInstalledPacks` (catálogo en/es/pt y plantillas) y `FixedDeviceIdentity`. El acceso a cada colección se serializa con un `Mutex` de coroutines. Los fakes de las pruebas del dominio **no se mueven**: el dominio no puede depender de un módulo que depende de él, y las pruebas siguen deterministas con ids secuenciales.

### D4. AppContainer manual
`AppContainer(regional: RegionalSettingsPort, device: DeviceIdentityPort, clock: ClockPort)`, en `composeApp/commonMain`, crea los repositorios en memoria, los contextos (`LedgerContext`, `CategorizationContext`, `DraftContext`), el `TextDraftInterpreter` y los casos de uso. `DemoSeed.seed(container)` crea al iniciar las billeteras de ejemplo con los casos de uso del dominio: "Billetera" en efectivo BOB como defecto, y "BNB Bs" banco con el alias "qr". En `androidMain`, `MainActivity` construye el contenedor con el locale y el desplazamiento horario actuales del sistema (`Locale.getDefault()`, `TimeZone.getDefault()`).

### D5. Presentador sin Compose, probado en JVM
`CaptureDemoPresenter(container)`, en `commonMain`, expone un `StateFlow<CaptureDemoState>`. Tiene `onTextChange` y `onCapture`. El estado es semántico, sin textos: `DraftCard(kind: MovementKindView, amount: Money?, walletName: String?, walletSuggested: Boolean, confidence: Confidence, category: CategoryView?, status)`. La pantalla Compose solo traduce el estado a textos. Las pruebas del presentador corren en `commonTest` como unit tests de Android en la JVM (`:composeApp:testDebugUnitTest`), sin emulador.

### D6. Textos y formato regional
- Los textos de la UI viven en recursos de Compose Multiplatform (`composeResources/values/strings.xml`, español), incluidos los nombres de las 12 categorías base y de los tipos de movimiento. El mapeo `BaseCategory -> StringResource` es un `when` exhaustivo: una categoría nueva sin texto no compila.
- `MoneyFormatter.format(money, locale)` en `commonMain`, puro: toma `Money.toNumericString()` y aplica los separadores decimal y de miles del locale (`UserLocale.decimalSeparator`, el de miles es el opuesto) y el código o símbolo de la moneda. No reescribe dígitos y se prueba en la JVM.

### D7. ktlint con Compose
`.editorconfig` agrega `ktlint_function_naming_ignore_when_annotated_with = Composable` para que las funciones `@Composable` en PascalCase no fallen el lint.

### D8. Comandos y CI
- `package.json`: `test` ejecuta `:core:domain:jvmTest :core:data-memory:jvmTest :composeApp:testDebugUnitTest`; se agrega `build:android` → `:composeApp:assembleDebug`.
- CI: el job `unit` usa las mismas tareas y un job nuevo `android` corre `./gradlew :composeApp:assembleDebug` en `ubuntu-latest`, que ya trae el Android SDK. El APK se sube como artefacto del workflow.
- Localmente, `local.properties` (agregado a `.gitignore`) define `sdk.dir`.

### D9. Verificación en el emulador
Al final se instala el APK en `Android14_API34` con `adb install`, se abre la app, se escriben las frases de los escenarios y se toma una captura de pantalla como evidencia. No hay pruebas instrumentadas en este cambio.

## Risks / Trade-offs
- [Combinaciones de versiones de AGP, CMP y Kotlin incompatibles] → D1 deja definido qué ajustar; la compilación local y en CI lo detecta enseguida.
- [El dominio compila dos veces (jvm y android)] → El build tarda un poco más; las pruebas siguen corriendo solo en jvm.
- [Datos perdidos al cerrar la app] → Es intencional y está en los non-goals; lo resuelve `local-persistence`.
- [Recursos de Compose en pruebas unitarias] → El presentador no usa recursos (D5), así que las pruebas no los necesitan.

## Migration Plan
No aplica: no hay usuarios ni datos. Se agrega `local.properties` local para compilar.

## Open Questions
- Icono y nombre visible definitivos: por ahora "CoinDen" con el icono por defecto.
