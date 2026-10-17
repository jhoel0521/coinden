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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.capture_hint
import coinden.composeapp.generated.resources.category_custom
import coinden.composeapp.generated.resources.category_none
import coinden.composeapp.generated.resources.dialog_cancel
import coinden.composeapp.generated.resources.dialog_ok
import coinden.composeapp.generated.resources.field_category
import coinden.composeapp.generated.resources.field_method
import coinden.composeapp.generated.resources.field_wallet
import coinden.composeapp.generated.resources.form_amount
import coinden.composeapp.generated.resources.form_before
import coinden.composeapp.generated.resources.form_before_empty
import coinden.composeapp.generated.resources.form_confirm
import coinden.composeapp.generated.resources.form_correct_note
import coinden.composeapp.generated.resources.form_currency
import coinden.composeapp.generated.resources.form_currency_wallet
import coinden.composeapp.generated.resources.form_date
import coinden.composeapp.generated.resources.form_discard
import coinden.composeapp.generated.resources.form_dismiss_change
import coinden.composeapp.generated.resources.form_exit_leave
import coinden.composeapp.generated.resources.form_exit_note
import coinden.composeapp.generated.resources.form_exit_stay
import coinden.composeapp.generated.resources.form_exit_title
import coinden.composeapp.generated.resources.form_fill
import coinden.composeapp.generated.resources.form_keep
import coinden.composeapp.generated.resources.form_kind
import coinden.composeapp.generated.resources.form_large_change
import coinden.composeapp.generated.resources.form_no_changes
import coinden.composeapp.generated.resources.form_none
import coinden.composeapp.generated.resources.form_note
import coinden.composeapp.generated.resources.form_other_date
import coinden.composeapp.generated.resources.form_phrase
import coinden.composeapp.generated.resources.form_reference
import coinden.composeapp.generated.resources.form_review_note
import coinden.composeapp.generated.resources.form_save
import coinden.composeapp.generated.resources.form_saved_note
import coinden.composeapp.generated.resources.form_saving
import coinden.composeapp.generated.resources.form_see_evidence
import coinden.composeapp.generated.resources.form_storage_error
import coinden.composeapp.generated.resources.form_title
import coinden.composeapp.generated.resources.form_title_correct
import coinden.composeapp.generated.resources.form_title_review
import coinden.composeapp.generated.resources.form_today
import coinden.composeapp.generated.resources.form_yesterday
import coinden.composeapp.generated.resources.kind_expense
import coinden.composeapp.generated.resources.kind_income
import coinden.composeapp.generated.resources.same_no
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.format.TimeFormatter
import com.coinden.app.theme.CoinDenIcon
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.CorrectionField
import com.coinden.core.domain.model.draft.FieldChange
import com.coinden.core.domain.model.draft.MovementType
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.PaymentMethod
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class DateChoice { TODAY, YESTERDAY, OTHER }

private const val DAY_MS = 86_400_000L
private val LocalFormEnabled = staticCompositionLocalOf { true }

