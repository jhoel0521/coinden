# Design: Bandeja de Borradores

## Context
Ver proposal.md (Why) y `specs/draft-inbox/spec.md`. El dominio ya tiene `Money` y `ExchangeRate` exactos y encapsulados, y `wallet-management` con libro append-only, `LedgerGuard` (control estricto cronológico), `LedgerContext` (repositorio, ids, reloj, dispositivo) y resultados tipados (`WalletOperationResult`, `WalletError`). Los casos de uso de registro generan el id del evento con `IdGeneratorPort`.

## Goals / Non-Goals

**Goals:**
- Transiciones del borrador como funciones puras sobre estados sellados, fáciles de probar con TDD.
- Una sola fuente de reglas de dinero: confirmar delega en `wallet-management`, sin duplicar validaciones de saldo.
- Contrato del intérprete estable, como el esquema de una herramienta MCP, para que IA local, reglas de texto u OCR lo implementen sin tocar el dominio.

**Non-Goals:**
- Resolver conflictos entre transiciones concurrentes más allá del orden cronológico (eso es `device-sync`).
- Persistir adjuntos (la evidencia solo guarda una referencia).

## Decisions

### D1. Evidencia y propuestas tipadas
```
Evidence(source: EvidenceSource {TEXT, VOICE, PHOTO, MANUAL}, text: String?, attachmentRef: String?)
ProposedAmount(value: String, currency: Currency?)       // decimal canónico; moneda opcional
WalletRef = ById(walletId) | ByHint(hint)

MovementProposal =
  Expense(amount: ProposedAmount?, wallet: WalletRef?, occurredAtEpochMs: Long?, note: String?)
  Income(...)                                            // mismos campos
  Transfer(out: ProposedAmount?, from: WalletRef?, into: ProposedAmount?, to: WalletRef?, occurredAtEpochMs?, note?)
  AdjustTo(target: ProposedAmount?, wallet: WalletRef?, occurredAtEpochMs?, note?)
  NotAMovement(intent: NonMovementIntent, note: String?)
  Unknown(note: String?)
NonMovementIntent { RECURRING_PAYMENT, LOAN, SAVINGS_GOAL, CATEGORY, WALLET_SETUP, ANNULMENT, QUERY, OTHER }
```
El tipo **es** el kind, así que no hay enum de kind ni bolsa de nulos. El monto llega como `ProposedAmount` (texto canónico + moneda opcional) y no como `Money`, porque el intérprete puede saber "45" sin saber la moneda; el dominio construye el `Money` con la moneda de la billetera usando `Money.parseCanonical`.
- *Alternativa descartada:* un `generateDraft(kind?, amount?, wallet?, toAmount?…)` plano. Las combinaciones válidas quedaban en reglas y no en tipos.

### D2. El borrador como máquina de estados sellada
```
Draft = Captured(evidence)
      | Incomplete(evidence, proposal, missing: Set<DraftField>, issues: Set<DraftIssue>)
      | Ready(evidence, movement: ReadyMovement)          // sin nulos
      | Unsupported(evidence, intent, note)
      | Confirmed(evidence, movement, ledgerEventId)
      | Discarded(evidence)
ReadyMovement = Expense(amount: Money, walletId, occurredAt, note)
              | Income(...) | Transfer(out: Money, fromId, into: Money, toId, occurredAt, note)
              | AdjustTo(target: Money, walletId, occurredAt, note)
```
Una función pura `DraftEvaluator.evaluate(proposal, WalletDirectory, capturedAt)` decide entre `Ready`, `Incomplete` o `Unsupported`. `WalletDirectory` es una instantánea de las billeteras del dueño (id, moneda, estado y alias). `Ready` solo se construye desde `evaluate`, así que sus campos no nulos están garantizados por tipos. `DraftField { KIND, AMOUNT, WALLET, TO_AMOUNT, TO_WALLET }` y `DraftIssue { INVALID_AMOUNT, CURRENCY_MISMATCH, UNKNOWN_WALLET_HINT, WALLET_ARCHIVED, SAME_WALLET, INCONSISTENT_AMOUNTS }` no llevan texto (principio 9).

### D3. Historial de transiciones como eventos append-only
```
DraftEvent(id, draftId, ownerId, deviceId, occurredAtEpochMs, recordedAtEpochMs) =
  Captured(evidence) | Interpreted(proposal) | Revised(proposal)
  | Confirmed(ledgerEventId, movement) | Discarded
```
`DraftProjection` ordena por (ocurrencia, registro, id), igual que `LedgerProjection`, y reproduce: `Captured` → estado Capturado; `Interpreted`/`Revised` → `evaluate(...)`; `Confirmed`/`Discarded` → terminal, y lo que llegue después se ignora. Los estados Listo, Incompleto e Inválido no se guardan: se recalculan con el directorio actual de billeteras (por ejemplo, si una billetera se archiva, el borrador vuelve a Incompleto con `WALLET_ARCHIVED`). `Revised` reemplaza la propuesta entera, sin parches por campo.
- *Alternativa descartada:* guardar el estado actual. Perdería correcciones entre dispositivos y no protegería contra la doble confirmación.

