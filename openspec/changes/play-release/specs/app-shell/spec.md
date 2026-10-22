## MODIFIED Requirements

### Requirement: APK Compilado en CI
La integración continua DEBE (MUST) compilar el APK de depuración en cada pull request, además de ejecutar el lint y las pruebas existentes. No vuelve a correr al unir a `main`: la regla de `main` exige que la rama esté al día, así que el pull request ya probó el mismo código.

#### Scenario: Pull request que rompe la app
- **WHEN** un cambio impide compilar la app Android
- **THEN** el job de compilación del APK falla en la integración continua

#### Scenario: Unir un pull request
- **WHEN** la persona une un pull request a `main`
- **THEN** la integración continua no vuelve a correr las pruebas y la compilación

## ADDED Requirements

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
