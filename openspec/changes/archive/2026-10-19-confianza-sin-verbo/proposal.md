# Proposal

## Why

La validación del MVP (issue #2, hallazgo F5) mostró que "almuerzo 25 bnb" y "taxi 10 bnb" quedan con confianza media y van a "Para revisar". Es exactamente la forma que enseña la app (el onboarding y el consejo de captura usan "almuerzo 45 bnb"), así que seguir el consejo no deja la captura lista.

## What Changes

- Una frase sin ningún verbo que nombra su billetera ("almuerzo 25 bnb") se lee como gasto **dicho** por la persona: es la forma corta que la app enseña. Sin billetera nombrada sigue deducido, porque podría ser un verbo de otro idioma que no se entiende.
- Un verbo reconocido con error de tipeo ("page") sigue contando como tipo deducido.
- Con monto y billetera dichos, esas frases quedan con confianza alta y van a "Listos".

## Capabilities

### New Capabilities
<!-- Ninguna. -->

### Modified Capabilities
- `text-interpreter`: el gasto sin verbo cuenta como dicho por la persona.

## Impact

- `core/domain`: `PhraseReader` (`model/interpret/Readers.kt`), origen del tipo cuando no hay verbo.
- Pruebas de feature de Carlos (bandeja) y unitarias del lector de frases.
