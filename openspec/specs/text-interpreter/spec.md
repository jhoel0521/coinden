# text-interpreter Specification

## Purpose
Convierte el texto de cualquier captura (frases escritas o dictadas y comprobantes de pago de cualquier banco o país) en propuestas de movimiento, de forma determinista y sin red, usando un lector genérico, paquetes de idioma y plantillas como datos, e indicando el origen de cada campo propuesto.

## Requirements

### Requirement: Interpretación Determinista en el Dispositivo
El intérprete DEBE (MUST) producir propuestas únicamente a partir del texto de la evidencia, su origen, el locale del usuario, la zona horaria del dispositivo, el directorio de billeteras y los paquetes y plantillas instalados, sin usar la red ni modelos. La misma entrada DEBE producir siempre las mismas propuestas.

#### Scenario: Misma entrada, misma salida
- **WHEN** se interpreta dos veces "page 3 bs para trasmporte" con el mismo locale, zona horaria y directorio
- **THEN** ambas interpretaciones producen exactamente las mismas propuestas

#### Scenario: Texto vacío
- **WHEN** la evidencia no tiene texto (por ejemplo, una foto cuyo OCR no devolvió nada)
- **THEN** el intérprete no devuelve propuestas y el borrador queda Capturado

### Requirement: Paquetes de Idioma como Datos
El intérprete DEBE (MUST) reconocer palabras clave (etiquetas de comprobante, verbos de movimiento, meses, conectores y preposiciones de nota) solo a través de paquetes de idioma definidos como datos. El paquete inglés está siempre disponible; los demás se usan solo si están instalados. Agregar un idioma no requiere cambiar código.

#### Scenario: Frase en español con el paquete instalado
- **WHEN** el paquete "es" está instalado y Carlos escribe "gaste 2 para un semillita"
- **THEN** el intérprete propone un gasto de 2 con la nota "un semillita"

#### Scenario: Frase en inglés sin paquetes adicionales
- **WHEN** solo está el paquete inglés y John escribe "paid 12 usd for lunch"
- **THEN** el intérprete propone un gasto de 12.00 USD con la nota "lunch"

#### Scenario: Frase en portugués con el paquete instalado
- **WHEN** el paquete "pt" está instalado y una usuaria de Brasil escribe "paguei 15 reais de almoço"
- **THEN** el intérprete propone un gasto de 15.00 BRL con la nota "almoço"

#### Scenario: Idioma sin paquete instalado
- **WHEN** el paquete "es" no está instalado y Carlos escribe "gaste 2 para un semillita"
- **THEN** el intérprete no reconoce el verbo y propone igualmente un gasto de 2 con el tipo como inferido, como en cualquier frase sin verbo

### Requirement: Lectura de Montos y Monedas según el Locale
El intérprete DEBE (MUST) leer montos con separadores decimales y de miles según el locale del usuario y reconocer la moneda por su código ISO o por su símbolo ("Bs", "US$", "R$", "€", "$"). Un símbolo compartido por varias monedas ("$") se resuelve con la moneda del locale y queda como inferido. Un monto que sigue siendo ambiguo toma la lectura del locale y queda como inferido. El intérprete nunca altera los dígitos leídos.

#### Scenario: Coma decimal escrita por el usuario
- **WHEN** Carlos, con locale es-BO, escribe "almuerzo 45,50"
- **THEN** el intérprete propone el monto 45.50

#### Scenario: Miles según el locale
- **WHEN** Facundo, con locale es-AR, captura un comprobante con "Monto $ 12.500,00"
- **THEN** el intérprete propone 12500.00 ARS con la moneda como inferida porque "$" se resolvió con su locale

#### Scenario: Monto en formato estadounidense
- **WHEN** John, con locale en-US, captura un comprobante con "Amount $1,234.56"
- **THEN** el intérprete propone 1234.56 USD

### Requirement: Lectura de Fechas y Horas con Zona Horaria
El intérprete DEBE (MUST) leer fechas y horas de los comprobantes en formatos numéricos y con nombres de mes del paquete de idioma, incluidos los indicadores "a. m." y "p. m.", y convertirlas a un instante usando la zona horaria del dispositivo. El orden día/mes de una fecha numérica ambigua sigue el locale y queda como inferido.

#### Scenario: Fecha con mes abreviado y "p. m."
- **WHEN** un comprobante de Yape dice "03 oct. 2026" y "12:54 p. m." y la zona horaria es UTC-4
- **THEN** el momento propuesto es el 3 de octubre de 2026 a las 12:54 hora local (16:54 UTC)

