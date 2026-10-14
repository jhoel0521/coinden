# local-persistence Specification

## Purpose
Guarda en el teléfono, cifrados, todos los datos del usuario (billeteras, libro, borradores, alias, categorización y perfil de monedas) para que sobrevivan al cierre de la app, en un formato de eventos versionado que mañana podrá viajar entre sus dispositivos.

## Requirements

### Requirement: Datos que Sobreviven al Cierre
Todo lo que el usuario registra DEBE (MUST) guardarse en el dispositivo y seguir disponible al volver a abrir la app: billeteras y su libro, borradores, alias y billetera por defecto, clasificaciones, reglas aprendidas, categorías propias y perfil de monedas. Lo que se lee después de reabrir es igual a lo que se guardó.

#### Scenario: Carlos reabre la app con sus datos
- **WHEN** Carlos crea sus billeteras, captura "almuerzo 45 qr", clasifica un gasto y cierra la app
- **THEN** al reabrirla sus billeteras tienen el mismo saldo, el borrador sigue pendiente con la misma propuesta y la regla aprendida vuelve a sugerir la misma categoría

#### Scenario: Perfil de monedas guardado
- **WHEN** Carlos configura bolivianos como moneda local con una tasa fija y reabre la app
- **THEN** el perfil de monedas tiene la misma moneda y la misma tasa exacta

### Requirement: Base Cifrada en el Dispositivo
Los datos DEBEN (MUST) guardarse cifrados por completo dentro del almacenamiento privado de la app. Sin la llave, el archivo de la base no debe poder leerse como base de datos ni revelar montos, notas o nombres en texto plano.

#### Scenario: Copia del archivo sin la llave
- **WHEN** alguien copia el archivo de la base fuera de la app y lo abre sin la llave
- **THEN** no puede leerlo como base de datos y no encuentra en él el texto de las capturas ni los nombres de las billeteras

### Requirement: Llave sin Contraseña Protegida por el Sistema
La llave de cifrado DEBE (MUST) generarse al azar en el primer inicio, quedar protegida por el almacén seguro de la plataforma y reutilizarse en cada inicio, sin pedir cuenta ni contraseña al usuario.

#### Scenario: Primer inicio
- **WHEN** el usuario abre la app por primera vez
- **THEN** la app crea la base cifrada sin pedirle contraseña ni registro

#### Scenario: Inicios siguientes
- **WHEN** el usuario vuelve a abrir la app
- **THEN** la app abre la misma base con la misma llave y encuentra sus datos

### Requirement: Eventos de Solo Agregar en Disco
Los eventos del libro, de los borradores y de las clasificaciones DEBEN (MUST) guardarse solo agregando: un evento con un id ya guardado se rechaza y el almacenamiento no ofrece forma de modificar ni borrar eventos. Las correcciones son eventos nuevos.

#### Scenario: Id repetido en el libro
- **WHEN** se intenta guardar un evento del libro con un id que ya existe
- **THEN** se rechaza y el evento original queda intacto

#### Scenario: Orden de los eventos
- **WHEN** se guardan varios eventos de un mismo borrador y luego se leen
- **THEN** se devuelven todos, en el orden en que se guardaron

### Requirement: Formato de Eventos Versionado y Exacto
Cada evento guardado DEBE (MUST) registrar su tipo y la versión de su formato, y al leerlo debe obtenerse un evento igual al que se guardó, para todos los tipos de evento. Los montos y las tasas se guardan en forma exacta, sin flotantes: un monto o una tasa leídos tienen exactamente los mismos dígitos, decimales y moneda.

#### Scenario: Ida y vuelta de todos los eventos
- **WHEN** se guarda y se vuelve a leer un evento de cada tipo (saldo inicial, ajuste, gasto, ingreso, transferencia con cambio de moneda, anulación, captura, interpretación, revisión, confirmación, descarte y clasificación)
- **THEN** cada evento leído es igual al guardado

#### Scenario: Precisión de satoshis y tasas fraccionarias
- **WHEN** se guarda una transferencia de 0.00000001 BTC y un perfil con una tasa que no tiene decimal exacto, como 1 USD = 1/3 de otra moneda personalizada
- **THEN** al leerlos el monto y la tasa son exactamente los mismos

### Requirement: Eventos Desconocidos se Conservan
Si el almacenamiento contiene un evento cuyo tipo o versión de formato esta versión de la app no reconoce, DEBE (MUST) conservarlo intacto y excluirlo de las lecturas del dominio, sin fallar ni descartarlo.

#### Scenario: Evento de una versión más nueva
- **WHEN** la base contiene, junto a eventos conocidos, un evento de un tipo que esta versión no conoce
- **THEN** las lecturas devuelven solo los eventos conocidos, la app no falla y el evento desconocido sigue guardado sin cambios

### Requirement: Identidad Estable del Dispositivo y del Usuario Local
El identificador del dispositivo y el del usuario local DEBEN (MUST) generarse una sola vez como UUID y conservarse entre sesiones, para que todos los eventos de ese dispositivo y usuario lleven siempre los mismos identificadores.

#### Scenario: Mismos ids al reabrir
- **WHEN** el usuario captura un gasto, cierra la app, la reabre y captura otro
- **THEN** ambos borradores tienen el mismo usuario dueño y sus eventos el mismo id de dispositivo

### Requirement: Mismo Comportamiento que el Almacenamiento en Memoria
El almacenamiento persistente DEBE (MUST) cumplir los mismos contratos que el almacenamiento en memoria, de modo que los casos de uso del dominio se comporten igual con cualquiera de los dos.

#### Scenario: Batería de contrato compartida
- **WHEN** se ejecuta la misma batería de pruebas de contrato de los repositorios contra el almacenamiento en memoria y contra el persistente
- **THEN** ambos la pasan completa
