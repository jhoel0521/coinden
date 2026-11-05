## ADDED Requirements

### Requirement: El Modelo Corre Solo en el Teléfono
El modelo de lenguaje DEBE (MUST) ejecutarse íntegramente en el dispositivo. Interpretar NO DEBE (MUST NOT) usar la red ni enviar a ningún servidor el texto, los montos, las imágenes, los audios ni ningún dato de la persona.

#### Scenario: Interpretar en modo avión
- **WHEN** Carlos está en modo avión y dicta "gasté 45 en almuerzo con bnb" con el modelo instalado
- **THEN** el modelo lo interpreta igual que con conexión

#### Scenario: Nada sale del teléfono
- **WHEN** el modelo interpreta cualquier captura
- **THEN** no se abre ninguna conexión de red para interpretarla

### Requirement: Entrada y Salida con Formato Cerrado
El modelo DEBE (MUST) recibir un prompt de sistema fijo, responder con temperatura 0 y producir únicamente el formato de salida acordado: las mismas propuestas tipadas que produce el respaldo determinista. Una respuesta que no cumple el formato se descarta como si no hubiera habido modelo.

#### Scenario: La misma captura dos veces
- **WHEN** se interpreta dos veces la misma captura con el mismo modelo y el mismo directorio de billeteras
- **THEN** las dos veces se obtiene la misma propuesta

#### Scenario: El modelo responde cualquier otra cosa
- **WHEN** el modelo devuelve texto que no cumple el formato acordado
- **THEN** la propuesta se descarta, se interpreta con el respaldo determinista y el borrador queda para revisar

#### Scenario: El modelo no puede inventar una billetera
- **WHEN** el modelo propone una billetera que no está en el directorio de la persona
- **THEN** esa billetera no se usa y el borrador queda para revisar

### Requirement: Las Reglas Verifican lo que el Modelo Propone
Toda propuesta del modelo DEBE (MUST) pasar por un validador determinista antes de llegar a un borrador. El validador comprueba que el monto propuesto aparezca en el texto interpretado o en un comprobante del paquete, que la moneda y la billetera existan, y que ningún hecho que trae un comprobante (monto, moneda, hora, referencia o billetera) haya sido cambiado. Lo que no pasa la verificación no se usa como hecho.

#### Scenario: Un monto que nadie dijo
- **WHEN** el modelo propone 45.00 Bs para un paquete cuyo comprobante dice 25.00 Bs y cuyo audio no menciona 45
- **THEN** las reglas no aceptan el monto y el pago queda para revisar

#### Scenario: El modelo contradice el comprobante
- **WHEN** el comprobante dice 25.00 Bs y el modelo propone 25.00 USD
- **THEN** la moneda del comprobante se conserva y el borrador queda para revisar con las dos versiones

#### Scenario: Propuesta respaldada
- **WHEN** el modelo propone un gasto de 25.00 Bs con la nota "comida" para un comprobante de 25.00 Bs
- **THEN** la verificación pasa y el borrador queda Listo

### Requirement: Modelo y Reglas que no Coinciden
Cuando el modelo y el respaldo determinista producen propuestas distintas para la misma captura, el borrador DEBE (MUST) quedar para revisar, mostrando qué entendió cada uno. NUNCA DEBE (MUST NOT) registrarse en silencio ninguna de las dos.

#### Scenario: Dos lecturas distintas
- **WHEN** el modelo entiende un gasto de 60.00 Bs en "comida" y las reglas entienden 60.00 Bs sin nota
- **THEN** el borrador queda para revisar con las dos lecturas a la vista

#### Scenario: Nada toca saldos
- **WHEN** cualquier borrador queda para revisar por desacuerdo
- **THEN** ningún saldo cambia hasta que la persona lo confirma

### Requirement: Respaldo Mínimo sin Modelo
Sin modelo instalado, la app DEBE (MUST) seguir interpretando con reglas deterministas: el monto y la moneda según el locale, la billetera por alias, el medio de pago, la fecha y la hora, y los comprobantes por plantilla o por el lector genérico. La app NO DEBE (MUST NOT) exigir el modelo para capturar, confirmar ni corregir nada.

#### Scenario: Teléfono que no puede con el modelo
- **WHEN** Carlos tiene un teléfono de 2 GB de memoria, donde el modelo no se ofrece, y escribe "almuerzo 45 bnb"
- **THEN** la app propone un gasto de 45 en "BNB Bs" con la nota "almuerzo"

#### Scenario: Un comprobante sin modelo
- **WHEN** Carlos comparte un comprobante de "Pago QR otros bancos" con "Importe Bs25.00" y no tiene modelo
- **THEN** el borrador propone un gasto de 25.00 BOB con su referencia, igual que antes

#### Scenario: Lo que el respaldo no alcanza a entender
- **WHEN** sin modelo llega un paquete con dos comprobantes y un audio que explica cuál es cuál
- **THEN** cada comprobante da su pago con sus hechos, el audio queda como nota y el borrador va a revisar, sin perder ninguna evidencia

### Requirement: El Modelo de Lenguaje es Opcional y se Puede Borrar
El modelo DEBE (MUST) descargarse solo si la persona lo pide, mostrando su tamaño antes, y poder borrarse en cualquier momento. La app DEBE (MUST) explicar, antes de descargarlo, que corre en el teléfono, que no envía datos y que solo propone.

#### Scenario: Borrar el modelo
- **WHEN** Carlos borra el modelo descargado
- **THEN** el espacio se libera y la app sigue funcionando con el respaldo mínimo

#### Scenario: Antes de descargar
- **WHEN** Carlos abre la pantalla del modelo de lenguaje
- **THEN** ve su tamaño, que corre en su teléfono, que no envía datos y que solo propone borradores que él confirma

### Requirement: El Modelo Nunca Registra
El modelo NUNCA DEBE (MUST NOT) crear, confirmar ni modificar un movimiento. Solo produce propuestas que terminan en borradores, y un borrador no afecta saldos hasta que la persona lo confirma.

#### Scenario: El modelo propone y Carlos decide
- **WHEN** el modelo interpreta un paquete y produce tres pagos
- **THEN** quedan tres borradores y ningún saldo cambia hasta que Carlos los confirma
