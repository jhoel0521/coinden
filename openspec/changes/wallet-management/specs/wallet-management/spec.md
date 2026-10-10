# Spec Delta

## Purpose
Permite al usuario representar cada "bolsillo" real de su dinero (efectivo, banco, billetera digital, cripto, tarjeta de crédito) como una billetera de una sola moneda, con un libro de movimientos append-only del que se deriva el saldo de forma exacta, cronológica, auditable y lista para sincronizarse entre dispositivos.

## ADDED Requirements

### Requirement: Creación de Billetera con Moneda Única y Saldo Inicial
El sistema DEBE (MUST) permitir crear una billetera con un nombre libre definido por el usuario, exactamente una moneda, un tipo (EFECTIVO, BANCO, DIGITAL, CRIPTO o CREDITO) y un saldo inicial expresado en la unidad mínima de esa moneda. El saldo inicial queda registrado como el primer movimiento del libro de la billetera.

#### Scenario: Carlos crea su billetera de efectivo en bolivianos
- **WHEN** Carlos crea la billetera "Billetera" de tipo EFECTIVO en BOB con saldo inicial de 150.00 Bs
- **THEN** el sistema registra la billetera activa con un movimiento de SALDO_INICIAL de 15000 centavos de BOB y su saldo es 150.00 Bs

#### Scenario: John crea una billetera de Bitcoin con precisión de satoshis
- **WHEN** John crea la billetera "Ledger" de tipo CRIPTO en BTC con saldo inicial de 0.05 BTC
- **THEN** el sistema registra un SALDO_INICIAL de 5000000 satoshis y su saldo es exactamente 0.05000000 BTC

#### Scenario: Nombre vacío rechazado
- **WHEN** el usuario intenta crear una billetera cuyo nombre está vacío o solo contiene espacios
- **THEN** el sistema rechaza la creación con un error de nombre inválido y no registra ninguna billetera

### Requirement: Política de Saldo Negativo por Billetera
Cada billetera DEBE (MUST) declarar si admite saldo negativo. Las billeteras de tipo CREDITO lo admiten por defecto y las de cualquier otro tipo no lo admiten por defecto; el usuario puede indicar la política explícitamente al crearla.

#### Scenario: Tarjeta de crédito de Facundo admite negativo por defecto
- **WHEN** Facundo crea la billetera "Visa Galicia" de tipo CREDITO en ARS con saldo inicial 0 y registra un gasto de 25,000.00 ARS
- **THEN** el sistema acepta el gasto y el saldo de la billetera es -25,000.00 ARS

#### Scenario: Efectivo no admite negativo por defecto
- **WHEN** Carlos crea la billetera "Billetera" de tipo EFECTIVO sin indicar política de negativos
- **THEN** la billetera queda configurada como no admite saldo negativo

### Requirement: Archivado de Billeteras sin Borrado
El sistema DEBE (MUST) permitir archivar y reactivar una billetera, y NO DEBE (MUST NOT) ofrecer el borrado de billeteras ni de sus movimientos. Una billetera archivada conserva todo su historial, no acepta movimientos nuevos y no se incluye en los saldos de billeteras activas.

#### Scenario: Archivar una billetera conserva el historial
- **WHEN** Carlos archiva la billetera "BNB Bs" que tiene 12 movimientos
- **THEN** la billetera queda en estado ARCHIVADA, sus 12 movimientos siguen disponibles para consulta y deja de aparecer entre las billeteras activas

#### Scenario: Billetera archivada rechaza movimientos nuevos
- **WHEN** el usuario intenta registrar un gasto en una billetera ARCHIVADA
- **THEN** el sistema rechaza el movimiento con un error de billetera archivada

#### Scenario: Reactivar una billetera archivada
- **WHEN** el usuario reactiva una billetera ARCHIVADA
- **THEN** la billetera vuelve a estado ACTIVA con el mismo saldo que tenía al archivarse y acepta movimientos nuevos

### Requirement: Saldo Derivado del Libro de Movimientos
El saldo de una billetera DEBE (MUST) calcularse siempre como la suma exacta, en unidades mínimas, de sus movimientos vigentes; un movimiento deja de ser vigente cuando existe una anulación que lo referencia. El sistema NO DEBE (MUST NOT) almacenar el saldo como un dato editable.

