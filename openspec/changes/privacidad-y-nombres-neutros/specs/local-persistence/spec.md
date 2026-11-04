# Spec Delta

## MODIFIED Requirements

### Requirement: Eventos de Solo Agregar en Disco
Los eventos del libro, de los borradores y de las clasificaciones DEBEN (MUST) guardarse solo agregando: un evento con un id ya guardado se rechaza y el almacenamiento no ofrece forma de modificar ni borrar eventos individuales. Las correcciones son eventos nuevos. La única excepción es el borrado total de los datos del dispositivo, que la persona pide y confirma explícitamente: elimina la base completa y su llave, nunca un evento suelto.

#### Scenario: Id repetido en el libro
- **WHEN** se intenta guardar un evento del libro con un id que ya existe
- **THEN** se rechaza y el evento original queda intacto

#### Scenario: Orden de los eventos
- **WHEN** se guardan varios eventos de un mismo borrador y luego se leen
- **THEN** se devuelven todos, en el orden en que se guardaron

#### Scenario: Ningún evento suelto se borra
- **WHEN** se busca en el almacenamiento una operación para borrar o modificar un solo evento
- **THEN** no existe: solo se puede agregar, y corregir es guardar un evento nuevo

#### Scenario: Borrado total por decisión de la persona
- **WHEN** Carlos confirma "Borrar mis datos"
- **THEN** la base completa y su llave se eliminan juntas, y no queda ningún evento ni rastro de ellos en el almacenamiento de la app
