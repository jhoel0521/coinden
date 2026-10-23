## MODIFIED Requirements

### Requirement: Paquetes de Idioma como Datos
El intérprete DEBE (MUST) reconocer palabras clave (etiquetas de comprobante, verbos de movimiento, meses, conectores y preposiciones de nota) solo a través de paquetes de idioma definidos como datos, que recibe ya cargados y **nunca trae en su propio código**. El paquete inglés viaja con la app y está siempre disponible; los demás se usan solo si están instalados. Agregar o corregir un idioma es cambiar un archivo de datos, no el código ni la versión de la app.

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

#### Scenario: El intérprete no conoce ninguna palabra por su cuenta
- **WHEN** se interpreta una frase sin ningún paquete instalado, ni siquiera el inglés
- **THEN** el intérprete no reconoce ningún verbo, mes ni etiqueta, porque no tiene palabras propias, y propone solo lo que se deduce de los números

### Requirement: Plantillas Conocidas como Datos
El intérprete DEBE (MUST) reconocer comprobantes que coinciden con una plantilla instalada, definida como datos (marcas que la identifican, etiquetas de cada campo, tipo de movimiento y billetera del emisor) que recibe ya cargada y **nunca trae en su propio código**, y tomar sus campos como evidencia. Si ninguna plantilla coincide, usa el lector genérico. Sumar el formato de un banco es instalar un archivo, no publicar una versión.

#### Scenario: Comprobante de Yape
- **WHEN** Carlos captura un comprobante con "¡Yapeaste!", "Bs 36", el destinatario y la fecha
- **THEN** el intérprete propone un gasto de 36.00 BOB desde la billetera con alias "yape", con el destinatario como nota, todo como evidencia

#### Scenario: Pago QR del Banco Económico
- **WHEN** Carlos captura un "Pago QR otros bancos" con "Importe Bs25.00", "Beneficiario POLLOS DEL VALLE" y "Cuenta origen: CA: 1111222233 (Bs)"
- **THEN** el intérprete propone un gasto de 25.00 BOB desde la billetera cuyo alias es esa cuenta, con la nota "POLLOS DEL VALLE"

#### Scenario: El banco de Facundo con su plantilla recién instalada
- **WHEN** se instala la plantilla del banco de Facundo y él captura uno de sus comprobantes
- **THEN** el intérprete toma los campos de esa plantilla como evidencia, sin que la app se haya actualizado
