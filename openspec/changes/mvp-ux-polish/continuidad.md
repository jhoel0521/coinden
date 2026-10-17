# Continuidad: cierre de UX/UI del MVP

## Estado al 16 de octubre de 2026

- Rama de trabajo: `codex/rumbo-al-mvp`, remoto `origin` (`jhoel0521/coinden`). No crear otra rama sin motivo ni sobrescribir cambios de colegas.
- Plan completo: propuesta, tres deltas, diseño y tareas. `openspec validate mvp-ux-polish --strict` pasó.
- No se implementaron aún las correcciones. Todos los checkboxes siguen pendientes; no confundir artefactos completos con funcionalidad terminada.
- La persona autorizó correcciones y entregas incrementales con commit y push. La skill de propuesta exige presentar estos artefactos y recibir una nueva petición de aplicación antes de editar código. Próximo paso: aplicar `mvp-ux-polish` cuando la persona lo confirme.
- Cotizaciones automáticas fuera del MVP por decisión explícita. El bloque 6 actualizará README; el plan ya conserva esta decisión.

## Entorno local

- Repo: `C:\laragon\www\coinden`.
- JDK: `C:\Program Files\Android\Android Studio\jbr`; fijar `JAVA_HOME` en la sesión si el terminal conserva Java 8.
- SDK: `C:\Users\Victus\AppData\Local\Android\Sdk`.
- Se solicitó instalar `emulator` y `system-images;android-36;google_apis;x86_64` mediante sdkmanager. La instalación continúa durante la preparación del plan; no dar por instalado el paquete hasta comprobar su `package.xml` y salida del proceso.
- `emulator.exe -accel-check`: `WHPX(10.0.22631) is installed and usable`, código 0. No hizo falta activar características ni reiniciar Windows.
- Log de instalación local: `C:\Users\Victus\AppData\Local\Temp\coinden-emulator-install.log`.
- AVD previsto: `CoinDen_MVP_API36`, todavía no confirmado como creado o arrancado en este registro inicial.

## Cómo continuar

1. Leer AGENTS, MANIFIESTO y README; comprobar `git status` y el remoto antes de editar.
2. Consultar `openspec status --change mvp-ux-polish --json` y `openspec instructions apply --change mvp-ux-polish --json`; leer los archivos indicados y usar sus líneas actuales de tareas.
3. Verificar entorno/emulador y continuar por el primer checkbox pendiente. El entorno no bloquea escribir pruebas JVM del siguiente bloque mientras descarga la imagen.
4. Cada bloque funcional: prueba roja, arreglo mínimo, prueba verde, lint, actualización de este registro y tareas, commit convencional en español sin saltar hooks y `git push origin codex/rumbo-al-mvp`.
5. Confirmar que `git rev-parse HEAD` coincide con `git rev-parse origin/codex/rumbo-al-mvp` después del push; no afirmar publicado si falla.
6. No cerrar issue #2 ni archivar hasta ejecutar los recorridos o acordar explícitamente las excepciones.

## Evidencia y límites

La auditoría previa está en `docs/audits/2026-10-16-mvp.md`; la validación manual pendiente se sigue en https://github.com/jhoel0521/coinden/issues/2. Las pruebas previas a este cambio no demuestran que estos nuevos escenarios pasen. Todavía no se afirma revisión visual ni pruebas de TalkBack, cámara o voz en el emulador.
