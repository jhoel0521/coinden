# Continuidad: cierre de UX/UI del MVP

## Estado al 16 de octubre de 2026

- Rama de trabajo: `codex/rumbo-al-mvp`, remoto `origin` (`jhoel0521/coinden`). No crear otra rama sin motivo ni sobrescribir cambios de colegas.
- Plan completo: propuesta, tres deltas, diseño y tareas. `openspec validate mvp-ux-polish --strict` pasó.
- No se implementaron aún las correcciones. Entorno y línea base completados (1.1 y 1.2); el primer bloque funcional pendiente es 2.1. No confundir artefactos completos con funcionalidad terminada.
- La persona autorizó correcciones y entregas incrementales con commit y push. La skill de propuesta exige presentar estos artefactos y recibir una nueva petición de aplicación antes de editar código. Próximo paso: aplicar `mvp-ux-polish` cuando la persona lo confirme.
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

La auditoría previa está en `docs/audits/2026-10-16-mvp.md`; la validación manual pendiente se sigue en https://github.com/jhoel0521/coinden/issues/2. La inspección visual de este turno se limita a las dos bienvenidas capturadas. Las pruebas previas a este cambio no demuestran que los nuevos escenarios pasen. No se afirma prueba de TalkBack, cámara, voz ni recorrido completo en el emulador.