/**
 * El formulario completo del movimiento. Escribir: arriba, una frase libre que llena los campos
 * con el intérprete. Revisar y corregir: precargado con lo propuesto; al corregir, cada campo que
 * cambia muestra su valor anterior y nada se aplica hasta tocar Confirmar.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DraftFormScreen(
    presenter: DraftFormPresenter,
    nowEpochMs: () -> Long,
    utcOffsetMinutes: Int,
    locale: UserLocale,
    onSaved: (CaptureResult) -> Unit,
    onDone: () -> Unit,
    onEvidence: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val mode = state.mode
    var dateChoice by remember { mutableStateOf(DateChoice.TODAY) }
    var picking by remember { mutableStateOf(false) }
    var askExit by remember { mutableStateOf(false) }
    val requestBack: () -> Unit = {
        when (presenter.requestExit()) {
            FormExit.LEAVE -> onBack()
            FormExit.ASK -> askExit = true
            FormExit.BLOCKED -> Unit
        }
    }
    FormBackHandler(enabled = true, onBack = requestBack)
    LaunchedEffect(presenter) {
        presenter.load()
        if (mode != FormMode.New && presenter.state.value.occurredAtEpochMs != null) dateChoice = DateChoice.OTHER
    }
    val changes = state.changes.associateBy { it.field }

    @Composable
    fun before(field: CorrectionField): String? = changes[field]?.let { beforeText(it, state.wallets, locale, utcOffsetMinutes) }

    CompositionLocalProvider(LocalFormEnabled provides (!state.busy && !state.completed)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = requestBack, enabled = !state.busy) { Icon(CoinDenIcon.CLOSE.vector, stringResource(Res.string.back)) }
                val title =
                    when (mode) {
                        FormMode.New -> Res.string.form_title
                        is FormMode.Review -> Res.string.form_title_review
                        is FormMode.Correct -> Res.string.form_title_correct
                    }
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                state.evidenceDraftId?.let { id ->
                    TextButton(onClick = { onEvidence(id) }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(Res.string.form_see_evidence))
                    }
                }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (mode is FormMode.Correct) {
                    if (state.largeAmount) Warning(stringResource(Res.string.form_large_change))
                    if (state.changes.isEmpty()) {
                        Text(
                            stringResource(Res.string.form_no_changes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            enabled = LocalFormEnabled.current,
                            value = state.phrase,
                            onValueChange = presenter::onPhraseChange,
                            label = { Text(stringResource(Res.string.form_phrase)) },
                            placeholder = { Text(stringResource(Res.string.capture_hint)) },
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalButton(
                            onClick = { scope.launch { presenter.fillFromPhrase() } },
                            enabled = state.phrase.isNotBlank() && LocalFormEnabled.current,
                            modifier = Modifier.heightIn(min = 56.dp),
                        ) { Text(stringResource(Res.string.form_fill)) }
                    }
                }

                Section(Res.string.form_kind, before(CorrectionField.KIND)) {
                    Choice(state.kind == FormKind.EXPENSE, Res.string.kind_expense) { presenter.onKindChange(FormKind.EXPENSE) }
                    Choice(state.kind == FormKind.INCOME, Res.string.kind_income) { presenter.onKindChange(FormKind.INCOME) }
                }

                OutlinedTextField(
                    enabled = LocalFormEnabled.current,
                    value = state.amount,
                    onValueChange = presenter::onAmountChange,
                    label = { Text(stringResource(Res.string.form_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = state.largeAmount,
                    supportingText = before(CorrectionField.AMOUNT)?.let { { Changed(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )

                Section(Res.string.field_wallet, before(CorrectionField.WALLET)) {
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
                    Section(Res.string.field_category, before(CorrectionField.CATEGORY)) {
                        Choice(state.category == null, Res.string.category_none) { presenter.onCategoryChange(null) }
                        BaseCategory.entries.forEach { category ->
                            val ref = CategoryRef.Base(category)
                            Choice(state.category == ref, category.label()) { presenter.onCategoryChange(ref) }
                        }
                    }
                }

                Section(Res.string.form_date, before(CorrectionField.OCCURRED_AT)) {
                    Choice(dateChoice == DateChoice.TODAY, Res.string.form_today) {
                        dateChoice = DateChoice.TODAY
                        presenter.onDateChange(null)
                    }
                    Choice(dateChoice == DateChoice.YESTERDAY, Res.string.form_yesterday) {
                        dateChoice = DateChoice.YESTERDAY
                        presenter.onDateChange(nowEpochMs() - DAY_MS)
                    }
                    val other = stringResource(Res.string.form_other_date)
                    val label =
                        state.occurredAtEpochMs
                            ?.takeIf { dateChoice == DateChoice.OTHER }
                            ?.let { "$other · ${TimeFormatter.dayMonthTime(it, utcOffsetMinutes, locale)}" } ?: other
                    ChoiceText(dateChoice == DateChoice.OTHER, label) { picking = true }
                }

                OutlinedTextField(
                    enabled = LocalFormEnabled.current,
                    value = state.note,
                    onValueChange = presenter::onNoteChange,
                    label = { Text(stringResource(Res.string.form_note)) },
                    supportingText = before(CorrectionField.NOTE)?.let { { Changed(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    enabled = LocalFormEnabled.current,
                    value = state.reference,
                    onValueChange = presenter::onReferenceChange,
                    label = { Text(stringResource(Res.string.form_reference)) },
                    singleLine = true,
                    supportingText = before(CorrectionField.REFERENCE)?.let { { Changed(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                val note =
                    when (mode) {
                        FormMode.New -> Res.string.form_saved_note
                        is FormMode.Review -> Res.string.form_review_note
                        is FormMode.Correct -> Res.string.form_correct_note
                    }
                Text(stringResource(note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (state.busy) Text(stringResource(Res.string.form_saving), modifier = Modifier.padding(horizontal = 16.dp))
            if (state.storageError) Warning(stringResource(Res.string.form_storage_error))
            state.problem?.let { Warning(problemText(it, locale)) }
            Actions(presenter, mode, onSaved, onDone)
        }
    }

    if (askExit) {
        AlertDialog(
            onDismissRequest = { askExit = false },
            title = { Text(stringResource(Res.string.form_exit_title)) },
            text = { Text(stringResource(Res.string.form_exit_note)) },
            confirmButton = {
                TextButton(onClick = {
                    askExit = false
                    onBack()
                }) { Text(stringResource(Res.string.form_exit_leave)) }
            },
            dismissButton = { TextButton(onClick = { askExit = false }) { Text(stringResource(Res.string.form_exit_stay)) } },
        )
    }
    if (picking) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.occurredAtEpochMs ?: nowEpochMs())
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

/** Botones de abajo según el modo; la acción principal ocupa el ancho. */
@Composable
private fun Actions(
    presenter: DraftFormPresenter,
    mode: FormMode,
    onSaved: (CaptureResult) -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val primary = Modifier.fillMaxWidth().heightIn(min = 60.dp)

    fun run(action: suspend () -> Boolean) {
        scope.launch { if (action()) onDone() }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val secondary =
            when (mode) {
                FormMode.New -> emptyList()
                is FormMode.Review ->
                    listOf(Res.string.form_discard to { run(presenter::discard) }, Res.string.form_keep to { run(presenter::keep) })
                is FormMode.Correct ->
                    if (mode.sourceDraftId == null) {
                        emptyList()
                    } else {
                        listOf(
                            Res.string.same_no to { run(presenter::distinct) },
                            Res.string.form_dismiss_change to { run(presenter::discard) },
                        )
                    }
            }
        if (secondary.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                secondary.forEach { (label, action) ->
                    OutlinedButton(
                        onClick = action,
                        enabled = LocalFormEnabled.current,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
        }
        if (mode == FormMode.New) {
            Button(onClick = {
                scope.launch {
                    presenter.save()?.let(onSaved)
                }
            }, enabled = LocalFormEnabled.current, modifier = primary, shape = MaterialTheme.shapes.extraLarge) {
                Text(stringResource(Res.string.form_save), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(onClick = {
                run(presenter::confirm)
            }, enabled = LocalFormEnabled.current, modifier = primary, shape = MaterialTheme.shapes.extraLarge) {
                Text(stringResource(Res.string.form_confirm), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** El valor anterior de un campo que cambia: "antes: 35,00 Bs" o "antes: vacío". */
@Composable
fun beforeText(
    change: FieldChange,
    wallets: List<FormWallet>,
    locale: UserLocale,
    utcOffsetMinutes: Int,
): String {
    val value: String? =
        when (change) {
            is FieldChange.Kind -> stringResource(change.before.label())
            is FieldChange.Amount -> MoneyFormatter.format(change.before, locale)
            is FieldChange.Wallet -> wallets.firstOrNull { it.id == change.before }?.name ?: change.before
            is FieldChange.ToAmount -> change.before?.let { MoneyFormatter.format(it, locale) }
            is FieldChange.ToWallet -> change.before?.let { id -> wallets.firstOrNull { it.id == id }?.name ?: id }
            is FieldChange.OccurredAt -> TimeFormatter.dayMonthTime(change.before, utcOffsetMinutes, locale)
            is FieldChange.Note -> change.before
            is FieldChange.Category ->
                when (val c = change.before) {
                    null -> stringResource(Res.string.category_none)
                    is CategoryRef.Base -> stringResource(c.category.label())
                    is CategoryRef.Custom -> stringResource(Res.string.category_custom)
                }
            is FieldChange.Reference -> change.before
        }
    return value?.let { stringResource(Res.string.form_before, it) } ?: stringResource(Res.string.form_before_empty)
}

fun MovementType.label(): StringResource =
    when (this) {
        MovementType.EXPENSE -> MovementKindView.EXPENSE.label()
        MovementType.INCOME -> MovementKindView.INCOME.label()
        MovementType.TRANSFER -> MovementKindView.TRANSFER.label()
        MovementType.ADJUSTMENT -> MovementKindView.ADJUSTMENT.label()
    }

@Composable
private fun Changed(text: String) {
    Text(text, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Warning(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(
    title: StringResource,
    before: String? = null,
    chips: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
        before?.let { Changed(it) }
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
    FilterChip(selected = selected, onClick = onClick, enabled = LocalFormEnabled.current, label = {
        Text(label)
    }, modifier = Modifier.heightIn(min = 48.dp))
}
