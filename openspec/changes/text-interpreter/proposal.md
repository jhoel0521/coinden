# Proposal: Intérprete de Texto Agnóstico (Text Interpreter)

## Why
`draft-inbox` ya guarda capturas y espera propuestas de un intérprete, pero no existe ninguno: hoy solo hay uno falso para pruebas. La captura rápida necesita un intérprete real que funcione **en cualquier país y con cualquier banco**: frases dictadas o escritas ("pagué 3 bs para transporte") y comprobantes de pago que nunca vio (Yape, Banco Económico, Mercado Pago, Pix, Zelle…). Escribir una regla por banco no escala. Además, la bandeja trata hoy igual a un monto leído de un comprobante y a una billetera que nadie mencionó; hace falta que cada borrador diga cuánta confianza merece para revisar primero lo dudoso.

## What Changes
- **Un solo intérprete de texto, determinista, en el dominio**: todo origen se convierte a texto antes (voz a texto u OCR son adaptadores) y un único `TextDraftInterpreter` produce propuestas. Misma entrada, misma salida; sin red y sin modelos.
- **Agnóstico por diseño, en tres capas**:
  1. **Lector genérico**: reconoce en cualquier comprobante el monto con su moneda, la fecha y hora, la contraparte, la cuenta y el sentido (pago, débito, retiro, recibo).
  2. **Paquetes de idioma como datos**: palabras clave (etiquetas, verbos, meses, conectores). El inglés viene incluido; los demás se instalan como datos. Esta versión trae en el catálogo español y portugués.
  3. **Plantillas conocidas como datos**: si un comprobante coincide con una plantilla (Yape, Banco Económico…), sus campos se toman como exactos.
- **Montos y fechas según el locale del usuario y la zona horaria del dispositivo**: "1.234,56" frente a "1,234.56" y "10/11" frente a "11/10" se resuelven con el locale; si sigue habiendo ambigüedad, se elige la lectura del locale y baja la confianza. Nunca se reescriben montos, fechas ni nombres.
- **Billetera sugerida por defecto**: una frase o un audio que no nombra billetera sugiere la billetera por defecto del usuario (normalmente efectivo); un comprobante usa su cuenta de origen o su emisor.
- **Origen de cada campo y confianza del borrador** (modifica `draft-inbox`): cada campo propuesto indica si salió de la evidencia, lo dijo el usuario, lo corrigió, se infirió o se sugirió por defecto. El borrador tiene confianza ALTA, MEDIA o BAJA según su campo más débil, y la bandeja se ordena de menor a mayor confianza.
- **Manifiesto y AGENTS.md**: la red también puede usarse para descargar paquetes de idioma o plantillas, sin enviar nunca datos del usuario.

### Non-Goals
- Voz a texto y OCR reales (adaptadores de plataforma).
- El mecanismo de descarga de paquetes y plantillas: el dominio define cómo se usan los instalados, no cómo se obtienen.
- Paquetes de idioma distintos de inglés, español y portugués.
- Detección de comprobantes duplicados por número de transacción.
- Traducción automática con IA como respaldo para idiomas sin paquete.
- Categorizar gastos (`expense-categorization`).

## Capabilities

### New Capabilities
- `text-interpreter`: interpretación determinista de texto (frases y comprobantes) en propuestas de movimiento, con lector genérico, paquetes de idioma y plantillas como datos, lectura de montos y fechas por locale, billetera sugerida y origen por campo.

### Modified Capabilities
- `draft-inbox`: las propuestas llevan el origen de cada campo; el borrador expone su confianza (ALTA, MEDIA, BAJA); la bandeja se ordena por confianza; se agrega la billetera por defecto configurable.

## Impact
- **Código nuevo** en `core/domain`: lector de montos y fechas, lector de frases, lector genérico de comprobantes, paquetes de idioma (`en` incluido; `es` y `pt` en catálogo), plantillas (Yape y Banco Económico como primeras), `TextDraftInterpreter` que implementa `DraftInterpreterPort`, y puertos para paquetes y plantillas instalados y para la configuración regional (locale y zona horaria).
- **draft-inbox**: `MovementProposal` agrega el origen por campo; `Draft` expone la confianza; `WalletDirectory` agrega la billetera por defecto; nuevo caso de uso para configurarla; la revisión del usuario marca los campos como corregidos.
- **Pruebas**: comprobantes **anonimizados** con la estructura real de los de Yape y Banco Económico, frases reales con errores de tipeo, y frases y comprobantes inventados en inglés, español y portugués para probar los tres paquetes y el lector genérico.
