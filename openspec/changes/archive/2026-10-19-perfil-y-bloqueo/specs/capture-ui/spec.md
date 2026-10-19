## REMOVED Requirements

### Requirement: Barra de Captura en Inicio
**Reason**: Inicio deja de cargar la barra de captura; Dictar, Foto y Escribir se ofrecen desde la pestaña central Capturar, disponible en todas las pestañas.
**Migration**: Ver "Capturar desde la Pestaña Central" en `app-navigation`.

## MODIFIED Requirements

### Requirement: Resultado de la Captura
Después de cada captura la app DEBE (MUST) indicar que se guardó como borrador, mostrar su tarjeta (con la referencia, si la hay) y ofrecer confirmarlo en ese momento, con la categoría sugerida si la hay, o dejarlo para la revisión. Confirmar aplica las reglas del libro; si el libro lo rechaza, el borrador sigue pendiente y se muestra el motivo. Si la app está bloqueada, confirmar pide desbloquear antes.

#### Scenario: Confirmar ahora
- **WHEN** Carlos captura "almuerzo 45 bnb" y toca Confirmar
- **THEN** el gasto de 45 Bs se registra en "BNB Bs" clasificado como comida y el saldo de Inicio baja 45 Bs

#### Scenario: Dejar para la noche
- **WHEN** Carlos captura un gasto y vuelve a Inicio sin confirmar
- **THEN** el borrador queda pendiente y los saldos no cambian

#### Scenario: Confirmación rechazada
- **WHEN** Carlos confirma un gasto de 45 Bs en "Billetera", que solo tiene 10 Bs
- **THEN** el borrador sigue pendiente y la tarjeta muestra que faltan 35 Bs

#### Scenario: Confirmar con la app bloqueada
- **WHEN** la app está bloqueada y Carlos toca Confirmar después de capturar
- **THEN** la app pide autenticarse antes de registrar el gasto
