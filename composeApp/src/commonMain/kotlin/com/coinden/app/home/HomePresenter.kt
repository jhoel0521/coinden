package com.coinden.app.home

import com.coinden.app.AppContainer
import com.coinden.app.ui.ScreenLoad
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Una billetera en Inicio: nombre (tal como lo escribió la persona), tipo, saldo y si es la por defecto. */
data class HomeWallet(
    val id: String,
    val name: String,
    val type: WalletType,
    val balance: Money,
    val isDefault: Boolean,
)

data class HomeState(
    val total: Money? = null,
    /** Saldos en monedas sin tasa de referencia: no se suman al total. */
    val unconverted: List<Money> = emptyList(),
    val wallets: List<HomeWallet> = emptyList(),
    val pendingDrafts: Int = 0,
    val load: ScreenLoad = ScreenLoad.LOADING,
    val loaded: Boolean = false,
)

/** Inicio: patrimonio en la moneda principal, billeteras con su saldo y borradores por revisar. */
class HomePresenter(
    private val container: AppContainer,
    private val query: (suspend () -> HomeState)? = null,
) {
    private val mutableState = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = mutableState.asStateFlow()
    private var generation = 0L

    suspend fun load() {
        val request = ++generation
        mutableState.value = state.value.copy(load = ScreenLoad.LOADING)
        try {
            val snapshot = query?.invoke() ?: readSnapshot()
            if (request == generation) mutableState.value = snapshot.copy(load = ScreenLoad.READY, loaded = true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (request == generation) mutableState.value = state.value.copy(load = ScreenLoad.ERROR)
        }
    }

    private suspend fun readSnapshot(): HomeState {
        val owner = container.ownerId
        val directory = container.drafts.directory(owner)
        val balances = container.walletBalances(owner)
        val portfolio = container.portfolio(owner, balances.map { it.balance })
        val wallets =
            balances.mapNotNull { b ->
                directory.byId(b.walletId)?.let { HomeWallet(it.id, it.name, it.type, b.balance, it.id == directory.defaultWalletId) }
            }
        return HomeState(
            total = portfolio.totalUnifiedAmount,
            unconverted = portfolio.unconverted,
            wallets = wallets,
            pendingDrafts = container.draftInbox(owner).pending.size,
        )
    }
}
