# Proposal

## Why

Un comprobante compartido trae los hechos del pago (monto, billetera, hora, referencia), pero muchas veces no dice **qué fue**: "Pago QR otros bancos" no dice si fue la comida o el somó. Hoy, para explicarlo, Carlos dicta otra captura, que crea **otro borrador**. Como "esto es de mi comida" no tiene monto, nunca dispara "¿Es el mismo gasto?", y quedan dos borradores sueltos que tiene que arreglar a mano. Además, compartir acepta un solo archivo, aunque en WhatsApp o en la galería se elijan varios.

El intérprete lee cada captura por separado, como si cada una fuera un gasto. Lo que la persona pide es que **todo lo que manda junto se entienda junto**: los comprobantes dicen los hechos y lo que dice o escribe los explica.

## What Changes

- **Paquete de captura.** Lo que la persona manda en un momento (N comprobantes, fotos, PDF, audios y notas) se guarda como un paquete. El paquete se interpreta **todo junto, una sola vez** al cerrarlo, y produce **de 1 a N borradores**, uno por pago real.
- **Página de contexto rápido.** Después de compartir o capturar, una página muestra todo lo juntado y permite, en loop:
  - **agregar** un audio, una nota o una foto extra;
  - **quitar** lo que no es de ese pago (queda guardado, pero no se interpreta).

  **Salir o tocar Listo cierra el paquete** y lo manda a la cola. Compartir una foto y salirse sigue funcionando como hoy.
- **Compartir N entradas.** Compartir acepta varios archivos de una vez, y todos van al mismo paquete. Un archivo repetido (misma huella) no se agrega dos veces.
- **Reabrir un paquete.** Mientras tenga borradores pendientes, un paquete se puede reabrir, y al cerrarse se vuelve a interpretar todo junto. Se reabre desde tres lugares:
  - **"Agregar contexto"** en la tarjeta de un borrador, en Revisar o en el formulario;
  - **"Agregar al último"** en la hoja de Capturar;
  - **"¿Es del último borrador? Agregar a ese"**, al compartir algo nuevo.
- **Intérprete del paquete** (el centro del cambio):
  - **Hechos:** cada comprobante es un pago con sus hechos.
  - **Frases:** el contexto (audios transcritos y notas) se parte en frases, en modo contexto: no necesitan monto ni verbo.
  - **Emparejar:** las frases con monto se emparejan con los comprobantes por monto y medio de pago. Si hay varios comprobantes con el mismo monto, se asignan por el orden de la hora del pago. El mismo monto con otro medio es otro pago.
  - **Contradicción:** si se contradice el único comprobante, va a revisión.
  - **Frases sin monto:** explican el pago que queda; con varios pagos posibles, se sugieren en todos y van a revisión.
  - **Hechos protegidos:** ningún hecho de un comprobante cambia en silencio.
  - **Modelo de lenguaje:** si hay uno disponible, recibe el mismo paquete con el mismo contrato y las reglas lo validan.
- **Revisión del paquete.** El visor muestra cada evidencia con lo que se leyó de ella. Los borradores en "Para revisar" dicen su motivo. Desde la revisión se puede:
  - **Quitar** una evidencia;
  - **Separar** una evidencia en otro borrador;
  - **resolver una contradicción** ("Usar 25" o "Usar 1500").

## Capabilities

### New Capabilities
- `capture-bundle`: el paquete de captura. Cubre:
  - qué junta;
  - cuándo se abre, se cierra y se reabre;
  - quitar sin borrar;
  - la huella para no duplicar;
  - los borradores que produce y cómo se reinterpreta sin pisar lo confirmado ni lo corregido a mano.

### Modified Capabilities
- `text-interpreter`: interpretación del paquete:
  - frases de contexto;
  - emparejar con comprobantes;
  - contexto explicativo;
  - hechos que no cambian en silencio.
- `capture-ui`:
  - compartir N entradas a la página de contexto;
  - la página de contexto en loop;
  - los accesos para reabrir un paquete;
  - el resultado de la captura con 1 a N borradores.
- `review-inbox`:
  - el visor de todas las evidencias con su texto;
  - Quitar y Separar;
  - el motivo de revisión y resolver una contradicción.
- `background-processing`: la cola procesa los paquetes cerrados. Lee cada evidencia y, cuando termina, interpreta el paquete.
- `draft-inbox`: la lectura posterior de la evidencia alimenta la interpretación del paquete, no la de un borrador suelto.
- `local-persistence`: actualizar a una versión nueva conserva todos los datos de la persona; la base se migra en el lugar y lo guardado antes se lee igual.

## Non-Goals

- **Repaso del día:** interpretar juntos todos los paquetes del día y proponer relaciones entre ellos. Es el próximo cambio, `repaso-del-dia`. Usa este mismo intérprete y se decide con datos de uso real.
- **Detectar por qué la persona salió de la app** (una llamada, otra app) o esperar un tiempo antes de cerrar. Salir siempre cierra el paquete, y reabrirlo cubre el resto.
- **Reabrir un paquete cuyos borradores ya están todos confirmados o descartados:** para eso sigue la corrección de movimientos.
- **Borrar evidencia:** quitar la excluye de la interpretación, pero queda guardada (append-only).
- **Mover una evidencia de un paquete a otro distinto:** se quita de uno y se agrega al otro.

## Impact

- **`core/domain`:**
  - el modelo y los eventos del paquete;
  - el intérprete del paquete con frases, emparejamiento y motivos de revisión;
  - los casos de uso para abrir, agregar, quitar, cerrar, reabrir y separar;
  - las pruebas de feature de Carlos con los casos acordados.
- **Paquetes de idioma (datos):** las palabras de contexto en es, en y pt, como "le di", "en efectivo", "acá te paso el comprobante", "esto es de", "luego" y "después".
- **`core/data-local` y `core/data-memory`:** persistir los eventos nuevos. Los borradores de antes se leen como paquetes de una sola evidencia. La migración del esquema se prueba abriendo una base creada por la versión 0.1.0: actualizar desde Play nunca borra datos.
- **`composeApp`:**
  - `ShareActivity` con `ACTION_SEND_MULTIPLE`;
  - la página de contexto con su grabador de audio;
  - los accesos para reabrir;
  - el visor de evidencias;
  - los textos en es, en y pt.
- **Cola de lectura:** procesa por paquete.
