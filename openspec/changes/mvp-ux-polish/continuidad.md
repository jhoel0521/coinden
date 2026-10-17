# Continuidad: cierre de UX/UI del MVP

## Estado al 16 de octubre de 2026

- Rama de trabajo: `codex/rumbo-al-mvp`, remoto `origin` (`jhoel0521/coinden`). No crear otra rama sin motivo ni sobrescribir cambios de colegas.
- Plan completo: propuesta, tres deltas, diseño y tareas. `openspec validate mvp-ux-polish --strict` pasó.
- La persona confirmó aplicar el cambio. Entorno (Bloque 1), guardado único (Bloque 2), salida consciente (Bloque 3: `468a056`) y cargas recuperables de Inicio, Bandeja e Historial (Bloque 4: `ad8e78f`) ya están implementados y publicados en `origin/codex/rumbo-al-mvp`.
- Por indicación explícita de la persona en esta sesión, se procede a cerrar en código (con commit y push por cada bloque) los pendientes restantes del MVP omitiendo la ejecución de tests/validación para que otro agente (o en una sesión futura) ejecute la batería de validación (Bloque 7):
  1. **Bloque 5:** Resolver nombres de categorías propias (activas, archivadas y fallback sin ID) en tarjetas de borrador, historial y formulario (conservar selección y mostrar nombre en `"antes: ..."`).
  2. **Bloque 6:** Identificar explícitamente el **patrimonio parcial** y su explicación (`es`/`en`/`pt`) en Inicio cuando hay monedas sin tasa de referencia, y mover `rate-fetch-adapter` a pos-MVP en `README.md`.
  3. **Pulido de íconos, UX/UI y compatibilidad KMP/iOS:** Completar trazos de "C", "D" y boca en `ic_launcher_monochrome.xml` (Android 13+), aplicar números tabulares (`tnum`) en montos de `SpendingInsightsScreen` e `HistoryScreen`, asegurar altura mínima de `48.dp` en botones restantes, y limpiar APIs exclusivas de JVM en `commonMain` (`Math.floorDiv`/`floorMod` en `SameMovementUseCases.kt` y `TimeFormatter.kt`, y `java.time.*` en `SpendingMonth.kt`).
- Se mantienen las entregas incrementales con commit y push. Marcar cada tarea de publicación solo después de verificar el remoto.
- Cotizaciones automáticas fuera del MVP por decisión explícita. El bloque 6 actualizará README; el plan ya conserva esta decisión.

## Entorno local

- Repo: `C:\laragon\www\coinden`.
- JDK: `C:\Program Files\Android\Android Studio\jbr`; fijar `JAVA_HOME` en la sesión si el terminal conserva Java 8.
- SDK: `C:\Users\Victus\AppData\Local\Android\Sdk`.
- Instalados emulador 37.2.12.0 (build 16428233) e imagen `system-images;android-36;google_apis;x86_64`, revisión 7, Android 16, extensión 17. `package.xml` presente y AVD reconocido.
- `emulator.exe -accel-check`: `WHPX(10.0.22631) is installed and usable`, código 0. No hizo falta activar características ni reiniciar Windows.
- Log de instalación local: `C:\Users\Victus\AppData\Local\Temp\coinden-emulator-install.log`.
- AVD creado: `CoinDen_MVP_API36`, perfil Pixel 2, 1080×1920, 2 cores, RAM efectiva 2560 MB; renderizado SwiftShader, sin ventana, WHPX operativo. En esta sesión responde como `emulator-5554` y `sys.boot_completed=1`. El proceso queda ejecutándose; no arrancar una segunda copia del mismo AVD.

## Preparación ejecutada y línea base

