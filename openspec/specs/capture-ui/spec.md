# capture-ui Specification

## Purpose
Hace que registrar un gasto tome segundos desde Inicio: dictar con la voz procesada en el teléfono, tomar o elegir la foto de un comprobante, llenar el formulario completo o compartir desde otra app, con el borrador visible al instante y la opción de confirmarlo ya o en la noche.

## Requirements

### Requirement: Dictado con Reconocimiento de Voz Local
Dictar DEBE (MUST) transcribir con el reconocimiento de voz del propio teléfono en el idioma de la app, sin enviar el audio fuera del dispositivo, y guardar lo dicho como un borrador de voz que pasa por el intérprete. Si el teléfono no ofrece reconocimiento local, DEBE informarlo y no usar un servicio en la nube.

#### Scenario: Gasto dictado
- **WHEN** Carlos toca Dictar, dice su gasto y el reconocimiento local lo transcribe como "almuerzo 45 bnb"
- **THEN** se guarda un borrador de voz con ese texto, interpretado como un gasto de 45 en "BNB Bs", y se muestra su tarjeta

#### Scenario: Sin reconocimiento local
- **WHEN** el teléfono no tiene reconocimiento de voz local disponible
- **THEN** la app avisa que el dictado no está disponible en este teléfono y no envía audio a ningún servicio

#### Scenario: Permiso denegado
- **WHEN** Carlos niega el permiso de micrófono
- **THEN** no se crea ningún borrador y la app explica para qué sirve el permiso

### Requirement: Foto o Galería con Evidencia Cifrada
Foto DEBE (MUST) abrir la cámara dentro de la app, con un botón para elegir una imagen de la galería sin pedir permisos de almacenamiento. La imagen se guarda cifrada en el almacenamiento privado de la app como evidencia de un borrador y, en seguida, se lee en el teléfono; la tarjeta del resultado se actualiza con lo leído.

#### Scenario: Foto del comprobante
- **WHEN** Carlos toca Foto y toma la foto de un comprobante
- **THEN** se guarda un borrador con la imagen como evidencia, se muestra su tarjeta y, al terminar la lectura, la tarjeta muestra el gasto propuesto

#### Scenario: Imagen de la galería
- **WHEN** Carlos toca Foto y elige una captura de pantalla de la galería
- **THEN** se guarda un borrador con esa imagen como evidencia y se lee igual que una foto

#### Scenario: Imagen ilegible fuera de la app
- **WHEN** alguien copia el archivo guardado de una imagen fuera de la app
- **THEN** el archivo no se puede abrir como imagen sin la llave de la app

### Requirement: Formulario Completo
Escribir DEBE (MUST) abrir una página con todos los campos del movimiento: tipo, monto, billetera, medio de pago, categoría, fecha, nota y referencia (número de transacción, opcional), y un campo de frase libre que, al escribirla, llena los campos con el intérprete. La moneda no se elige: es la de la billetera y se muestra junto al monto. La nota y la referencia van en "Más detalles", que se abre solo si se toca o si ya traen un valor. Guardar crea un borrador con lo indicado en el formulario como dicho por el usuario.

#### Scenario: Llenar con una frase
- **WHEN** Carlos escribe "almuerzo 45 bnb" en la frase del formulario
- **THEN** el formulario propone un gasto de 45 en "BNB Bs" con la categoría comida, y Carlos puede cambiar cualquier campo antes de guardar

#### Scenario: Formulario completo a mano
- **WHEN** Carlos elige gasto, 120 Bs, "BNB Bs", tarjeta, supermercado y la fecha de ayer, y guarda
- **THEN** se guarda un borrador Listo con esos datos y confianza alta

#### Scenario: Faltan datos
- **WHEN** Carlos guarda el formulario sin monto
- **THEN** se guarda igual como borrador Incompleto, sin perder lo que escribió

#### Scenario: Referencia escrita a mano
- **WHEN** Carlos llena un gasto de 120 Bs en "BNB Bs" y escribe la referencia "77001234"
- **THEN** el borrador guardado trae la referencia "77001234"

#### Scenario: Pegar un comprobante en la frase
- **WHEN** Carlos pega en la frase el texto de un pago QR con "Nro. de transacción: 400000001" y toca Llenar
- **THEN** el campo referencia queda con "400000001"

#### Scenario: Moneda de la billetera
- **WHEN** Carlos elige "Dolares" en el formulario
- **THEN** junto al monto se muestra USD y no hay un campo para elegir otra moneda

