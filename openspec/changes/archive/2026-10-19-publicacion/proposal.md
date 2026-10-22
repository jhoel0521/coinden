# Proposal

## Why

Para publicar en Google Play hace falta una política de privacidad en una dirección pública, y el proyecto quiere vivir de donaciones con el código a la vista, coherente con "sin servidores y verificable". El repo pasa a ser público con una licencia que proteja que las mejoras sigan abiertas, y la app tiene que llevar a la persona a esa página.

## What Changes

- Licencia MPL-2.0 para el código; el nombre "CoinDen" y el logo quedan reservados (`TRADEMARKS.md`).
- Sitio estático en `site/`, publicado con GitHub Pages: inicio, política de privacidad y cómo apoyar, en español, inglés y portugués. Sin JavaScript de terceros ni analítica.
- Donaciones solo en el sitio, el README y GitHub (GitHub Sponsors y PayPal.me); la app no tiene botón de pago.
- En Perfil → Acerca de, un enlace "Sitio y política de privacidad" que abre el sitio en el idioma de la app.

## Capabilities

### New Capabilities
<!-- Ninguna: el sitio y la licencia no son comportamiento de la app. -->

### Modified Capabilities
- `app-navigation`: Acerca de, en Perfil, enlaza al sitio y a la política de privacidad en el idioma de la app.

## Impact

- Raíz del repo: `LICENSE`, `TRADEMARKS.md`, `.github/FUNDING.yml`, README.
- `site/` y `.github/workflows/pages.yml` (GitHub Pages con `actions/deploy-pages`).
- `composeApp`: enlace en `ProfileScreen` con `LocalUriHandler`; textos en es, en y pt.
- Pasos de la persona: hacer público el repo, activar Pages con la fuente "GitHub Actions" y activar GitHub Sponsors.
- Fuera de alcance: botón de donación dentro de la app, analítica del sitio, dominio propio.