- sdkmanager instaló el emulador pero dejó la imagen detenida a cero bytes. Se interrumpió esa descarga y se obtuvo el ZIP directamente de `https://dl.google.com/android/repository/sys-img/google_apis/x86_64-36_r07.zip` con curl. Tamaño: 1.895.447.397 bytes. SHA-1 del catálogo oficial de SDK y del archivo coinciden: `c6bf44bdcd885bb902b4ba752d111a073ad7a817`. Se extrajo bajo el directorio SDK de imágenes; avdmanager reconoció el paquete y creó su metadata. El ZIP permanece en Temp, no en Git.
- Creación: `avdmanager.bat create avd --name CoinDen_MVP_API36 --package "system-images;android-36;google_apis;x86_64" --device pixel_2`, contestando `no`. Emitió aviso de `devices.xml` ausente, pero el AVD quedó creado y arrancó correctamente; no se consideró suficiente el código de salida para darlo por operativo.
- Arranque: `emulator.exe -avd CoinDen_MVP_API36 -no-window -no-audio -no-boot-anim -gpu swiftshader -no-snapshot -memory 2048 -cores 2`. En PowerShell usar `Start-Process -WindowStyle Hidden` y redirigir stdout/stderr a archivos Temp.
- El primer arranque tuvo ADB offline; `adb reconnect offline` restableció el enlace. Se esperó a `adb -s emulator-5554 shell getprop sys.boot_completed` = `1` antes de instalar.
- Instalación: `adb -s emulator-5554 install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk` → `Success`.
- Apertura: `adb -s emulator-5554 shell am start -W -n com.coinden.app/.MainActivity` → `Status: ok`; `dumpsys activity activities` confirmó `topResumedActivity` de CoinDen y `pidof com.coinden.app` devolvió un proceso vivo.
- En el primer arranque apareció un ANR de **System UI**, no una excepción de CoinDen. Se pulsó Wait y el sistema se recuperó; la muestra consultada de `logcat -s AndroidRuntime` no mostró crash. No equivale a demostrar ausencia de ANR en recorridos posteriores.
- Evidencia versionada: `evidencia/bienvenida-en.png` y `evidencia/bienvenida-es.png`. Se verificó visualmente apertura de bienvenida y cambio inglés → español, con saludo, explicación y botón traducidos. No se completó onboarding ni se validó todavía portugués, fuentes grandes u otras pantallas.
- `npm test` → `BUILD SUCCESSFUL`; 562 pruebas registradas, cero fallos/errores. Gradle reutilizó resultados `UP-TO-DATE` donde no hubo cambios; no presentarlo como nuevas regresiones ejecutadas contra arreglos aún inexistentes. Log local: `C:\Users\Victus\AppData\Local\Temp\coinden-ux-baseline-tests.log`.
- `openspec validate mvp-ux-polish --strict` pasó antes de publicar el plan. Commit inicial ya pusheado: `a09b616`.
- Referencia de arranque: https://developer.android.com/studio/run/emulator-commandline . El AVD es un recurso local: colegas en otros equipos deben crearlo con estos comandos, no esperar recibirlo mediante Git.

## Cómo continuar

1. Leer AGENTS, MANIFIESTO y README; comprobar `git status` y el remoto antes de editar.
2. Consultar `openspec status --change mvp-ux-polish --json` y `openspec instructions apply --change mvp-ux-polish --json`; leer los archivos indicados y usar sus líneas actuales de tareas.
3. Verificar entorno/emulador y continuar por el primer checkbox pendiente. El entorno no bloquea escribir pruebas JVM del siguiente bloque mientras descarga la imagen.
4. Cada bloque funcional: prueba roja, arreglo mínimo, prueba verde, lint, actualización de este registro y tareas, commit convencional en español sin saltar hooks y `git push origin codex/rumbo-al-mvp`.
5. Confirmar que `git rev-parse HEAD` coincide con `git rev-parse origin/codex/rumbo-al-mvp` después del push; no afirmar publicado si falla.
6. No cerrar issue #2 ni archivar hasta ejecutar los recorridos o acordar explícitamente las excepciones.

## Evidencia y límites

### Bloque 2: guardado único

Publicado y HEAD remoto verificado: `4e7a1d1`.

`DraftFormWriteTest`: cuatro pruebas primero fallaron con el código anterior (doble toque, fallo previo a captura, fallo en revisión, confirmar frente a descartar). Ahora pasan; verifican también progreso, campos intactos y saldo sin cambios. La escritura guarda el ID devuelto para retomar revisión y entrega resultado/navegación una sola vez. `save()` devuelve null si está ocupado, terminado o falla; UI deshabilitada durante escritura y avisos es/en/pt fuera del scroll.

Comando de verificación: `gradlew.bat :composeApp:ktlintFormat ktlintCheck :composeApp:testDebugUnitTest --console=plain` → `BUILD SUCCESSFUL`. Log local: `coinden-write-verified.log` en Temp. La firma nullable actualiza los consumidores existentes. Sigue fuera de alcance una caída del proceso o fallo que ocurra tras persistir pero antes de devolver el ID; no se afirma transaccionalidad nueva.

### Bloque 3: salida consciente

`DraftFormExitTest` reprodujo en rojo que volver de evidencia pisaba el monto editado. Ahora se guarda el presentador por entrada de navegación y la carga inicial ocurre una sola vez. Snapshot explícito de campos editables detecta cambios y su reversión; `requestExit` decide salir, preguntar o bloquear mientras escribe. Un puerto expect/actual conecta Atrás Android, sin nuevas dependencias. Compartir mientras el formulario está abierto ya no fuerza su cierre; espera a Inicio.

`gradlew.bat :composeApp:ktlintFormat :composeApp:testDebugUnitTest :composeApp:assembleDebug --console=plain` pasó. Pruebas de salida limpia/revertida, edición de corrección sin tocar saldo, recarga tras evidencia y bloqueo durante escritura; recursos es/en/pt verificados por las pruebas existentes. Lint específico registrado en `coinden-exit-lint.log`.

