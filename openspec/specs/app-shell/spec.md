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

### Requirement: Presentación Traducible y Regional
La interfaz DEBE (MUST) tomar todos sus textos de recursos traducibles (español como primer idioma) y mostrar los montos con el formato regional del dispositivo sin alterar sus dígitos. Los enums y códigos del dominio se traducen solo en la capa de presentación.

#### Scenario: Monto con formato regional
- **WHEN** la pantalla muestra 1234.50 BOB con un locale que usa coma decimal
- **THEN** el monto se muestra con coma decimal y los mismos dígitos, por ejemplo "1.234,50"

#### Scenario: Categoría traducida
- **WHEN** el dominio sugiere la categoría base FOOD
- **THEN** la pantalla muestra el nombre traducido desde los recursos de la interfaz, por ejemplo "Comida"

### Requirement: APK Compilado en CI
La integración continua DEBE (MUST) compilar el APK de depuración en cada pull request, además de ejecutar el lint y las pruebas existentes. No vuelve a correr al unir a `main`: la regla de `main` exige que la rama esté al día, así que el pull request ya probó el mismo código.

#### Scenario: Pull request que rompe la app
- **WHEN** un cambio impide compilar la app Android
- **THEN** el job de compilación del APK falla en la integración continua

#### Scenario: Unir un pull request
- **WHEN** la persona une un pull request a `main`
- **THEN** la integración continua no vuelve a correr las pruebas y la compilación

### Requirement: Icono de Lanzamiento Identificable
El APK DEBE (MUST) declarar un icono de CoinDen reconocible en el lanzador y selector de apps, con versión adaptable, redonda y monocromática. Usa la identidad de `design/brand/`: fondo sólido #1C140F y el arte del ícono como primer plano dentro de la zona segura (66 de 108 dp), de modo que ninguna máscara recorte el arte; la versión monocromática es su silueta. Para Android 8.0 hay mipmaps de cada densidad.

#### Scenario: Icono incluido en APK
- **WHEN** se compila el APK
- **THEN** el manifiesto referencia recursos de icono existentes y empaquetados para las versiones soportadas

#### Scenario: Máscara redonda
- **WHEN** el lanzador aplica una máscara redonda o squircle al ícono
- **THEN** se ven completas las orejas del búho y la base del cofre, sobre el fondo #1C140F sin bordes de otro tono

### Requirement: Splash con la Marca
Al abrir la app, Android DEBE (MUST) mostrar el splash con fondo #1C140F y el ícono de CoinDen centrado, en todas las versiones soportadas, hasta que la primera pantalla esté lista.

#### Scenario: Abrir la app
- **WHEN** Carlos abre CoinDen en frío
- **THEN** ve el fondo #1C140F con el ícono centrado y luego la bienvenida o Inicio

### Requirement: Versión de Publicación Firmada
El proyecto DEBE (MUST) generar una versión de publicación optimizada (código y recursos reducidos) en formato AAB, firmada con la llave de subida de la persona. La llave y sus contraseñas nunca se guardan en el repositorio: se leen de un archivo local ignorado por git o de los secretos del CI. El número de versión sale de la etiqueta `vX.Y.Z`. La versión optimizada funciona igual que la de depuración: base cifrada, lectura de comprobantes, dictado y bloqueo.

#### Scenario: Etiqueta de versión
- **WHEN** la persona publica la etiqueta `v0.2.0`
- **THEN** el CI arma un AAB firmado con versión "0.2.0" y lo deja para descargar

#### Scenario: Sin llave en la máquina
- **WHEN** se arma la versión de publicación sin el archivo de la llave
- **THEN** se firma con la llave de depuración para probarla localmente, y no sirve para subir a Play

#### Scenario: La versión optimizada funciona
- **WHEN** se instala la versión de publicación y Carlos hace el onboarding, captura un gasto y activa el bloqueo
- **THEN** todo funciona igual que en la versión de depuración