#### Scenario: Fecha numérica ambigua
- **WHEN** un comprobante dice "10/11/2026" y el locale es en-US
- **THEN** la fecha propuesta es el 11 de octubre de 2026, marcada como inferida

### Requirement: Lector Genérico de Comprobantes
El intérprete DEBE (MUST) proponer movimientos a partir de comprobantes de bancos desconocidos usando solo su estructura: monto con moneda cerca de una etiqueta de importe, fecha y hora, contraparte cerca de una etiqueta de destinatario, cuenta de origen cerca de una etiqueta de cuenta, y el sentido (pago, débito, retiro o recibo) por las palabras del paquete. Lo que encuentre por etiqueta es evidencia; lo que deduzca, inferido.

#### Scenario: Comprobante de un banco nunca visto
- **WHEN** Facundo captura un comprobante con "Transferiste", "Monto $ 8.000,00", "Para: Kiosco Don Pepe" y "11/10/2026 09:15"
- **THEN** el intérprete propone un gasto de 8000.00 ARS con la nota "Kiosco Don Pepe" y el momento del 11 de octubre de 2026 a las 09:15

#### Scenario: Retiro de efectivo genérico
- **WHEN** un comprobante desconocido dice "Retiro de efectivo" y "Monto Bs10.00"
- **THEN** el intérprete propone una transferencia de 10.00 BOB desde la cuenta del comprobante hacia la billetera por defecto

### Requirement: Plantillas Conocidas como Datos
El intérprete DEBE (MUST) reconocer comprobantes que coinciden con una plantilla instalada, definida como datos (marcas que la identifican, etiquetas de cada campo, tipo de movimiento y billetera del emisor), y tomar sus campos como evidencia. Si ninguna plantilla coincide, usa el lector genérico.

#### Scenario: Comprobante de Yape
- **WHEN** Carlos captura un comprobante con "¡Yapeaste!", "Bs 36", el destinatario y la fecha
- **THEN** el intérprete propone un gasto de 36.00 BOB desde la billetera con alias "yape", con el destinatario como nota, todo como evidencia

#### Scenario: Pago QR del Banco Económico
- **WHEN** Carlos captura un "Pago QR otros bancos" con "Importe Bs25.00", "Beneficiario POLLOS DEL VALLE" y "Cuenta origen: CA: 1111222233 (Bs)"
- **THEN** el intérprete propone un gasto de 25.00 BOB desde la billetera cuyo alias es esa cuenta, con la nota "POLLOS DEL VALLE"

### Requirement: Interpretación de Frases
El intérprete DEBE (MUST) leer frases escritas o dictadas tolerando errores comunes de tipeo: el tipo de movimiento por los verbos del paquete (gasto si no hay verbo), el monto, la moneda si se nombra, la billetera por alias, la nota (lo que sigue a la preposición de nota o, si no hay, las palabras restantes) y varios movimientos unidos por un conector.

#### Scenario: Frase con errores de tipeo
- **WHEN** Carlos escribe "page 3 bs para trasmporte"
- **THEN** el intérprete propone un gasto de 3.00 BOB con la nota "trasmporte"

#### Scenario: Alias en la frase
- **WHEN** Carlos escribe "almuerzo 45 bnb" y "bnb" es alias de "BNB Bs"
- **THEN** el intérprete propone un gasto de 45 en "BNB Bs" con la nota "almuerzo"

#### Scenario: Dos movimientos en una frase
- **WHEN** Carlos escribe "almuerzo 45 y micro 3"
- **THEN** el intérprete propone dos gastos, de 45 y de 3

#### Scenario: Cambio de moneda dictado
- **WHEN** Carlos dice "vendi 50 dolares a 11.88"
- **THEN** el intérprete propone una transferencia de 50.00 USD que recibe 594.00 en la moneda de la billetera por defecto

### Requirement: Billetera Sugerida por Defecto
Cuando una frase o un audio no nombra billetera ni medio de pago, el intérprete DEBE (MUST) proponer la billetera por defecto del usuario como sugerida, salvo que la frase diga una moneda que esa billetera no acepta. Un comprobante nunca usa la billetera por defecto como origen: usa la cuenta del comprobante o la del emisor de la plantilla.

#### Scenario: Gasto hormiga en efectivo
- **WHEN** la billetera por defecto de Carlos es "Billetera" y escribe "gaste 2 para un semillita"
- **THEN** el intérprete propone un gasto de 2 en "Billetera" con la billetera marcada como sugerida

#### Scenario: Comprobante con cuenta desconocida
- **WHEN** un comprobante trae una cuenta de origen que no es alias de ninguna billetera
- **THEN** la billetera queda faltante en lugar de usar la billetera por defecto