#### Scenario: Más detalles con referencia
- **WHEN** Carlos llena el formulario con un comprobante que trae referencia
- **THEN** "Más detalles" aparece abierto y muestra la referencia

### Requirement: Compartir a CoinDen
La app DEBE (MUST) aparecer como destino al compartir texto, imágenes, PDF o audios desde otras apps. Al compartir, CoinDen guarda la captura, muestra un aviso corto y devuelve a la otra app, sin abrir su pantalla; el texto pasa por el intérprete y la imagen, el PDF o el audio se guardan cifrados y se procesan en la cola. Si el onboarding no terminó, abre la app para terminarlo. Al volver a CoinDen, Inicio y la barra ya cuentan lo compartido entre los pendientes.

#### Scenario: Texto de un comprobante
- **WHEN** Carlos comparte desde su banco el texto de un comprobante de pago por QR
- **THEN** ve "Guardado en CoinDen", sigue en la app del banco y CoinDen tiene un borrador con los datos leídos del comprobante

#### Scenario: Imagen compartida
- **WHEN** Carlos comparte la imagen de un comprobante desde Yape
- **THEN** ve "Guardado en CoinDen", sigue en Yape y la imagen cifrada se lee en la cola

#### Scenario: Comprobante en PDF
- **WHEN** Carlos comparte desde su banca por internet el comprobante en PDF
- **THEN** ve "Guardado en CoinDen" y el PDF cifrado se lee en la cola

#### Scenario: Nota de voz de WhatsApp
- **WHEN** Carlos comparte desde WhatsApp una nota de voz
- **THEN** ve "Guardado en CoinDen", sigue en WhatsApp y el audio cifrado se transcribe en la cola si hay un modelo de voz o reconocimiento local

#### Scenario: Antes de terminar el onboarding
- **WHEN** alguien comparte un comprobante sin haber terminado el onboarding
- **THEN** CoinDen se abre para terminarlo y, al llegar a Inicio, guarda la captura

#### Scenario: Pegar un texto que no se puede compartir
- **WHEN** Carlos copia un mensaje en WhatsApp y lo pega en la frase del formulario
- **THEN** el formulario se llena con lo que el intérprete entiende de ese texto

#### Scenario: Volver después de compartir
- **WHEN** Carlos tiene CoinDen abierta con 1 borrador pendiente, comparte dos comprobantes desde su banco y vuelve a CoinDen
- **THEN** Inicio y la pestaña Revisar muestran 3 pendientes sin cambiar de pantalla

### Requirement: Resultado de la Captura
Después de cada captura la app DEBE (MUST) indicar que se guardó como borrador, mostrar su tarjeta (con la referencia, si la hay) y ofrecer confirmarlo en ese momento, con la categoría sugerida si la hay, o dejarlo para la revisión. Confirmar aplica las reglas del libro; si el libro lo rechaza, el borrador sigue pendiente y se muestra el motivo. Si la app está bloqueada, confirmar pide desbloquear antes.

#### Scenario: Confirmar ahora
- **WHEN** Carlos captura "almuerzo 45 bnb" y toca Confirmar
- **THEN** el gasto de 45 Bs se registra en "BNB Bs" clasificado como comida y el saldo de Inicio baja 45 Bs

#### Scenario: Dejar para la noche
- **WHEN** Carlos captura un gasto y vuelve a Inicio sin confirmar
- **THEN** el borrador queda pendiente y los saldos no cambian

#### Scenario: Confirmación rechazada
- **WHEN** Carlos confirma un gasto de 45 Bs en "Billetera", que solo tiene 10 Bs
- **THEN** el borrador sigue pendiente y la tarjeta muestra que faltan 35 Bs

#### Scenario: Confirmar con la app bloqueada
- **WHEN** la app está bloqueada y Carlos toca Confirmar después de capturar
- **THEN** la app pide autenticarse antes de registrar el gasto

### Requirement: Pregunta de Mismo Gasto en el Resultado
Si la captura puede ser el mismo movimiento que otra, la tarjeta del resultado DEBE (MUST) preguntar "¿Es el mismo gasto?" mostrando la otra captura (nota, monto, hora y billetera) con los botones "Son el mismo" y "Son distintos". Si la otra captura ya está confirmada y la nueva es un comprobante real, "Son el mismo" abre el formulario de corrección con los cambios resaltados.

