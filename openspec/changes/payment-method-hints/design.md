# Design: Medios de Pago como Pistas, no como Billeteras

## Context
Ver proposal.md (Why) y los deltas de `text-interpreter`, `draft-inbox` y `app-shell`. Hoy `PhraseReader` resuelve la billetera solo por alias (`WalletDirectory.byHint`) y, si no hay alias, sugiere la billetera por defecto; "qr" funciona como billetera porque la demo y las pruebas lo definen como alias de "BNB Bs". `MovementProposal` no tiene dónde guardar un medio de pago. El formato persistido de la propuesta es `ProposalDto` versión 1 en `core/data-local`, leído con `ignoreUnknownKeys`.

## Goals / Non-Goals

**Goals:**
- Que un medio de pago nunca se confunda con una billetera, en ningún idioma.
- Sugerir de forma determinista y explicable, sin inventar certeza.

**Non-Goals:**
- Preferencias aprendidas por medio de pago y edición de medios por billetera (ver proposal).

## Decisions

### D1. `PaymentMethod` en el dominio, sin idioma
Enum nuevo en `model/wallet`, por **de dónde puede salir el dinero**, no por canal:
- `CASH`: efectivo (en la moneda de la billetera).
- `TRANSFER`: transferencia desde una cuenta: QR, Pix, banca por internet o en ventanilla.
- `CARD`: tarjeta de débito o de crédito.

Alternativas descartadas: separar `QR`, `PIX` o `INSTANT` de `TRANSFER`. Todos son transferencias desde una cuenta y tienen las mismas candidatas. Separarlos obligaría a cada país a agregar un valor al dominio en vez de una palabra al paquete. El canal exacto (QR, ventanilla) lo da el comprobante cuando existe.

### D2. Medios aceptados por tipo de billetera
`WalletType.acceptedMethods` (propiedad pura del dominio):

| Tipo | Medios |
|---|---|
| CASH | CASH |
| BANK | TRANSFER, CARD |
| DIGITAL | TRANSFER |
| CREDIT | CARD |
| CRYPTO | TRANSFER |

Además, toda billetera solo acepta su propia moneda (`Wallet.accepts`): el efectivo en Bs, en USD y en BRL son billeteras distintas.

Editarlo por billetera queda para `onboarding-wallets-ui`. Entonces `Wallet` tendrá un conjunto propio con este valor por defecto, sin cambiar la regla de sugerencia.

### D3. Palabras de medio en `LanguagePack`
Nuevo campo `paymentMethodWords: Map<String, PaymentMethod>` (con valor por defecto vacío y unión en `merged()`):
- en: `cash` → CASH, `qr` → TRANSFER, `transfer` → TRANSFER, `card` → CARD, `debit` → CARD
- es: `efectivo` → CASH, `qr` → TRANSFER, `transferencia` → TRANSFER, `tarjeta` → CARD, `debito` → CARD
- pt: `dinheiro` → CASH, `pix` → TRANSFER, `qr` → TRANSFER, `transferencia` → TRANSFER, `ted` → TRANSFER, `cartao` → CARD, `debito` → CARD

Coincidencia exacta de una palabra normalizada (sin tolerancia de tipeo, como los marcadores de cambio). Las palabras del medio se consumen y no van a la nota.

### D4. Regla de sugerencia (pura, en el intérprete)
1. Si hay alias en la frase → esa billetera, `STATED` (gana sobre el medio).
2. Si hay medio → candidatas = billeteras activas cuyo tipo acepta el medio y, si se dijo la moneda, que acepten esa moneda.
3. Sin medio pero con una moneda que la billetera por defecto no acepta → candidatas = billeteras activas de esa moneda, primero las de efectivo (si hay alguna de efectivo, solo esas).
4. Sin alias, sin medio y sin conflicto de moneda → la billetera por defecto, `SUGGESTED` (como hoy).

Elección entre las candidatas de 2 y 3 (determinista):
- ninguna → billetera `null` (el evaluador la marca faltante);
- la billetera por defecto, si es candidata;
- si no, la candidata en la moneda local del usuario (`UserLocale.currency`), si hay;
- si no, la primera en el orden del directorio (orden de creación).
Siempre `SUGGESTED`.

`SUGGESTED` ya da confianza BAJA y el borrador puede quedar Listo, pero nunca se confirma solo: confirmar es siempre una acción del usuario (`draft-inbox`). Esto aplica a gastos e ingresos; en un cambio de moneda, el medio filtra el origen junto con la moneda entregada (la regla de `exchange-phrases` se mantiene).

### D5. El medio dicho viaja en la propuesta
`MovementProposal.Expense` e `Income` ganan `paymentMethod: PaymentMethod? = null` (y `Transfer` para su origen). No es un campo del movimiento listo ni del libro: es contexto de la captura para que la revisión filtre candidatas. `ProposalDto` agrega `paymentMethod` opcional. Sigue siendo la versión 1, porque un lector anterior lo ignora (`ignoreUnknownKeys`) y uno nuevo lee `null` en datos viejos. La prueba de ida y vuelta lo cubre.

### D6. Corrección de datos de ejemplo y docs
- `DemoSeed`: "BNB Bs" con alias `bnb`.
- Pruebas: los casos que usaban "qr" como alias pasan a `bnb`, o pasan a probar el medio con varias cuentas de Carlos (BNB, BE y Yape).
- `AGENTS.md`, regla 4: "Categoría ≠ medio de pago ≠ billetera: 'QR', 'Pix' o 'efectivo' son medios (filtran billeteras candidatas); 'BNB' o 'Yape' son alias de billeteras; la categoría dice en qué se gastó."
- README: el ejemplo "Almuerzo 45 Bs con QR" explica que el medio QR deja elegir entre las cuentas que lo aceptan.

## Risks / Trade-offs
- [Comprobantes de transferencia interbancaria hecha en ventanilla] → hoy los lee el lector genérico si traen etiquetas de importe y cuenta; una plantilla específica se agrega cuando haya un ejemplo anonimizado del formato real, nunca copiando datos personales.
- [Recomendar "la primera" puede no ser la cuenta real] → queda sugerida, con confianza BAJA y el medio guardado para elegir entre las candidatas en la revisión. Una preferencia aprendida lo mejorará en otro cambio.
- [Palabras comunes como "qr" o "cash" en una nota] → solo se reconocen como palabras completas; una nota que diga "cash" se pierde como nota, aceptable frente a confundir la billetera.
- [Cambiar ejemplos en specs ya archivados] → se hace con deltas MODIFIED, no editando el historial.

## Migration Plan
No hay usuarios. Los borradores guardados con la demo anterior (alias "qr") siguen leyéndose: "qr" ahora es un medio, y el alias viejo, si existe en la base de un dispositivo de prueba, sigue resolviendo como alias. Para empezar limpio basta con borrar los datos de la app.
