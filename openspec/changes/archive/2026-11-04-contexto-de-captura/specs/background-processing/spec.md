## MODIFIED Requirements

### Requirement: Lo Pendiente Sale de los Datos
La cola DEBE (MUST) procesar los paquetes cerrados que tienen evidencias con archivo sin leer, sin una lista aparte: lee cada evidencia de a una y, cuando no queda ninguna por leer (o ninguna se puede leer), interpreta el paquete. Un paquete abierto no se procesa. Una evidencia ya leída o quitada no se vuelve a leer, y un paquete cuyos borradores ya están todos confirmados o descartados no se vuelve a interpretar.

#### Scenario: Cola cortada a mitad
- **WHEN** el sistema corta la cola después de leer la primera de tres fotos
- **THEN** al retomarse lee solo las dos que faltan

#### Scenario: Lectura hecha en otro dispositivo
- **WHEN** el celular ya leyó una foto y ese historial llegó a la PC
- **THEN** la cola de la PC no vuelve a leerla

#### Scenario: Capturas de antes de la lectura
- **WHEN** Carlos abre la app y tiene borradores Capturados con fotos guardadas antes de que existiera la lectura
- **THEN** la cola los lee solos, sin tocar ningún botón

#### Scenario: Audios que esperaban un modelo de voz
- **WHEN** Carlos tenía una nota de voz sin transcribir y descarga un modelo de voz
- **THEN** la cola la transcribe sola

#### Scenario: Paquete abierto
- **WHEN** Carlos tiene abierta la página de contexto con una foto y un audio
- **THEN** la cola no lee ni interpreta ese paquete hasta que se cierre

#### Scenario: Interpretar al terminar de leer
- **WHEN** un paquete cerrado tiene una foto y un audio sin leer
- **THEN** la cola lee la foto, transcribe el audio y recién entonces interpreta el paquete una sola vez
