# Proposal

## Why

La marca actual (un búho-moneda dibujado con formas simples) se reemplaza por una identidad nueva y mejor: un búho que guarda un cofre con candado, rodeado de monedas. Así se cuenta lo que es CoinDen, "tu guarida de monedas" privada. El arte viene del diseñador en `design/brand/` y tiene que verse bien como ícono en cualquier lanzador, en el splash y en la bienvenida.

## What Changes

- **Ícono de Android:**
  - Ícono adaptable con fondo sólido #1C140F.
  - Primer plano: el arte del ícono dentro de la zona segura (66 de 108 dp, centrado), así ninguna máscara recorta las orejas ni la base del cofre.
  - Versión monocromática: la silueta del primer plano.
  - Se generan mipmaps de mdpi a xxxhdpi.
- **Splash:** fondo #1C140F con el ícono centrado, usando la API SplashScreen de Android, también en Android 8 a 11.
- **Logo dentro de la app:**
  - La ilustración completa, exportada a WebP @2x y @3x, dentro de una placa oscura redondeada para que se vea en los temas claro y oscuro.
  - Debajo, "CoinDen" y "Tu guarida de monedas" como texto real, con la tipografía de la app y traducido.
  - Se usa en la bienvenida, y en Inicio con el ícono pequeño junto al nombre.
- **Se quita la marca anterior:** `CoinDenLogo`, el búho-moneda vectorial y sus recursos de ícono.
- **iOS:** `design/brand/coinden-icono-app-1024.png` queda listo para `Assets.xcassets/AppIcon` cuando exista el proyecto de iOS (`ios-shell`).

## Non-Goals

- Cambiar los colores de la interfaz, las tipografías o los componentes.
- Crear el proyecto de iOS: hoy no existe, y el ícono y la LaunchScreen de iOS se aplicarán en `ios-shell`.
- Convertir la ilustración del logo a vector XML: pesa unos 850 KB y tiene más de mil trazos.

## Capabilities

### New Capabilities

### Modified Capabilities
- `app-shell`: el ícono de lanzamiento usa la nueva identidad (fondo #1C140F, primer plano en la zona segura, monocromo y mipmaps), y se agrega el splash con la marca.
- `app-theme`: se agrega cómo se presenta el logo de CoinDen dentro de la app (placa oscura y texto real debajo).

## Impact

- `composeApp/androidMain/res`: nuevos `mipmap-*` (primer plano y monocromo en PNG), `values/colors.xml`, tema del splash y `drawable` del splash. Se eliminan `drawable/ic_launcher_foreground.xml` e `ic_launcher_monochrome.xml`.
- `composeApp/commonMain`:
  - `composeResources/drawable` con `coinden_logo.webp` en dos densidades y `coinden_icon.webp`.
  - Nuevo componente `BrandLogo` / `BrandMark` en lugar de `CoinDenLogo` / `CoinDenWordmark`.
- Nueva dependencia: `androidx.core:core-splashscreen`, para el splash en Android 8 a 11.
- `design/brand/` con los tres archivos originales del diseñador.
- Script `scripts/brand/export_brand.py`, que regenera los íconos y WebP desde los SVG (reproducible).
