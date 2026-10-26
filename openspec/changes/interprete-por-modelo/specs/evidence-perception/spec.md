## MODIFIED Requirements

### Requirement: Modelos Descargables y Verificados
Los modelos que no vienen con la app —de voz, de lectura **y de lenguaje**— DEBEN (MUST) descargarse solo cuando la persona lo pide, mostrando su tamaño antes, desde una dirección pública fija y sin enviar ningún dato del usuario. Antes de usarse se verifica su huella; un archivo que no coincide se borra. La persona puede borrar un modelo descargado, y la app funciona completa sin modelos.

#### Scenario: Descargar el modelo de voz
- **WHEN** Carlos elige descargar el modelo de voz recomendado
- **THEN** la app muestra su tamaño, lo descarga, verifica su huella y lo deja listo para transcribir

#### Scenario: Descargar el modelo de lenguaje
- **WHEN** Carlos elige descargar el modelo de lenguaje recomendado
- **THEN** la app muestra su tamaño, lo descarga, verifica su huella y lo deja listo para interpretar

#### Scenario: La app sin ningún modelo
- **WHEN** Carlos no descargó ningún modelo
- **THEN** puede capturar, confirmar y corregir todo, con el respaldo determinista y la lectura de comprobantes

## ADDED Requirements

### Requirement: Cuándo se Ofrece el Modelo de Lenguaje
La app DEBE (MUST) ofrecer el modelo de lenguaje solo en teléfonos que puedan correrlo, con reglas fijas que se pueden probar: hace falta un procesador de 64 bits, al menos 4 GB de memoria y espacio libre suficiente para el archivo. Donde no se cumple, el modelo NO DEBE (MUST NOT) ofrecerse, y la app lo dice en lugar de dejar que falle.

#### Scenario: Teléfono de referencia
- **WHEN** Carlos tiene un teléfono de 64 bits con 4 GB de memoria y espacio libre
- **THEN** la app le ofrece el modelo pequeño como recomendado y muestra su tamaño

#### Scenario: Teléfono potente
- **WHEN** Ana tiene un teléfono de 64 bits con 8 GB de memoria y espacio de sobra
- **THEN** la app le recomienda el modelo mediano, más preciso, y muestra también el pequeño

#### Scenario: Teléfono que no puede
- **WHEN** Facundo tiene un teléfono de 2 GB de memoria, o de 32 bits, o sin espacio libre
- **THEN** la app no le ofrece el modelo de lenguaje y le explica que la captura funciona igual con el respaldo

#### Scenario: El modelo no arranca
- **WHEN** el modelo está descargado pero no logra cargarse en memoria
- **THEN** la app interpreta con el respaldo determinista y avisa una vez, sin cerrarse
