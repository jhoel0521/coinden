package com.coinden.app.insights

import com.coinden.app.AppContainer
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.ProfileRates
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.usecase.category.CategoryTotal
import com.coinden.core.domain.usecase.category.SpendingReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SpendingSnapshot(
    val report: SpendingReport,
    val customNames: Map<String, String> = emptyMap(),
    val convertedCurrencies: Set<String> = emptySet(),
)

data class SpendingRow(
    val total: CategoryTotal,
    val customName: String?,
    val convertedCurrencies: Set<String>,
) {
    val hasConverted: Boolean get() = convertedCurrencies.isNotEmpty()
}

enum class SpendingLoad { LOADING, READY, ERROR }

data class SpendingInsightsState(
    val month: SpendingMonth,
    val canNext: Boolean = false,
    val load: SpendingLoad = SpendingLoad.LOADING,
    val total: Money? = null,
    val rows: List<SpendingRow> = emptyList(),
) {
    val partial: Boolean get() = rows.any { it.total.hasUnconvertedAmounts }
    val hasConverted: Boolean get() = rows.any { it.hasConverted }
    val empty: Boolean get() = load == SpendingLoad.READY && rows.isEmpty()
}

/** Lectura local sin escrituras ni red; solo se publica la última solicitud. */
class SpendingInsightsPresenter(
    private val container: AppContainer,
    private val query: suspend (SpendingMonth, Int) -> SpendingSnapshot = { month, offset ->
        withContext(Dispatchers.Default) {
            val (from, to) = month.bounds(offset)
            val report = container.spendingByCategory(container.ownerId, from, to)
            val converter = ProfileRates.of(container.storage.profiles.getProfile(container.ownerId), emptyList()).converter
            val converted =
                report.rows
                    .flatMap {
                        it.byCurrency
                    }.filter { converter.canConvert(it.currency, report.currency) }
                    .map { it.currency.code }
                    .toSet()
            val names =
                container.storage.categorization
                    .customCategoriesOf(container.ownerId)
                    .associate { it.id to it.name }
            SpendingSnapshot(report, names, converted)
        }
    },
) {
    private fun currentMonth() = SpendingMonth.at(container.clock.nowEpochMs(), container.utcOffsetMinutes)

    private val mutableState = MutableStateFlow(SpendingInsightsState(currentMonth()))
    val state: StateFlow<SpendingInsightsState> = mutableState.asStateFlow()
    private var generation = 0L

    suspend fun load() = select(state.value.month)

    suspend fun previous() = select(state.value.month.shift(-1))

    suspend fun next() {
        val next = state.value.month.shift(1)
        if (next <= currentMonth()) select(next)
    }

    private suspend fun select(requested: SpendingMonth) {
        val current = currentMonth()
        val month = minOf(requested, current)
        val request = ++generation
        mutableState.value = SpendingInsightsState(month, canNext = month < current)
        try {
            val snapshot = query(month, container.utcOffsetMinutes)
            val rows =
                snapshot.report.rows.map { total ->
                    val name = (total.category as? CategoryRef.Custom)?.let { snapshot.customNames[it.id] }
                    val converted =
                        total.byCurrency
                            .map { it.currency.code }
                            .filter { it in snapshot.convertedCurrencies }
                            .toSet()
                    SpendingRow(total, name, converted)
                }
            if (request == generation) {
                mutableState.value = SpendingInsightsState(month, month < currentMonth(), SpendingLoad.READY, snapshot.report.total, rows)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (request == generation) mutableState.value = SpendingInsightsState(month, month < currentMonth(), SpendingLoad.ERROR)
        }
    }
}
