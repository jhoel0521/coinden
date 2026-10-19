## MODIFIED Requirements

### Requirement: Acceso y Estado Local
Perfil DEBE (MUST) ofrecer «Billeteras y tasas», con billeteras activas y archivadas, sus saldos, predeterminada y monedas configuradas. La lectura y las acciones deben informar carga, guardado o error traducido y permitir reintentar sin borrar datos. Todo funciona sin red.

#### Scenario: Consultar y reabrir
- **WHEN** Carlos abre «Billeteras y tasas» desde Perfil después del onboarding y vuelve a abrirla
- **THEN** conserva sus billeteras y tasas guardadas sin repetir el onboarding