### Requirement: Origen de Cada Campo Propuesto
El intérprete DEBE (MUST) indicar para cada campo propuesto si salió de la evidencia (etiqueta o plantilla), lo dijo el usuario, se infirió por una regla de lectura o se sugirió por defecto, sin alterar el texto original: montos, fechas y nombres se proponen tal como aparecen.

#### Scenario: Orígenes de una frase
- **WHEN** se interpreta "page 3 bs para trasmporte" sin billetera nombrada
- **THEN** el monto y la moneda son dichos, el tipo es inferido y la billetera es sugerida

#### Scenario: Nombres sin traducir
- **WHEN** un comprobante en español tiene el beneficiario "POLLOS COPACABANA"
- **THEN** la nota propuesta es exactamente "POLLOS COPACABANA"

### Requirement: Cambio de Moneda por Entrada y Salida
En una frase de cambio de moneda, el intérprete DEBE (MUST) reconocer el monto recibido que sigue a un marcador de recepción del paquete de idioma y proponerlo como lo que entra, dicho por el usuario. Si la frase también dice una tasa, el monto recibido tiene prioridad. Un conector seguido de un marcador de recepción no separa la frase en dos movimientos.

#### Scenario: Carlos en el librecambista
- **WHEN** Carlos escribe "vendí 11 dólares, me dieron 112 bs"
- **THEN** el intérprete propone una sola transferencia que entrega 11.00 USD y recibe 112.00 BOB, sin nota

#### Scenario: Cambio unido por un conector
- **WHEN** Carlos escribe "vendi 100 dolares y me dieron 1190 bs"
- **THEN** el intérprete propone una sola transferencia de 100.00 USD que recibe 1190.00 BOB, y ningún gasto

#### Scenario: Cambio con "por"
- **WHEN** Carlos escribe "cambié 100 usd por 1190 bs"
- **THEN** el intérprete propone una transferencia de 100.00 USD que recibe 1190.00 BOB

#### Scenario: Lo recibido manda sobre la tasa
- **WHEN** Carlos escribe "vendí 100 dólares a 11.80, me dieron 1190 bs"
- **THEN** el intérprete propone recibir 1190.00 BOB, no 1180.00

#### Scenario: Monto recibido sin moneda
- **WHEN** Carlos escribe "vendí 100 dólares, me dieron 1190" y su billetera por defecto es de bolivianos
- **THEN** el intérprete propone recibir 1190 en la billetera por defecto, en su moneda

#### Scenario: Inglés y portugués
- **WHEN** John escribe "sold 100 usd, got 1190 bob" y Ana escribe "vendi 100 dólares, recebi 530 reais"
- **THEN** el intérprete propone para John recibir 1190.00 BOB y para Ana recibir 530.00 BRL

#### Scenario: La forma con tasa sigue funcionando
- **WHEN** Carlos dice "vendi 50 dolares a 11.88"
- **THEN** el intérprete propone, como hasta ahora, una transferencia de 50.00 USD que recibe 594.00 en la moneda de la billetera por defecto

### Requirement: Billeteras Sugeridas por Moneda en un Cambio
En un cambio de moneda que no nombra billeteras, el intérprete DEBE (MUST) sugerir como origen la única billetera activa en la moneda entregada, y como destino la billetera por defecto si es de la moneda recibida o, si no, la única billetera activa de esa moneda. Si hay varias candidatas o ninguna, ese campo queda sin proponer. Ambas sugerencias se marcan como sugeridas.

#### Scenario: Origen y destino sugeridos
- **WHEN** Carlos, con una sola billetera en dólares ("Dólares colchón") y su billetera de efectivo en bolivianos por defecto, escribe "vendí 11 dólares, me dieron 112 bs"
- **THEN** el intérprete sugiere "Dólares colchón" como origen y la billetera de efectivo como destino, y el borrador queda Listo con confianza baja

#### Scenario: Varias billeteras en la moneda entregada
- **WHEN** Carlos tiene dos billeteras en dólares y escribe "vendí 11 dólares, me dieron 112 bs"
- **THEN** el origen queda sin proponer y el borrador queda Incompleto para elegirlo en la revisión

#### Scenario: Confirmar con la tasa exacta
- **WHEN** Carlos confirma el borrador de "vendí 11 dólares, me dieron 112 bs"
- **THEN** el libro registra la salida de 11.00 USD y la entrada de 112.00 BOB con la tasa derivada exacta 112/11

