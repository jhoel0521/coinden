# Tasks

## 1. Repo público

- [x] 1.1 Auditar todo el historial de git antes de hacerlo público: secretos, keystores, `.env` y datos reales de comprobantes.
- [x] 1.2 `LICENSE` (MPL-2.0), `TRADEMARKS.md`, `.github/FUNDING.yml` y secciones de licencia y apoyo en el README.

## 2. Sitio (GitHub Pages)

- [x] 2.1 `site/` con inicio, política de privacidad y apoyar, en es, en y pt, sin JavaScript de terceros ni analítica.
- [x] 2.2 `.github/workflows/pages.yml` que publica `site/` con `actions/deploy-pages`.

## 3. Enlace en la app (app-navigation: Perfil de la Persona)

- [x] 3.1 Probar la regla pura que elige la página según el idioma (es → `/coinden/`, en → `/coinden/en/`, pt → `/coinden/pt/`) e implementarla.
- [x] 3.2 "Sitio y política de privacidad" en Acerca de, abriendo el navegador con `LocalUriHandler`; textos en es, en y pt.

## 4. Cierre

- [x] 4.1 `npm test`, `npm run lint`, `npm run build:android` y `openspec validate --all` en verde.

## Workflow follow-up

- La persona hace público el repo, activa Pages (fuente "GitHub Actions") y GitHub Sponsors, y entrega su enlace de PayPal.me.
- Comprobar que `https://jhoel0521.github.io/coinden/privacidad/` responde sin login.
- Archivar `publicacion` tras la unión.
