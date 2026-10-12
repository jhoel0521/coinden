# Proposal: Bandeja de Borradores (Draft Inbox)

## Why
El corazón de CoinDen es registrar gastos hormiga en segundos y ordenarlos con calma en la noche. Hoy el dominio solo permite registrar movimientos ya confirmados y completos (`wallet-management`), lo que obliga a decidir billetera, moneda y monto exactos en el momento de pagar. Hace falta un paso intermedio: una captura que nunca se pierde, que un intérprete (texto, voz, foto o IA local) convierta en una propuesta, y que el usuario revise y confirme después bajo las reglas estrictas del libro.

## What Changes
- **Captura que nunca falla**: toda captura guarda su evidencia original (texto, referencia a audio o foto, origen, hora y dispositivo) como borrador, aunque nadie la haya entendido.
- **Intérpretes como puerto**: un puerto de dominio recibe la evidencia y el catálogo de billeteras del usuario, y devuelve propuestas tipadas. El dominio no interpreta texto libre; IA local, reglas de texto u OCR son adaptadores (fuera de este cambio). Una captura puede producir varias propuestas ("almuerzo 45 y micro 3"), y cada una es un borrador.
- **Propuestas tipadas por movimiento**: gasto, ingreso, transferencia (misma moneda o cambio de moneda) y ajuste a un saldo objetivo. Cada campo puede faltar. También existen "no es un movimiento" (recurrente, préstamo, meta, categoría, consulta…) y "no se entendió".
- **Billetera por id o por alias**: la propuesta indica la billetera por su id o por una pista ("qr", "meru") que el dominio resuelve con alias definidos por el usuario.
- **Borrador como máquina de estados**: Capturado → Incompleto ⇄ Listo → Confirmado, más Descartado y No soportado. Cada estado tiene solo sus datos y su transición; un borrador Listo no tiene campos faltantes.
- **Confirmación con las reglas del libro**: confirmar un borrador Listo registra el movimiento mediante `wallet-management`, con su control estricto de saldo. Si se rechaza, el borrador sigue Listo con el motivo.
- **Bandeja de revisión**: lista los borradores pendientes y avisa, sin bloquear, si al confirmarlos alguna billetera quedaría con saldo negativo.
- **Sync-ready**: cada transición del borrador es un evento append-only. El movimiento confirmado usa un id derivado del borrador, así que confirmar el mismo borrador en dos dispositivos no duplica el gasto.

### Non-Goals
- Intérpretes reales (reglas de texto, voz a texto, OCR, IA local) y almacenamiento de adjuntos: van en `capture-adapters`. Aquí solo hay un intérprete falso para pruebas.
- Pagos recurrentes, préstamos y gastos esperados del mes (`commitments`), categorías (`expense-categorization`) y metas (`savings-goals`): se capturan como "no es un movimiento".
- Dividir un borrador en varios movimientos (por ítem de un ticket).
- Crear billeteras o anular movimientos desde un borrador.
- Sincronización real entre dispositivos (`device-sync`).

## Capabilities

### New Capabilities
- `draft-inbox`: captura de evidencia, propuestas tipadas de movimiento, resolución de billeteras por alias, ciclo de vida del borrador como máquina de estados con historial de transiciones, bandeja de revisión con saldo proyectado y confirmación idempotente hacia el libro de movimientos.

### Modified Capabilities
<!-- Ninguna: la confirmación usa los casos de uso de wallet-management sin cambiar sus requisitos. -->

## Impact
- **Código nuevo** en `core/domain`: modelo de borrador (estados, eventos, propuestas, evidencia), proyección del historial, puertos de intérprete, repositorio de borradores y alias, y casos de uso de captura, revisión, confirmación, descarte y bandeja.
- **wallet-management**: los casos de uso de registro aceptan opcionalmente un id de evento dado, para que el id del movimiento confirmado se derive del borrador. Sus requisitos no cambian.
- **Pruebas**: unitarias de la máquina de estados y de la proyección, y de feature con el día de Carlos (cajero olvidado, cambio al paralelo, ticket sin billetera, "créame un pago recurrente"), John (satoshis) y Facundo (tarjeta).
