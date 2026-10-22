# Tasks

## 1. CI (app-shell: APK Compilado en CI)

- [x] 1.1 `ci.yml` corre en pull requests y a mano, no en `push` a `main`.

## 2. Versión de publicación (app-shell: Versión de Publicación Firmada)

- [x] 2.1 `release` con R8 y recursos reducidos; `proguard-rules.pro` para el JNI de Whisper y lo que haga falta.
- [x] 2.2 Firma desde `keystore.properties` o variables de entorno (ignorado por git); sin llave, firma de depuración.
- [x] 2.3 `versionCode`/`versionName` desde propiedades de Gradle.
- [x] 2.4 `release.yml`: con la etiqueta `vX.Y.Z`, AAB firmado con los secretos del repositorio como artefacto.
- [x] 2.5 Instalar la versión optimizada en el emulador y recorrer onboarding, captura, lectura de un comprobante y bloqueo.
- [x] 2.6 Hallazgo de la prueba: al confirmar desde la hoja de captura con la pestaña Revisar abierta, la lista no se recargaba y el gasto seguía en "Listos"; ahora se recarga como Inicio.

## 3. Licencias (app-navigation: Perfil de la Persona)

- [x] 3.1 Prueba de la lista de licencias (cada librería con su licencia y su texto) e implementación; pantalla y enlace en Acerca de; textos en es, en y pt.
- [x] 3.2 Crédito del autor en Acerca de ("Hecho por @jhoel0521", enlace al repositorio), en es, en y pt.

## 4. Ficha de Play

- [x] 4.1 `docs/play/`: textos en es, en y pt, ícono de 512, gráfico de 1024×500, capturas y respuestas para las declaraciones; guía paso a paso.

## 5. Cierre

- [x] 5.1 `npm test`, `npm run lint`, `npm run build:android`, `bundleRelease` y `openspec validate --all` en verde.

## Workflow follow-up

- La persona crea su llave de subida y la cuenta de Play Console, carga los secretos del repositorio y publica la primera etiqueta.
- Archivar tras la unión.
