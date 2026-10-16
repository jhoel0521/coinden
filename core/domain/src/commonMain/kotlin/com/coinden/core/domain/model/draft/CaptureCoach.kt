package com.coinden.core.domain.model.draft

/** Qué conviene decir la próxima vez para que la captura quede lista sin revisar. */
enum class CaptureTip { SAY_WALLET, SAY_AMOUNT }

/**
 * Consejos de captura, puros: qué faltó en un borrador pendiente y con qué palabra la persona
 * nombra a una billetera. Sin textos: la presentación arma el consejo y el ejemplo.
 */
object CaptureCoach {
    /** Lo que faltó: la billetera (o quedó sugerida) y el monto. Nada si está completo o no es pendiente con propuesta. */
    fun tipsFor(draft: Draft): Set<CaptureTip> {
        val (proposal, missing) =
            when (draft) {
                is Draft.Ready -> draft.proposal to emptySet()
                is Draft.Incomplete -> draft.proposal to draft.missing
                // Una frase que no se entendió: sin monto, las reglas no entienden nada. Una foto o un audio sin leer, nada.
                is Draft.Captured -> return if (draft.header.evidence.isUnreadPhrase) setOf(CaptureTip.SAY_AMOUNT) else emptySet()
                else -> return emptySet()
            }
        val wallet =
            when (proposal) {
                is MovementProposal.Expense -> proposal.wallet
                is MovementProposal.Income -> proposal.wallet
                is MovementProposal.Transfer -> proposal.from
                is MovementProposal.Unknown -> return setOf(CaptureTip.SAY_AMOUNT)
                else -> return emptySet()
            }
        val amount =
            when (proposal) {
                is MovementProposal.Expense -> proposal.amount
                is MovementProposal.Income -> proposal.amount
                is MovementProposal.Transfer -> proposal.out
                else -> null
            }
        return buildSet {
            if (wallet == null ||
                DraftField.WALLET in missing ||
                proposal.origins[DraftField.WALLET] == FieldOrigin.SUGGESTED
            ) {
                add(CaptureTip.SAY_WALLET)
            }
            if (amount == null) add(CaptureTip.SAY_AMOUNT)
        }
    }

    /** La billetera del ejemplo: la sugerida en el borrador o, si no hay, la por defecto. */
    fun exampleWalletId(
        draft: Draft,
        directory: WalletDirectory,
    ): String? {
        val proposal =
            when (draft) {
                is Draft.Ready -> draft.proposal
                is Draft.Incomplete -> draft.proposal
                else -> null
            }
        val ref = (proposal as? MovementProposal.Expense)?.wallet ?: (proposal as? MovementProposal.Income)?.wallet
        val suggested =
            when (ref) {
                is WalletRef.ById -> directory.byId(ref.walletId)?.id
                is WalletRef.ByHint -> directory.byHint(ref.hint)?.id
                null -> null
            }
        return suggested ?: directory.defaultWalletId
    }

    /**
     * La palabra con que la persona puede nombrar la billetera al dictar: su alias más corto o, si no
     * tiene, la palabra más corta de su nombre que el intérprete reconoce ([nameHints]: palabra -> id).
     */
    fun walletWord(
        walletId: String,
        directory: WalletDirectory,
        nameHints: Map<String, String>,
    ): String? =
        directory.aliases
            .filterValues { it == walletId }
            .keys
            .minByOrNull { it.length }
            ?: nameHints.filterValues { it == walletId }.keys.minByOrNull { it.length }
}

/** Capturas desde una fecha: cuántas hubo y cuántas quedaron listas sin revisar. */
data class CaptureStats(
    val total: Int,
    val readyAlone: Int,
)

private val Evidence.isUnreadPhrase: Boolean get() = attachmentRef == null && !text.isNullOrBlank()
