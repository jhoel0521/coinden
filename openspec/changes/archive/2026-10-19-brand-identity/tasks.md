# Tasks: Identidad de Marca

## 1. Exportación reproducible

- [x] 1.1 Copiar los tres archivos del diseñador a `design/brand/`. Escribir `scripts/brand/export_brand.py`, que desde los SVG genera:
  - el primer plano adaptable (arte al 61 % del lienzo de 108 dp y fondo igualado a #1C140F) en mipmaps de mdpi a xxxhdpi;
  - el monocromo (silueta del arte);
  - el ícono del splash;
  - el logo WebP @2x y @3x y el ícono WebP para `composeResources`.

  Verificar con vistas previas de las máscaras redonda y squircle.

## 2. Android

- [x] 2.1 Reemplazar el ícono adaptable (`mipmap-anydpi-v26` y `v33`) por fondo #1C140F, primer plano en mipmaps y monocromo. Quitar `ic_launcher_foreground.xml` e `ic_launcher_monochrome.xml`. Actualizar `AppIdentityTest`.
- [x] 2.2 Splash con `core-splashscreen`: tema `Theme.CoinDen.Splash` con fondo #1C140F e ícono centrado en `MainActivity` (req. Splash con la Marca).

## 3. Logo en la app

- [x] 3.1 `BrandLogo` (placa #1C140F redondeada con el WebP, y debajo "CoinDen" y el lema traducido como texto) y `BrandMark` (ícono pequeño y nombre) en `ui/components`. Reemplazar `CoinDenLogo` y `CoinDenWordmark` en la bienvenida e Inicio, y borrar `theme/CoinDenLogo.kt` y sus textos que ya no se usan (req. Logo de CoinDen en la App).
- [x] 3.2 Verificar que `npm test`, `npm run lint` y `npm run build:android` pasan. En el emulador: ícono en el cajón, en la pantalla principal con íconos temáticos y en la hoja de compartir; splash en frío; bienvenida en claro y oscuro. Capturas en el PR.

## Workflow follow-up

- Archivar `brand-identity` tras unir el PR.
- `ios-shell`: usar `design/brand/coinden-icono-app-1024.png` en AppIcon y una LaunchScreen #1C140F con el ícono.
