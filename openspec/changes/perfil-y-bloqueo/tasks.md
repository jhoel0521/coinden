# Tasks

## 1. Preferencia del bloqueo (app-lock: Bloqueo Opcional)

- [x] 1.1 Agregar a la batería de contrato (`RepositoryContracts.kt`) que `appLockEnabled` empieza en `false`, se guarda y se apaga; agregar `appLockEnabled`/`setAppLockEnabled` a `PreferencesRepositoryPort` e implementarlo en memoria y en la base local (bandera `APP_LOCK`, D7).

## 2. Regla pura del bloqueo (app-lock)

- [x] 2.1 Escribir `AppLockTest` (Carlos): desactivado nunca bloquea; activo arranca bloqueado; 30 s en segundo plano no bloquea y 2 min sí (límite exacto en 60 s); qué destinos piden desbloqueo y que Capturar y el formulario nuevo no. Implementar `AppLock` y `AppLockController` (D2).
- [x] 2.2 Definir el puerto `DeviceAuth` con `FakeDeviceAuth` y probar `AppLockController.setEnabled`: con `Success` guarda, con `Cancelled` o `Failed` no cambia, con `NoScreenLock` queda deshabilitado; `ensureUnlocked` desbloquea solo con `Success` (D3).

## 3. Navegación con pestañas (app-navigation)

- [x] 3.1 Probar `NavTabs.order` (Inicio, Revisar, Capturar, Movimientos, Perfil; Capturar al centro) e implementarlo en `LayoutRules.kt` (D5).
- [x] 3.2 Crear `CdNavBar` en `ui/components` con insignia de pendientes, indicador de la elegida y Capturar destacado; íconos `HOME`, `INBOX`, `LIST`, `USER`, `PLUS_CIRCLE`.
- [x] 3.3 Ruta raíz con la pestaña elegida y `CdNavBar`; Capturar abre una hoja con Dictar, Foto y Escribir y al cerrar vuelve a la pestaña donde estaba; formulario, evidencia, corrección, cámara, ajustes, privacidad y modelos siguen a pantalla completa (app-navigation: Capturar desde la Pestaña Central; capture-ui).

## 4. Pantallas (app-navigation, app-onboarding, review-inbox, spending-insights, wallet-settings)

- [x] 4.1 Simplificar `HomeScreen`: patrimonio, aviso "N borradores por revisar" que lleva a Revisar (oculto sin pendientes) y billeteras; quitar accesos, idioma, nota de privacidad y barra de captura. Actualizar la prueba de presentador si hace falta.
- [x] 4.2 Revisar como pestaña: `ReviewInboxScreen` sin botón de cerrar.
- [x] 4.3 `MovementsScreen` con segmentado "Historial | Gastos del mes", reusando `HistoryScreen` y `SpendingInsightsScreen` sin su barra superior.
- [x] 4.4 `ProfileScreen`: Seguridad, Idioma, Billeteras y tasas, Privacidad, Modelos de voz y Acerca de. El interruptor usa `AppLockController.setEnabled`, cubierto por `AppLockTest` (no hizo falta un presentador aparte).
- [x] 4.5 Pantalla de privacidad con la tarjeta del bloqueo, apagada por defecto (app-onboarding: Privacidad).
- [x] 4.6 Textos nuevos en `values`, `values-en` y `values-pt`.

## 5. Bloqueo en la app (app-lock)

- [x] 5.1 `LockedPanel` en las pestañas protegidas mientras está bloqueada, sin crear sus presentadores; la insignia de Revisar no muestra números bloqueada (Saldos Ocultos hasta Desbloquear).
- [x] 5.2 Confirmar desde el resultado de la captura llama a `ensureUnlocked` antes; revisar y corregir (formulario) están detrás del bloqueo; `AppLockTest` cubre que con la autenticación cancelada no se desbloquea (Capturar sin Desbloquear; capture-ui: Resultado de la Captura).
- [x] 5.3 `AndroidDeviceAuth` con `androidx.biometric` (dependencia en `libs.versions.toml`), `MainActivity` como `FragmentActivity`, `ProcessLifecycleOwner` para segundo plano y `FLAG_SECURE` con el bloqueo activo (D3, D4; Volver a Bloquear tras un Minuto; Sin Saldos en Apps Recientes).

## 6. Cierre

- [x] 6.1 `npm test`, `npm run lint`, `npm run build:android` y `openspec validate --all` en verde; `RawMaterialUsageTest` sin usos sueltos.
- [x] 6.2 Emulador (la persona configura el bloqueo de pantalla): bloqueo activo, panel bloqueado tras 65 s y no tras 30 s, borrador capturado sin desbloquear, confirmar pide el PIN y luego confirma, saldos visibles al desbloquear, ventana con `FLAG_SECURE`. Capturas de pestañas y Perfil en `docs/audits/evidence/2026-10-perfil-y-bloqueo/`; con el bloqueo activo el sistema no permite capturas, y la huella no se probó (el emulador solo tiene PIN).

## Workflow follow-up

- PR hacia `main` enlazado a la issue #2; la unión la hace la persona.
- Archivar `perfil-y-bloqueo` tras la unión.
