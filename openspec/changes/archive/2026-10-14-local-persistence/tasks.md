# Tasks: Datos Guardados y Cifrados en el Teléfono

## 1. Dominio y reglas del proyecto

- [x] 1.1 Escribir pruebas de la forma canónica de `ExchangeRate` (ida y vuelta de tasas decimales, fraccionarias como 1/3, inversas e identidad; rechazo de textos inválidos, ceros y monedas cambiadas) e implementar `toCanonical()` y `parseCanonical(text, from, to)`; verificar que pasan con `:core:domain:jvmTest` junto a las 234 existentes.
- [x] 1.2 Unificar la redacción del principio 6 en `MANIFIESTO.md`, la regla 3 de `AGENTS.md` y `openspec/config.yaml` (nunca flotantes; unidades mínimas en memoria, forma canónica exacta al persistir) y verificar que los tres textos dicen lo mismo.

## 2. Módulo y esquema

- [x] 2.1 Agregar al catálogo SQLDelight, el driver JVM, SQLCipher for Android, AndroidX SQLite, kotlinx.serialization y su plugin; crear `:core:data-local` (jvm + android, namespace `com.coinden.core.data.local`) con el esquema `.sq` de D3 sin sentencias `UPDATE` ni `DELETE` sobre `events` y `event_refs`; verificar que `:core:data-local:compileKotlinJvm` y la compilación Android generan la base.

## 3. Mapeo y formato de eventos

- [x] 3.1 Escribir las pruebas de ida y vuelta (un evento de cada tipo del libro, de borradores y de clasificación; billetera, regla, categoría propia y perfil con cada estrategia de tasa; satoshis y tasa 1/3) e implementar los DTOs y el mapeo dominio ↔ JSON versión 1 de D4 hasta que pasen.
- [x] 3.2 Escribir la prueba de eventos desconocidos (fila cruda con tipo inventado y otra con versión futura: se omiten al leer, no fallan y la fila sigue intacta) e implementar el registro de lectores de D5 hasta que pase.

## 4. Repositorios persistentes

- [x] 4.1 Escribir la batería de contrato por puerto (orden de inserción, id repetido rechazado sin tocar el original, eventos por billetera incluida la transferencia, borradores por dueño, clasificaciones por movimiento y por dueño, upsert de billeteras, alias, billetera por defecto, reglas, categorías y perfil) con su implementación en memoria, y verificar que `core/data-memory` la pasa.
- [x] 4.2 Implementar los cinco repositorios SQLDelight con dispatcher inyectado y transacciones (D3, D8) y verificar que pasan la misma batería de contrato sobre `JdbcSqliteDriver`.
- [x] 4.3 Implementar `app_identity` (D7) y escribir la prueba de reapertura sobre un archivo temporal: datos, orden de eventos, ids de dispositivo y dueño se conservan al cerrar y abrir otro driver; verificar que pasa.
- [x] 4.4 Escribir la prueba de feature de Carlos con la base persistente (crea billeteras, captura "almuerzo 45 qr" y "page 3 bs para trasmporte", clasifica un gasto y configura su perfil BOB; se cierra y reabre la base; saldos, borrador pendiente, sugerencia por la regla aprendida y perfil son los mismos) y verificar que pasa.

## 5. Cifrado en Android

- [x] 5.1 Implementar la llave (passphrase aleatoria protegida por `AndroidKeyStore`, D6) y la apertura de la base con SQLCipher en `androidMain`; escribir pruebas instrumentadas (el archivo no empieza con `SQLite format 3`, no contiene en texto plano una nota ni el nombre de una billetera, y se reabre con la llave guardada) y verificar que pasan en el emulador `Android14_API34` con `connectedDebugAndroidTest`.

## 6. App

- [x] 6.1 Cambiar `AppContainer` para recibir repositorios, identidad y dueño por constructor; en `CoinDenApplication` abrir la base cifrada, leer o crear la identidad y sembrar la demo solo si el dueño no tiene billeteras; probar en `composeApp` que sembrar dos veces sobre los mismos repositorios no duplica billeteras ni alias, y verificar que las pruebas existentes de la app siguen pasando.
- [x] 6.2 Escribir la prueba de `CaptureDemoPresenter.load()` (al abrir muestra los borradores pendientes guardados, más recientes primero) e implementarlo hasta que pase.

## 7. Comandos, CI y documentación

- [x] 7.1 Agregar `:core:data-local:jvmTest` a `npm test` y al job `unit` del CI; actualizar la arquitectura y los comandos de `AGENTS.md` y la sección "Desarrollo" del README (incluido cómo correr las pruebas instrumentadas); verificar que `npm test`, `npm run lint` y `npm run build:android` pasan.

## 8. Verificación en el dispositivo

- [x] 8.1 Instalar el APK en el emulador, capturar "page 3 bs para trasmporte" y "almuerzo 45 qr", cerrar la app por completo (forzar detención), reabrirla y verificar que los borradores siguen ahí y que no hay billeteras duplicadas; comprobar con `adb` que el archivo de la base no es legible como SQLite; registrar el tamaño del APK y guardar una captura como evidencia.

## Workflow follow-up

- Archivar `local-persistence` con `opsx-archive` tras la revisión.
- Siguiente del MVP: `onboarding-wallets-ui` (quitar `DemoSeed`). El PIN opcional queda como cambio aparte.
