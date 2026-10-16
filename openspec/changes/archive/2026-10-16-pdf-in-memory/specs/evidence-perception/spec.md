# Spec Delta

## ADDED Requirements

### Requirement: Lectura sin Copias Descifradas
Leer una foto, un PDF o un audio NO DEBE (MUST NOT) escribir la evidencia descifrada en el almacenamiento del teléfono, ni siquiera por un momento: se descifra y se lee solo en memoria. Si quedó una copia temporal de una versión anterior de la app, se borra al iniciar.

#### Scenario: PDF escaneado
- **WHEN** la cola lee el PDF escaneado de un comprobante de Carlos
- **THEN** obtiene sus páginas como imágenes y, durante y después de la lectura, no hay ninguna copia del PDF en el almacenamiento de la app

#### Scenario: Restos de una versión anterior
- **WHEN** Carlos abre la app y en la caché quedó un PDF temporal de una lectura interrumpida
- **THEN** la app lo borra al iniciar
