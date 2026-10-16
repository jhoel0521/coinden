# evidence-perception Specification

## Purpose
Convierte en texto, dentro del teléfono, las fotos, los PDF y los audios que la persona capturó o compartió, para que el mismo intérprete de siempre proponga el movimiento. Combina lectores libres y del sistema; el lector de Google solo se usa con consentimiento y ningún lector envía datos del usuario.

## Requirements

### Requirement: Lectura de Fotos en el Teléfono
El sistema DEBE (MUST) leer el texto de cada foto con un lector libre que corre en el teléfono y, si la persona aceptó el lector de Google, también con ese lector. Cuando hay dos lecturas, usa la que el intérprete entiende mejor: primero la que coincide con una plantilla, luego la que trae número de transacción y luego la que trae monto. Si ninguna lectura trae texto, el borrador sigue Capturado con su foto.

#### Scenario: Foto de un pago QR
- **WHEN** Carlos fotografía un comprobante de "Pago QR otros bancos" con "Importe Bs25.00" y "Nro. de transacción: 400000001"
- **THEN** el borrador pasa a proponer un gasto de 25.00 BOB con la referencia "400000001", y la foto sigue como evidencia

#### Scenario: Sin aceptar el lector de Google
- **WHEN** Carlos no aceptó el lector de Google y toma una foto
- **THEN** la foto se lee solo con el lector libre y el lector de Google nunca se inicia

#### Scenario: Foto que no se puede leer
- **WHEN** Carlos fotografía un comprobante borroso del que ningún lector saca texto
- **THEN** el borrador sigue Capturado con la foto, para completarlo a mano

### Requirement: Lectura de PDF
El sistema DEBE (MUST) tomar el texto que el PDF ya trae y, si no trae texto (un PDF escaneado), leer como foto cada una de sus primeras 5 páginas.

#### Scenario: PDF de la banca por internet
- **WHEN** Carlos comparte el PDF de un comprobante con su texto, "Importe Bs118.71" y "Nro. de transacción : 1000000001"
- **THEN** el borrador pasa a proponer un gasto de 118.71 BOB con la referencia "1000000001"

#### Scenario: PDF escaneado
- **WHEN** Carlos comparte un PDF que solo contiene la imagen de un comprobante
- **THEN** la página se lee como foto y el borrador propone lo que el intérprete entiende de ese texto

### Requirement: Transcripción de Audios
El sistema DEBE (MUST) transcribir en el teléfono los audios guardados de hasta 5 minutos, en el idioma de la app, con el modelo de voz descargado o, si no hay modelo, con el reconocimiento local del sistema cuando el teléfono lo ofrece. Si no hay ninguno, el borrador sigue Capturado y la app indica que descargar un modelo permite transcribirlo. Nunca se usa un reconocimiento en la nube.

#### Scenario: Nota de voz de WhatsApp
- **WHEN** Carlos comparte una nota de voz donde dice "gasté 40 en taxi" y tiene descargado el modelo de voz
- **THEN** el borrador guarda el texto transcrito y pasa a proponer un gasto de 40 con la nota "taxi"

#### Scenario: Sin modelo ni reconocimiento local
- **WHEN** el teléfono de Carlos no tiene reconocimiento local y Carlos no descargó el modelo
- **THEN** el borrador sigue Capturado con el audio y la tarjeta ofrece descargar el modelo

#### Scenario: Audio demasiado largo
- **WHEN** Carlos comparte un audio de 12 minutos
- **THEN** no se transcribe y el borrador sigue Capturado con el audio

### Requirement: Modelos Descargables y Verificados
Los modelos de lectura que no vienen con la app DEBEN (MUST) descargarse solo cuando la persona lo pide, mostrando su tamaño antes, desde una dirección pública fija y sin enviar ningún dato del usuario. Antes de usarse se verifica su huella; un archivo que no coincide se borra. La persona puede borrar un modelo descargado, y la app funciona completa sin modelos.

#### Scenario: Descargar el modelo de voz
- **WHEN** Carlos elige descargar el modelo de voz recomendado
- **THEN** la app muestra su tamaño, lo descarga, verifica su huella y lo deja listo para transcribir

### Requirement: Modelo Recomendado según el Teléfono
La app DEBE (MUST) ofrecer todos los modelos de voz y marcar como recomendado el que corresponde al teléfono según su memoria, su procesador y el espacio libre, con reglas fijas que se pueden probar. Si ningún modelo cabe o el teléfono no puede correrlo, recomienda el reconocimiento del sistema. La persona puede elegir otro.

#### Scenario: Teléfono medio
- **WHEN** Carlos tiene un teléfono de 64 bits con 6 GB de memoria y 20 GB libres
- **THEN** la app recomienda el modelo mediano y muestra también los otros con su tamaño

#### Scenario: Teléfono potente
- **WHEN** Ana tiene un teléfono de 64 bits con 12 GB de memoria y 50 GB libres
- **THEN** la app recomienda el modelo más preciso

#### Scenario: Teléfono modesto o sin espacio
- **WHEN** Facundo tiene un teléfono con 3 GB de memoria, o le quedan menos de 400 MB libres
- **THEN** la app recomienda el modelo pequeño o, si tampoco cabe, el reconocimiento del sistema

#### Scenario: Elegir otro modelo
- **WHEN** Carlos elige el modelo más preciso aunque no sea el recomendado
- **THEN** la app muestra su tamaño, avisa que puede ser lento en su teléfono y lo descarga si Carlos confirma

#### Scenario: Descarga alterada
- **WHEN** el archivo descargado no coincide con la huella esperada
- **THEN** la app lo borra, no lo usa y avisa que la descarga falló

### Requirement: Lector de Google Solo con Consentimiento
El lector de Google DEBE (MUST) estar apagado hasta que la persona lo acepte tras un aviso que diga qué recibe Google (modelo y sistema del teléfono, versión de la app, un identificador de la instalación, tiempos, tamaños, eventos y errores) y qué nunca recibe (imágenes, textos, montos ni datos del usuario). La persona puede apagarlo en cualquier momento y, desde entonces, no se vuelve a iniciar.

#### Scenario: Aceptar el lector de Google
- **WHEN** Carlos acepta el lector de Google en la pantalla de privacidad
- **THEN** sus fotos se leen con ambos lectores y la elección se recuerda al reabrir la app

#### Scenario: Apagarlo después
- **WHEN** Carlos apaga el lector de Google desde la pantalla de privacidad
- **THEN** las fotos siguientes se leen solo con el lector libre

### Requirement: Lectura sin Red
Leer fotos, PDF y audios NO DEBE (MUST NOT) requerir conexión: solo descargar un modelo usa la red.

#### Scenario: Modo avión
- **WHEN** Carlos está en modo avión y comparte el comprobante de un pago QR
- **THEN** el borrador se lee y propone el gasto igual que con conexión

### Requirement: Lectura sin Copias Descifradas
Leer una foto, un PDF o un audio NO DEBE (MUST NOT) escribir la evidencia descifrada en el almacenamiento del teléfono, ni siquiera por un momento: se descifra y se lee solo en memoria. Si quedó una copia temporal de una versión anterior de la app, se borra al iniciar.

#### Scenario: PDF escaneado
- **WHEN** la cola lee el PDF escaneado de un comprobante de Carlos
- **THEN** obtiene sus páginas como imágenes y, durante y después de la lectura, no hay ninguna copia del PDF en el almacenamiento de la app

#### Scenario: Restos de una versión anterior
- **WHEN** Carlos abre la app y en la caché quedó un PDF temporal de una lectura interrumpida
- **THEN** la app lo borra al iniciar
