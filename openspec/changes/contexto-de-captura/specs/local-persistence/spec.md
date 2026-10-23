## ADDED Requirements

### Requirement: Actualizar sin Perder Datos
Instalar una versión nueva de CoinDen sobre una anterior DEBE (MUST) conservar todos los datos de la persona: billeteras, movimientos, borradores con su evidencia cifrada, categorías, alias, configuración y la llave de cifrado. La base se migra en el lugar, nunca se borra ni se vuelve a crear, y lo guardado con el formato anterior se lee igual que antes. Un cambio de esquema siempre trae su migración y una prueba que abre una base creada por la versión anterior.

#### Scenario: Actualizar de 0.1.0 a 0.2.0
- **WHEN** Carlos tiene en la versión 0.1.0 tres billeteras, diez movimientos confirmados y dos borradores con foto, e instala la 0.2.0 desde Play
- **THEN** al abrir la app ve las mismas billeteras, saldos, movimientos y borradores, con sus fotos

#### Scenario: Borradores de antes en el formato nuevo
- **WHEN** la 0.2.0 lee un borrador guardado por la 0.1.0, que no tiene paquete
- **THEN** lo muestra como un paquete cerrado con su evidencia, sin cambiar sus datos

#### Scenario: Base de una versión anterior en la prueba
- **WHEN** la prueba abre una base creada con el esquema de la versión anterior
- **THEN** la migración termina sin errores y todos los eventos se leen iguales
