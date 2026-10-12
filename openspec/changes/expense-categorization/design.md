# Design: Categorización de Gastos

## Context
Ver proposal.md (Why) y los specs. Hoy `LedgerEvent.Expense` guarda billetera, monto y fechas, sin descripción. La nota de un gasto capturado vive en `ReadyMovement.note` y queda en `DraftEvent.Confirmed`, y el id del movimiento confirmado es `draft:<id>`. `text-interpreter` ya tiene paquetes de idioma (`LanguagePack`) como datos, `TextNormalizer` con tolerancia a una edición y el puerto `InstalledPacksPort`. El consolidado de `currency-configuration` arma las tasas desde la estrategia del perfil (`GetConsolidatedPortfolioUseCase`).

## Goals / Non-Goals

**Goals:**
- Un categorizador puro y determinista, que se pueda explicar ("por la regla X", "por la palabra Y").
- No tocar el libro de dinero: clasificar y reclasificar son eventos de otro registro.
- Ninguna dependencia de idioma en el modelo: categorías base por enum y nombres propios como datos del usuario.

**Non-Goals:**
- Migrar la descripción al libro de dinero.
- Sincronizar reglas entre dispositivos (son metadatos y el último gana, igual que los alias).

## Decisions

### D1. Categorías: enum base y propias
```
enum BaseCategory { FOOD, GROCERIES, TRANSPORT, HEALTH, SUBSCRIPTIONS, BILLS, HOUSING,
                    LEISURE, SHOPPING, EDUCATION, PEOPLE, FEES }
sealed CategoryRef = Base(BaseCategory) | Custom(id: String)
CustomCategory(id, ownerId, name, archived)
```
"Sin categoría" es la ausencia de `CategoryRef` (null), no una categoría más. Así el reporte no confunde "nadie eligió" con una elección.

### D2. Registro de clasificación append-only
```
ClassificationEvent(id, movementId, ownerId, deviceId, occurredAtEpochMs, recordedAtEpochMs,
                    description: String?, category: CategoryRef?, origin: CategoryOrigin)
enum CategoryOrigin { RULE, PACK, USER }
```
La clasificación vigente de un movimiento es su evento más reciente en orden (ocurrencia, registro, id), igual que en el libro y en los borradores. Si un evento trae la descripción en null, conserva la anterior. Solo se aceptan movimientos `LedgerEvent.Expense`; para cualquier otro se responde `CategorizationError.NotAnExpense`.
- *Alternativa descartada:* un campo `category` en `LedgerEvent.Expense`. Recategorizar obligaría a anular y registrar dinero de nuevo.

### D3. Reglas aprendidas como estado
`CategoryRule(ownerId, key, category)`, donde `key` es la descripción normalizada con `TextNormalizer`. Se guarda en `CategorizationRepositoryPort.saveRule` (reemplaza la de la misma clave). Se crea o actualiza cada vez que el usuario **elige** una categoría (origen USER) para un gasto con descripción, incluso si acepta una sugerencia del paquete: la eligió él.

### D4. Categorizador puro y su orden
```
Categorizer.suggest(description, rules, packs, activeCustom): CategorySuggestion?
CategorySuggestion(category, confidence, reason: SuggestionReason)
SuggestionReason = UserRule(key) | PackWord(word) 
```
1. **Reglas:** la regla cuya `key` coincide con la descripción normalizada o está contenida en ella como palabras completas; si varias coinciden, gana la `key` más larga. → HIGH.
2. **Paquete:** la primera palabra de la descripción, en orden, que coincide con `LanguagePack.categoryWords` de los paquetes activos (exacta o con una edición en palabras de 4 letras o más). → MEDIUM.
3. Si no hay coincidencia → null.

Las reglas que apuntan a una categoría propia archivada se ignoran. `Confidence` es el enum de `draft-inbox` (LOW, MEDIUM, HIGH).

