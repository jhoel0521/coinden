package com.coinden.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Cuenta las veces que la app volvió al frente. Lo que la persona compartió mientras estaba en
 * otra app se guardó sin abrir CoinDen; al volver, las pantallas recargan con esta señal.
 */
class ForegroundSignal {
    private val mutableReturns = MutableStateFlow(0)
    val returns: StateFlow<Int> = mutableReturns.asStateFlow()

    fun onForeground() = mutableReturns.update { it + 1 }
}
