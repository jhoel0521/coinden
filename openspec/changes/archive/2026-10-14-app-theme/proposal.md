# Proposal: Identidad Visual "Cobre" (App Theme)

## Why
La demo actual usa el tema genérico de Material y se ve pobre. Ya existe un diseño completo de CoinDen (lienzo "CoinDen", guía de estilo, componentes y las 7 pantallas del MVP) con una identidad cálida y confiable: la paleta "Cobre", tipografías propias y una tarjeta de borrador que muestra lo sugerido, la confianza y el medio de pago tal como los modela el dominio. Antes de construir las pantallas reales (`onboarding-wallets-ui` en adelante) hace falta tener ese tema en el código, para que cada pantalla nueva nazca con el estilo definitivo y no haya que rehacerla.

## What Changes
- **Herramientas al día**: el tema necesita fuentes variables con peso real, que Compose Multiplatform 1.7.3 no soporta. Se actualiza a la última combinación estable compatible: Kotlin 2.4.21, Compose Multiplatform 1.11.1 (la 1.12 ya exige AGP 9), Android Gradle Plugin 8.13.2 (el último de la línea 8), compileSdk y targetSdk 36, y las librerías al día (SQLDelight, SQLCipher, kotlinx.serialization, coroutines, AndroidX, ktlint). Sin cambios de comportamiento: las mismas pruebas deben seguir pasando.
- **Dos temas, según el modo del teléfono**: "Cobre" (claro) y "Cobre noche" (oscuro), con todos los roles de color de Material 3 más dos colores semánticos propios: éxito y advertencia.
- **Tipografías propias incluidas en la app**: Bricolage Grotesque para títulos y el logotipo, Manrope para el cuerpo; montos con números tabulares. Las fuentes viajan dentro del APK: la app no descarga nada.
- **Forma y elevación del diseño**: esquinas suaves, botones tipo píldora, sombras leves, objetivos táctiles de al menos 48 dp.
- **Tarjeta de borrador del diseño en la demo**: estado del borrador, monto, billetera y medio de pago separados, campos sugeridos con borde punteado y la etiqueta "sugerida", confianza con color, ícono y texto, y categoría con su motivo.
- **Accesible por diseño**: el texto principal cumple contraste WCAG AA en ambos temas; la confianza y lo sugerido nunca dependen solo del color.
- **Preparado para más temas**: cada tema es una tabla de colores; sumar luego un verde claro y un verde oscuro no cambia pantallas.

### Non-Goals
- Android Gradle Plugin 9: obliga a separar la app Android de `composeApp` en otro módulo y a migrar las librerías al plugin Android-KMP; queda para un cambio propio.
- Los temas Musgo, Medianoche y Alto contraste del lienzo, y un selector de tema en Ajustes (más adelante).
- Las pantallas nuevas del MVP (Inicio, Onboarding, Bandeja, Detalle, Resumen): llegan con sus propios cambios usando este tema.
- Logotipo e ícono de la app definitivos: se agrega el logotipo del diseño solo dentro de la demo; el ícono del lanzador queda para la publicación.
- Animaciones y transiciones.

## Capabilities

### New Capabilities
- `app-theme`: identidad visual de la app: temas claro y oscuro "Cobre" según el modo del sistema, tipografías incluidas, contraste accesible y la forma de mostrar borradores (lo sugerido, la confianza y el medio de pago).

### Modified Capabilities
<!-- Ninguna: la demo cambia su aspecto, no su comportamiento (sus escenarios de app-shell siguen igual). -->

## Impact
- **Build**: `gradle/libs.versions.toml`, los `build.gradle.kts` de cada módulo (compileSdk 36) y los cambios de API que traigan las versiones nuevas.
- **`composeApp`**: nuevo paquete de tema (`CoinDenTheme`, paletas, tipografía, formas), fuentes en `composeResources/font`, la pantalla de demo rediseñada y nuevos textos en `strings.xml`.
- **Pruebas**: contraste de las paletas y mapeo de la confianza a su presentación, en la JVM; verificación visual en el emulador en modo claro y oscuro.
- **APK**: crece por las dos fuentes (del orden de cientos de KB).
- **Sin cambios** en el dominio ni en la persistencia.
