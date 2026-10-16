# Proposal

## Why

Lo que se dicta o escribe en el día muchas veces llega sin billetera o sin monto, y termina en "Para revisar". Mientras más completo venga lo diario, menos hay que revisar en la noche. Carlos y su pareja necesitan que la app les **enseñe, sin molestar**, a dar un poco más de información, con sus propias palabras.

## What Changes

- **Consejo según lo que faltó:** si a un borrador le faltó la billetera (o quedó sugerida) o el monto, su tarjeta (al capturar y en la bandeja) muestra un consejo corto. Si no faltó nada, no aparece.
- **Ejemplo con sus palabras:** el consejo trae una frase de ejemplo hecha con la nota y el monto de esa captura y una palabra de una billetera real de la persona (un alias o una palabra de su nombre), por ejemplo «power 10 bnb».
- **Contador de la semana en la bandeja:** "Esta semana N de M capturas quedaron listas sin revisar".

## Non-Goals

- Guardar un medio de pago ("qr", "efectivo") como alias de una billetera: el medio no nombra una billetera (todos los bancos cobran por QR).
- Avisos, notificaciones o preguntas que interrumpan la captura.
- Consejos generados por un modelo de lenguaje (llegarán con `local-llm-interpreter`).
- Usar la glosa del comprobante o la cercanía en la hora para reconocer el mismo pago (anotado para después).

## Capabilities

### New Capabilities
- `capture-coaching`: consejos según lo que faltó en cada captura, el ejemplo con las palabras de la persona y el contador semanal de capturas listas sin revisar.

### Modified Capabilities

## Impact

- `core/domain`: reglas puras de qué consejo corresponde, qué palabra nombra una billetera y el cálculo del contador.
- `composeApp`: la tarjeta del borrador muestra el consejo; la bandeja muestra el contador; textos en es, en y pt.
- Todo ocurre en el teléfono; no se envía nada.
