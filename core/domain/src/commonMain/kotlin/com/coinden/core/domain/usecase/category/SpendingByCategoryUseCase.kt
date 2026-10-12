package com.coinden.core.domain.usecase.category

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.ProfileRates
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.LedgerProjection
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort

/**
 * Gasto de una categoría en el período: por moneda y consolidado en la moneda local.
 * [category] null = Sin categoría. [hasUnconvertedAmounts] avisa si alguna moneda no tenía tasa
 * (queda en [byCurrency] pero no suma en [consolidated]).
 */
data class CategoryTotal(
    val category: CategoryRef?,
    val byCurrency: List<Money>,
    val consolidated: Money,
    val hasUnconvertedAmounts: Boolean = false,
)

/** Respuesta a "¿en qué se me fue el dinero?" para un período, de mayor a menor. */
data class SpendingReport(
    val currency: Currency,
    val rows: List<CategoryTotal>,
    val total: Money,
)

/**
 * Caso de Uso: gasto de un período por categoría. Solo cuentan los gastos vigentes (sin anulados)
 * de todas las billeteras del usuario; transferencias, retiros y cambios no son gasto. Consolida con
 * las mismas tasas que el patrimonio ([ProfileRates]).
 */
class SpendingByCategoryUseCase(
    private val ctx: CategorizationContext,
    private val profiles: CurrencyProfileRepositoryPort,
    private val cryptoRates: List<ExchangeRate> = ProfileRates.DEFAULT_CRYPTO_RATES,
) {
    suspend operator fun invoke(
        ownerId: String,
        fromEpochMs: Long,
        toEpochMs: Long,
    ): SpendingReport {
        val rates = ProfileRates.of(profiles.getProfile(ownerId), cryptoRates)
        val local = rates.localCurrency

        val expenses =
            ctx.wallets
                .listWallets(ownerId)
                .flatMap { wallet -> LedgerProjection(wallet, ctx.wallets.eventsForWallet(wallet.id)).effectiveEvents }
                .filterIsInstance<LedgerEvent.Expense>()
                .filter { it.occurredAtEpochMs in fromEpochMs until toEpochMs }
                .distinctBy { it.id }

        val byCategory = expenses.groupBy { ctx.current(it.id)?.category }

        val rows =
            byCategory.map { (category, items) ->
                val byCurrency =
                    items
                        .groupBy { it.amount.currency.code }
                        .values
                        .map { same -> same.map { it.amount }.reduce { acc, m -> acc + m } }
                        .sortedBy { it.currency.code != local.code }
                var consolidated = Money.zero(local)
                var unconverted = false
                byCurrency.forEach { amount ->
                    runCatching { rates.converter.convert(amount, local) }
                        .onSuccess { consolidated += it }
                        .onFailure { unconverted = true }
                }
                CategoryTotal(category, byCurrency, consolidated, unconverted)
            }.sortedByDescending { it.consolidated }

        val total = rows.fold(Money.zero(local)) { acc, row -> acc + row.consolidated }
        return SpendingReport(local, rows, total)
    }
}
