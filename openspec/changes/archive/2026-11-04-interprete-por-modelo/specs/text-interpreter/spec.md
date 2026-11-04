## REMOVED Requirements

### Requirement: Interpretación Determinista en el Dispositivo
**Motivo:** contradice el principio 4 del Manifiesto, que pone al modelo de lenguaje a interpretar primero. El determinismo sigue siendo obligatorio, pero del validador y del respaldo, no de la interpretación. Lo reemplaza "Respaldo Determinista en el Dispositivo".

### Requirement: Interpretación de Frases
**Motivo:** describe entender el lenguaje con listas de verbos y conectores por idioma. Entender intenciones pasa al modelo; leer montos, monedas, fechas y alias queda en el respaldo. Lo reemplazan "Lectura de Frases del Respaldo" e "Intenciones que Entiende el Modelo".

## ADDED Requirements

### Requirement: Respaldo Determinista en el Dispositivo
Cuando no hay modelo de lenguaje, o cuando su respuesta se descarta, el respaldo DEBE (MUST) producir propuestas únicamente a partir del texto de la evidencia, su origen, el locale del usuario, la zona horaria del dispositivo, el directorio de billeteras y los paquetes y plantillas instalados, sin usar la red. La misma entrada DEBE producir siempre las mismas propuestas.

#### Scenario: Misma entrada, misma salida
- **WHEN** se interpreta dos veces "almuerzo 45 bnb" con el mismo locale, zona horaria y directorio, sin modelo
- **THEN** ambas interpretaciones producen exactamente las mismas propuestas

#### Scenario: Texto vacío
- **WHEN** la evidencia no tiene texto (por ejemplo, una foto cuyo OCR no devolvió nada)
- **THEN** el respaldo no devuelve propuestas y el borrador queda Capturado

#### Scenario: El respaldo no adivina intenciones
- **WHEN** sin modelo llega "estaba comiendo y luego un ambulante pasó y le di 25 en efectivo"
- **THEN** el respaldo propone un gasto de 25.00 Bs en efectivo con el texto como nota, sin separar los dos momentos, y el borrador queda para revisar

### Requirement: Lectura de Frases del Respaldo
El respaldo DEBE (MUST) leer de una frase escrita o dictada el monto, la moneda si se nombra, la billetera por alias, el medio de pago y la fecha, tolerando errores comunes de tipeo. Sin modelo, una frase sin otra señal es un gasto. El respaldo NO DEBE (MUST NOT) depender de listas de verbos, conectores ni preposiciones por idioma.

#### Scenario: Frase con errores de tipeo
- **WHEN** Carlos escribe "page 3 bs para trasmporte"
- **THEN** el respaldo propone un gasto de 3.00 BOB con la nota "trasmporte"

#### Scenario: Alias en la frase
- **WHEN** Carlos escribe "almuerzo 45 bnb" y "bnb" es alias de "BNB Bs"
- **THEN** el respaldo propone un gasto de 45 en "BNB Bs" con la nota "almuerzo"

#### Scenario: Dos montos en una frase
- **WHEN** Carlos escribe "almuerzo 45 y micro 3" sin modelo
- **THEN** el respaldo propone dos gastos, de 45 y de 3, por sus montos

#### Scenario: Una frase en un idioma sin paquete
- **WHEN** Wei escribe una frase en chino con un monto y el alias de una billetera suya, sin paquete chino
- **THEN** el respaldo propone igualmente el gasto con ese monto y esa billetera

### Requirement: Intenciones que Entiende el Modelo
Entender *qué* se pagó, *a quién*, *por qué* y *qué explica qué* DEBE (MUST) ser tarea del modelo de lenguaje, no de listas de palabras. Sin modelo, esas capturas no se pierden: quedan con sus hechos y van a revisar.

#### Scenario: Cambio de moneda dictado, con modelo
- **WHEN** Carlos dice "vendi 50 dolares a 11.88" con el modelo instalado
- **THEN** el modelo propone una transferencia de 50.00 USD que recibe 594.00 en la moneda de la billetera por defecto, y las reglas verifican que los dos montos salgan de lo dicho

#### Scenario: Cambio de moneda dictado, sin modelo
- **WHEN** Carlos dice "vendi 50 dolares a 11.88" sin modelo
- **THEN** el respaldo propone un movimiento de 50.00 USD con el texto como nota y el borrador queda para revisar, sin inventar la tasa

#### Scenario: A quién se le pagó
- **WHEN** Carlos dice "le di 25 al ambulante" con el modelo instalado
- **THEN** el modelo propone un gasto de 25.00 Bs con la nota "ambulante", y las reglas verifican que el monto esté en lo dicho