### Requirement: Medio de Pago como Pista de Billeteras Candidatas
El intérprete DEBE (MUST) reconocer en una frase el medio de pago con palabras del paquete de idioma, sin tratarlo como billetera: efectivo, transferencia (QR, Pix, banca por internet o ventanilla) o tarjeta. Las candidatas son las billeteras activas que aceptan ese medio según su tipo y, si se dijo, la moneda. La propuesta conserva el medio dicho para la revisión.

#### Scenario: Varias cuentas que aceptan QR
- **WHEN** Carlos tiene "Billetera" en efectivo (por defecto), "BNB Bs", "BE Bs" y "Yape", y escribe "almuerzo 45 qr"
- **THEN** el intérprete recomienda "BNB Bs", la primera de sus cuentas que aceptan transferencias, como sugerida y con confianza baja, y la propuesta conserva el medio transferencia

#### Scenario: Una sola cuenta que acepta QR
- **WHEN** Carlos tiene "Billetera" en efectivo (por defecto) y solo "BNB Bs" como banco, y escribe "almuerzo 45 qr"
- **THEN** el intérprete sugiere "BNB Bs" con la billetera marcada como sugerida

#### Scenario: La billetera por defecto acepta el medio
- **WHEN** la billetera por defecto de Carlos es "BE Bs" y escribe "almuerzo 45 qr" teniendo también "BNB Bs"
- **THEN** el intérprete recomienda "BE Bs" como sugerida

#### Scenario: Ninguna billetera acepta el medio
- **WHEN** Carlos solo tiene billeteras en efectivo y escribe "almuerzo 45 qr"
- **THEN** la billetera queda sin proponer y el borrador queda Incompleto

#### Scenario: Pix en Brasil
- **WHEN** Ana tiene dos cuentas digitales en reales y escribe "paguei 30 reais no pix"
- **THEN** el intérprete recomienda una de sus cuentas como sugerida, con confianza baja

#### Scenario: El alias gana sobre el medio
- **WHEN** Carlos definió el alias "bnb" para "BNB Bs" y escribe "almuerzo 45 qr bnb"
- **THEN** el intérprete propone "BNB Bs" como dicho por el usuario

### Requirement: Efectivo y Moneda Separan Billeteras
El efectivo en cada moneda es una billetera distinta. Si la frase dice una moneda que la billetera elegida no acepta, el intérprete DEBE (MUST) recomendar entre las billeteras activas de esa moneda (primero las de efectivo si el medio no se dijo). Entre varias candidatas recomienda la billetera por defecto si es candidata; si no, la de la moneda local del usuario; si no, la primera en el orden de sus billeteras.

#### Scenario: Efectivo sin moneda
- **WHEN** Carlos tiene "Billetera" en bolivianos (por defecto) y "Dólares colchón" en efectivo, y escribe "pague 20 en efectivo"
- **THEN** el intérprete sugiere "Billetera"

#### Scenario: Efectivo en dólares
- **WHEN** Carlos tiene "Billetera" en bolivianos (por defecto) y "Dólares colchón" en efectivo, y escribe "pague 20 dolares en efectivo"
- **THEN** el intérprete sugiere "Dólares colchón"

#### Scenario: Moneda dicha sin medio
- **WHEN** Carlos tiene "Billetera" en bolivianos (por defecto), "Dólares colchón" en efectivo y "Dólares banco", y escribe "pague 20 dolares"
- **THEN** el intérprete recomienda "Dólares colchón", el efectivo en esa moneda, como sugerida y con confianza baja

#### Scenario: Efectivo en una moneda que no tiene
- **WHEN** Carlos no tiene billeteras en reales y escribe "pague 30 reais en efectivo"
- **THEN** la billetera queda sin proponer y el borrador queda Incompleto

### Requirement: Billeteras Reconocidas por su Nombre
El intérprete DEBE (MUST) reconocer una billetera, como dicha por el usuario, cuando la frase contiene una palabra de su nombre, sin apodos configurados. No cuentan las palabras de moneda, medio de pago o genéricas del paquete de idioma, ni una palabra que comparten dos billeteras activas. Se toleran errores de tipeo como en los verbos.

#### Scenario: Banco por su sigla
- **WHEN** Carlos tiene "BNB Bs" y escribe "almuerzo 45 bnb"
- **THEN** el intérprete propone el gasto en "BNB Bs" como dicho por el usuario

#### Scenario: Palabra genérica descartada
- **WHEN** Carlos tiene "Banco Económico" y "BNB Bs", y escribe "pague 20 economico"
- **THEN** el intérprete propone "Banco Económico"; la palabra "banco" sola no identifica ninguna billetera

