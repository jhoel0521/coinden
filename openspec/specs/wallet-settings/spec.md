# wallet-settings Specification

## Purpose

Permite mantener billeteras y tasas manuales después del primer inicio sin perder el historial ni volver al onboarding.

## Requirements

### Requirement: Acceso y Estado Local
Inicio DEBE (MUST) ofrecer «Billeteras y tasas», con billeteras activas y archivadas, sus saldos, predeterminada y monedas configuradas. La lectura y las acciones deben informar carga, guardado o error traducido y permitir reintentar sin borrar datos. Todo funciona sin red.

#### Scenario: Consultar y reabrir
- **WHEN** Carlos abre la configuración después del onboarding y vuelve a abrirla
- **THEN** conserva sus billeteras y tasas guardadas sin repetir el onboarding

### Requirement: Crear Billetera Después del Inicio
La persona DEBE (MUST) poder crear una billetera con nombre, tipo, moneda configurada y saldo inicial exacto. Un nombre inválido o monto ilegible no escribe datos. Solo crédito permite saldo inicial negativo. Pulsaciones repetidas mientras se guarda no duplican la operación.

#### Scenario: Nueva billetera
- **WHEN** Carlos agrega «Meru» con 12.50 USD después del onboarding
- **THEN** aparece activa con ese saldo y un único evento inicial

#### Scenario: Monto inválido
- **WHEN** Carlos intenta crear efectivo con saldo -10 o monto ilegible
- **THEN** ve un error y no se crea la billetera

#### Scenario: Deuda inicial
- **WHEN** Facundo agrega una tarjeta con saldo inicial -25.00 ARS
- **THEN** se conserva la deuda inicial exacta

### Requirement: Archivar Reactivar y Elegir Predeterminada
La app DEBE (MUST) permitir archivar y reactivar billeteras propias sin borrar eventos, y elegir una activa como predeterminada. No permite archivar la última activa. Al archivar la predeterminada elige otra activa, priorizando efectivo en moneda principal. Las acciones sobre billeteras ajenas o inexistentes se rechazan.

#### Scenario: Archivar la predeterminada
- **WHEN** Carlos archiva su efectivo predeterminado y tiene otra billetera activa
- **THEN** otra activa queda predeterminada y la archivada conserva su libro

#### Scenario: Última activa
- **WHEN** Carlos intenta archivar su única billetera activa
- **THEN** se informa que necesita conservar una activa y no vuelve al onboarding

#### Scenario: Reactivar
- **WHEN** Carlos reactiva una billetera archivada
- **THEN** vuelve con el mismo saldo e historial y puede elegirla como predeterminada

#### Scenario: Billetera ajena
- **WHEN** una acción intenta seleccionar o archivar una billetera de otro propietario
- **THEN** se rechaza sin cambiar billeteras, preferencias ni eventos

### Requirement: Monedas y Tasas Manuales Editables
La app DEBE (MUST) permitir agregar monedas del catálogo y actualizar o quitar su referencia hacia la moneda principal. La tasa debe ser positiva, exacta, interpretada con el formato regional y guardada con su fecha; no cambia montos del libro ni otras referencias. Quitar una tasa debe dejar la moneda sin convertir, incluso si había una referencia antigua.

#### Scenario: Actualizar dólares
- **WHEN** Carlos cambia su referencia de 11.20 a 10.00 BOB por USD
- **THEN** patrimonio y reporte usan 10.00 al recargarse, y el libro conserva sus montos originales

#### Scenario: Completar y quitar una referencia
- **WHEN** Carlos agrega BRL, guarda una referencia y después la quita
- **THEN** BRL sigue configurada y sus montos quedan sin convertir sin recuperar una tasa anterior

#### Scenario: Error de referencia
- **WHEN** Carlos intenta guardar cero, una tasa negativa o un texto inválido
- **THEN** se informa el error y se conserva la tasa anterior
