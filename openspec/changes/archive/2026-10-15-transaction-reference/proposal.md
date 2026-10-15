# Proposal: Número de Transacción y Gastos Repetidos

## Why
Un mismo pago llega muchas veces a CoinDen: se dicta apurado ("acabo de pagar un taxi a 40 bs"), después se comparte el comprobante, a veces dos veces por distracción, y en la noche se mira el extracto. Hoy cada captura es un gasto nuevo y el saldo puede contar dos veces lo mismo. Además, el número de transacción que trae casi todo comprobante (el "Nro. de transacción" del banco, el "ID da transação" de un Pix o el hash de una transacción cripto) se lee y se pierde. Hace falta guardar ese número y una regla simple, en la que la app nunca decide: si dos capturas parecen el mismo gasto, pregunta, y la persona elige. Conviene hacerlo antes que la lectura de fotos (`perception-adapters`) y el cotejo con el extracto (`statement-reconciliation`), que usarán esta misma pregunta.

## What Changes
- **Número de transacción**: los gastos, ingresos y transferencias leídos de un comprobante llevan su referencia, tal como aparece, tomada de la etiqueta de la plantilla o de las etiquetas del paquete de idioma (datos, no código). En efectivo, frases y dictados no hay referencia, salvo que se escriba a mano en el formulario.
- **"¿Es el mismo gasto?"**: cada captura se guarda siempre como su propio borrador. Si dos movimientos del mismo tipo tienen el mismo monto y moneda y son del mismo día, la app pregunta si son el mismo. Si comparten el mismo archivo, el mismo texto o la misma referencia, el aviso es más directo ("ya enviaste este comprobante").
- **Son distintos**: quedan los dos y la app no vuelve a preguntar por ese par.
- **Son el mismo**: queda uno solo con lo que sabían los dos. Del comprobante se toman monto, billetera, hora y referencia; de lo que dijo la persona, la nota. La otra captura queda descartada con su evidencia, enlazada a la que quedó. Si una ya estaba confirmada, se le agregan la referencia y la evidencia sin registrar otro gasto.
- **Formulario**: campo "Referencia" opcional; pegar un comprobante en la frase también lo llena.
- **Tarjeta y resultado de la captura**: muestran la referencia y la pregunta con sus dos botones.

### Non-Goals
- Cotejar el extracto del banco (`statement-reconciliation`, después de `perception-adapters`); usará esta misma coincidencia línea por línea.
- Leer la referencia desde fotos o PDF (`perception-adapters`, con estas mismas etiquetas).
- Unir automáticamente: siempre lo decide la persona.
- Mover dinero de un movimiento ya confirmado: si al unir la billetera no coincide, se avisa para corregirlo en la revisión.
- Validar la referencia con el banco o una blockchain.

## Capabilities

### New Capabilities
- `capture-matching`: detecta capturas que pueden ser el mismo movimiento, pregunta a la persona y aplica su decisión (mantenerlas separadas o unirlas sin perder evidencia ni contar dos veces).

### Modified Capabilities
- `text-interpreter`: lee el número de transacción con la etiqueta de la plantilla o del paquete de idioma, sin inventarlo en frases.
- `draft-inbox`: el borrador y el movimiento confirmado conservan la referencia.
- `capture-ui`: el formulario suma el campo "Referencia" y el resultado de la captura muestra la referencia y la pregunta "¿Es el mismo gasto?".

## Impact
- **`core/domain`**: referencia en propuestas y movimientos listos; etiquetas de referencia en plantillas y paquetes (en, es, pt); huella del archivo en la evidencia; eventos de borrador para "son distintos" y "son el mismo"; caso de uso de coincidencias.
- **`core/data-local`**: campos opcionales y los eventos nuevos en el JSON versionado (compatible con lo guardado).
- **`composeApp`**: huella del archivo al guardarlo, campo del formulario, pregunta en la tarjeta; textos en es, en y pt.
- **Sin dependencias nuevas** y sin red.
