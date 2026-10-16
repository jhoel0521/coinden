# Design: Notas sin "por" y Requisito de Resultado Dividido

## Context
Ver proposal.md. `PhraseReader.note()` toma lo que sigue a la primera preposición de nota del paquete (`LanguagePack.notePrepositions`) o, si no hay ninguna, las palabras no usadas. Hoy el español tiene "para", "de" y "en"; el portugués "de", "para" y "com"; el inglés "for" y "on".

## Goals / Non-Goals

**Goals:**
- Que "por" y sus equivalentes no queden como nota, sin perder notas que hoy funcionan.

**Non-Goals:**
- Una lista general de palabras de relleno ("con", artículos).

## Decisions

### D1. Preposiciones como datos
- es: se agrega "por".
- pt: se agregan "por", "pelo" y "pela".
- en: se agregan "by" y "via".

### D2. Nota de respaldo
En `PhraseReader.note()`:
1. Si después de la preposición quedan palabras no usadas, esas son la nota (como hoy).
2. Si no queda ninguna, la nota son las palabras no usadas de toda la frase que no sean preposiciones de nota.
3. Si tampoco hay, no hay nota.

Así "almuerzo 45 por qr" conserva "almuerzo" y "gaste 10 bs por qr" queda sin nota.

### D3. Requisito dividido
Solo cambia el spec: el comportamiento y las pruebas de la pregunta ya existen (`SameMovementPresenterTest`).

## Risks / Trade-offs
- [Una nota que empiece con "por" pierde esa palabra, como "por la tarde"] → aceptable: la nota es un resumen para clasificar, y "la tarde" se entiende igual.
