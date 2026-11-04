## MODIFIED Requirements

### Requirement: APK Compilado en CI
La integración continua DEBE (MUST) compilar el APK de depuración en cada pull request que toque código, además de ejecutar el lint y las pruebas existentes. No vuelve a correr al unir a `main`: la regla de `main` exige que la rama esté al día, así que el pull request ya probó el mismo código. Un pull request cuyos archivos son solo documentación (`*.md`, `docs/`, `openspec/`, `site/`, `design/`, `.github/FUNDING.yml`) no compila la app, pero sus cuatro checks obligatorios se saltan y cuentan como aprobados, para no bloquear la unión. Un pull request en borrador no corre la integración continua hasta quedar listo para revisión.

#### Scenario: Pull request que rompe la app
- **WHEN** un cambio impide compilar la app Android
- **THEN** el job de compilación del APK falla en la integración continua

#### Scenario: Unir un pull request
- **WHEN** la persona une un pull request a `main`
- **THEN** la integración continua no vuelve a correr las pruebas y la compilación

#### Scenario: Pull request de solo documentación
- **WHEN** un pull request cambia únicamente documentación, specs, diseño o el sitio estático
- **THEN** la integración continua no compila la app y los checks obligatorios de `main` quedan como aprobados por saltados

#### Scenario: Pull request que toca código
- **WHEN** un pull request cambia al menos un archivo fuera de documentación, specs, diseño y sitio
- **THEN** corren el lint, las pruebas, la verificación de artefactos y la compilación del APK de depuración

#### Scenario: Pull request en borrador
- **WHEN** un pull request está marcado como borrador
- **THEN** la integración continua no corre hasta que se marca listo para revisión

## ADDED Requirements

### Requirement: Versión Firmada por Etiqueta
El proyecto DEBE (MUST) producir el AAB firmado para Google Play al empujar una etiqueta `v<mayor>.<menor>.<parche>`, con el nombre de versión tomado de la etiqueta y el código de versión calculado como `mayor*10000 + menor*100 + parche`. Solo se publica si el commit de la etiqueta ya está en `main`.

#### Scenario: Etiqueta en main
- **WHEN** se empuja la etiqueta `v0.2.2` sobre un commit que ya está en `main`
- **THEN** se genera el AAB firmado con nombre de versión 0.2.2 y código de versión 202

#### Scenario: Etiqueta fuera de main
- **WHEN** se empuja una etiqueta sobre un commit que no está en `main`
- **THEN** el workflow falla y no se produce ningún AAB
