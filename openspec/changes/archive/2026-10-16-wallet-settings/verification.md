# Verificación

- `npm test`: correcto; 562 pruebas, 0 fallos y 0 omitidas (dominio 371, memoria 6, base local JVM 48, app 137).
- `npm run lint`: correcto.
- `npm run build:android`: correcto.
- `openspec validate wallet-settings --strict`: correcto.
- `git diff --check`: correcto.
- `aapt2 dump badging`: el APK declara CoinDen e `ic_launcher` y contiene los recursos del icono Android.

Pruebas nuevas: presentador de ajustes (creación, reactivación, última activa, propiedad, predeterminada y referencias); pulsaciones concurrentes y cancelación entre escrituras; quitar referencias BTC sin recuperar precios de ejemplo; cotización extrema sin caída de UI; desbordamiento del reporte distinto de falta de tasa; saludo traducido, referencias de iconos y paridad de argumentos de textos. Integración con SQLite real: cerrar/reabrir conserva billeteras, libro, referencias y selección de idioma.

La auditoría detectó y corrigió el título fijo en español, la recreación del árbol de navegación al traducir, el precio BTC de ejemplo en Inicio, la captura excesiva de errores de conversión y la cancelación de una creación incompleta. Informe: `docs/audits/2026-10-16-mvp.md`.

Se preserva la cadena de contexto hasta Activity mediante un contexto temático con configuración de idioma, para no perder los propietarios de permisos/cámara al traducir. No se modifican recursos globales de Android.

La persona acepta pruebas unitarias e integración para esta entrega y difiere la prueba manual. No se han ejecutado renderizado real, TalkBack ni interacción con teléfono. Seguimiento: https://github.com/jhoel0521/coinden/issues/2.
