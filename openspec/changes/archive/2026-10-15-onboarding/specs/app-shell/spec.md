# Spec Delta

## MODIFIED Requirements

### Requirement: Demostración de Captura
La pantalla de captura, que se abre desde Inicio, DEBE (MUST) permitir escribir una frase, pasarla por el intérprete real y la bandeja de borradores, y mostrar el borrador resultante: tipo de movimiento, monto, billetera indicando si es sugerida, nivel de confianza y categoría sugerida con su motivo, o que no hay categoría.

#### Scenario: Gasto hormiga sin billetera
- **WHEN** el usuario, con "Billetera" en efectivo (por defecto) y "BNB Bs", escribe "page 3 bs para trasmporte" y lo captura
- **THEN** la pantalla muestra un gasto de 3 Bs en la billetera de efectivo marcada como sugerida, con confianza baja y sin categoría

#### Scenario: Gasto por QR
- **WHEN** el usuario, con "Billetera" en efectivo (por defecto) y "BNB Bs", escribe "almuerzo 45 qr" y lo captura
- **THEN** la pantalla muestra un gasto de 45 Bs en la billetera de banco marcada como sugerida (es la única que acepta QR), con confianza baja y la categoría sugerida comida por la palabra "almuerzo"

#### Scenario: Gasto con alias y categoría sugerida
- **WHEN** el usuario, con "Billetera" en efectivo (por defecto) y "BNB Bs", escribe "almuerzo 45 bnb" y lo captura
- **THEN** la pantalla muestra un gasto de 45 Bs en la billetera de banco, sin marcarla como sugerida, con confianza media y la categoría sugerida comida por la palabra "almuerzo"

#### Scenario: Frase sin monto
- **WHEN** el usuario escribe "hola" y lo captura
- **THEN** la pantalla indica que el borrador quedó capturado sin propuesta, sin cerrarse con error

## REMOVED Requirements

### Requirement: Datos Guardados y Billeteras de Ejemplo
**Reason**: La app ya no siembra billeteras de ejemplo: las crea la persona en el onboarding (`app-onboarding`). Que los datos sobrevivan al cierre y que el libro sea de solo agregar ya está cubierto por `local-persistence`.
**Migration**: Ver los requisitos "Billeteras Iniciales sin Configurar Apodos" y "Primer Inicio y Siguientes" de `app-onboarding`.
