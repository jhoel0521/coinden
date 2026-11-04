# Proposal

## Why

La integración continua cambió en los PR #19 y #21 sin pasar por OpenSpec: ahora un pull
request que solo toca documentación, specs, diseño o el sitio no compila la app, y los PR en
borrador no gastan minutos. Además hay dos workflows nuevos (versión para Google Play por
etiqueta y publicación del sitio). El spec de `app-shell` no lo dice, así que el código y el
spec ya no coinciden en el comportamiento del requisito "APK Compilado en CI".

Para quién: quien mantiene el proyecto y necesita saber qué checks esperar según el tipo de
cambio, y los agentes que abren pull requests contra `main`.

## What Changes

- El requisito "APK Compilado en CI" documenta que un pull request que solo toca `*.md`,
  `docs/`, `openspec/`, `site/`, `design/` o `.github/FUNDING.yml` **no compila la app**: los
  cuatro checks obligatorios de `main` se saltan y cuentan como aprobados.
- Documenta que un pull request en borrador no corre la integración continua hasta quedar
  listo para revisión.
- Documenta que una etiqueta `v*.*.*` produce el AAB firmado y que solo se publica si la
  etiqueta está en `main`; el código de versión sale de la etiqueta (`major*10000+minor*100+patch`).
- El escenario "Unir un pull request" **no cambia**: la CI sigue sin correr al unir a `main`
  (el workflow `ci.yml` solo escucha pull requests y ejecución a mano).

## Non-Goals

- No se cambian los workflows ni las reglas de `main`; este cambio solo registra lo que ya
  existe.
- No se documenta aquí la publicación del sitio (`pages.yml`), que ya está en `project-site`.
