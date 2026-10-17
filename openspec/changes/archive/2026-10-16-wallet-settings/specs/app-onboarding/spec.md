## MODIFIED Requirements

### Requirement: Bienvenida Multilingüe con Elección de Idioma
La primera vez que se abre la app DEBE (MUST) mostrarse una bienvenida con el saludo en el idioma elegido, que permita elegir español, inglés o portugués y proponga el del teléfono si es uno de esos tres (si no, inglés). Los nombres de los idiomas se muestran en su propia lengua. Debe indicar que no hace falta cuenta ni registro.

#### Scenario: Teléfono en portugués
- **WHEN** Ana abre la app por primera vez con el teléfono en portugués
- **THEN** la bienvenida propone portugués y muestra su saludo en portugués

#### Scenario: Teléfono en un idioma no soportado
- **WHEN** Wei abre la app por primera vez con el teléfono en chino
- **THEN** la bienvenida propone inglés

#### Scenario: Cambiar el saludo sin perder la pantalla
- **WHEN** John elige inglés o portugués desde una pantalla de la app
- **THEN** los textos siguen su selección sin reiniciar la navegación y la bienvenida no conserva un título fijo en español
