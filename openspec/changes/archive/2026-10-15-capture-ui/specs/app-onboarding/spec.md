# Spec Delta

## MODIFIED Requirements

### Requirement: Pantalla de Inicio
Inicio DEBE (MUST) mostrar el patrimonio total en la moneda principal (sumando cada otra moneda con su tasa de referencia, y mostrando aparte, sin sumar, las que no tienen tasa), cada billetera activa con su saldo, cuántos borradores esperan revisión y la barra de captura con los botones Dictar, Foto y Escribir.

#### Scenario: Inicio de Carlos
- **WHEN** Carlos, con 100 Bs, 500 Bs y 289 USD a 10,20 y dos borradores pendientes, abre Inicio
- **THEN** ve un patrimonio de 3.547,80 Bs, sus tres billeteras con su saldo y "2 borradores por revisar"

#### Scenario: Moneda sin tasa aparte
- **WHEN** Carlos tiene 600 Bs en bolivianos y 50 BRL sin tasa de referencia
- **THEN** ve un patrimonio de 600,00 Bs y, aparte, 50,00 BRL sin convertir

#### Scenario: Capturar desde Inicio
- **WHEN** Carlos toca Escribir en Inicio
- **THEN** se abre el formulario completo y, al volver con el botón Atrás, regresa a Inicio
