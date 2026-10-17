package com.coinden.app.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.category_none
import coinden.composeapp.generated.resources.spending_category_missing
import coinden.composeapp.generated.resources.spending_empty
import coinden.composeapp.generated.resources.spending_error
import coinden.composeapp.generated.resources.spending_estimate
import coinden.composeapp.generated.resources.spending_loading
import coinden.composeapp.generated.resources.spending_missing_rate
import coinden.composeapp.generated.resources.spending_month_1
import coinden.composeapp.generated.resources.spending_month_10
import coinden.composeapp.generated.resources.spending_month_11
import coinden.composeapp.generated.resources.spending_month_12
import coinden.composeapp.generated.resources.spending_month_2
import coinden.composeapp.generated.resources.spending_month_3
import coinden.composeapp.generated.resources.spending_month_4
import coinden.composeapp.generated.resources.spending_month_5
import coinden.composeapp.generated.resources.spending_month_6
import coinden.composeapp.generated.resources.spending_month_7
import coinden.composeapp.generated.resources.spending_month_8
import coinden.composeapp.generated.resources.spending_month_9
import coinden.composeapp.generated.resources.spending_next
import coinden.composeapp.generated.resources.spending_no_conversion
import coinden.composeapp.generated.resources.spending_original
import coinden.composeapp.generated.resources.spending_partial
import coinden.composeapp.generated.resources.spending_previous
import coinden.composeapp.generated.resources.spending_retry
import coinden.composeapp.generated.resources.spending_title
import coinden.composeapp.generated.resources.spending_total
import com.coinden.app.capture.label
import com.coinden.app.format.MoneyFormatter
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SpendingInsightsScreen(
    presenter: SpendingInsightsPresenter,
    locale: UserLocale,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(presenter) { presenter.load() }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(Res.string.back))
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    stringResource(Res.string.spending_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(state.month.monthResource()) + " " + state.month.year,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { scope.launch { presenter.previous() } },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(stringResource(Res.string.spending_previous)) }
                        OutlinedButton(
                            onClick = { scope.launch { presenter.next() } },
                            enabled = state.canNext,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(stringResource(Res.string.spending_next)) }
                    }
                }
            }
            when (state.load) {
                SpendingLoad.LOADING -> item { Text(stringResource(Res.string.spending_loading)) }
                SpendingLoad.ERROR ->
                    item {
                        Column {
                            Text(stringResource(Res.string.spending_error))
                            TextButton(onClick = { scope.launch { presenter.load() } }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(Res.string.spending_retry))
                            }
                        }
                    }
                SpendingLoad.READY -> {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(if (state.partial) Res.string.spending_partial else Res.string.spending_total))
                                Text(
                                    if (state.hasConverted || state.empty) {
                                        state.total?.let { MoneyFormatter.format(it, locale) }.orEmpty()
                                    } else {
                                        stringResource(Res.string.spending_no_conversion)
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                                Text(stringResource(Res.string.spending_estimate), style = MaterialTheme.typography.bodySmall)
                                if (state.partial) Text(stringResource(Res.string.spending_missing_rate))
                            }
                        }
                    }
                    if (state.empty) item { Text(stringResource(Res.string.spending_empty)) }
                    items(state.rows) { row -> SpendingCategoryRow(row, locale) }
                }
            }
        }
    }
}

@Composable
private fun SpendingCategoryRow(
    row: SpendingRow,
    locale: UserLocale,
) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                when (val category = row.total.category) {
                    null -> stringResource(Res.string.category_none)
                    is CategoryRef.Base -> stringResource(category.category.label())
                    is CategoryRef.Custom -> row.customName ?: stringResource(Res.string.spending_category_missing)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (row.hasConverted) {
                    MoneyFormatter.format(row.total.consolidated, locale)
                } else {
                    stringResource(Res.string.spending_no_conversion)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(Res.string.spending_original), style = MaterialTheme.typography.labelMedium)
            row.total.byCurrency.forEach { Text(MoneyFormatter.format(it, locale)) }
            if (row.total.hasUnconvertedAmounts) Text(stringResource(Res.string.spending_missing_rate))
        }
    }
}

fun SpendingMonth.monthResource(): StringResource =
    when (month) {
        1 -> Res.string.spending_month_1
        2 -> Res.string.spending_month_2
        3 -> Res.string.spending_month_3
        4 -> Res.string.spending_month_4
        5 -> Res.string.spending_month_5
        6 -> Res.string.spending_month_6
        7 -> Res.string.spending_month_7
        8 -> Res.string.spending_month_8
        9 -> Res.string.spending_month_9
        10 -> Res.string.spending_month_10
        11 -> Res.string.spending_month_11
        else -> Res.string.spending_month_12
    }
