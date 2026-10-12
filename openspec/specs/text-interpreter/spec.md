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
- **WHEN** Carlos escribe "almuerzo 45 qr" y "qr" es alias de "BNB Bs"
- **THEN** el intérprete propone un gasto de 45 en "BNB Bs" con la nota "almuerzo"

#### Scenario: Dos movimientos en una frase
- **WHEN** Carlos escribe "almuerzo 45 y micro 3"
- **THEN** el intérprete propone dos gastos, de 45 y de 3

#### Scenario: Cambio de moneda dictado
- **WHEN** Carlos dice "vendi 50 dolares a 11.88"
- **THEN** el intérprete propone una transferencia de 50.00 USD que recibe 594.00 en la moneda de la billetera por defecto

### Requirement: Billetera Sugerida por Defecto
Cuando una frase o un audio no nombra billetera, el intérprete DEBE (MUST) proponer la billetera por defecto del usuario como sugerida. Un comprobante nunca usa la billetera por defecto como origen: usa la cuenta del comprobante o la del emisor de la plantilla.

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
