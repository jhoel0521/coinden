# Spec Delta

## MODIFIED Requirements

### Requirement: Icono de Lanzamiento Identificable
El APK DEBE (MUST) declarar un icono de CoinDen reconocible en el lanzador y selector de apps, con versión adaptable, redonda y monocromática. Usa la identidad de `design/brand/`: fondo sólido #1C140F y el arte del ícono como primer plano dentro de la zona segura (66 de 108 dp), de modo que ninguna máscara recorte el arte; la versión monocromática es su silueta. Para Android 8.0 hay mipmaps de cada densidad.

#### Scenario: Icono incluido en APK
- **WHEN** se compila el APK
- **THEN** el manifiesto referencia recursos de icono existentes y empaquetados para las versiones soportadas

#### Scenario: Máscara redonda
- **WHEN** el lanzador aplica una máscara redonda o squircle al ícono
- **THEN** se ven completas las orejas del búho y la base del cofre, sobre el fondo #1C140F sin bordes de otro tono

## ADDED Requirements

### Requirement: Splash con la Marca
Al abrir la app, Android DEBE (MUST) mostrar el splash con fondo #1C140F y el ícono de CoinDen centrado, en todas las versiones soportadas, hasta que la primera pantalla esté lista.

#### Scenario: Abrir la app
- **WHEN** Carlos abre CoinDen en frío
- **THEN** ve el fondo #1C140F con el ícono centrado y luego la bienvenida o Inicio