### D4. Captura: un borrador por propuesta y nunca falla
`CaptureDraftUseCase(repository, interpreter, directory, ids, clock, device)`, invocado con `(ownerId, evidence)`, devuelve `List<Draft>`. Registra `Captured` y luego llama `interpreter.interpret(evidence, directory)` dentro de un `try`. Si devuelve N ≥ 1 propuestas, el primer borrador recibe `Interpreted(p1)` y por cada propuesta extra se crea otro borrador con su propio `Captured` (misma evidencia) e `Interpreted(pi)`. Si falla o devuelve una lista vacía, el borrador queda Capturado.

### D5. Puerto del intérprete como herramienta MCP
```
interface DraftInterpreterPort {
    suspend fun interpret(evidence: Evidence, wallets: WalletDirectory): List<MovementProposal>
}
```
El directorio es el "esquema" que se le da al intérprete (ids, monedas y alias disponibles). Las pruebas usan `ScriptedInterpreter`, que devuelve propuestas fijas por texto de evidencia.

### D6. Alias de billetera en draft-inbox
`WalletAliasRepositoryPort` (`setAliases(walletId, Set<String>)`, `aliasesFor(ownerId): Map<String, String>`) y `SetWalletAliasesUseCase`. Los alias se normalizan en minúsculas y sin espacios de borde. Un alias que apunta a dos billeteras se rechaza con `AliasAlreadyUsed`. Viven aquí y no en `Wallet` porque solo sirven para interpretar capturas; así `wallet-management` no cambia. Son metadatos (estado actual, el último gana al sincronizar), no dinero.

### D7. Confirmación delegada e idempotente
`ConfirmDraftUseCase(draftRepository, walletRepository, registerMovement, registerTransfer, clock, device, ids)`:
1. Si el estado proyectado no es `Ready`, devuelve `DraftResult.Rejected(NotReady(missing, issues))` o `Rejected(TerminalState)`.
2. `ledgerEventId = "draft:" + draftId`. Si el libro ya tiene ese evento (otro dispositivo lo confirmó), solo registra `Confirmed` y no crea otro movimiento.
3. Delega: Expense/Income → `RegisterMovementUseCase`; Transfer → `RegisterTransferUseCase`; AdjustTo → calcula `target - saldo actual` con `LedgerProjection`; si es cero devuelve `Rejected(NothingToAdjust)`, si no registra un AJUSTE.
4. Si el libro acepta, registra `DraftEvent.Confirmed`. Si rechaza, devuelve `Rejected(Wallet(error))` y no registra nada, de modo que el borrador sigue Listo. El motivo se devuelve y no se guarda.

Para el punto 2, `RegisterMovementUseCase` y `RegisterTransferUseCase` reciben un parámetro opcional `eventId: String? = null` (si viene, se usa en lugar de `ids.newId()`). El comportamiento de `wallet-management` no cambia y sus pruebas siguen iguales.

### D8. Bandeja con saldo proyectado
`GetDraftInboxUseCase(ownerId)` devuelve `DraftInbox(pending: List<Draft>, projections: List<ProjectedBalance(walletId, current, projected, hasShortfall)>)`. El saldo proyectado es el saldo actual más el efecto de los borradores Listos (en un AdjustTo, el saldo pasa a ser el objetivo). Solo se avisa (`hasShortfall`) si la billetera no admite negativos y el proyectado es menor que cero. El control cronológico estricto se aplica al confirmar (D7), no aquí.

### D9. Resultados tipados
`DraftResult = Ok(draft) | Rejected(DraftError)` con `DraftError = NotFound | TerminalState | NotReady(missing, issues) | NothingToAdjust | Wallet(WalletError) | AliasAlreadyUsed`. Sin textos.

### Organización del código
```
model/draft/        Evidence, ProposedAmount, WalletRef, MovementProposal, NonMovementIntent,
                    Draft, ReadyMovement, DraftField, DraftIssue, DraftEvent, DraftProjection,
                    WalletDirectory, DraftEvaluator, DraftResult, DraftError
port/output/        DraftRepositoryPort, DraftInterpreterPort, WalletAliasRepositoryPort
usecase/draft/      CaptureDraft, ReviseDraft, ConfirmDraft, DiscardDraft, GetDraftInbox, SetWalletAliases
test unit/          DraftEvaluatorTest, DraftProjectionTest
test feature/       CarlosDraftDayFeatureTest, JohnDraftFeatureTest, FacundoDraftFeatureTest + fakes
```

## Risks / Trade-offs
- [Reproducir el historial en cada consulta] → Son pocos eventos por borrador. La bandeja proyecta solo los pendientes.
- [El estado depende del directorio actual de billeteras] → Es intencional: un borrador Listo cuya billetera se archivó no debe confirmarse a ciegas.
- [Ids derivados `"draft:<id>"` conviven con ids generados] → Siguen siendo globales y únicos porque el id del borrador lo es. Se documenta en el KDoc del caso de uso.
- [Captura con N propuestas crea N borradores] → Si el intérprete separa mal, el usuario descarta o revisa cada uno; la evidencia compartida permite ver el origen.

## Migration Plan
No aplica: no hay datos persistidos ni consumidores externos.

## Open Questions
- Límite de antigüedad para avisar de borradores olvidados (por ejemplo, más de 7 días): es presentación y no cambia el modelo.