#### Scenario: Saldo como suma de movimientos
- **WHEN** la billetera "Billetera" de Carlos tiene SALDO_INICIAL 150.00 Bs, GASTO 3.00 Bs e INGRESO 50.00 Bs
- **THEN** el saldo calculado es 197.00 Bs

#### Scenario: Un movimiento anulado no cuenta
- **WHEN** la billetera tiene SALDO_INICIAL 150.00 Bs y un GASTO de 45.00 Bs que luego fue anulado
- **THEN** el saldo calculado es 150.00 Bs

### Requirement: Registro de Gastos, Ingresos y Ajustes
El sistema DEBE (MUST) permitir registrar en una billetera activa movimientos de GASTO e INGRESO con un monto estrictamente positivo, y de AJUSTE con un monto distinto de cero positivo o negativo, siempre en la moneda de la billetera. Un monto en otra moneda se rechaza.

#### Scenario: Gasto en efectivo
- **WHEN** Carlos registra un GASTO de 3.00 Bs en "Billetera" que tiene 150.00 Bs
- **THEN** el sistema registra el movimiento y el saldo pasa a 147.00 Bs

#### Scenario: Ajuste negativo por dinero faltante
- **WHEN** Carlos cuenta su efectivo, ve que tiene 140.00 Bs en lugar de 147.00 Bs y registra un AJUSTE de -7.00 Bs
- **THEN** el saldo pasa a 140.00 Bs y el ajuste queda en el historial

#### Scenario: Moneda distinta a la de la billetera
- **WHEN** el usuario intenta registrar un GASTO de 12.00 USD en la billetera "Billetera" en BOB
- **THEN** el sistema rechaza el movimiento con un error de moneda incompatible

#### Scenario: Monto cero o negativo en gasto
- **WHEN** el usuario intenta registrar un GASTO de 0.00 Bs o de -5.00 Bs
- **THEN** el sistema rechaza el movimiento con un error de monto inválido

### Requirement: Transferencias y Cambios de Moneda con Dos Montos
El sistema DEBE (MUST) permitir transferir entre dos billeteras activas distintas registrando el monto que sale (en la moneda de origen) y el monto que entra (en la moneda de destino), cada uno estrictamente positivo y en su unidad mínima. Si ambas monedas son iguales los montos deben coincidir. La tasa de una transferencia entre monedas se deriva de ambos montos y no se almacena.

#### Scenario: Carlos vende dólares al paralelo
- **WHEN** Carlos transfiere desde "Dólares colchón" (USD) 50.00 USD y recibe en "Billetera" (BOB) 594.00 Bs
- **THEN** "Dólares colchón" disminuye 50.00 USD, "Billetera" aumenta 594.00 Bs y la tasa derivada de la operación es 11.88 Bs por USD

#### Scenario: Retiro del cajero en la misma moneda
- **WHEN** Carlos transfiere 200.00 Bs desde "BNB Bs" hacia "Billetera"
- **THEN** "BNB Bs" disminuye 200.00 Bs y "Billetera" aumenta 200.00 Bs como una única operación

#### Scenario: Montos distintos en la misma moneda
- **WHEN** el usuario intenta transferir 200.00 Bs desde "BNB Bs" y recibir 195.00 Bs en "Billetera"
- **THEN** el sistema rechaza la transferencia con un error de montos inconsistentes

#### Scenario: Transferencia a la misma billetera
- **WHEN** el usuario intenta transferir desde "Billetera" hacia "Billetera"
- **THEN** el sistema rechaza la transferencia con un error de origen y destino iguales

### Requirement: Corrección por Anulación sin Editar el Historial
El sistema DEBE (MUST) corregir movimientos únicamente registrando un evento de ANULACION que referencia al movimiento original; el original no se modifica ni se elimina. Anular una transferencia anula ambos lados juntos. No se puede anular una anulación ni un movimiento ya anulado.

#### Scenario: Corregir un gasto mal registrado
- **WHEN** Carlos anula el GASTO de 54.00 Bs y registra un nuevo GASTO de 45.00 Bs
- **THEN** el historial contiene el gasto original, su anulación y el gasto correcto, y el saldo refleja solo los 45.00 Bs

#### Scenario: Anular una transferencia revierte ambos lados
- **WHEN** Carlos anula la transferencia de 50.00 USD a 594.00 Bs
- **THEN** "Dólares colchón" recupera los 50.00 USD y "Billetera" pierde los 594.00 Bs

