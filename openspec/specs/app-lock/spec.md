# app-lock Specification

## Purpose
Protege los saldos y movimientos con la autenticación del propio teléfono (huella, rostro, PIN o patrón) sin frenar la captura: bloqueada, la app no muestra montos pero deja anotar borradores, y confirmar pide desbloquear.

## Requirements

### Requirement: Bloqueo Opcional con la Autenticación del Teléfono
La app DEBE (MUST) ofrecer un bloqueo opcional que se desbloquea con la autenticación del propio teléfono (huella, rostro, PIN o patrón). Viene apagado. Activarlo o desactivarlo pide autenticarse una vez; si la autenticación falla o se cancela, la elección no cambia. La elección se guarda en el dispositivo y se mantiene al reabrir. CoinDen nunca recibe ni guarda la huella ni el PIN.

#### Scenario: Activar el bloqueo
- **WHEN** Carlos activa "Bloquear con huella o PIN" en Perfil y se autentica con su huella
- **THEN** el bloqueo queda activo y sigue activo al reabrir la app

#### Scenario: Autenticación cancelada
- **WHEN** Carlos activa el bloqueo y cancela el aviso de autenticación
- **THEN** el bloqueo sigue apagado

#### Scenario: Teléfono sin bloqueo de pantalla
- **WHEN** el teléfono de Carlos no tiene huella, rostro, PIN ni patrón configurado
- **THEN** la opción aparece deshabilitada con el aviso de que primero active un bloqueo de pantalla en el teléfono y un botón que abre los ajustes de seguridad del teléfono

#### Scenario: Volver después de configurar el bloqueo de pantalla
- **WHEN** Carlos toca "Configurar bloqueo de pantalla", pone un PIN en el teléfono y vuelve a CoinDen
- **THEN** la opción del bloqueo ya se puede activar, sin cerrar ni reabrir la app

### Requirement: Saldos Ocultos hasta Desbloquear
Con el bloqueo activo, la app DEBE (MUST) abrirse bloqueada. Bloqueada, Inicio, Revisar, Movimientos y Perfil muestran un aviso con un botón "Desbloquear" en lugar de su contenido, y no se leen ni muestran saldos, patrimonio, borradores pendientes, movimientos ni ajustes. Al desbloquear, cada pestaña muestra su contenido.

#### Scenario: Abrir la app bloqueada
- **WHEN** Carlos tiene el bloqueo activo y abre CoinDen
- **THEN** Inicio muestra "Desbloquea para ver tus saldos" y ningún monto

#### Scenario: Desbloquear
- **WHEN** Carlos toca "Desbloquear" y se autentica
- **THEN** Inicio muestra su patrimonio y sus billeteras

### Requirement: Capturar sin Desbloquear
Con la app bloqueada, la persona DEBE (MUST) poder capturar: Dictar, Foto, Escribir y compartir a CoinDen guardan borradores sin pedir autenticación. Confirmar un borrador (desde el resultado de la captura o el formulario) pide desbloquear antes y, si la autenticación falla, el borrador sigue pendiente y los saldos no cambian.

#### Scenario: Anotar un gasto bloqueada
- **WHEN** la app está bloqueada y Carlos toca Capturar, luego Escribir, anota "almuerzo 45 bnb" y guarda
- **THEN** el borrador queda guardado sin pedir la huella ni el PIN

#### Scenario: Confirmar pide desbloquear
- **WHEN** la app está bloqueada y Carlos toca Confirmar en el resultado de la captura
- **THEN** la app pide autenticarse y, al lograrlo, confirma el gasto

#### Scenario: Confirmación sin autenticar
- **WHEN** la app está bloqueada, Carlos toca Confirmar y cancela la autenticación
- **THEN** el borrador sigue pendiente y los saldos no cambian

#### Scenario: Compartir un comprobante bloqueada
- **WHEN** la app está bloqueada y Carlos comparte un comprobante a CoinDen
- **THEN** se guarda como borrador sin pedir autenticación

### Requirement: Volver a Bloquear tras un Minuto
Con el bloqueo activo, la app DEBE (MUST) volver a bloquearse cuando estuvo 1 minuto o más en segundo plano. Si vuelve antes, sigue desbloqueada.

#### Scenario: Vuelta rápida
- **WHEN** Carlos desbloquea, cambia a otra app 30 segundos y vuelve
- **THEN** CoinDen sigue desbloqueada

#### Scenario: Vuelta tarde
- **WHEN** Carlos desbloquea, deja CoinDen en segundo plano 2 minutos y vuelve
- **THEN** CoinDen está bloqueada

### Requirement: Sin Saldos en Apps Recientes
Con el bloqueo activo, la app DEBE (MUST) evitar que la vista de apps recientes del sistema y las capturas de pantalla muestren su contenido.

#### Scenario: Apps recientes
- **WHEN** Carlos tiene el bloqueo activo y abre la vista de apps recientes
- **THEN** la miniatura de CoinDen no muestra saldos
