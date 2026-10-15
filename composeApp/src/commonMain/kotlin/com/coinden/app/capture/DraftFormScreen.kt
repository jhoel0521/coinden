package com.coinden.app.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.capture_hint
import coinden.composeapp.generated.resources.category_none
import coinden.composeapp.generated.resources.dialog_cancel
import coinden.composeapp.generated.resources.dialog_ok
import coinden.composeapp.generated.resources.field_category
import coinden.composeapp.generated.resources.field_method
import coinden.composeapp.generated.resources.field_wallet
import coinden.composeapp.generated.resources.form_amount
import coinden.composeapp.generated.resources.form_currency
import coinden.composeapp.generated.resources.form_currency_wallet
import coinden.composeapp.generated.resources.form_date
import coinden.composeapp.generated.resources.form_fill
import coinden.composeapp.generated.resources.form_kind
import coinden.composeapp.generated.resources.form_none
import coinden.composeapp.generated.resources.form_note
import coinden.composeapp.generated.resources.form_other_date
import coinden.composeapp.generated.resources.form_phrase
import coinden.composeapp.generated.resources.form_reference
import coinden.composeapp.generated.resources.form_save
import coinden.composeapp.generated.resources.form_saved_note
import coinden.composeapp.generated.resources.form_title
import coinden.composeapp.generated.resources.form_today
import coinden.composeapp.generated.resources.form_yesterday
import coinden.composeapp.generated.resources.kind_expense
import coinden.composeapp.generated.resources.kind_income
import com.coinden.app.theme.CoinDenIcon
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.wallet.PaymentMethod
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class DateChoice { TODAY, YESTERDAY, OTHER }

private const val DAY_MS = 86_400_000L

/**
 * Escribir: todos los campos del movimiento y, arriba, una frase libre que los llena con el
 * intérprete (también sirve para pegar un mensaje que no se puede compartir).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DraftFormScreen(
    presenter: DraftFormPresenter,
    nowEpochMs: () -> Long,
    utcOffsetMinutes: Int,
    onSaved: (CaptureResult) -> Unit,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var dateChoice by remember { mutableStateOf(DateChoice.TODAY) }
    var picking by remember { mutableStateOf(false) }
    LaunchedEffect(presenter) { presenter.load() }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(CoinDenIcon.CLOSE.vector, stringResource(Res.string.back)) }
            Text(stringResource(Res.string.form_title), style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.phrase,
                    onValueChange = presenter::onPhraseChange,
                    label = { Text(stringResource(Res.string.form_phrase)) },
                    placeholder = { Text(stringResource(Res.string.capture_hint)) },
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(
                    onClick = { scope.launch { presenter.fillFromPhrase() } },
                    enabled = state.phrase.isNotBlank(),
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { Text(stringResource(Res.string.form_fill)) }
            }

            Section(Res.string.form_kind) {
                Choice(state.kind == FormKind.EXPENSE, Res.string.kind_expense) { presenter.onKindChange(FormKind.EXPENSE) }
                Choice(state.kind == FormKind.INCOME, Res.string.kind_income) { presenter.onKindChange(FormKind.INCOME) }
            }

            OutlinedTextField(
                value = state.amount,
                onValueChange = presenter::onAmountChange,
                label = { Text(stringResource(Res.string.form_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Section(Res.string.field_wallet) {
                state.wallets.forEach { wallet ->
                    ChoiceText(state.walletId == wallet.id, wallet.name) { presenter.onWalletChange(wallet.id) }
                }
            }

            val currencies = state.wallets.map { it.currency }.distinctBy { it.code }
            if (currencies.size > 1 || state.currency != null) {
                Section(Res.string.form_currency) {
                    Choice(state.currency == null, Res.string.form_currency_wallet) { presenter.onCurrencyChange(null) }
                    currencies.forEach { currency ->
                        ChoiceText(state.currency?.code == currency.code, currency.code) { presenter.onCurrencyChange(currency) }
                    }
                }
            }

            Section(Res.string.field_method) {
                Choice(state.method == null, Res.string.form_none) { presenter.onMethodChange(null) }
                PaymentMethod.entries.forEach { method ->
                    Choice(state.method == method, method.label()) { presenter.onMethodChange(method) }
                }
            }

            if (state.kind == FormKind.EXPENSE) {
                Section(Res.string.field_category) {
                    Choice(state.category == null, Res.string.category_none) { presenter.onCategoryChange(null) }
                    BaseCategory.entries.forEach { category ->
                        val ref = CategoryRef.Base(category)
                        Choice(state.category == ref, category.label()) { presenter.onCategoryChange(ref) }
                    }
                }
            }

            Section(Res.string.form_date) {
                Choice(dateChoice == DateChoice.TODAY, Res.string.form_today) {
                    dateChoice = DateChoice.TODAY
                    presenter.onDateChange(null)
                }
                Choice(dateChoice == DateChoice.YESTERDAY, Res.string.form_yesterday) {
                    dateChoice = DateChoice.YESTERDAY
                    presenter.onDateChange(nowEpochMs() - DAY_MS)
                }
                Choice(dateChoice == DateChoice.OTHER, Res.string.form_other_date) { picking = true }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = presenter::onNoteChange,
                label = { Text(stringResource(Res.string.form_note)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.reference,
                onValueChange = presenter::onReferenceChange,
                label = { Text(stringResource(Res.string.form_reference)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(Res.string.form_saved_note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Button(
            onClick = { scope.launch { onSaved(presenter.save()) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 60.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) { Text(stringResource(Res.string.form_save), style = MaterialTheme.typography.titleMedium) }
    }

    if (picking) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = nowEpochMs())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    // El selector devuelve la medianoche UTC del día: se guarda el mediodía local de ese día.
                    picker.selectedDateMillis?.let {
                        presenter.onDateChange(it + DAY_MS / 2 - utcOffsetMinutes * 60_000L)
                        dateChoice = DateChoice.OTHER
                    }
                    picking = false
                }) { Text(stringResource(Res.string.dialog_ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(Res.string.dialog_cancel)) } },
        ) { DatePicker(picker) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(
    title: StringResource,
    chips: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
    }
}

@Composable
private fun Choice(
    selected: Boolean,
    label: StringResource,
    onClick: () -> Unit,
) = ChoiceText(selected, stringResource(label), onClick)

@Composable
private fun ChoiceText(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
}
