# Proposal

## Why

CoinDen está lista para publicarse en Google Play, pero no tiene versión de publicación: no hay compilación release optimizada ni firmada, ni AAB, ni el material que pide la ficha. Además, el CI corre dos veces por cada unión a `main` (en el PR y otra vez al unir) aunque la regla de `main` ya exige probar la rama al día.

## What Changes

- Versión release con R8 (código y recursos reducidos) y reglas para el código nativo y las librerías que lo necesitan.
- Firma con la llave de subida de la persona, leída de `keystore.properties` o de variables de entorno; la llave y sus contraseñas nunca entran al repositorio. Sin llave, la release local se firma con la llave de depuración para poder probarla.
- `versionCode` y `versionName` desde propiedades de Gradle (en el CI, desde la etiqueta `vX.Y.Z`).
- Workflow `release.yml`: al publicar una etiqueta `vX.Y.Z`, arma el AAB firmado con los secretos del repositorio y lo deja como artefacto.
- Pantalla "Licencias de código abierto" en Perfil → Acerca de, con cada librería, su licencia y el texto completo.
- Material de la ficha en `docs/play/`: textos en es, en y pt, ícono de 512, gráfico de 1024×500, capturas y respuestas preparadas para las declaraciones de Play Console.
- El CI corre en los pull requests (y a mano), no otra vez al unir a `main`.

## Capabilities

### New Capabilities
<!-- Ninguna. -->

### Modified Capabilities
- `app-shell`: versión de publicación firmada y CI sin doble ejecución.
- `app-navigation`: Acerca de ofrece las licencias de código abierto.

## Impact

- `composeApp/build.gradle.kts`, `composeApp/proguard-rules.pro`, `.gitignore`.
- `.github/workflows/ci.yml` y `release.yml` (nuevo).
- `composeApp`: pantalla de licencias y textos de licencias como recursos.
- `scripts/brand/export_brand.py` (ícono 512 y gráfico destacado) y `docs/play/`.
- Fuera de alcance: subir a Play automáticamente (Play Developer API) — se agrega cuando exista la app en Play Console.