#### Scenario: Doble anulación rechazada
- **WHEN** el usuario intenta anular un movimiento que ya tiene una anulación
- **THEN** el sistema rechaza la operación con un error de movimiento ya anulado

### Requirement: Control Estricto de Saldo Cronológico
En una billetera que no admite negativos, el sistema DEBE (MUST) rechazar cualquier movimiento nuevo, incluidas las anulaciones, tras el cual el saldo acumulado quede negativo en algún punto de la historia ordenada por fecha de ocurrencia. El rechazo indica la billetera, el faltante máximo y el primer momento en que el saldo queda negativo.

#### Scenario: Olvidó registrar el retiro del cajero
- **WHEN** "Billetera" tiene 10.00 Bs y Carlos intenta registrar un GASTO de 45.00 Bs por el almuerzo
- **THEN** el sistema rechaza el gasto indicando la billetera "Billetera", un faltante de 35.00 Bs y la fecha del almuerzo

#### Scenario: Registrar primero el retiro desbloquea el gasto
- **WHEN** después del rechazo Carlos registra el retiro de 200.00 Bs del cajero ocurrido antes del almuerzo y vuelve a registrar el GASTO de 45.00 Bs
- **THEN** el sistema acepta el gasto y el saldo de "Billetera" es 165.00 Bs

#### Scenario: Movimiento con fecha pasada que deja un hueco negativo
- **WHEN** "Billetera" recibió 200.00 Bs a las 14:00 con 10.00 Bs previos, y Carlos registra un GASTO de 45.00 Bs ocurrido a las 13:00
- **THEN** el sistema rechaza el gasto porque a las 13:00 el saldo habría sido -35.00 Bs, aunque el saldo final sería positivo

#### Scenario: Anulación de un ingreso que deja el saldo negativo
- **WHEN** "Billetera" tiene un INGRESO de 200.00 Bs del que dependen gastos posteriores por 180.00 Bs, y Carlos intenta anular ese ingreso
- **THEN** el sistema rechaza la anulación indicando el faltante resultante

### Requirement: Detección de Billetera Descuadrada
El sistema DEBE (MUST) informar como DESCUADRADA a toda billetera que no admite negativos cuyo libro contenga algún punto cronológico con saldo negativo, sin rechazar ni alterar los movimientos ya registrados. Esto cubre movimientos que llegan sin pasar por la validación, como los fusionados desde otro dispositivo.

#### Scenario: Movimientos fusionados desde otro dispositivo
- **WHEN** el libro de "Billetera" recibe por fusión gastos registrados en el celular y en la PC que juntos dejan el saldo en -20.00 Bs
- **THEN** el sistema conserva todos los movimientos, informa la billetera como DESCUADRADA con un faltante de 20.00 Bs y un AJUSTE positivo posterior la vuelve a cuadrar

### Requirement: Identidad y Trazabilidad Lista para Sincronización
Cada billetera y cada movimiento DEBE (MUST) tener un identificador global único generado en el dispositivo, y cada movimiento DEBE registrar el dispositivo que lo creó, el momento en que ocurrió y el momento en que se registró. El orden cronológico usa el momento de ocurrencia y, ante empate, el de registro y luego el identificador.

#### Scenario: Gasto registrado en la noche con hora del almuerzo
- **WHEN** a las 21:00 Carlos registra desde su celular un GASTO que ocurrió a las 13:00
- **THEN** el movimiento guarda ocurrencia 13:00, registro 21:00, el identificador del celular y un identificador global único

#### Scenario: Orden determinista ante la misma hora
- **WHEN** dos movimientos de la misma billetera tienen igual momento de ocurrencia
- **THEN** el sistema los ordena por momento de registro y, si también coincide, por identificador, obteniendo siempre el mismo orden

### Requirement: Saldos de Billeteras Activas para la Vista Consolidada
El sistema DEBE (MUST) exponer los saldos de todas las billeteras activas del usuario, cada uno en su propia moneda, para que la vista consolidada del patrimonio los use como fuente en lugar de montos ingresados a mano.

#### Scenario: Patrimonio de Carlos desde sus billeteras
- **WHEN** Carlos tiene activas "Billetera" 165.00 Bs, "BNB Bs" 20,000.00 Bs y "Dólares colchón" 450.00 USD, y una billetera archivada con 300.00 Bs
- **THEN** el sistema entrega los tres saldos activos en BOB, BOB y USD, excluye la archivada y la vista consolidada los valora con la tasa configurada por Carlos