#### Scenario: Error de tipeo
- **WHEN** Carlos tiene "Banco Económico" y escribe "pague 20 economco"
- **THEN** el intérprete propone "Banco Económico"

#### Scenario: Palabra compartida
- **WHEN** Carlos tiene "Dólares colchón" y "Dólares banco", y escribe "pague 20 colchon"
- **THEN** el intérprete propone "Dólares colchón"; la palabra "dolares" no identifica a ninguna de las dos

#### Scenario: La moneda en el nombre no es un apodo
- **WHEN** Carlos tiene "BNB Bs" y "Billetera" en bolivianos, y escribe "page 3 bs para trasmporte"
- **THEN** "bs" se lee como la moneda y la billetera sugerida sigue siendo la por defecto

### Requirement: Moneda de la Región desde el Catálogo
Cuando una frase usa un símbolo o palabra de moneda compartida por varios países ("$", "pesos"), el intérprete DEBE (MUST) resolverla con la moneda de la región del dispositivo obtenida del catálogo mundial de monedas, no de una tabla de países propia, y marcarla como inferida.

#### Scenario: Pesos en Argentina
- **WHEN** Facundo, con la región Argentina, escribe "pague 5000 pesos"
- **THEN** el intérprete propone 5000 ARS con la moneda marcada como inferida

#### Scenario: Pesos en una región que el dominio no listaba
- **WHEN** una persona con la región República Dominicana escribe "pague 500 pesos"
- **THEN** el intérprete propone 500 DOP con la moneda marcada como inferida

### Requirement: Motivo de la Billetera Sugerida
Cuando el intérprete sugiere una billetera que la frase no nombró, DEBE (MUST) indicar el motivo como dato, sin texto: es la billetera por defecto, es la única que acepta el medio de pago o la moneda dichos, o es una recomendada entre varias candidatas (con cuántas había).

#### Scenario: Única cuenta que acepta QR
- **WHEN** Carlos tiene "Billetera" en efectivo (por defecto) y solo "BNB Bs" como banco, y escribe "almuerzo 45 qr"
- **THEN** la propuesta sugiere "BNB Bs" con el motivo "única que acepta el medio de pago"

#### Scenario: Varias cuentas que aceptan QR
- **WHEN** Carlos tiene "BNB Bs", "BE Bs" y "Yape", y escribe "almuerzo 45 qr"
- **THEN** la propuesta sugiere una de ellas con el motivo "recomendada entre 3 candidatas"

#### Scenario: Billetera por defecto
- **WHEN** Carlos escribe "page 3 bs para trasmporte" sin nombrar billetera ni medio
- **THEN** la propuesta sugiere su billetera por defecto con el motivo "billetera por defecto"

#### Scenario: Billetera dicha sin motivo
- **WHEN** Carlos escribe "almuerzo 45 bnb"
- **THEN** la billetera es la dicha por el usuario y la propuesta no trae motivo de sugerencia

### Requirement: Número de Transacción del Comprobante
El intérprete DEBE (MUST) proponer, junto al movimiento que lee de un comprobante, su número de transacción tal como aparece, tomado de la etiqueta de la plantilla o, sin plantilla, de las etiquetas de referencia del paquete de idioma. Sin etiqueta reconocible no hay referencia. Una frase escrita o dictada nunca trae referencia.

#### Scenario: Pago QR con plantilla
- **WHEN** Carlos captura un "Pago QR otros bancos" del Banco Económico con "Nro. de transacción: 400000001"
- **THEN** la propuesta de gasto trae la referencia "400000001" como evidencia

#### Scenario: Yape sin dos puntos
- **WHEN** Carlos captura un comprobante de Yape con la línea "Nro. de transacción 900000001"
- **THEN** la propuesta de gasto trae la referencia "900000001"

#### Scenario: Banco desconocido
- **WHEN** Facundo captura un comprobante sin plantilla con "Operación N° 123456"
- **THEN** la propuesta trae la referencia "123456"

#### Scenario: Pix y pago en inglés
- **WHEN** Ana captura un Pix con "ID da transação E00000000000000000001" y John un pago con "Confirmation 000111222"
- **THEN** las propuestas traen las referencias "E00000000000000000001" y "000111222"

#### Scenario: No confundir otros números con la referencia
- **WHEN** un comprobante trae "Nro. de cédula de identidad: 0000000" y "Código de autorización: 100001" además de "Nro. de transacción: 8000001"
- **THEN** la referencia es "8000001"

#### Scenario: Frase sin referencia
- **WHEN** Carlos escribe o dicta "almuerzo 45 bnb"
- **THEN** la propuesta no trae referencia
