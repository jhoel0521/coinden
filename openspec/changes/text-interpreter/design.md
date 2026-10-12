# Design: Intérprete de Texto Agnóstico

## Context
Ver proposal.md (Why) y los specs de `text-interpreter` y `draft-inbox`. `draft-inbox` define `DraftInterpreterPort.interpret(evidence, wallets): List<MovementProposal>`, `Evidence(source, text, attachmentRef)`, `ProposedAmount(value canónico, currency?)`, `WalletRef.ById/ByHint`, `WalletDirectory(wallets, aliases)` y `DraftEvaluator`. `Money.parseCanonical` es estricto (punto decimal), así que el intérprete debe entregar montos ya normalizados. En `commonMain` no hay base de datos de zonas horarias ni `kotlinx-datetime`.

## Goals / Non-Goals

**Goals:**
- Un intérprete puro y determinista, probado con TDD sobre comprobantes anonimizados y frases reales.
- Agregar un idioma o un banco solo con datos, sin código.
- No reescribir nunca montos, fechas ni nombres: se leen y se normalizan, no se traducen.

**Non-Goals:**
- Precisión de reglas de horario de verano: la zona horaria llega como desplazamiento fijo del dispositivo en el momento de interpretar.
- Interpretar ingresos complejos (sueldos con descuentos) o comprobantes con varios importes.

## Decisions

### D1. Paquete de idioma como datos
```
LanguagePack(
  code: String,                               // "en", "es"
  amountLabels, counterpartyLabels, accountLabels, noteLabels: Set<String>,
  expenseWords, incomeWords, withdrawalWords, exchangeWords: Set<String>,
  notePrepositions, conjunctions: Set<String>,   // "para"/"for", "y"/"and"
  months: Map<String, Int>,                    // "oct" -> 10, "octubre" -> 10
  meridiem: Map<String, Boolean>,              // "p. m." -> true (pm)
)
```
`LanguagePacks.ENGLISH` está en `commonMain` y siempre activo. `LanguagePacks.SPANISH` y `LanguagePacks.PORTUGUESE` están en el catálogo y cada uno pero solo se usa si `InstalledPacksPort.languagePacks()` lo incluye. Así el dominio no depende de cómo se descarga (D9). Todas las palabras se comparan normalizadas: minúsculas, sin tildes y sin signos. Por eso "pagué", "page" y "PAGUE" coinciden con "pague".
- *Alternativa descartada:* traducir el texto a inglés con un modelo. Podría alterar montos, fechas y nombres, y no es determinista.

### D2. Tolerancia a errores de tipeo
Los verbos se reconocen por coincidencia exacta normalizada o con **distancia de edición ≤ 1** para palabras de 4 letras o más ("page" ↔ "pague": inserción). La nota se toma literal del texto original, sin corregirla ("trasmporte" queda así). Es determinista y se explica por sí misma.

### D3. Lectura de números según el locale
`AmountReader.read(token, locale): ReadAmount(canonical: String, origin)`. Recibe el locale (`UserLocale(language, country)`) y su separador decimal (tabla por país: BO, US y MX usan punto; AR, BR, CL, ES y CO usan coma):
- Un solo separador seguido de 1 o 2 dígitos → decimal ("45,50", "25.00").
- Patrón de grupos de 3 (`1.234,56`, `1,234.56`, `12.500`) → miles según el locale.
- Si ambas lecturas son posibles ("1.234" en es-BO), toma la del locale y marca INFERRED.

El resultado es un decimal canónico para `ProposedAmount`; los dígitos nunca cambian, solo se normalizan los separadores.

### D4. Monedas por código o símbolo
Tabla global de símbolos como datos: `"Bs" → BOB`, `"US$" / "USD" → USD`, `"R$" → BRL`, `"€" → EUR`, `"S/" → PEN`, más los códigos ISO y las palabras de moneda de cada paquete ("dolares", "bolivianos"). `"$"` es ambiguo: se resuelve con la moneda del país del locale (US → USD, AR → ARS, CL → CLP, MX → MXN) y queda INFERRED. Si no hay moneda en el texto, `ProposedAmount.currency` queda null y `DraftEvaluator` usa la de la billetera (ya existente).

### D5. Fechas y horas a instante
`DateTimeReader` reconoce `dd/MM/yyyy`, `dd-MM-yyyy`, `dd/MMM/yyyy`, `dd MMM. yyyy` y `MMM dd, yyyy`, con meses del paquete, y horas `HH:mm[:ss]` o `h:mm a. m./p. m./AM/PM`. El orden día/mes ambiguo (`10/11/2026`) sigue el locale (en-US → mes primero) y queda INFERRED. La conversión a epoch usa un algoritmo civil (días desde 1970 de Howard Hinnant) más `RegionalSettings.utcOffsetMinutes`, sin dependencias. Sin hora explícita, el campo de fecha no se propone y el borrador usa el momento de captura.

### D6. Origen por campo en draft-inbox
```
enum FieldOrigin { EVIDENCE, STATED, CORRECTED, INFERRED, SUGGESTED }
enum Confidence { LOW, MEDIUM, HIGH }                // orden para la bandeja
MovementProposal.*(..., origins: Map<DraftField, FieldOrigin> = emptyMap())
DraftField += CURRENCY, OCCURRED_AT
```
`Draft.Ready` y `Draft.Incomplete` exponen `confidence`: el mínimo entre los orígenes de los campos **presentes**, donde un campo sin entrada cuenta como STATED (requisito "campo sin origen indicado"). `ReviseDraftUseCase` reemplaza los orígenes de la nueva propuesta por CORRECTED. `GetDraftInboxUseCase` ordena por `(confidence, capturedAt)`; Capturado y No soportado cuentan como LOW.

