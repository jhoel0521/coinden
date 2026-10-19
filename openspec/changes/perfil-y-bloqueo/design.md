# Design

## Context

- `App.kt` arma un `NavHost` con rutas planas (`HOME`, `INBOX`, `HISTORY`, `SPENDING`, `SETTINGS`, `FORM`, …). Inicio (`home/HomeScreen.kt`) enlaza a todo y lleva la `CaptureBar` (`capture/CaptureViews.kt`) al pie.
- La base está cifrada con SQLCipher; su llave vive en el Android Keystore y se abre sin pedir nada (`core/data-local/.../AndroidDatabase.kt`). La captura en segundo plano (`QuickCapture`, compartir) escribe en esa base sin interfaz.
- Las preferencias son banderas clave-valor en la base local (`LocalRepositories.kt`, `GOOGLE_READER`) y en memoria (`InMemoryRepositories.kt`), con una batería de contrato común (`RepositoryContracts.kt`).
- `MainActivity` es un `ComponentActivity` y usa `installSplashScreen()`.

## Goals / Non-Goals

**Goals:**
- Que la regla de bloqueo sea lógica pura con pruebas, separada de la plataforma.
- Que mientras la app está bloqueada no se lea ningún saldo, ni siquiera en memoria.

**Non-Goals:**
- Cifrar la base con una llave derivada del PIN: la captura sin desbloquear necesita la base abierta.
- PIN propio de CoinDen, tiempo configurable u ocultar montos dentro de la captura.
- iOS: el puerto queda listo; su adaptador llega con `ios-shell`.

## Decisions

### D1. El bloqueo es una puerta de presentación
La base sigue abriéndose con la llave del Keystore. El bloqueo decide qué pantallas se componen: mientras está bloqueada, las pestañas protegidas muestran `LockedPanel` y **no se crean** sus presentadores (`HomePresenter`, `ReviewInboxPresenter`, `HistoryPresenter`, `SpendingInsightsPresenter`, `WalletSettingsPresenter`), así que no se leen saldos.
Alternativa descartada: derivar la llave de la base del PIN. Impediría capturar sin desbloquear, que es la regla central.

### D2. Regla pura `AppLock` (`composeApp/commonMain/.../lock/AppLock.kt`)
- `data class AppLockState(enabled: Boolean, unlocked: Boolean, backgroundSince: Instant?)` y funciones puras:
  - `start(enabled)`: arranca bloqueada si está activo.
  - `onBackground(now)` y `onForeground(now)`: vuelve a bloquear si `now - backgroundSince >= 60 s`.
  - `unlock()` y `isLocked()`.
- `requiresUnlock(destination: LockedDestination)`: `HOME`, `REVIEW`, `MOVEMENTS`, `PROFILE`, `SETTINGS`, `CONFIRM` y `EVIDENCE` piden desbloqueo; `CAPTURE` y `FORM_NEW` no.
- El tiempo viene del `ClockPort` del contenedor. Un `AppLockController` guarda el estado en un `StateFlow` y lo comparten la barra, las pestañas y la confirmación.

### D3. Puerto `DeviceAuth` (`composeApp/commonMain/.../lock/DeviceAuth.kt`)
- `availability(): AuthAvailability` puede ser `Available`, `NoScreenLock` o `Unsupported`.
- `suspend fun authenticate(): AuthResult` puede ser `Success`, `Cancelled` o `Failed`.
- En `androidMain`, `AndroidDeviceAuth` usa `androidx.biometric.BiometricPrompt` y `BiometricManager.canAuthenticate`:
  - API 30 o más: `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`;
  - API 26 a 29: `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`, porque `DEVICE_CREDENTIAL` solo se combina con `STRONG` desde la API 30.
- `MainActivity` pasa a `FragmentActivity`, que `BiometricPrompt` necesita.
- En las pruebas, `FakeDeviceAuth` devuelve un resultado fijo.

### D4. Ciclo de vida y apps recientes
- `ProcessLifecycleOwner` (`androidx.lifecycle:lifecycle-process`) avisa `ON_STOP` y `ON_START` a `AppLockController.onBackground/onForeground`.
- `MainActivity` observa el estado: con el bloqueo activo pone `WindowManager.LayoutParams.FLAG_SECURE`, y lo quita al desactivarlo.

