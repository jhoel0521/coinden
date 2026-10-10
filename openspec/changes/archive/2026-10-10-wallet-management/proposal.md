# Proposal: Billeteras y Libro de Movimientos (Wallet Management)

## Why
Hoy el dominio solo sabe consolidar una lista suelta de montos (`holdings`): no existe el concepto de "bolsillo" del que sale o entra cada centavo (efectivo, banco con QR, Meru, dólares en el colchón). Sin billeteras ni un libro de movimientos no se puede responder "¿en qué se me fue el dinero?", ni construir más adelante los borradores, la categorización, los compromisos, el dinero libre o la sincronización P2P. Esta es la base de todo el roadmap.

## What Changes
- Nueva entidad **Billetera**: nombre libre del usuario, una sola moneda, tipo (efectivo, banco, digital, cripto, crédito), política de saldo negativo y estado activa/archivada. Las billeteras **nunca se borran**: solo se archivan.
- Nuevo **libro de movimientos append-only** por billetera. El saldo **no se guarda**: se calcula como la suma de los movimientos vigentes. Tipos de movimiento: saldo inicial, ajuste, gasto, ingreso, transferencia y anulación.
- **Transferencia con dos montos**: mueve dinero entre dos billeteras guardando el monto que sale y el que entra, cada uno en la unidad mínima de su moneda. En un cambio de moneda ("vendí 50 USD a 11.88") la tasa **se deriva** de ambos montos y no se guarda como decimal.
- **Correcciones como eventos**: corregir un movimiento significa anularlo con un nuevo evento de anulación y registrar el correcto. El historial nunca se edita ni se pisa.
- **Control estricto de saldo**: en una billetera que no admite negativos, se rechaza cualquier movimiento (incluida una anulación) que deje el saldo negativo **en algún momento** de su historia cronológica. El rechazo indica la billetera, cuánto falta y desde cuándo. Las billeteras de crédito admiten negativos por defecto.
- **Billetera descuadrada**: si el libro contiene un negativo prohibido que no pasó por la validación (por ejemplo, al fusionar movimientos de otro dispositivo), la billetera se informa como descuadrada en lugar de rechazar hechos ya registrados.
- **Sync-ready**: identificadores globales generados en el dispositivo, cada movimiento registra en qué dispositivo se creó, cuándo ocurrió y cuándo se registró.
- **Saldos para el consolidado**: los saldos de las billeteras activas alimentan la vista consolidada de `currency-configuration`, en lugar de una lista armada a mano.

### Non-Goals
- Borradores y revisión diaria (`draft-inbox`): aquí todo movimiento nace confirmado.
- Categorías de gasto (`expense-categorization`).
- Pagos recurrentes y préstamos entre personas (`commitments`).
- Sincronización P2P real entre dispositivos (`device-sync`): solo se deja el modelo preparado y la regla de "descuadrada".
- Persistencia física (SQLite/SQLCipher) y UI: solo puertos y adaptadores en memoria para pruebas.
- Limpiar los textos en español que quedaron en el dominio de currency-hub (va en un cambio aparte, `domain-i18n-cleanup`).

## Capabilities

### New Capabilities
- `wallet-management`: billeteras de una moneda con ciclo de vida activa/archivada, un libro de movimientos append-only con saldo derivado, transferencias entre monedas con dos montos, correcciones por anulación y control estricto de saldo cronológico.

### Modified Capabilities
<!-- Ninguna: la vista consolidada de currency-configuration ya recibe saldos; este cambio solo le aporta la fuente. -->

## Impact
- **Código nuevo** en `core/domain`: modelo de billetera y movimientos, casos de uso, puertos de salida (repositorio de billeteras y movimientos, generador de identificadores, reloj e identidad del dispositivo) y adaptadores en memoria para las pruebas.
- **Sin dependencias nuevas**: los identificadores y el reloj entran por puertos, así las pruebas son deterministas.
- **Pruebas**: unitarias del ledger y del validador cronológico, y de feature con Carlos (Bolivia: efectivo, BNB, dólares en el colchón y cambio al paralelo), John (USA: BTC en satoshis) y Facundo (Argentina: tarjeta de crédito con saldo negativo).
- **Sin cambios incompatibles** en `currency-configuration`.
