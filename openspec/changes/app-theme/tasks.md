# Tasks: Identidad Visual "Cobre"

## 1. Paletas y contraste

- [ ] 1.1 Escribir la prueba de contraste (pares texto/fondo de D1 en `Cobre` y `CobreNoche`, mínimo 4.5:1) e implementar `CoinDenPalette` con las dos paletas del lienzo hasta que pase con `:composeApp:testDebugUnitTest`.

## 2. Tema

- [ ] 2.1 Descargar las fuentes Bricolage Grotesque y Manrope (OFL, Google Fonts) en los pesos 400, 600, 700 y 800 a `composeResources/font/` junto con su licencia; implementar `Typography`, `Shapes`, `LocalCoinDenColors` y `CoinDenTheme` según el modo del sistema (D2–D4); reemplazar `MaterialTheme` en `App.kt` y verificar que `:composeApp:assembleDebug` compila.

## 3. Tarjeta de borrador

- [ ] 3.1 Escribir pruebas de `Confidence.style()` (alta → éxito, media → advertencia, baja → error, cada una con ícono y texto) y de que `DraftCard` trae el medio de pago (QR → transferencia; "almuerzo 45 bnb" → sin medio); implementarlos hasta que pasen.
- [ ] 3.2 Implementar `DraftCardView` y rediseñar `CaptureDemoScreen` según D5 y D6, con todos los textos nuevos en `strings.xml`; verificar que compila, que `npm test` y `npm run lint` pasan y que no hay textos fijos en el código de las pantallas.

## 4. Verificación en el dispositivo

- [ ] 4.1 Instalar el APK en el emulador, capturar "page 3 bs para trasmporte", "almuerzo 45 qr", "almuerzo 45 bnb" y "hola", y tomar capturas en modo claro y en modo oscuro (`adb shell cmd uimode night yes|no`); verificar contra los escenarios de `app-theme` y guardar las capturas como evidencia.

## Workflow follow-up

- Archivar `app-theme` con `opsx-archive` tras la revisión.
- Siguiente del MVP: `onboarding-wallets-ui`, ya con el tema.
- Más adelante: temas verde claro y verde oscuro (nuevas paletas, sin cambiar pantallas).
