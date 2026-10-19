## MODIFIED Requirements

### Requirement: Logo de CoinDen en la App
La app DEBE (MUST) mostrar el logo de CoinDen (el búho con el cofre y las monedas) como imagen transparente, sin placa ni cuadro de fondo, con sus contornos oscuros, de modo que se lea igual sobre el fondo claro y el oscuro. Debajo van "CoinDen" y "Tu guarida de monedas" como texto real con la tipografía de la app y traducido, nunca dentro de la imagen. El ícono de la cabecera también es transparente. El ícono del lanzador y el splash conservan su fondo #1C140F, que el sistema necesita. No queda ningún rastro del logo anterior.

#### Scenario: Bienvenida en tema claro
- **WHEN** Carlos abre la bienvenida con el teléfono en modo claro
- **THEN** ve el logo sin cuadro de fondo, sobre el fondo claro de la app, y debajo "CoinDen" y "Tu guarida de monedas" como texto

#### Scenario: Bienvenida en inglés
- **WHEN** Carlos elige English en la bienvenida
- **THEN** el texto debajo del logo dice "Your coin den" y el logo no cambia

#### Scenario: Cabecera en tema oscuro
- **WHEN** Carlos abre Inicio con el teléfono en modo oscuro
- **THEN** el ícono junto a "CoinDen" se ve sin cuadro de fondo y con sus contornos visibles
