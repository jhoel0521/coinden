# Proposal

## Why

La validación del MVP (issue #2) dejó hallazgos chicos que molestan en el uso diario y bloquean cerrar la issue antes de publicar en Play Store: el consejo de captura aparece en comprobantes donde nadie dictó (F4), el contador de pendientes queda viejo después de compartir (F3), "Confirmar los 1 listos" no tiene singular (O4), archivar la última billetera pregunta antes de avisar que no se puede (O5) y el formulario pierde lo escrito al rotar el teléfono o cambiar el tamaño de letra.

## What Changes

- El consejo de captura solo aparece en frases escritas o dictadas de una línea; no en fotos, PDF ni comprobantes de texto compartidos (F4).
- Al volver a la app, Inicio y la barra recargan el número de borradores pendientes (F3).
- "Confirmar el listo" / "Confirmar los N listos" con plurales en es, en y pt (O4).
- Con la última billetera activa, Archivar avisa de inmediato que hace falta conservar una, sin el diálogo de confirmación (O5).
- El formulario de captura conserva lo escrito cuando Android recrea la pantalla (rotar, cambiar tamaño de letra o idioma del sistema).
- O1 ("R$" en lugar de "BRL") no cambia: la app muestra el código de todas las monedas por igual ("BOB", "USD", "BRL").

## Capabilities

### New Capabilities
<!-- Ninguna. -->

### Modified Capabilities
- `capture-coaching`: el consejo solo en frases, no en comprobantes.
- `capture-ui`: lo compartido se refleja al volver a la app; el formulario sobrevive a recrear la pantalla.
- `wallet-settings`: la última billetera activa avisa sin diálogo.

## Impact

- `core/domain`: `CaptureCoach.tipsFor`.
- `composeApp`: señal de primer plano en la Application, `MainScreen`, plurales en `ReviewInboxScreen`, `WalletSettingsPresenter`/`Screen`, presentadores de formulario fuera de la pantalla recreada (`App.kt`).
