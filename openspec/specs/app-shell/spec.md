# app-shell Specification

## Purpose
Ofrece la primera versión instalable de CoinDen: una app Android construida con Compose Multiplatform que ejecuta el dominio real dentro del teléfono, con datos en memoria temporales y una pantalla de demostración de captura, como base para la persistencia y las pantallas del MVP.

## Requirements

### Requirement: Dominio Compilado para Android
El dominio DEBE (MUST) compilarse para Android además de JVM, sin cambiar su comportamiento: todas sus pruebas existentes deben seguir pasando y el código del dominio no debe depender de clases de Android.

#### Scenario: Pruebas del dominio intactas
- **WHEN** se agrega el destino Android al dominio
- **THEN** las pruebas existentes del dominio siguen pasando sin modificaciones

#### Scenario: Dominio sin dependencias de Android
- **WHEN** se revisa el código común del dominio
- **THEN** no importa ninguna clase de Android ni de Compose

### Requirement: App Instalable en Android
El proyecto DEBE (MUST) producir un APK de depuración que se instale y abra en dispositivos Android 8.0 (API 26) o superiores, con el identificador de aplicación `com.coinden.app`.

#### Scenario: Compilar el APK
- **WHEN** se ejecuta la tarea de compilación del APK de depuración
- **THEN** se genera un APK instalable de la app

#### Scenario: Abrir la app en el emulador
- **WHEN** se instala el APK en el emulador Android 14 y se abre la app
- **THEN** la app muestra la pantalla de demostración de captura sin cerrarse con error

### Requirement: Demostración de Captura
La pantalla de captura, que se abre desde Inicio, DEBE (MUST) permitir escribir una frase, pasarla por el intérprete real y la bandeja de borradores, y mostrar el borrador resultante: tipo de movimiento, monto, billetera indicando si es sugerida, nivel de confianza y categoría sugerida con su motivo, o que no hay categoría.

#### Scenario: Gasto hormiga sin billetera
- **WHEN** el usuario, con "Billetera" en efectivo (por defecto) y "BNB Bs", escribe "page 3 bs para trasmporte" y lo captura
- **THEN** la pantalla muestra un gasto de 3 Bs en la billetera de efectivo marcada como sugerida, con confianza baja y sin categoría

#### Scenario: Gasto por QR
- **WHEN** el usuario, con "Billetera" en efectivo (por defecto) y "BNB Bs", escribe "almuerzo 45 qr" y lo captura
- **THEN** la pantalla muestra un gasto de 45 Bs en la billetera de banco marcada como sugerida (es la única que acepta QR), con confianza baja y la categoría sugerida comida por la palabra "almuerzo"

#### Scenario: Gasto con alias y categoría sugerida
- **WHEN** el usuario, con "Billetera" en efectivo (por defecto) y "BNB Bs", escribe "almuerzo 45 bnb" y lo captura
- **THEN** la pantalla muestra un gasto de 45 Bs en la billetera de banco, sin marcarla como sugerida, con confianza media y la categoría sugerida comida por la palabra "almuerzo"

#### Scenario: Frase sin monto
- **WHEN** el usuario escribe "hola" y lo captura
- **THEN** la pantalla indica que el borrador quedó capturado sin propuesta, sin cerrarse con error

### Requirement: Presentación Traducible y Regional
La interfaz DEBE (MUST) tomar todos sus textos de recursos traducibles (español como primer idioma) y mostrar los montos con el formato regional del dispositivo sin alterar sus dígitos. Los enums y códigos del dominio se traducen solo en la capa de presentación.

#### Scenario: Monto con formato regional
- **WHEN** la pantalla muestra 1234.50 BOB con un locale que usa coma decimal
- **THEN** el monto se muestra con coma decimal y los mismos dígitos, por ejemplo "1.234,50"

#### Scenario: Categoría traducida
- **WHEN** el dominio sugiere la categoría base FOOD
- **THEN** la pantalla muestra el nombre traducido desde los recursos de la interfaz, por ejemplo "Comida"

### Requirement: APK Compilado en CI
La integración continua DEBE (MUST) compilar el APK de depuración en cada push y pull request, además de ejecutar el lint y las pruebas existentes.

#### Scenario: Pull request que rompe la app
- **WHEN** un cambio impide compilar la app Android
- **THEN** el job de compilación del APK falla en la integración continua
