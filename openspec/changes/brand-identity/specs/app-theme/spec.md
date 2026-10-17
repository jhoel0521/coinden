# Spec Delta

## ADDED Requirements

### Requirement: Logo de CoinDen en la App
La app DEBE (MUST) mostrar el logo de CoinDen (el búho con el cofre y las monedas) como imagen dentro de una placa oscura #1C140F de esquinas redondeadas, igual en los temas claro y oscuro. Debajo van "CoinDen" y "Tu guarida de monedas" como texto real con la tipografía de la app y traducido, nunca dentro de la imagen. No queda ningún rastro del logo anterior.

#### Scenario: Bienvenida en tema claro
- **WHEN** Carlos abre la bienvenida con el teléfono en modo claro
- **THEN** ve el logo sobre la placa oscura y debajo "CoinDen" y "Tu guarida de monedas" como texto

#### Scenario: Bienvenida en inglés
- **WHEN** Carlos elige English en la bienvenida
- **THEN** el texto debajo del logo dice "Your coin den" y el logo no cambia
