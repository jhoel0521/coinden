# Proposal

## Why

Hoy CoinDen captura en segundos (dictado, foto, PDF, compartir) y procesa todo en segundo plano, pero Inicio solo muestra *cuántos* borradores hay pendientes: no hay dónde revisarlos con calma en la noche, corregir lo que la IA leyó mal ni ver el comprobante. Además, un gasto ya confirmado (por ejemplo, "almuerzo por qr" de 35 Bs) no se puede corregir cuando llega su comprobante real (32 Bs) sin que la app cambie datos por su cuenta. Carlos y su pareja, cada uno en su propio teléfono, necesitan una bandeja donde **nada cambia sin su aprobación**.

## What Changes

- **Bandeja de revisión**: una pantalla con los saldos proyectados (con aviso de faltante), una sección "Revisar" (preguntas de mismo gasto, cambios pendientes, capturas sin leer, borradores incompletos o de confianza baja o media) y una sección "Listos".
- **Revisar es un formulario editable**: tocar un borrador abre el formulario completo con lo propuesto. Ahí se corrige lo que haga falta (por ejemplo, 320 leído por 32,0) y se toca Confirmar, Guardar o Descartar.
- **"Confirmar los N listos"**: confirma de una vez solo los borradores Listos de confianza ALTA sin preguntas pendientes, mostrando antes un resumen. Lo que el libro rechace queda pendiente con su motivo.
- **Ver la evidencia**: foto y PDF a pantalla completa y reproducción del audio, descifrados solo en memoria.
- **Corrección de movimientos confirmados con aprobación**: cualquier cambio a un movimiento confirmado, incluso llenar un dato vacío como el número de transacción, pasa por el mismo formulario, con los campos que cambian resaltados junto a su valor anterior. Se aplica todo junto o nada. Corregir el dinero anula el movimiento anterior y registra el nuevo (append-only); corregir solo la nota, la categoría o la referencia no toca el libro.
- **Cambios pendientes desde un comprobante**: si un comprobante real coincide con un gasto ya confirmado, la app no cambia nada sola. Crea un cambio pendiente que se resuelve en el formulario: Confirmar, Descartar o "Son distintos". Si el comprobante es idéntico (misma referencia y mismos datos), solo se guarda como evidencia del gasto. El texto y el audio nunca generan cambios sobre un confirmado.
- **Aviso de diferencia grande**: si el monto nuevo es el doble o más, o la mitad o menos, del anterior, el formulario lo marca para mirarlo con cuidado, sin bloquear.
- **Historial de movimientos**: lista de los movimientos confirmados, del más reciente al más antiguo, con su evidencia y la opción Corregir.
- **BREAKING (comportamiento)**: "Son el mismo" con un gasto ya confirmado deja de agregar la referencia por su cuenta. Ahora abre el formulario de cambios. Una billetera distinta ya no impide unirlos: aparece como un cambio más para aprobar.

## Non-Goals

- Cotejar extractos bancarios contra lo confirmado (queda para `statement-reconciliation`, que generará estos mismos cambios pendientes).
- Detectar que un comprobante corresponde a un confirmado con **otro monto** (por ejemplo 35 contra 32): hoy solo se pregunta con el mismo monto y día; la persona puede corregir a mano desde el historial.
- Avisos o notificaciones para revisar en la noche.
- Editar transferencias, cambios de moneda y ajustes en el formulario (sigue siendo de gastos e ingresos); esos borradores se confirman o descartan desde su tarjeta.
- Filtros, búsqueda o reportes en el historial.
- El modelo de lenguaje local (`local-llm-interpreter`).
- Resolver conflictos de dos correcciones simultáneas en dos dispositivos (no hay sincronización todavía; el diseño lo deja preparado).

## Capabilities

### New Capabilities
- `review-inbox`: la pantalla de revisión (secciones, saldos proyectados, formulario editable de revisión, confirmar en lote, visor de evidencia e historial de movimientos con Corregir).
- `movement-correction`: reglas para corregir un movimiento confirmado con aprobación de la persona (anulación y nuevo movimiento, cambios sin dinero, cambios pendientes desde un comprobante, comprobante idéntico, aviso de diferencia grande e historial de la corrección).

### Modified Capabilities
- `capture-matching`: "Unir con un Movimiento Confirmado" pasa a crear un cambio pendiente que la persona aprueba en el formulario, en lugar de agregar la referencia sola; una billetera distinta ya no bloquea.
- `capture-ui`: "Pregunta de Mismo Gasto en el Resultado": "Son el mismo" con un confirmado abre el formulario de cambios.

## Impact

- `core/domain`: nuevos eventos de borrador para la corrección y el cambio pendiente, caso de uso de corrección (anulación + nuevo movimiento con id derivado), cálculo de diferencias y aviso de diferencia grande, selección de "listos para confirmar en lote".
- `core/data-local`: nuevos tipos de evento versionados en el sobre JSON (sin migración de tablas).
- `composeApp`: pantalla de bandeja, modo revisión/corrección de `DraftFormPresenter`/`DraftFormScreen`, visor de evidencia (Android: imagen, PDF con pdfbox-android y audio desde memoria), historial de movimientos, rutas nuevas y textos en es/en/pt.
- Sin dependencias nuevas.
