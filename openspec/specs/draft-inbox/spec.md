# draft-inbox Specification

## Purpose
Permite capturar en segundos cualquier evidencia de un movimiento de dinero (texto, voz, foto o formulario), convertirla en borradores mediante intérpretes intercambiables y revisarlos con calma, para confirmarlos en el libro de movimientos bajo sus reglas estrictas sin perder nunca una captura.

## Requirements

### Requirement: Captura que Nunca Pierde Evidencia
El sistema DEBE (MUST) guardar toda captura como borrador con su evidencia original (origen texto, voz, foto o manual; texto o referencia al adjunto; momento de captura y dispositivo), aunque el intérprete no entienda nada o falle. Capturar nunca se rechaza.

#### Scenario: Captura entendida
- **WHEN** Carlos escribe "almuerzo 45 qr" a las 12:58
- **THEN** el sistema guarda un borrador con ese texto como evidencia, origen texto, hora 12:58 y el dispositivo de Carlos

#### Scenario: Captura que nadie entiende
- **WHEN** Carlos toma una foto borrosa y el intérprete no reconoce nada
- **THEN** el sistema guarda igualmente un borrador con la referencia a la foto, que aparece en la bandeja para completarlo a mano

#### Scenario: Intérprete que falla
- **WHEN** el intérprete lanza un error al procesar un audio
- **THEN** el sistema conserva el borrador en estado Capturado con su evidencia, sin perder la captura

### Requirement: Propuestas Tipadas de Movimiento
El sistema DEBE (MUST) recibir del intérprete propuestas tipadas en las que el tipo define el movimiento: gasto, ingreso, transferencia (misma moneda o cambio de moneda), ajuste a un saldo objetivo, "no es un movimiento" con su intención, o "no se entendió". Cualquier campo de una propuesta puede faltar y cada campo presente indica su origen: evidencia, dicho, corregido, inferido o sugerido. Una captura puede producir varias propuestas y cada una genera su propio borrador con la misma evidencia.

#### Scenario: Dos movimientos en una frase
- **WHEN** Carlos dice "almuerzo 45 y micro 3" y el intérprete propone dos gastos
- **THEN** el sistema crea dos borradores, uno por gasto, que comparten la misma evidencia

#### Scenario: Cambio de moneda propuesto
- **WHEN** el intérprete propone una transferencia de 50.00 USD desde "Dólares colchón" hacia "Billetera" recibiendo 594.00 Bs
- **THEN** el borrador queda Listo y su tasa derivada es 1 USD = 11.88 BOB

#### Scenario: Campo sin origen indicado
- **WHEN** una propuesta llega sin origen para alguno de sus campos, como las que arma un formulario manual
- **THEN** el sistema considera ese campo como dicho por el usuario

### Requirement: Resolución de Billetera por Id o Alias
El sistema DEBE (MUST) aceptar la billetera de una propuesta por su id o por una pista de texto, y resolver las pistas con alias definidos por el usuario para cada billetera, sin distinguir mayúsculas. Una pista sin coincidencia deja la billetera como faltante.

#### Scenario: Alias definido por el usuario
- **WHEN** Carlos definió el alias "bnb" para "BNB Bs" y una propuesta trae la pista "BNB"
- **THEN** el borrador usa la billetera "BNB Bs"

#### Scenario: Pista desconocida
- **WHEN** una propuesta trae la pista "tigo" y ninguna billetera tiene ese alias
- **THEN** el borrador queda Incompleto con la billetera como faltante y la pista sin resolver indicada

### Requirement: Ciclo de Vida del Borrador
El borrador DEBE (MUST) seguir la máquina de estados Capturado → Incompleto ⇄ Listo → Confirmado, con Descartado alcanzable desde cualquier estado no terminal y No soportado para lo que no es un movimiento. Confirmado y Descartado son terminales. Un borrador No soportado puede descartarse o revisarse con una propuesta de movimiento.

#### Scenario: Completar un borrador incompleto
- **WHEN** Carlos completa un borrador de foto que solo tenía la nota "HIPERMAXI" con un gasto de 18.50 Bs en "Billetera"
- **THEN** el borrador pasa de Incompleto a Listo

#### Scenario: Estados terminales
- **WHEN** el usuario intenta revisar o descartar un borrador ya Confirmado
- **THEN** el sistema rechaza la operación y el borrador sigue Confirmado

#### Scenario: Descartar un error
- **WHEN** Carlos descarta un borrador capturado por error
- **THEN** el borrador queda Descartado y conserva su evidencia y su historial

