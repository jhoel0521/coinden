# Design: Identidad Visual "Cobre"

## Context
Ver proposal.md (Why) y el spec `app-theme`. La fuente de verdad visual es el lienzo de diseño "CoinDen" (tableros Guía de estilo, Componentes, Tarjeta de borrador e Inicio). Hoy `App.kt` envuelve la demo en `MaterialTheme` sin parámetros, y `CaptureDemoScreen` usa componentes Material sin estilo propio. `DraftCard` (estado semántico del presentador) no incluye todavía el medio de pago que el dominio ya guarda en la propuesta.

## Goals / Non-Goals

**Goals:**
- Un único punto de estilo (`CoinDenTheme`) que usen todas las pantallas.
- Tokens copiados del lienzo, con contraste verificado por prueba.

**Non-Goals:**
- Temas extra, selector de tema y pantallas nuevas (ver proposal).

## Decisions

### D0. Versiones
| Pieza | De | A | Motivo |
|---|---|---|---|
| Kotlin (+ plugins compose y serialization) | 2.0.21 | 2.4.21 | última estable; requisito de CMP 1.12 (mínimo 2.1) |
| Compose Multiplatform | 1.7.3 | 1.11.1 | fuentes variables con peso (`variationSettings`). La 1.12.1 (Jetpack Compose 1.12) exige AGP 9.1 y compileSdk 37: llega con el cambio de AGP 9 |
| Android Gradle Plugin | 8.5.2 | 8.13.2 | último AGP 8; Kotlin 2.4.21 admite AGP 8.5.2–9.3.1 |
| Gradle | 8.14.3 | 8.14.3 | ya cumple AGP 8.13 y Kotlin 2.4 (7.6.3–9.7.0) |
| compileSdk / targetSdk | 34 | 36 | requerido por las librerías AndroidX nuevas; plataforma 36 ya instalada |
| SQLDelight | 2.0.2 | 2.4.1 | compatible con Kotlin 2.4 |
| SQLCipher / AndroidX SQLite | 4.6.1 / 2.4.0 | 4.19.1 / 2.7.1 | al día |
| kotlinx.serialization / coroutines | 1.7.3 / 1.9.0 | 1.11.0 / 1.11.0 | al día |
| Activity Compose | 1.9.3 | 1.13.0 | al día |
| ktlint Gradle | 12.1.2 | 14.2.0 | al día |

Fuente de compatibilidad: guía de compatibilidad de Kotlin Multiplatform y tabla de versiones de Compose Multiplatform. AGP 9 se descarta aquí: con él, el plugin de Kotlin Multiplatform deja de ser compatible con `com.android.application` y `com.android.library` en el mismo módulo, lo que obliga a separar la app y migrar las librerías (cambio propio). Si alguna librería no compila con esta combinación, se baja solo esa pieza a la versión compatible más alta y se anota aquí.

### D1. Paletas como datos
`CoinDenPalette` (data class en `commonMain/.../theme`) con los roles de Material 3 que usa el diseño más `success`, `onSuccess`, `successContainer`, `onSuccessContainer`, `warning`, `warningContainer` y `onWarningContainer`. Dos instancias, `Cobre` y `CobreNoche`, con los hex del lienzo:

| Rol | Cobre | Cobre noche |
|---|---|---|
| primary / onPrimary | `#9A4A22` / `#FFFFFF` | `#FFB693` / `#561F05` |
| primaryContainer / on | `#FFDBCB` / `#3A1203` | `#7A3410` / `#FFDBCB` |
| secondary / on | `#6E5A4C` / `#FFFFFF` | `#D9C2B2` / `#3D2D22` |
| secondaryContainer / on | `#F6DECD` / `#271810` | `#554337` / `#F6DECD` |
| tertiary / on | `#2F6672` / `#FFFFFF` | `#9ACFDD` / `#00363F` |
| tertiaryContainer / on | `#C9EAF3` / `#001F26` | `#144E59` / `#C9EAF3` |
| error / on | `#B3261E` / `#FFFFFF` | `#F2B8B5` / `#601410` |
| errorContainer / on | `#F9DEDC` / `#410E0B` | `#8C1D18` / `#F9DEDC` |
| surface / onSurface | `#FFF8F3` / `#221A15` | `#1A1210` / `#F0DFD7` |
| onSurfaceVariant | `#53443B` | `#D8C2B7` |
| outline / outlineVariant | `#85736A` / `#D8C2B7` | `#A08D83` / `#53443B` |
| surfaceContainer (lowest, low, —, high, highest) | `#FFFFFF`, `#FCF1EA`, `#F6EBE3`, `#F0E5DD`, `#EADFD8` | `#140D0B`, `#221A17`, `#271E1B`, `#322825`, `#3D332F` |
| success / successContainer / on | `#2E6B3A` / `#CDEFD0` / `#0B2912` | `#A6D6A9` / `#1D5229` / `#CDEFD0` |
| warning / warningContainer / on | `#8A5300` / `#FFDDB0` / `#2C1700` | `#FFB95C` / `#6A4000` / `#FFDDB0` |

