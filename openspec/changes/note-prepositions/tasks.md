# Tasks: Notas sin "por" y Requisito de Resultado Dividido

## 1. Dominio

- [ ] 1.1 Escribir en `PhraseReaderTest` los escenarios de la nota ("gasté 10 por power" → "power"; "gaste 10 bs por qr" → sin nota; "almuerzo 45 por qr" → "almuerzo"; "paguei 30 pelo pix" y "paid 12 by card" → sin nota; "page 3 bs para trasmporte" sigue con "trasmporte") e implementar D1 y D2 hasta que pasen con `:core:domain:jvmTest`.

## 2. Verificación

- [ ] 2.1 Verificar que `npm test`, `npm run lint` y `npm run build:android` pasan y que la validación estricta de `capture-ui` ya no avisa por el requisito largo.

## Workflow follow-up

- Archivar `note-prepositions` con `opsx-archive` tras la revisión.
