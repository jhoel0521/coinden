package com.coinden.app.movements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.history_title
import coinden.composeapp.generated.resources.spending_title
import com.coinden.app.AppContainer
import com.coinden.app.capture.FormMode
import com.coinden.app.history.HistoryPresenter
import com.coinden.app.history.HistoryScreen
import com.coinden.app.insights.SpendingInsightsPresenter
import com.coinden.app.insights.SpendingInsightsScreen
import com.coinden.app.ui.components.CdChoice
import com.coinden.app.ui.components.CdOption
import com.coinden.core.domain.model.interpret.UserLocale
import org.jetbrains.compose.resources.stringResource

/** Qué muestra Movimientos. */
enum class MovementsView { HISTORY, SPENDING }

/** Movimientos: el historial y los gastos del mes, uno u otro sin salir de la pestaña. */
@Composable
fun MovementsScreen(
    container: AppContainer,
    locale: UserLocale,
    utcOffsetMinutes: Int,
    onCorrect: (FormMode) -> Unit,
    onEvidence: (String) -> Unit,
) {
    var view by rememberSaveable { mutableStateOf(MovementsView.HISTORY) }
    val history = remember { HistoryPresenter(container) }
    val spending = remember { SpendingInsightsPresenter(container) }
    Column(Modifier.fillMaxSize()) {
        CdChoice(
            listOf(
                CdOption(MovementsView.HISTORY, stringResource(Res.string.history_title)),
                CdOption(MovementsView.SPENDING, stringResource(Res.string.spending_title)),
            ),
            selected = view,
            onSelect = { view = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        when (view) {
            MovementsView.HISTORY -> HistoryScreen(history, locale, utcOffsetMinutes, onCorrect, onEvidence, showHeader = false)
            MovementsView.SPENDING -> SpendingInsightsScreen(spending, locale, showHeader = false)
        }
    }
}
