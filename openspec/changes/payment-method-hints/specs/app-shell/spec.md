# Spec Delta

## MODIFIED Requirements

### Requirement: Demostración de Captura
La pantalla principal DEBE (MUST) permitir escribir una frase, pasarla por el intérprete real y la bandeja de borradores, y mostrar el borrador resultante: tipo de movimiento, monto, billetera indicando si es sugerida, nivel de confianza y categoría sugerida con su motivo, o que no hay categoría.

#### Scenario: Gasto hormiga sin billetera
- **WHEN** el usuario escribe "page 3 bs para trasmporte" y lo captura
- **THEN** la pantalla muestra un gasto de 3 Bs en la billetera de efectivo marcada como sugerida, con confianza baja y sin categoría

#### Scenario: Gasto por QR
- **WHEN** el usuario escribe "almuerzo 45 qr" y lo captura
- **THEN** la pantalla muestra un gasto de 45 Bs en la billetera de banco marcada como sugerida (es la única que acepta QR), con confianza baja y la categoría sugerida comida por la palabra "almuerzo"

#### Scenario: Gasto con alias y categoría sugerida
- **WHEN** el usuario escribe "almuerzo 45 bnb" y lo captura
- **THEN** la pantalla muestra un gasto de 45 Bs en la billetera de banco, sin marcarla como sugerida, con confianza media y la categoría sugerida comida por la palabra "almuerzo"

#### Scenario: Frase sin monto
- **WHEN** el usuario escribe "hola" y lo captura
- **THEN** la pantalla indica que el borrador quedó capturado sin propuesta, sin cerrarse con error

### Requirement: Datos Guardados y Billeteras de Ejemplo
La app DEBE (MUST) usar el almacenamiento local cifrado para todos los puertos de almacenamiento del dominio, de modo que los datos sobrevivan al cierre. Mientras no exista la pantalla de billeteras, al iniciar con la base vacía DEBE cargar billeteras de ejemplo: una billetera de efectivo en bolivianos (la billetera por defecto) y una billetera de banco con el alias "bnb". Si la base ya tiene datos, no carga nada.

#### Scenario: Billeteras de ejemplo al iniciar
- **WHEN** se abre la app por primera vez
- **THEN** existen la billetera de efectivo como billetera por defecto y la billetera de banco con el alias "bnb"

#### Scenario: Sin billeteras duplicadas al reabrir
- **WHEN** se cierra y se vuelve a abrir la app
- **THEN** siguen existiendo solo las dos billeteras de ejemplo, con sus saldos y alias

#### Scenario: Borradores recordados
- **WHEN** el usuario captura "page 3 bs para trasmporte", cierra la app y la vuelve a abrir
- **THEN** la pantalla de demostración muestra ese borrador capturado antes

#### Scenario: Repositorios con las reglas del dominio
- **WHEN** se registra un evento en el libro con un id que ya existe
- **THEN** el repositorio lo rechaza porque el libro es de solo agregar
