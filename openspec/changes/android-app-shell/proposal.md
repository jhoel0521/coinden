# Proposal: Primera App Instalable (Android App Shell)

## Why
Todo el dominio del MVP está listo (monedas, billeteras, borradores, intérprete y categorías), pero solo existe como código probado en la PC: no hay nada que se pueda instalar en un teléfono. Hace falta la estructura de la app (el "shell"): un APK que se compile, se instale y se abra, con el dominio corriendo dentro de Android, para que los siguientes pasos del MVP (persistencia y pantallas reales) tengan dónde vivir. Se hace con Compose Multiplatform para que la revisión desde la PC, central en la visión, reutilice las mismas pantallas más adelante.

## What Changes
- **El dominio también se compila para Android**: `core/domain` agrega el destino Android sin cambiar su comportamiento; las pruebas existentes siguen en verde.
- **Nuevo módulo `core/data-memory`**: implementaciones en memoria de los puertos (billeteras, libro, borradores, alias, categorías, paquetes instalados, configuración regional, reloj, ids y dispositivo) para que la app funcione hasta que llegue `local-persistence`. Al cerrar la app los datos se pierden; es temporal y está documentado.
- **Nuevo módulo `composeApp` con Compose Multiplatform**: interfaz compartida en `commonMain` y entrada Android en `androidMain`. En el MVP solo está activo Android; Desktop e iOS se agregan después sin reescribir pantallas.
- **Pantalla de demostración de captura**: al abrir la app hay billeteras de ejemplo (efectivo por defecto, banco con alias "qr") y una caja de texto. Lo que se escribe pasa por el intérprete real y la bandeja de borradores, y se muestra el borrador resultante: tipo, monto, billetera (indicando si es sugerida), confianza y categoría sugerida.
- **Primera capa de presentación**: los textos de la interfaz salen de recursos traducibles (español primero) y los montos se muestran con el formato regional del dispositivo, sin alterar sus dígitos. El dominio sigue sin idioma.
- **Contenedor de dependencias manual (`AppContainer`)** que arma los casos de uso del dominio con los adaptadores en memoria, sin librerías de inyección.
- **CI**: GitHub Actions compila también el APK de depuración.

### Non-Goals
- Guardar datos entre sesiones (`local-persistence`).
- Pantallas reales de billeteras, captura, revisión e informes (`onboarding-wallets-ui`, `capture-ui`, `review-inbox-ui`, `insights-ui`).
- Voz, OCR y "compartir a CoinDen" (`perception-adapters`, `capture-ui`).
- Destinos Desktop, iOS y Web.
- Publicación en tiendas, firma de release y ofuscación.

## Capabilities

### New Capabilities
- `app-shell`: app instalable en Android construida con Compose Multiplatform, dominio compilado para Android, adaptadores en memoria temporales, demostración de captura conectada al dominio real, primera capa de presentación traducible y APK compilado en CI.

### Modified Capabilities
<!-- Ninguna: el dominio no cambia su comportamiento. -->

## Impact
- **Build**: `settings.gradle.kts`, `gradle/libs.versions.toml` (Android Gradle Plugin, plugins de Compose Multiplatform y del compilador de Compose, AndroidX Activity), `core/domain/build.gradle.kts` (destino Android) y nuevos `core/data-memory` y `composeApp`.
- **Entorno local**: `local.properties` con la ruta del Android SDK (ignorado por git). La máquina de desarrollo ya tiene SDK 34/36 y los emuladores `Android14_API34` y `Small_Phone`.
- **CI**: un job nuevo que compila el APK en `ubuntu-latest`.
- **Pruebas**: las 234 del dominio siguen igual; se agregan pruebas de los adaptadores en memoria, del presentador de la demo y del formato de montos, más una verificación manual en el emulador.