### D7. Billetera por defecto
`WalletAliasRepositoryPort` agrega `setDefaultWallet(ownerId, walletId?)` y `defaultWalletFor(ownerId)`. `WalletDirectory` agrega `defaultWalletId: String?`, que se calcula así: la elección explícita si la billetera sigue activa; si no, la única billetera activa de tipo CASH; si no, null. Nuevo `SetDefaultWalletUseCase`. El intérprete usa `WalletRef.ById(defaultWalletId)` con origen SUGGESTED solo en frases; en comprobantes, solo como destino de un retiro.

### D8. Tres lectores y el orden de decisión
```
TextDraftInterpreter(packs, templates, settings): DraftInterpreterPort
  1. texto vacio -> []
  2. activePacks = ENGLISH + instalados
  3. si alguna plantilla coincide (todas sus marcas presentes) -> TemplateReader   (EVIDENCE)
  4. si el texto parece comprobante (>= 2 etiquetas de los paquetes, o source == PHOTO)
        -> GenericReceiptReader                                                    (EVIDENCE/INFERRED)
  5. si no -> PhraseReader (divide por conectores seguidos de un numero)           (STATED/INFERRED/SUGGESTED)
```
**`ReceiptTemplate` como datos:** `id`, `markers: List<String>`, `kind: TemplateKind { EXPENSE, WITHDRAWAL, INCOME }`, `amountLabel`, `counterpartyLabel?`, `noteLabel?`, `accountLabel?`, `issuerWalletHint?` ("yape"), `dateTimeLabel?`. Los valores se toman de la línea de la etiqueta o de la siguiente. La cuenta de origen se resuelve buscando un alias igual al número de cuenta leído (o contenido en él), así que el usuario pone el número de cuenta como alias de su billetera.

**`GenericReceiptReader`:**
- monto: el primer monto con moneda en la línea de una etiqueta de importe o en la siguiente; si no, el monto con moneda más grande del texto, como INFERRED;
- contraparte: la línea de una etiqueta de destinatario;
- sentido: palabras de retiro → transferencia hacia la billetera por defecto; de ingreso → ingreso; si no → gasto.

**`PhraseReader`:**
- tokens: verbo → tipo; número (+ moneda) → monto;
- alias: la palabra coincide con un alias → billetera;
- nota: lo que sigue a la preposición de nota; si no hay, las palabras que no se usaron, en su orden original;
- cambio: verbo de cambio + "a <tasa>" → `Transfer(out = monto en la moneda dicha, into = monto × tasa con ExchangeRate.parse, to = defecto)`.

### D9. Puertos de paquetes, plantillas y configuración regional
```
interface InstalledPacksPort { fun languagePacks(): List<LanguagePack>; fun receiptTemplates(): List<ReceiptTemplate> }
interface RegionalSettingsPort { fun settings(): RegionalSettings }  // UserLocale + utcOffsetMinutes
```
El catálogo inicial (`LanguagePacks.SPANISH`, `LanguagePacks.PORTUGUESE`, `ReceiptTemplates.YAPE_BO`, `BE_QR_PAYMENT`, `BE_ECOMMERCE_DEBIT` y `BE_CASH_WITHDRAWAL`) vive como datos en `commonMain`, para que un adaptador de descarga o de instalación lo ofrezca. Las pruebas usan un `FakeInstalledPacks`.

### D10. Datos de prueba anonimizados
Las pruebas reproducen la **estructura** de los comprobantes reales (orden de líneas, etiquetas, formatos de monto y fecha) con **nombres, cuentas, NIT y números de transacción inventados**. Nunca se versionan datos reales.

### Organización del código
```
model/interpret/    LanguagePack, LanguagePacks, ReceiptTemplate, ReceiptTemplates, UserLocale,
                    RegionalSettings, TextNormalizer, AmountReader, CurrencyReader, DateTimeReader,
                    PhraseReader, GenericReceiptReader, TemplateReader, TextDraftInterpreter
model/draft/        + FieldOrigin, Confidence, origins en MovementProposal, confidence en Draft
port/output/        + InstalledPacksPort, RegionalSettingsPort; WalletAliasRepositoryPort + default
usecase/draft/      + SetDefaultWalletUseCase; Revise marca CORRECTED; Inbox ordena por confianza
test unit/          TextNormalizerTest, AmountReaderTest, DateTimeReaderTest, PhraseReaderTest,
                    GenericReceiptReaderTest, TemplateReaderTest, DraftConfidenceTest
test feature/       CarlosReceiptsFeatureTest (comprobantes BO anonimizados + frases),
                    WorldReceiptsFeatureTest (AR, BR, US genericos inventados), fakes de paquetes
```

## Risks / Trade-offs
- [El lector genérico se equivoca con comprobantes muy distintos] → Sus campos deducidos quedan INFERRED (confianza MEDIA o BAJA) y se revisan primero en la bandeja; agregar una plantilla los vuelve ALTA.
- [Un alias corto coincide por accidente dentro de un texto largo] → En comprobantes solo se buscan alias junto a la etiqueta de cuenta; en frases, solo palabras completas.
- [Desplazamiento horario fijo] → Un comprobante de antes de un cambio de horario puede quedar corrido una hora. En Bolivia no hay horario de verano; se documenta.
- [Distancia de edición 1 genera falsos positivos] → Solo se aplica a verbos de 4 letras o más y se marca INFERRED.

## Migration Plan
No aplica: no hay datos persistidos. Los nuevos campos de `MovementProposal` y `WalletDirectory` tienen valores por defecto, así que las pruebas de `draft-inbox` siguen compilando.
