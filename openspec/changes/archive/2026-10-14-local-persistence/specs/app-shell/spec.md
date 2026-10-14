# Spec Delta

## REMOVED Requirements

### Requirement: Datos en Memoria Temporales
**Reason**: La app pasa a guardar sus datos en el almacenamiento local cifrado (capacidad `local-persistence`); los datos ya no se pierden al cerrar.
**Migration**: Lo reemplaza el requisito "Datos Guardados y Billeteras de Ejemplo". El almacenamiento en memoria se conserva solo para pruebas.

## ADDED Requirements

### Requirement: Datos Guardados y Billeteras de Ejemplo
La app DEBE (MUST) usar el almacenamiento local cifrado para todos los puertos de almacenamiento del dominio, de modo que los datos sobrevivan al cierre. Mientras no exista la pantalla de billeteras, al iniciar con la base vacía DEBE cargar billeteras de ejemplo: una billetera de efectivo en bolivianos (la billetera por defecto) y una billetera de banco con el alias "qr". Si la base ya tiene datos, no carga nada.

#### Scenario: Billeteras de ejemplo al iniciar
- **WHEN** se abre la app por primera vez
- **THEN** existen la billetera de efectivo como billetera por defecto y la billetera de banco con el alias "qr"

#### Scenario: Sin billeteras duplicadas al reabrir
- **WHEN** se cierra y se vuelve a abrir la app
- **THEN** siguen existiendo solo las dos billeteras de ejemplo, con sus saldos y alias

#### Scenario: Borradores recordados
- **WHEN** el usuario captura "page 3 bs para trasmporte", cierra la app y la vuelve a abrir
- **THEN** la pantalla de demostración muestra ese borrador capturado antes

#### Scenario: Repositorios con las reglas del dominio
- **WHEN** se registra un evento en el libro con un id que ya existe
- **THEN** el repositorio lo rechaza porque el libro es de solo agregar