### D5. Navegación con pestañas
- Una ruta raíz `ROOT` compone un andamio con la pestaña elegida y `CdNavBar` abajo; la pestaña elegida se guarda con `rememberSaveable`.
- `FORM`, `REVIEW`, `CORRECT`, `EVIDENCE`, `CAMERA`, `SETTINGS`, `PRIVACY` y `VOICE_MODELS` siguen como rutas a pantalla completa, sin barra.
- `NavTabs.order` (regla pura en `ui/components/LayoutRules.kt`): `[HOME, REVIEW, CAPTURE, MOVEMENTS, PROFILE]`, con Capturar en el índice central. Probado.
- `CdNavBar(tabs, selected, onSelect, badges)`:
  - cada pestaña lleva ícono y etiqueta, y un área táctil de al menos 48 dp;
  - la elegida se marca con un indicador de fondo, el ícono en `primary` y la etiqueta en negrita;
  - Capturar es un círculo `primary` de 56 dp y no queda "elegido": abre una `CdSheet` con la `CaptureBar` existente.
- Íconos nuevos en `CoinDenIcon`: `HOME`, `INBOX`, `LIST`, `USER`, `PLUS_CIRCLE`. Revisar muestra `CdBadge` con el número de pendientes, que viene de `HomePresenter`; con la app bloqueada, el número no se muestra.

### D6. Pantallas
- `HomeScreen` conserva la marca, `CdCard` de patrimonio, un aviso "N borradores por revisar" que cambia a la pestaña Revisar (`CdNotice` con acción) y la lista de billeteras. Se quitan los accesos, el idioma, la nota de privacidad y la `CaptureBar`.
- `movements/MovementsScreen.kt`: `CdChoice` segmentado "Historial | Gastos del mes" encima del contenido de `HistoryScreen` o de `SpendingInsightsScreen`. Esas pantallas ganan un parámetro `showTopBar = false` para ocultar su barra cuando van dentro de la pestaña.
- `profile/ProfileScreen.kt` y `ProfilePresenter`:
  - Seguridad: `CdSwitchRow` y estado. Activar o desactivar llama a `DeviceAuth.authenticate()` y solo con `Success` guarda `setAppLockEnabled`.
  - Idioma: `CdChoice`.
  - Billeteras y tasas, Privacidad y Modelos de voz: `CdListOption`.
  - Acerca de: versión.
- `LockedPanel` (`lock/LockedPanel.kt`): ícono `LOCK`, el texto "Desbloquea para ver tus saldos" y `CdActions` con "Desbloquear".
- Confirmar con la app bloqueada (`CaptureResultSheet`, formulario): la vista llama a `lock.ensureUnlocked()`. Si devuelve `true`, confirma; si no, no hace nada.
- Privacidad (`PerceptionScreens.kt`): una `CdCard` con `CdSwitchRow` del bloqueo, con la misma lógica de `ProfilePresenter`, reutilizada desde `AppLockController.setEnabled(enabled, auth)`.

### D7. Preferencia
- `PreferencesRepositoryPort` gana `appLockEnabled(): Boolean`, que es `false` por defecto, y `setAppLockEnabled(Boolean)`.
- La base local la guarda como la bandera `APP_LOCK`, igual que `GOOGLE_READER`, sin migración de esquema.
- La batería de contrato prueba los dos adaptadores.

### D8. Dependencias nuevas
`androidx.biometric:biometric` y `androidx.lifecycle:lifecycle-process`, declaradas en `gradle/libs.versions.toml` y usadas solo en `androidMain`.

## Risks / Trade-offs

- **Quien tenga acceso root puede leer la base, porque la llave no depende del PIN.** Es el mismo modelo de hoy; el bloqueo protege de miradas con el teléfono desbloqueado, no de un atacante con root. Queda escrito en Acerca de / Privacidad.
- **Teléfono sin bloqueo de pantalla:** la opción se deshabilita con un aviso. No hay modo de forzarlo.
- **La persona quita el bloqueo del teléfono después de activar el de CoinDen:** `availability()` deja de ser `Available`. En ese caso, la app desbloquea y desactiva el bloqueo con un aviso, para no dejar a nadie fuera de sus datos.
- **El emulador necesita un bloqueo de pantalla para probar:** lo configura la persona.