### D5. Palabras típicas en los paquetes de idioma
`LanguagePack` agrega `categoryWords: Map<String, BaseCategory>` (palabras normalizadas). Hay datos para `en`, `es` y `pt`, por ejemplo "almuerzo", "pollo", "salteña", "semillita" → FOOD; "micro", "taxi", "uber", "transporte" → TRANSPORT; "farmacia" → HEALTH; "netflix", "youtube", "spotify", "streaming" → SUBSCRIPTIONS; "lunch", "coffee" → FOOD; "almoço", "lanche" → FOOD. `merged()` las une.

### D6. Integración con draft-inbox
- `ConfirmDraftUseCase(draftId, category: CategoryRef? = null)`. Si el libro acepta y el movimiento es un gasto, registra un `ClassificationEvent` con `movementId = draft:<id>`, `description = movement.note`, origen USER si el usuario pasó categoría o null si no, y aprende la regla (D3). La confirmación idempotente (otro dispositivo ya confirmó) no duplica la clasificación si ya existe una del mismo movimiento.
- `GetDraftInboxUseCase` agrega `suggestions: Map<draftId, CategorySuggestion>` para los borradores de gasto con nota. No cambia `Draft.confidence`.

Para no acoplar `draft-inbox` al paquete de categorización con dependencias circulares, `DraftContext` recibe un `CategorizationContext` opcional, que es null en las pruebas existentes de draft-inbox para que sigan iguales.

### D7. Reporte de gasto por categoría
`SpendingByCategoryUseCase(ownerId, fromEpochMs, toEpochMs): SpendingReport`:
- toma de todas las billeteras del dueño los `Expense` vigentes (sin anulados, con `LedgerProjection`) cuyo `occurredAt` está en `[from, to)`;
- agrupa por la categoría vigente (null = Sin categoría);
- suma por moneda con `Money + Money`;
- consolida en la moneda local con un `CurrencyConverter` armado desde la estrategia del perfil.

La construcción de tasas se extrae de `GetConsolidatedPortfolioUseCase` a una función compartida (`ProfileRates.converterFor(profile, cryptoRates)`), así el consolidado y el reporte usan exactamente las mismas tasas. Resultado: `SpendingReport(rows: List<CategoryTotal(category, byCurrency, consolidated)>, total)`, ordenado por `consolidated` descendente.

### D8. Puertos
```
CategorizationRepositoryPort {
  appendClassification(e), classificationsFor(movementId), classificationsOf(ownerId)
  saveRule(rule), rulesOf(ownerId)
  saveCustomCategory(c), customCategoriesOf(ownerId)
}
```
Las clasificaciones son append-only; reglas y categorías propias son estado (metadatos).

### Organización del código
```
model/category/     BaseCategory, CategoryRef, CustomCategory, CategoryOrigin, ClassificationEvent,
                    ClassificationProjection, CategoryRule, Categorizer, CategorySuggestion,
                    SpendingReport, CategorizationError
model/interpret/    LanguagePack + categoryWords (en/es/pt)
model/              ProfileRates (extraído de GetConsolidatedPortfolioUseCase)
port/output/        CategorizationRepositoryPort
usecase/category/   CreateCustomCategory, ArchiveCustomCategory, ClassifyExpense (reclasificar),
                    SpendingByCategory, CategorizationContext
usecase/draft/      Confirm con categoría; Inbox con sugerencias
```

## Risks / Trade-offs
- [Reglas demasiado generales] → Una regla "pago" aplicada a todo. Mitigación: la clave es la descripción completa normalizada y gana la más larga; el usuario puede reclasificar y la regla se actualiza.
- [Descripción ausente en gastos directos] → No hay aprendizaje para ellos hasta que el usuario escriba una descripción al clasificar.
- [Palabras típicas regionales] → "salteña" o "micro" son bolivianas. Son datos del paquete y la comunidad puede ampliarlas; las reglas del usuario corrigen cualquier caso.
- [Extraer ProfileRates toca currency-configuration] → Refactor sin cambio de comportamiento; lo protegen las pruebas de las 5 personas.

## Migration Plan
No aplica: no hay datos persistidos. Los parámetros nuevos tienen valores por defecto, así que las pruebas existentes siguen compilando.
