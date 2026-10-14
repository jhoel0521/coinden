# Proposal: Medios de Pago como Pistas, no como Billeteras (Payment Method Hints)

## Why
Hasta hoy tratamos "qr" como alias de una billetera ("qr" = BNB Bs), y es un error de concepto: QR es un **medio de pago**. En Bolivia todos los bancos y billeteras digitales cobran y pagan por QR; en Brasil pasa lo mismo con Pix. Si Carlos tiene BNB, Banco Económico y Yape y dice "almuerzo 45 qr", nadie puede saber de qué cuenta salió el dinero: solo el comprobante lo dice. Proponer BNB como si Carlos lo hubiera dicho es inventar un dato con confianza que no tiene. El error está en specs (`draft-inbox`, `text-interpreter`, `app-shell`), en la regla 4 de `AGENTS.md`, en el README, en la demo y en las pruebas.

## What Changes
- **Tres medios de pago, reconocidos como datos del paquete de idioma**: efectivo ("efectivo", "cash", "dinheiro"), transferencia ("qr", "pix", "transferencia", "transfer", "ted": QR y Pix son transferencias rápidas por banca por internet; la transferencia en ventanilla también lo es) y tarjeta ("tarjeta", "card", "cartão"). Un medio no identifica una billetera: solo dice qué billeteras pudieron pagar.
- **Billeteras candidatas por medio y por moneda**: el efectivo sale de una billetera de efectivo, las transferencias de una cuenta de banco o digital, la tarjeta de un banco o una tarjeta de crédito. La moneda separa, por ejemplo, el efectivo en Bs del efectivo en dólares o en reales: son billeteras distintas.
- **La moneda dicha también filtra sin medio**: "pagué 20 dólares" no puede salir de la billetera en Bs; se recomienda una billetera en dólares (primero el efectivo).
- **Regla de sugerencia**: una sola candidata → se sugiere; varias → se recomienda una (la por defecto si es candidata; si no, la de tu moneda local; si no, la primera en el orden de tus billeteras) con confianza baja; ninguna → la billetera queda faltante. Nunca se confirma sola: confirmar sigue siendo decisión del usuario.
- **El borrador recuerda el medio dicho**, para que la revisión ofrezca solo las billeteras que lo aceptan.
- **Alias solo para nombres que identifican una cuenta** ("bnb", "yape", "colchón"). Un alias dicho gana sobre el medio.
- **Corrección de la base**: los specs, la regla 4 de `AGENTS.md`, el README, la demo (el banco de ejemplo pasa a tener el alias "bnb") y las pruebas dejan de tratar "qr" como billetera. Carlos pasa a tener varias cuentas de banco en sus pruebas.

### Non-Goals
- Aprender qué cuenta usa cada uno con cada medio ("cuando dices qr casi siempre es BNB"): queda para un cambio posterior, como las reglas de categorías.
- Editar qué medios acepta cada billetera (llega con `onboarding-wallets-ui`); por ahora se deduce del tipo.
- Comprobantes: ya identifican la cuenta por plantilla o etiqueta y no cambian. Los comprobantes de transferencia interbancaria hecha en ventanilla se agregan como plantilla cuando haya un ejemplo (anonimizado) del formato real.
- Pantallas de revisión con selector de candidatas (`review-inbox-ui`); aquí solo se deja el dato listo.

## Capabilities

### New Capabilities
<!-- Ninguna: corrige capacidades existentes. -->

### Modified Capabilities
- `text-interpreter`: reconoce medios de pago en las frases y sugiere billeteras candidatas por medio y moneda; la billetera por defecto solo se usa si la frase no nombra billetera ni medio; el ejemplo de alias deja de usar "qr".
- `draft-inbox`: el ejemplo de alias pasa a "bnb" y la confirmación de "almuerzo 45 qr" deja de asumir BNB.
- `app-shell`: la demo muestra "almuerzo 45 qr" con la billetera de banco sugerida y confianza baja, y el banco de ejemplo tiene el alias "bnb".

## Impact
- **`core/domain`**: nuevo enum de medio de pago (efectivo, transferencia, tarjeta), medios aceptados por tipo de billetera, campo de medios en `LanguagePack` (en/es/pt), lectura en `PhraseReader`, el medio dicho en la propuesta de borrador.
- **`core/data-local`**: el formato JSON de la propuesta agrega el medio como campo opcional (compatible con la versión 1: los lectores anteriores lo ignoran).
- **`composeApp`**: `DemoSeed` y pruebas de la demo.
- **Docs**: regla 4 de `AGENTS.md` y la explicación de categoría y medio de pago del README.
- **Pruebas**: se reescriben los casos con "qr" como alias; las demás siguen igual.
