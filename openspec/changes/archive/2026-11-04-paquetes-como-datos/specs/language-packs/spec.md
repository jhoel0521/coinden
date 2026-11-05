## ADDED Requirements

### Requirement: Las Palabras de Cada Idioma son un Archivo
Las palabras de cada idioma DEBEN (MUST) vivir en archivos de datos, nunca en el código de la app. Agregar o corregir un idioma NO DEBE (MUST NOT) requerir una versión nueva de la app.

#### Scenario: Un idioma nuevo sin tocar la app
- **WHEN** se instala un paquete de un idioma que la app nunca tuvo, con sus verbos y sus meses
- **THEN** el intérprete reconoce esas palabras sin que se haya publicado una versión nueva

#### Scenario: Corregir una palabra mal traducida
- **WHEN** se instala una versión corregida del paquete de un idioma ya instalado
- **THEN** el intérprete usa las palabras corregidas y descarta las anteriores

### Requirement: El Formato de Cada Banco es un Archivo
Los formatos de comprobante DEBEN (MUST) vivir en archivos de datos. Una plantilla declara las marcas que la identifican, la etiqueta de cada campo, el tipo de movimiento y la billetera del emisor. Sumar un banco NO DEBE (MUST NOT) requerir una versión nueva de la app.

#### Scenario: El formato de un banco nuevo
- **WHEN** se instala la plantilla del comprobante de un banco que la app no conocía
- **THEN** los comprobantes de ese banco se leen con sus campos como evidencia, igual que los de un banco ya conocido

### Requirement: La App Funciona sin Descargar Nada
El paquete de idioma inglés y las plantillas base DEBEN (MUST) viajar dentro de la app y estar disponibles desde la primera vez que se abre, sin red y sin que la persona descargue nada. Todo lo demás es opcional.

#### Scenario: Primer arranque sin conexión
- **WHEN** John instala la app, la abre en modo avión y escribe "paid 12 usd for lunch"
- **THEN** el intérprete propone un gasto de 12.00 USD con la nota "lunch", sin haber descargado ningún archivo

#### Scenario: El idioma de la persona no está instalado
- **WHEN** Sophie usa la app en francés y no hay paquete francés instalado
- **THEN** la app funciona con el paquete inglés y ofrece descargar el francés, sin bloquear ninguna captura

### Requirement: Descarga Opcional y Verificada
Los paquetes y las plantillas que no vienen con la app DEBEN (MUST) descargarse solo cuando la persona lo pide, mostrando su tamaño antes, desde una dirección pública fija y sin enviar ningún dato de la persona. Antes de usarse se verifica su huella; un archivo que no coincide se borra y no se usa. La persona puede borrar lo que descargó.

#### Scenario: Descargar el paquete de español
- **WHEN** Carlos elige descargar el paquete de español
- **THEN** la app muestra su tamaño, lo descarga, verifica su huella y el intérprete empieza a reconocer esas palabras

#### Scenario: Descarga alterada
- **WHEN** el archivo descargado no coincide con la huella esperada
- **THEN** la app lo borra, no lo usa y avisa que la descarga falló

#### Scenario: La descarga no manda datos de la persona
- **WHEN** la app descarga un paquete o una plantilla
- **THEN** la petición no lleva montos, textos, imágenes, billeteras ni identificadores de la persona

### Requirement: Un Archivo Inválido No Rompe la App
Un paquete o una plantilla que no se puede leer (archivo dañado, formato desconocido o versión de formato más nueva que la que la app entiende) DEBE (MUST) descartarse sin afectar a los demás ni a la captura. La app sigue con lo que sí cargó.

#### Scenario: Paquete dañado
- **WHEN** uno de los paquetes instalados está dañado y los demás están bien
- **THEN** la app carga los demás, ignora el dañado y la captura sigue funcionando

#### Scenario: Formato más nuevo que la app
- **WHEN** un paquete declara una versión de formato que esta app todavía no entiende
- **THEN** la app lo ignora en lugar de fallar, y sigue con los paquetes que sí entiende

### Requirement: Varios Paquetes Activos a la Vez
Cuando hay varios paquetes instalados, el intérprete DEBE (MUST) buscar en todos a la vez. Ante la misma palabra en dos paquetes, vale la del paquete del idioma de la app.

#### Scenario: Carlos escribe mezclando idiomas
- **WHEN** están activos el paquete inglés y el español y Carlos escribe "pague 20 for lunch"
- **THEN** el intérprete reconoce el verbo del paquete español y la palabra de categoría del inglés, y propone un gasto de 20 con la categoría Comida