`CoinDenPalette.toColorScheme()` arma el `ColorScheme` de Material 3. Los colores extra (éxito, advertencia) viajan en un `CompositionLocal` (`LocalCoinDenColors`). Sumar un tema verde es agregar otra instancia; ninguna pantalla cambia. Los hex se guardan como `Long` ARGB en la paleta (datos puros, probables en la JVM sin Compose).

### D2. Tema según el sistema
`CoinDenTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)` elige `CobreNoche` u `Cobre` y provee `colorScheme`, `typography`, `shapes` y `LocalCoinDenColors`. `App.kt` reemplaza `MaterialTheme` por `CoinDenTheme`. Compose recompone al cambiar el modo del sistema.

### D3. Tipografía incluida
Fuentes OFL de Google Fonts guardadas en `composeResources/font/` (Bricolage Grotesque y Manrope, en sus pesos usados: 400, 600, 700, 800). `Typography` de Material 3: `display*`, `headline*` y `title*` con Bricolage Grotesque; `body*` y `label*` con Manrope, en los tamaños del lienzo (display 36, headline 26, título 20 y 17, cuerpo 16 y 14, etiqueta 14 y 12). Los montos usan `fontFeatureSettings = "tnum, lnum"` con un estilo `amount` (18) y `amountLarge` (30). Las licencias OFL van junto a las fuentes. No se usa la descarga de fuentes de Google (principio: sin red para la app).

### D4. Formas y elevación
`Shapes` con los radios "suave" del lienzo: extraSmall 6, small 10, medium 16, large 24, extraLarge 32; botones principales como píldora. Alturas mínimas: botones 48 dp, botón de captura 60 dp, filas de billetera 64 dp.

### D5. Tarjeta de borrador
`DraftCard` (presentador) gana `paymentMethod: PaymentMethod?`, leído de la propuesta. La presentación de la confianza es una función pura y probada, `Confidence.style()` → rol de color (éxito, advertencia o error), ícono y texto, con un `when` exhaustivo; `PaymentMethod.label()` igual. El composable `DraftCardView` sigue el tablero "Tarjeta de borrador": cabecera con estado y hora, evidencia original en cursiva, monto grande con números tabulares, filas de billetera y medio de pago (la billetera sugerida con borde punteado y la palabra "sugerida"), categoría con su motivo y el chip de confianza. Los íconos son vectores propios de stroke (sin emojis), como en el lienzo.

### D6. Demo rediseñada, sin cambiar su comportamiento
`CaptureDemoScreen` adopta el estilo del tablero "Captura": logotipo (vector del lienzo), aviso de privacidad, campo grande y botón "Capturar" tipo píldora, y la lista de `DraftCardView`. Los escenarios de `app-shell` siguen igual; solo cambia el aspecto. Todos los textos nuevos van a `strings.xml`.

## Risks / Trade-offs
- [Los hex del lienzo pueden cambiar] → viven en un solo archivo de paletas y la prueba de contraste avisa si un cambio rompe la accesibilidad.
- [Las fuentes suman peso] → solo los 4 pesos usados, del orden de cientos de KB frente a los 31 MB actuales.
- [El diseño muestra un dato desactualizado ("almuerzo 45 qr" con confianza media)] → la implementación sigue la regla vigente (sugerida, confianza baja); el lienzo se puede corregir aparte.

## Migration Plan
No aplica: solo cambia el aspecto de la demo.
