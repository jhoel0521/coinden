# Proposal

## Why

En la verificación de la issue #2, con la letra del sistema al máximo (2,0), "Português" se corta en el selector de idioma de la bienvenida. El grupo decide mostrar segmentados sumando el ancho de todas las etiquetas, pero los segmentados se reparten el ancho en partes iguales: la etiqueta más larga recibe solo su parte, menos la marca de la elegida. La spec ya pide que los segmentados se usen solo si caben; el código no lo cumple.

## What Changes

- Un grupo de elección única usa segmentados solo si la etiqueta más larga entra entera en su parte del ancho (con relleno y la marca de la elegida). Si no, pasa a la fila deslizable, como ya hace cuando no caben.
- Afecta a todos los grupos que usan esa elección: idioma (bienvenida y Perfil), tipo de movimiento, moneda de la billetera y periodo de Movimientos.

## Capabilities

### New Capabilities
<!-- Ninguna. -->

### Modified Capabilities
- `ui-components`: "Elección Única según la Cantidad de Opciones" precisa cuándo caben los segmentados y suma el escenario de los idiomas con la letra al máximo.

## Non-Goals

- Cambiar la forma de los mosaicos con ícono ni la cuadrícula de "Más…".
- Ajustar el tamaño de letra de la app: se respeta el del sistema.

## Impact

- `composeApp/.../ui/components/LayoutRules.kt` (`ChoiceLayout`) y `Choice.kt`, con su prueba en `LayoutRulesTest`.
