## MODIFIED Requirements

### Requirement: Archivar Reactivar y Elegir Predeterminada
La app DEBE (MUST) permitir archivar y reactivar billeteras propias sin borrar eventos, y elegir una activa como predeterminada. No permite archivar la última activa. Al archivar la predeterminada elige otra activa, priorizando efectivo en moneda principal. Las acciones sobre billeteras ajenas o inexistentes se rechazan.

#### Scenario: Archivar la predeterminada
- **WHEN** Carlos archiva su efectivo predeterminado y tiene otra billetera activa
- **THEN** otra activa queda predeterminada y la archivada conserva su libro

#### Scenario: Última activa
- **WHEN** Carlos intenta archivar su única billetera activa
- **THEN** se informa de inmediato, sin el diálogo de confirmación, que necesita conservar una activa, y no vuelve al onboarding

#### Scenario: Reactivar
- **WHEN** Carlos reactiva una billetera archivada
- **THEN** vuelve con el mismo saldo e historial y puede elegirla como predeterminada

#### Scenario: Billetera ajena
- **WHEN** una acción intenta seleccionar o archivar una billetera de otro propietario
- **THEN** se rechaza sin cambiar billeteras, preferencias ni eventos
