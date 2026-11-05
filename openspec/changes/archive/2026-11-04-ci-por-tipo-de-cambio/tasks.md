# Tasks

## 1. Registrar la CI por tipo de cambio (app-shell)

- [x] 1.1 Redactar el delta de `app-shell` con los escenarios "Pull request de solo documentación", "Pull request que toca código", "Pull request en borrador", "Etiqueta en main" y "Etiqueta fuera de main"; verificar contra `.github/workflows/ci.yml` (job `changes`, filtro de archivos, condición de borrador) y `release.yml` (verificación de la etiqueta y cálculo de versión)
- [x] 1.2 Confirmar que "Unir un pull request" sigue cierto: `ci.yml` solo escucha `pull_request` y `workflow_dispatch`; verificar con la lectura del `on:` del workflow
- [x] 1.3 Revisar con `openspec validate ci-por-tipo-de-cambio --strict`; verificar la salida
- [x] 1.4 Archivar con `opsx-archive` y sincronizar el delta al spec principal de `app-shell` cuando la persona lo acepte