### Requirement: Validación para Estar Listo
Un borrador DEBE (MUST) estar Listo solo cuando su propuesta tiene todos los datos para registrar el movimiento sin contradicciones: montos positivos leídos en forma canónica, billeteras resueltas y activas, moneda coincidente con la billetera, billeteras distintas en una transferencia y montos iguales si es la misma moneda. Si no, queda Incompleto con la lista de faltantes y de problemas.

#### Scenario: Moneda que no coincide con la billetera
- **WHEN** una propuesta de gasto trae 12.00 USD con la billetera "BNB Bs"
- **THEN** el borrador queda Incompleto con el problema de moneda incompatible

#### Scenario: Monto sin moneda
- **WHEN** una propuesta de gasto trae el monto "45" sin moneda y la billetera "Billetera" en BOB
- **THEN** el sistema toma la moneda de la billetera y el borrador queda Listo con 45.00 Bs

#### Scenario: Monto no canónico
- **WHEN** una propuesta trae el monto "45,50"
- **THEN** el borrador queda Incompleto con el problema de monto inválido, porque normalizar formatos regionales es tarea del intérprete

### Requirement: Confirmación con las Reglas del Libro
Confirmar un borrador Listo DEBE (MUST) registrar su movimiento en el libro aplicando todas las reglas de `wallet-management`, con el momento de ocurrencia de la propuesta o, si falta, el de la captura. Si el libro rechaza el movimiento, el borrador sigue Listo con el motivo del rechazo. Solo un borrador Listo puede confirmarse. Al confirmar un gasto, el usuario puede indicar una categoría; el sistema registra la clasificación con la nota del borrador como descripción y, si no se indicó categoría, el gasto queda Sin categoría. La categoría nunca bloquea la confirmación.

#### Scenario: Confirmar el almuerzo
- **WHEN** Carlos confirma a las 21:00 el borrador "almuerzo 45 bnb" capturado a las 12:58
- **THEN** el libro registra un gasto de 45.00 Bs en "BNB Bs" ocurrido a las 12:58 y el borrador queda Confirmado con el id del movimiento

#### Scenario: Rechazo por saldo
- **WHEN** Carlos confirma un gasto de 45.00 Bs en "Billetera", que solo tiene 10.00 Bs
- **THEN** el borrador sigue Listo con el motivo de saldo insuficiente (faltan 35.00 Bs) y el libro no cambia

#### Scenario: Confirmar un borrador incompleto
- **WHEN** el usuario intenta confirmar un borrador Incompleto
- **THEN** el sistema rechaza la confirmación indicando los datos faltantes

#### Scenario: Confirmar con categoría
- **WHEN** Carlos confirma el borrador "POLLOS DEL VALLE 25 Bs" eligiendo "comida"
- **THEN** el gasto queda clasificado como "comida" con la descripción "POLLOS DEL VALLE" y nace la regla correspondiente

#### Scenario: Confirmar sin categoría
- **WHEN** Carlos confirma un gasto sin indicar categoría
- **THEN** el gasto se registra igual y queda como Sin categoría

### Requirement: Ajuste a un Saldo Objetivo
Una propuesta de ajuste DEBE (MUST) expresar el saldo real que el usuario dice tener, no la diferencia. Al confirmar, el sistema calcula la diferencia contra el saldo actual de la billetera y registra ese ajuste; si no hay diferencia, la confirmación se rechaza porque no hay nada que ajustar.

#### Scenario: "En la billetera tengo 140"
- **WHEN** "Billetera" tiene 147.00 Bs y Carlos confirma un ajuste a 140.00 Bs
- **THEN** el libro registra un ajuste de -7.00 Bs y el saldo queda en 140.00 Bs

#### Scenario: Nada que ajustar
- **WHEN** Carlos confirma un ajuste a 140.00 Bs y la billetera ya tiene 140.00 Bs
- **THEN** el sistema rechaza la confirmación por no haber diferencia y el borrador sigue Listo

### Requirement: Capturas que No Son Movimientos
Cuando el intérprete reconoce algo que no es un movimiento (pago recurrente, préstamo, meta, categoría, alta de billetera, anulación o consulta), el sistema DEBE (MUST) guardar el borrador como No soportado con su intención y su evidencia, en lugar de rechazar la captura.

#### Scenario: "Créame un pago recurrente de Netflix"
- **WHEN** Carlos dicta "créame un pago recurrente de Netflix de 12 dólares"
- **THEN** el sistema guarda un borrador No soportado con la intención de pago recurrente, visible en la bandeja para descartarlo o convertirlo en un gasto

#### Scenario: Convertir un no soportado en gasto
- **WHEN** Carlos revisa ese borrador con un gasto de 12.00 USD en "Meru"
- **THEN** el borrador pasa a Listo y puede confirmarse como gasto