Emulador: completado onboarding (USD por región US del AVD, billetera de prueba con 100 USD), salida limpia con Atrás del sistema sin diálogo, edición a 12 y Atrás del sistema con diálogo, seguir editando conservó 12, botón superior también mostró diálogo y salir sin guardar volvió con saldo 100 y cero pendientes. Luego frase «almuerzo 12» → borrador de 12 sin descontar saldo; revisión cambió a 13, abrió evidencia y volvió con 13 intacto, sin aviso ni nueva escritura. Capturas `evidencia/salida-dialogo.png` y `evidencia/edicion-tras-evidencia.png`. La revisión de 13 queda abierta y sin confirmar al final de este recorrido para continuar pruebas.

### Bloque 4: carga y recuperación de pantallas

Publicado en `ad8e78f` (`feat(ux): distinguir carga, error y reintento en Inicio, historial y bandeja`). Se implementó `LoadNotice` (`ScreenLoad.LOADING`, `READY`, `ERROR`), señal de datos anteriores desactualizados (`load_stale`), control de generación contra respuestas fuera de orden y bloqueo de acciones incompatibles en `HomePresenter`/`HomeScreen`, `ReviewInboxPresenter`/`ReviewInboxScreen` e `HistoryPresenter`/`HistoryScreen`, junto con `ScreenLoadingTest.kt` y traducciones `es`/`en`/`pt`.

### Bloque 5: nombres de categorías propias

Implementado en `DraftCard`/`DraftCardView`, `HistoryPresenter`/`HistoryScreen` y `DraftFormPresenter`/`DraftFormScreen`, más prueba de escenarios con Carlos, John y Sophie en `HistoryPresenterTest`:
- `CategoryView` e `HistoryItem` transportan el nombre guardado de `CategoryRef.Custom` resuelto desde `customCategoriesOf(ownerId)` (incluyendo categorías archivadas).
- Si una categoría propia ya no tiene nombre disponible, la vista usa `Res.string.spending_category_missing` sin exponer nunca su UUID ni la etiqueta genérica `category_custom`.
- `DraftFormPresenter` carga las categorías propias del usuario, conserva en las opciones cualquier categoría propia ya asignada (aunque esté archivada o sin nombre) y resuelve su nombre real tanto en los chips como en `beforeText` (`"antes: ..."`).

### Bloque 6: claridad del patrimonio y alcance del MVP

Publicado en `23dd131`. Implementado en `HomePresenter`, `HomeScreen`, recursos `es`/`en`/`pt` (`home_partial` y `home_unconverted` con explicación de que no se incluye en el total), pruebas en `HomePresenterTest` y `SpendingResourcesTest`, y actualización de `README.md` moviendo `rate-fetch-adapter` (cotizaciones por red/enlaces) a la sección *"Después del MVP"*.

### Pulido adicional de íconos, UX/UI y compatibilidad KMP (Android / iOS)

Implementado en código:
- `composeApp/src/androidMain/res/drawable/ic_launcher_monochrome.xml`: completado el isotipo monocromo para Android 13+ con el recorte de la boca (`M46 104 V95...`) y los trazos de la "C" y la "D" en los ojos, manteniendo simetría con `ic_launcher_foreground.xml` y `CoinDenLogo.kt`.
- Tipografía tabular (`CoinDenTheme.amounts.amount` / `amountLarge`) aplicada a los importes de `HistoryScreen.kt` y `SpendingInsightsScreen.kt`, y altura mínima de `48.dp` en botones de `OnboardingScreens.kt` y `PerceptionScreens.kt`.
- Limpieza de APIs exclusivas de JVM en `commonMain` / `commonTest` para dejar libre el camino a iOS (`iosArm64` / `iosSimulatorArm64`): reemplazados `Math.floorDiv`/`Math.floorMod` en `SameMovementUseCases.kt` y `TimeFormatter.kt` por funciones de la stdlib de Kotlin, y reemplazada `java.time.*` en `SpendingMonth.kt` por cálculo civil de Howard Hinnant en Kotlin puro.

### Próximo paso para el siguiente agente (Bloque 7: validación final)

Todo el código de los bloques 1 a 6 y el pulido de íconos/KMP ya está escrito, commiteado y pusheado en `origin/codex/rumbo-al-mvp`. Para cerrar el Bloque 7 (`7.1`, `7.2`, `7.3`) cuando se retome la sesión de validación:
1. Verificar que `local.properties` apunte al Android SDK instalado en la máquina (o definir `ANDROID_HOME`).
2. Ejecutar `npm test`, `npm run lint`, `npm run build:android`, `openspec validate mvp-ux-polish --strict` y `./gradlew :core:data-local:connectedDebugAndroidTest` en el emulador.
3. Instalar el APK compilado en el emulador y completar el recorrido visual de cierre (`7.2`) y actualización final (`7.3`).

La auditoría integral actualizada está en `docs/audits/2026-10-16-mvp.md`; la validación manual pendiente se sigue en https://github.com/jhoel0521/coinden/issues/2. No se afirma prueba de TalkBack, cámara, voz ni recorrido completo en el emulador.
