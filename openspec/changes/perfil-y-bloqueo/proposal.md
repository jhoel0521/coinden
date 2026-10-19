# Proposal

## Why

Inicio está sobrecargado: en una sola pantalla conviven el patrimonio, los botones de Revisar, Historial y Gastos del mes, las billeteras, el idioma, la nota de privacidad y la barra de captura. No hay un lugar propio para la configuración de la persona ("Mi perfil") y cualquiera que tome el teléfono desbloqueado ve todos los saldos. La persona quiere proteger sus saldos con la huella o el PIN del teléfono sin perder lo más importante de CoinDen: anotar un gasto en segundos, aunque la app esté bloqueada.

## What Changes

- Barra de pestañas abajo con cinco destinos: Inicio, Revisar, Capturar (al centro, destacado), Movimientos y Perfil. Capturar abre Dictar, Foto y Escribir.
- Inicio queda solo con la marca, el patrimonio, un aviso de pendientes por revisar y las billeteras. Los botones de Revisar, Historial y Gastos del mes, el idioma, la nota de privacidad y la barra de captura salen de Inicio.
- Movimientos reúne el historial y los gastos del mes.
- Nueva pantalla Perfil: Seguridad (bloqueo), Idioma, Billeteras y tasas, Privacidad, Modelos de voz y Acerca de.
- Bloqueo opcional con la huella, el rostro, el PIN o el patrón del propio teléfono. Apagado por defecto; se ofrece en la pantalla de privacidad del onboarding y en Perfil.
- Con el bloqueo activo, la app arranca bloqueada y vuelve a bloquearse tras 1 minuto o más en segundo plano. Bloqueada no muestra saldos, patrimonio, bandeja, historial ni ajustes, pero se puede capturar: Dictar, Foto, Escribir y compartir a CoinDen guardan borradores sin pedir desbloqueo. Confirmar un borrador pide desbloquear antes.
- Con el bloqueo activo, la vista de apps recientes del sistema no muestra el contenido de la app.

## Capabilities

### New Capabilities
- `app-lock`: bloqueo opcional con la autenticación del teléfono que protege saldos y movimientos, cuándo se vuelve a bloquear y qué se puede hacer bloqueada (capturar sí, ver o confirmar no).
- `app-navigation`: barra de pestañas con Capturar al centro, qué contiene cada pestaña y la pantalla Perfil.

### Modified Capabilities
- `capture-ui`: la captura se abre desde la pestaña central en lugar de una barra fija en Inicio; confirmar desde el resultado con la app bloqueada pide desbloquear.
- `app-onboarding`: Inicio deja de mostrar la barra de captura y los accesos; la pantalla de privacidad se reabre desde Perfil y ofrece el bloqueo.
- `review-inbox`: la bandeja se abre desde la pestaña Revisar y el historial desde Movimientos.
- `spending-insights`: "Gastos del mes" se abre desde Movimientos.
- `wallet-settings`: "Billeteras y tasas" se abre desde Perfil.

## Impact

- `composeApp/commonMain`: barra de pestañas (`ui/components`), pantallas Movimientos y Perfil, Inicio más simple, regla pura de bloqueo y puerto de autenticación del dispositivo, rutas en `App.kt`, textos en es, en y pt.
- `composeApp/androidMain`: autenticación con `androidx.biometric` (dependencia nueva), `MainActivity` como `FragmentActivity`, `FLAG_SECURE` con el bloqueo activo y ciclo de vida del proceso.
- `core/domain`: `PreferencesRepositoryPort` gana la preferencia del bloqueo; sus adaptadores en memoria y en la base local la guardan.
- Sin servidores ni datos que salgan del teléfono: la autenticación la hace el sistema operativo y CoinDen nunca ve la huella ni el PIN.
- Fuera de alcance: PIN propio de CoinDen, derivar la llave de la base de un PIN, tiempo de bloqueo configurable y ocultar montos dentro de la captura.
