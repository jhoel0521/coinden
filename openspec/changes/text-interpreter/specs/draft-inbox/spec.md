# Spec Delta

## MODIFIED Requirements

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

### Requirement: Bandeja de Revisión con Saldo Proyectado
El sistema DEBE (MUST) listar los borradores pendientes de un usuario (Capturado, Incompleto, Listo y No soportado), ordenados de menor a mayor confianza y, a igual confianza, por momento de captura, y, para cada billetera afectada por borradores Listos, el saldo que quedaría al confirmarlos, avisando sin bloquear cuando una billetera que no admite negativos quedaría por debajo de cero.

#### Scenario: Aviso de faltante proyectado
- **WHEN** "Billetera" tiene 10.00 Bs y hay borradores Listos por 3.00 Bs y 45.00 Bs en ella
- **THEN** la bandeja muestra un saldo proyectado de -38.00 Bs con un aviso de faltante, y los borradores siguen pendientes

#### Scenario: Terminales fuera de la bandeja
- **WHEN** Carlos tiene borradores Confirmados y Descartados
- **THEN** la bandeja no los incluye entre los pendientes

#### Scenario: Lo dudoso primero
- **WHEN** Carlos tiene un borrador de un comprobante con confianza ALTA capturado a las 10:00 y "gaste 2 para un semillita" con confianza BAJA capturado a las 15:00
- **THEN** la bandeja muestra primero el borrador de confianza BAJA

## ADDED Requirements

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