#### Scenario: Preguntar en el momento
- **WHEN** Carlos comparte el comprobante de 10.00 Bs después de haber dictado "gasté 10 en power" ese día
- **THEN** la tarjeta pregunta si es el mismo gasto que "power", con "Son el mismo" y "Son distintos"

#### Scenario: Responder Son el mismo
- **WHEN** Carlos toca "Son el mismo"
- **THEN** la tarjeta pasa a mostrar el borrador unido y Inicio cuenta un borrador pendiente menos

#### Scenario: Responder Son distintos
- **WHEN** Carlos toca "Son distintos"
- **THEN** la pregunta desaparece y quedan los dos borradores

#### Scenario: Son el mismo que un gasto confirmado
- **WHEN** Carlos confirmó en la mañana el taxi de 40.00 Bs, en la tarde toma la foto de su comprobante y toca "Son el mismo"
- **THEN** se abre el formulario de corrección del taxi con la referencia del comprobante resaltada y nada cambia hasta que Carlos confirma

### Requirement: Acciones del Formulario sin Duplicados
La app DEBE (MUST) ejecutar una sola acción de escritura a la vez por formulario, mostrar progreso y deshabilitar acciones incompatibles. Guardar repetidamente el mismo formulario nuevo no DEBE crear capturas adicionales. Un fallo recuperable DEBE mantener los campos, explicar el fallo y permitir reintentar sobre la captura ya creada, si existe.

#### Scenario: Doble toque al guardar
- **WHEN** Carlos toca Guardar dos veces mientras se escribe su gasto
- **THEN** se crea un único borrador, se informa el progreso y el saldo no cambia

#### Scenario: Reintento después de crear la captura
- **WHEN** falla la revisión o preparación del resultado después de crear la captura de Carlos y él reintenta
- **THEN** se reutiliza el borrador creado, sin otra captura, y se mantienen los campos

#### Scenario: Fallo antes de crear la captura
- **WHEN** falla el almacenamiento antes de guardar el nuevo borrador
- **THEN** el formulario conserva sus campos, muestra el error y permite reintentar sin anunciar éxito

#### Scenario: Acciones incompatibles durante confirmación
- **WHEN** Sophie confirma desde revisión o corrección y vuelve a tocar una acción de escritura
- **THEN** solo se ejecuta la primera acción y no se navega dos veces

### Requirement: Salida Consciente del Formulario
Al intentar salir mediante el control superior o Atrás del sistema, un formulario con cambios sin guardar DEBE (MUST) ofrecer seguir editando o salir sin guardar. Durante una escritura no DEBE permitir una salida que cancele esa operación. Sin cambios, salir DEBE ser inmediato. Abrir evidencia no cuenta como abandonar la edición.

#### Scenario: Continuar editando
- **WHEN** Carlos modifica un monto e intenta volver y elige seguir editando
- **THEN** permanece en el formulario con todos los campos intactos

#### Scenario: Salir sin guardar
- **WHEN** John modifica una corrección y acepta salir sin guardar
- **THEN** vuelve a la pantalla anterior y el movimiento confirmado no cambia

#### Scenario: Formulario sin cambios
- **WHEN** Carlos abre un formulario y vuelve sin editarlo
- **THEN** vuelve sin diálogo y sin crear un borrador

#### Scenario: Salida durante guardado
- **WHEN** Carlos intenta volver mientras el formulario guarda
- **THEN** permanece visible el progreso hasta que termina la escritura y no se cancela por esa salida

#### Scenario: Consultar evidencia
- **WHEN** Carlos abre la evidencia desde una revisión con campos editados y regresa
- **THEN** conserva la edición sin una advertencia de abandono ni nuevas escrituras

### Requirement: Formulario que Sobrevive a Recrear la Pantalla
El formulario de captura DEBE (MUST) conservar lo que la persona escribió o eligió cuando Android recrea la pantalla (al rotar el teléfono o cambiar el tamaño de letra o el idioma del sistema). Recrear la pantalla nunca guarda ni descarta el borrador por su cuenta.

#### Scenario: Rotar con el formulario a medias
- **WHEN** Carlos escribe "almuerzo 45", elige "Transferencia o QR" y rota el teléfono
- **THEN** el formulario sigue con "almuerzo 45" y "Transferencia o QR" elegidos

#### Scenario: Cambiar el tamaño de letra
- **WHEN** Carlos tiene el formulario a medias y cambia el tamaño de letra del sistema
- **THEN** al volver a CoinDen el formulario conserva lo que tenía