### Requirement: Bandeja de Revisión con Saldo Proyectado
El sistema DEBE (MUST) listar los borradores pendientes de un usuario (Capturado, Incompleto, Listo y No soportado), ordenados de menor a mayor confianza y, a igual confianza, por momento de captura, y, para cada billetera afectada por borradores Listos, el saldo que quedaría al confirmarlos, avisando sin bloquear cuando una billetera que no admite negativos quedaría por debajo de cero. Para cada borrador de gasto la bandeja muestra además la categoría sugerida con su confianza y su motivo, sin que la categoría afecte la confianza del borrador.

#### Scenario: Aviso de faltante proyectado
- **WHEN** "Billetera" tiene 10.00 Bs y hay borradores Listos por 3.00 Bs y 45.00 Bs en ella
- **THEN** la bandeja muestra un saldo proyectado de -38.00 Bs con un aviso de faltante, y los borradores siguen pendientes

#### Scenario: Terminales fuera de la bandeja
- **WHEN** Carlos tiene borradores Confirmados y Descartados
- **THEN** la bandeja no los incluye entre los pendientes

#### Scenario: Lo dudoso primero
- **WHEN** Carlos tiene un borrador de un comprobante con confianza ALTA capturado a las 10:00 y "gaste 2 para un semillita" con confianza BAJA capturado a las 15:00
- **THEN** la bandeja muestra primero el borrador de confianza BAJA

#### Scenario: Categoría sugerida en la bandeja
- **WHEN** Carlos tiene pendiente el borrador "almuerzo 45 qr"
- **THEN** la bandeja lo muestra con la categoría sugerida "comida", confianza MEDIA y el motivo "almuerzo"

### Requirement: Historial de Transiciones y Confirmación Idempotente
Cada transición de un borrador DEBE (MUST) registrarse como un evento append-only con id global, dispositivo y momentos de ocurrencia y registro, y el estado actual DEBE derivarse de reproducir los eventos en orden. El movimiento confirmado usa un id derivado del borrador, de modo que confirmar el mismo borrador más de una vez nunca registra dos movimientos.

#### Scenario: Revisión en dos dispositivos
- **WHEN** el celular captura un borrador y la PC lo revisa con otro monto
- **THEN** el historial conserva ambas transiciones y el estado actual refleja la revisión más reciente en orden cronológico

#### Scenario: Confirmar dos veces el mismo borrador
- **WHEN** el mismo borrador se confirma en el celular y luego en la PC
- **THEN** el libro contiene un único movimiento con el id derivado del borrador y la segunda confirmación no registra nada nuevo

### Requirement: Confianza del Borrador
Todo borrador con propuesta DEBE (MUST) exponer una confianza ALTA, MEDIA o BAJA calculada de forma determinista como la del campo propuesto más débil: evidencia, dicho y corregido valen ALTA; inferido, MEDIA; sugerido, BAJA. Cuando el usuario revisa un borrador, los campos de su nueva propuesta quedan como corregidos.

#### Scenario: Comprobante con todo en la evidencia
- **WHEN** un borrador viene de un comprobante cuyo monto, fecha y billetera salieron de la evidencia
- **THEN** su confianza es ALTA

#### Scenario: Frase sin billetera
- **WHEN** un borrador viene de "page 3 bs para trasmporte" con la billetera sugerida por defecto
- **THEN** su confianza es BAJA

#### Scenario: La revisión sube la confianza
- **WHEN** Carlos revisa ese borrador y confirma la billetera "Billetera" con el mismo monto
- **THEN** sus campos quedan como corregidos y su confianza pasa a ALTA

### Requirement: Billetera por Defecto Configurable
El sistema DEBE (MUST) permitir que el usuario elija una billetera activa propia como billetera por defecto para las capturas que no nombran billetera, e informarla al intérprete junto con el directorio de billeteras. Si el usuario no eligió ninguna y tiene exactamente una billetera activa de efectivo, esa es la billetera por defecto; en otro caso no hay billetera por defecto.

#### Scenario: Única billetera de efectivo
- **WHEN** Carlos no eligió billetera por defecto y su única billetera activa de efectivo es "Billetera"
- **THEN** la billetera por defecto es "Billetera"

#### Scenario: Elección explícita
- **WHEN** Carlos elige "Meru" como billetera por defecto
- **THEN** las capturas que no nombran billetera sugieren "Meru"

#### Scenario: Sin billetera por defecto
- **WHEN** Carlos tiene dos billeteras de efectivo y no eligió ninguna
- **THEN** no hay billetera por defecto y la billetera de esas capturas queda faltante
