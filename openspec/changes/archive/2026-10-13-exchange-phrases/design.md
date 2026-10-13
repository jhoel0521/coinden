# Design: Cambios de Moneda por Entrada y Salida

## Context
Ver proposal.md (Why) y el delta de `text-interpreter`. `PhraseReader` (en `core/domain/.../interpret/Readers.kt`) hoy:
1. Divide la frase por conectores del paquete (`conjunctions`) cuando cada parte tiene un número: "vendi 100 dolares y me dieron 1190 bs" se parte en dos y la segunda parte se lee como un gasto de 1190.
2. Detecta el tipo por el primer verbo (`exchangeWords` → `Kind.EXCHANGE`).
3. En un cambio, solo busca una tasa tras `rateMarkers` ("a 11.88") y calcula lo recibido con `exchangeInto` hacia la billetera por defecto (`TO_AMOUNT` inferido).
4. El monto es el primer número no usado; las palabras sobrantes van a la nota ("me dieron 112 bs").
5. El origen de un cambio solo sale de un alias; si no hay, queda `null`.
`TextNormalizer.words` elimina la coma que no está entre dígitos, así que "dólares, me dieron" llega como palabras sueltas.

## Goals / Non-Goals

**Goals:**
- Reconocer lo recibido con marcadores definidos como datos en los paquetes.
- Mantener intacto el comportamiento existente (todas las pruebas actuales siguen pasando).

**Non-Goals:**
- Cambios sin verbo de cambio y comprobantes de cambio (ver proposal).

## Decisions

### D1. Marcadores de recepción en `LanguagePack`
Nuevo campo `receivedMarkers: Set<String>` (con valor por defecto vacío para no romper paquetes de prueba), que admite frases de varias palabras ya normalizadas:
- en: `got`, `received`, `for`
- es: `me dieron`, `me dio`, `recibi`, `por`
- pt: `me deram`, `recebi`, `por`

`merged()` los une como al resto. Se buscan como secuencia exacta de palabras normalizadas consecutivas (la normalización ya quita tildes: "recibí" = "recibi"). Sin tolerancia de tipeo: marcadores cortos como "por" o "for" darían falsos positivos; si hiciera falta, se agrega la variante al paquete. Alternativa descartada: reutilizar `incomeWords` ("recibi" ya es verbo de ingreso), porque "dieron", "got" o "for" no son ingresos y "por"/"for" solo son marcadores dentro de un cambio.

### D2. Un marcador solo cuenta dentro de un cambio y antes de un monto
Un marcador se reconoce únicamente si el segmento es `Kind.EXCHANGE` y la palabra siguiente al marcador es un monto (con moneda opcional delante o detrás, como el monto principal). Así "for transportation" o "para trasmporte" siguen siendo notas, y "recibí 500 bs" sigue siendo un ingreso (no hay verbo de cambio).

### D3. Segmentación: el conector antes de un marcador no corta
En `segments`, un conector no abre un segmento nuevo si las palabras que lo siguen empiezan con un marcador de recepción. La regla es de datos (marcadores del paquete), no de idioma fijo en el código.

### D4. Lectura del cambio
Orden dentro de un segmento `EXCHANGE`:
1. Buscar el marcador de recepción y leer el monto recibido (`into`), marcando sus palabras como usadas. Origen `STATED` (o `INFERRED` si la lectura del número fue ambigua o la moneda se infirió), igual que el monto principal.
2. Buscar la tasa (`rateMarkers`) como hoy; si hay monto recibido, la tasa se consume (no va a la nota) pero no se usa.
3. Leer el monto entregado (`out`) como el primer número no usado.
4. Si no hay monto recibido, se mantiene el cálculo actual con la tasa (`TO_AMOUNT` inferido).
Si el monto recibido no trae moneda, se deja sin moneda: el evaluador toma la de la billetera destino, como ya hace con cualquier monto.

### D5. Billeteras sugeridas por moneda
Funciones puras sobre `WalletDirectory`:
- `from`: alias si se nombró; si no, la única billetera activa con la moneda de `out` (si hay exactamente una) → `SUGGESTED`.
- `to`: la billetera por defecto si acepta la moneda recibida (o si no se sabe la moneda recibida, como hoy); si no, la única billetera activa con esa moneda → `SUGGESTED`.
Si no hay candidata única, el campo queda `null` y el evaluador marca el faltante. Esto aplica a frases de cambio; retiros y movimientos simples no cambian.

## Risks / Trade-offs
- ["por"/"for" son palabras muy comunes] → solo cuentan dentro de un cambio y antes de un monto (D2); pruebas de regresión con las notas existentes.
- [Sugerir el origen por moneda puede equivocarse si el usuario tiene una billetera de dólares archivada] → solo se consideran billeteras activas y la sugerencia queda marcada, con confianza baja, para revisarse.
- [Ignorar la tasa dicha cuando hay monto recibido oculta una posible contradicción] → es el trato real lo que importa (principio de tasas reales); validar la coherencia queda fuera de alcance.

## Migration Plan
No aplica: cambia solo la lectura de frases nuevas. Los borradores ya guardados conservan su propuesta (la evidencia y los eventos no se reinterpretan).
