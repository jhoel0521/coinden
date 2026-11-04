## MODIFIED Requirements

### Requirement: Compartir a CoinDen
La app DEBE (MUST) aparecer como destino al compartir uno o varios textos, imágenes, PDF o audios desde otras apps. Al compartir, CoinDen guarda todo cifrado en un paquete y muestra la página de contexto rápido encima de la otra app, sin abrir su pantalla principal. Al tocar Listo o salir, el paquete se cierra, se procesa en la cola y la persona vuelve a la otra app. Si el onboarding no terminó, abre la app para terminarlo. Al volver a CoinDen, Inicio y la barra ya cuentan lo compartido entre los pendientes.

#### Scenario: Texto de un comprobante
- **WHEN** Carlos comparte desde su banco el texto de un comprobante de pago por QR
- **THEN** ve la página de contexto sobre la app del banco y, al salir, sigue en el banco y CoinDen tiene un borrador con los datos leídos del comprobante

#### Scenario: Imagen compartida
- **WHEN** Carlos comparte la imagen de un comprobante desde Yape
- **THEN** ve la página de contexto, al salir sigue en Yape y la imagen cifrada se lee en la cola

#### Scenario: Comprobante en PDF
- **WHEN** Carlos comparte desde su banca por internet el comprobante en PDF
- **THEN** ve la página de contexto y, al salir, el PDF cifrado se lee en la cola

#### Scenario: Nota de voz de WhatsApp
- **WHEN** Carlos comparte desde WhatsApp una nota de voz
- **THEN** ve la página de contexto, al salir sigue en WhatsApp y el audio cifrado se transcribe en la cola si hay un modelo de voz o reconocimiento local

#### Scenario: Antes de terminar el onboarding
- **WHEN** alguien comparte un comprobante sin haber terminado el onboarding
- **THEN** CoinDen se abre para terminarlo y, al llegar a Inicio, guarda la captura

#### Scenario: Pegar un texto que no se puede compartir
- **WHEN** Carlos copia un mensaje en WhatsApp y lo pega en la frase del formulario
- **THEN** el formulario se llena con lo que el intérprete entiende de ese texto

#### Scenario: Volver después de compartir
- **WHEN** Carlos tiene CoinDen abierta con 1 borrador pendiente, comparte dos comprobantes desde su banco y vuelve a CoinDen
- **THEN** Inicio y la pestaña Revisar muestran 3 pendientes sin cambiar de pantalla

#### Scenario: Varias fotos de una vez
- **WHEN** Carlos elige dos fotos en WhatsApp y las comparte juntas a CoinDen
- **THEN** la página de contexto muestra las dos en el mismo paquete

### Requirement: Resultado de la Captura
Después de cerrar el paquete de una captura hecha en la app, la app DEBE (MUST) indicar que se guardó como borrador, mostrar la tarjeta de cada borrador que produjo (con la referencia, si la hay) y ofrecer confirmarlo en ese momento, con la categoría sugerida si la hay, o dejarlo para la revisión. Confirmar aplica las reglas del libro; si el libro lo rechaza, el borrador sigue pendiente y se muestra el motivo. Si la app está bloqueada, confirmar pide desbloquear antes.

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

#### Scenario: Un paquete con dos pagos
- **WHEN** Carlos toma la foto de dos comprobantes en la app y cierra la página de contexto
- **THEN** ve las tarjetas de los dos borradores, cada uno con su Confirmar

## ADDED Requirements

### Requirement: Página de Contexto Rápido
Después de compartir o capturar en la app, CoinDen DEBE (MUST) mostrar la página de contexto rápido del paquete. La página muestra todo lo juntado (cada comprobante, foto, audio y nota) y permite, en loop: grabar un audio, escribir una nota, sumar una foto y quitar lo que no es de ese pago. Tocar Listo o salir de la página, por cualquier motivo, cierra el paquete. La página no muestra montos ni saldos, así que no pide desbloquear aunque la app esté bloqueada.

#### Scenario: Comprobante sin glosa más un audio
- **WHEN** Carlos comparte el comprobante QR de 25.00 Bs, toca Grabar audio, dice "esto es de mi comida" y toca Listo
- **THEN** vuelve a la app del banco y CoinDen tiene un solo borrador con el comprobante y el audio

#### Scenario: Varios audios
- **WHEN** Carlos graba un audio, después otro y después escribe una nota en la misma página
- **THEN** la página muestra el comprobante, los dos audios y la nota, en ese orden

#### Scenario: Quitar desde la página
- **WHEN** Carlos sumó por error una foto y la quita en la página
- **THEN** la foto se muestra como quitada y el paquete se cierra sin interpretarla

#### Scenario: Salir sin agregar nada
- **WHEN** Carlos comparte el comprobante y vuelve a la app del banco sin tocar la página
- **THEN** el paquete se cierra con el comprobante y nada se pierde

#### Scenario: Micrófono sin permiso
- **WHEN** Carlos toca Grabar audio y no da permiso de micrófono
- **THEN** la página sigue abierta con Escribir nota, Sumar foto y Listo

#### Scenario: Con la app bloqueada
- **WHEN** la app está bloqueada y Carlos comparte un comprobante y graba un audio
- **THEN** la página no pide desbloquear ni muestra montos, y el audio entra al paquete

### Requirement: Agregar Contexto a un Borrador
La app DEBE (MUST) ofrecer reabrir el paquete de un borrador pendiente desde tres lugares: "Agregar contexto" en la tarjeta del resultado, en Revisar y en el formulario; "Agregar al último" en la hoja de Capturar, que muestra el último borrador pendiente (nota, monto y hace cuánto); y, al compartir algo nuevo, la página de contexto pregunta "¿Es del último borrador?" con "Agregar a ese". Agregar a ese suma lo compartido al paquete de ese borrador en vez de crear uno nuevo.

#### Scenario: Agregar contexto desde Revisar
- **WHEN** en la noche Carlos toca "Agregar contexto" en el borrador del comprobante QR de 25.00 Bs y graba "esto es de mi comida"
- **THEN** se reabre su paquete, y al salir el borrador se vuelve a interpretar con la nota "comida"

#### Scenario: Agregar al último
- **WHEN** Carlos compartió un comprobante hace 1 minuto, abre Capturar y toca "Agregar al último"
- **THEN** se abre la página de contexto con el paquete de ese comprobante

#### Scenario: La segunda foto de WhatsApp
- **WHEN** Carlos compartió una foto desde WhatsApp, vuelve por la segunda, la comparte y toca "Agregar a ese"
- **THEN** la segunda foto entra al paquete de la primera y no se crea otro borrador

#### Scenario: Sin borradores pendientes
- **WHEN** Carlos no tiene borradores pendientes
- **THEN** la hoja de Capturar no muestra "Agregar al último" y la página de contexto no pregunta por el último borrador
