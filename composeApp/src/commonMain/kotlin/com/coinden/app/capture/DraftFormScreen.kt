package com.coinden.app.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import coinden.composeapp.generated.resources.form_before
import coinden.composeapp.generated.resources.form_before_empty
import coinden.composeapp.generated.resources.form_confirm
import coinden.composeapp.generated.resources.form_correct_note
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
import coinden.composeapp.generated.resources.form_more_categories
import coinden.composeapp.generated.resources.form_more_details
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
import coinden.composeapp.generated.resources.spending_category_missing
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.format.TimeFormatter
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.CdAction
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdChoice
import com.coinden.app.ui.components.CdCollapsible
import com.coinden.app.ui.components.CdDialog
import com.coinden.app.ui.components.CdField
import com.coinden.app.ui.components.CdIconAction
import com.coinden.app.ui.components.CdNotice
import com.coinden.app.ui.components.CdOption
import com.coinden.app.ui.components.CdSection
import com.coinden.app.ui.components.NoticeKind
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
    fun before(field: CorrectionField): String? =
        changes[field]?.let { beforeText(it, state.wallets, state.customCategories, locale, utcOffsetMinutes) }

    val enabled = !state.busy && !state.completed
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CdIconAction(CoinDenIcon.CLOSE.vector, stringResource(Res.string.back), onClick = requestBack)
            val title =
                when (mode) {
                    FormMode.New -> Res.string.form_title
                    is FormMode.Review -> Res.string.form_title_review
                    is FormMode.Correct -> Res.string.form_title_correct
                }
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            state.evidenceDraftId?.let { id ->
                CdAction(stringResource(Res.string.form_see_evidence), { onEvidence(id) }, role = ActionRole.Tertiary, enabled = enabled)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (mode is FormMode.Correct) {
                if (state.largeAmount) CdNotice(NoticeKind.Warning, stringResource(Res.string.form_large_change))
                if (state.changes.isEmpty()) {
                    Text(
                        stringResource(Res.string.form_no_changes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            } else {
                CdSection(title = null) {
                    CdField(
                        state.phrase,
                        presenter::onPhraseChange,
                        label = stringResource(Res.string.form_phrase),
                        placeholder = stringResource(Res.string.capture_hint),
                        singleLine = false,
                        enabled = enabled,
                    )
                    CdActions(
                        listOf(
                            CdActionSpec(
                                stringResource(Res.string.form_fill),
                                { scope.launch { presenter.fillFromPhrase() } },
                                enabled = state.phrase.isNotBlank() && enabled,
                            ),
                        ),
                    )
                }
            }

            CdSection(stringResource(Res.string.form_kind), before = before(CorrectionField.KIND)) {
                CdChoice(
                    listOf(
                        CdOption(FormKind.EXPENSE, stringResource(Res.string.kind_expense)),
                        CdOption(FormKind.INCOME, stringResource(Res.string.kind_income)),
                    ),
                    selected = state.kind,
                    onSelect = presenter::onKindChange,
                    enabled = enabled,
                )
            }

            CdField(
                state.amount,
                presenter::onAmountChange,
                label = stringResource(Res.string.form_amount),
                before = before(CorrectionField.AMOUNT),
                warning = state.largeAmount,
                suffix = state.walletCurrency?.code,
                keyboardType = KeyboardType.Decimal,
                enabled = enabled,
            )

            CdSection(stringResource(Res.string.field_wallet), before = before(CorrectionField.WALLET)) {
                CdChoice(
                    state.wallets.map { CdOption(it.id, it.name) },
                    selected = state.walletId,
                    onSelect = { presenter.onWalletChange(it) },
                    open = true,
                    enabled = enabled,
                )
            }

            CdSection(stringResource(Res.string.field_method)) {
                CdChoice(
                    listOf(CdOption<PaymentMethod?>(null, stringResource(Res.string.form_none), icon = CoinDenIcon.HELP.vector)) +
                        PaymentMethod.entries.map { CdOption<PaymentMethod?>(it, stringResource(it.label()), icon = it.icon().vector) },
                    selected = state.method,
                    onSelect = presenter::onMethodChange,
                    enabled = enabled,
                )
            }

            if (state.kind == FormKind.EXPENSE) {
                CdSection(stringResource(Res.string.field_category), before = before(CorrectionField.CATEGORY)) {
                    val selectedCustom = (state.category as? CategoryRef.Custom)?.id
                    val previousCustom =
                        (changes[CorrectionField.CATEGORY]?.let { (it as? FieldChange.Category)?.before } as? CategoryRef.Custom)?.id
                    val missing = stringResource(Res.string.spending_category_missing)
                    val options =
                        listOf(CdOption<CategoryRef?>(null, stringResource(Res.string.category_none))) +
                            BaseCategory.entries.map { CdOption<CategoryRef?>(CategoryRef.Base(it), stringResource(it.label())) } +
                            state.customCategories
                                .filter { !it.archived || it.id == selectedCustom || it.id == previousCustom }
                                .map { CdOption<CategoryRef?>(CategoryRef.Custom(it.id), it.name ?: missing) }
                    CdChoice(
                        options,
                        selected = state.category,
                        onSelect = presenter::onCategoryChange,
                        usage = state.categoryUsage.mapKeys { (key, _) -> key as CategoryRef? },
                        moreLabel = stringResource(Res.string.form_more_categories),
                        enabled = enabled,
                    )
                }
            }

            CdSection(stringResource(Res.string.form_date), before = before(CorrectionField.OCCURRED_AT)) {
                val other = stringResource(Res.string.form_other_date)
                val otherLabel =
                    state.occurredAtEpochMs
                        ?.takeIf { dateChoice == DateChoice.OTHER }
                        ?.let { "$other · ${TimeFormatter.dayMonthTime(it, utcOffsetMinutes, locale)}" } ?: other
                CdChoice(
                    listOf(
                        CdOption(DateChoice.TODAY, stringResource(Res.string.form_today)),
                        CdOption(DateChoice.YESTERDAY, stringResource(Res.string.form_yesterday)),
                        CdOption(DateChoice.OTHER, otherLabel),
                    ),
                    selected = dateChoice,
                    onSelect = { choice ->
                        when (choice) {
                            DateChoice.TODAY -> {
                                dateChoice = DateChoice.TODAY
                                presenter.onDateChange(null)
                            }
                            DateChoice.YESTERDAY -> {
                                dateChoice = DateChoice.YESTERDAY
                                presenter.onDateChange(nowEpochMs() - DAY_MS)
                            }
                            DateChoice.OTHER -> picking = true
                        }
                    },
                    enabled = enabled,
                )
            }

            CdCollapsible(stringResource(Res.string.form_more_details), initiallyOpen = state.hasDetails) {
                CdField(
                    state.note,
                    presenter::onNoteChange,
                    label = stringResource(Res.string.form_note),
                    before = before(CorrectionField.NOTE),
                    singleLine = false,
                    enabled = enabled,
                )
                CdField(
                    state.reference,
                    presenter::onReferenceChange,
                    label = stringResource(Res.string.form_reference),
                    before = before(CorrectionField.REFERENCE),
                    enabled = enabled,
                )
            }
            val note =
                when (mode) {
                    FormMode.New -> Res.string.form_saved_note
                    is FormMode.Review -> Res.string.form_review_note
                    is FormMode.Correct -> Res.string.form_correct_note
                }
            Text(stringResource(note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.busy) Text(stringResource(Res.string.form_saving))
            if (state.storageError) CdNotice(NoticeKind.Error, stringResource(Res.string.form_storage_error))
            state.problem?.let { CdNotice(NoticeKind.Error, problemText(it, locale)) }
            Actions(presenter, mode, enabled, onSaved, onDone)
        }
    }

    if (askExit) {
        CdDialog(
            title = stringResource(Res.string.form_exit_title),
            text = stringResource(Res.string.form_exit_note),
            onDismiss = { askExit = false },
            confirm =
                CdActionSpec(stringResource(Res.string.form_exit_leave), {
                    askExit = false
                    onBack()
                }),
            dismiss = CdActionSpec(stringResource(Res.string.form_exit_stay), { askExit = false }),
        )
    }
    if (picking) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.occurredAtEpochMs ?: nowEpochMs())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                CdAction(stringResource(Res.string.dialog_ok), {
                    // El selector devuelve la medianoche UTC del día: se guarda el mediodía local de ese día.
                    picker.selectedDateMillis?.let {
                        presenter.onDateChange(it + DAY_MS / 2 - utcOffsetMinutes * 60_000L)
                        dateChoice = DateChoice.OTHER
                    }
                    picking = false
                }, role = ActionRole.Tertiary)
            },
            dismissButton = { CdAction(stringResource(Res.string.dialog_cancel), { picking = false }, role = ActionRole.Tertiary) },
        ) { DatePicker(picker) }
    }
}

/** Las acciones del formulario según el modo, como un grupo de hermanas ([CdActions]). */
@Composable
private fun Actions(
    presenter: DraftFormPresenter,
    mode: FormMode,
    enabled: Boolean,
    onSaved: (CaptureResult) -> Unit,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    fun run(action: suspend () -> Boolean) {
        scope.launch { if (action()) onDone() }
    }
    val actions =
        when (mode) {
            FormMode.New ->
                listOf(
                    CdActionSpec(
                        stringResource(Res.string.form_save),
                        { scope.launch { presenter.save()?.let(onSaved) } },
                        ActionRole.Primary,
                        enabled,
                    ),
                )
            is FormMode.Review ->
                listOf(
                    CdActionSpec(stringResource(Res.string.form_discard), { run(presenter::discard) }, ActionRole.Danger, enabled),
                    CdActionSpec(stringResource(Res.string.form_keep), { run(presenter::keep) }, ActionRole.Secondary, enabled),
                    CdActionSpec(stringResource(Res.string.form_confirm), { run(presenter::confirm) }, ActionRole.Primary, enabled),
                )
            is FormMode.Correct ->
                listOfNotNull(
                    mode.sourceDraftId?.let {
                        CdActionSpec(
                            stringResource(Res.string.same_no),
                            { run(presenter::distinct) },
                            ActionRole.Secondary,
                            enabled,
                        )
                    },
                    mode.sourceDraftId?.let {
                        CdActionSpec(
                            stringResource(Res.string.form_dismiss_change),
                            { run(presenter::discard) },
                            ActionRole.Danger,
                            enabled,
                        )
                    },
                    CdActionSpec(stringResource(Res.string.form_confirm), { run(presenter::confirm) }, ActionRole.Primary, enabled),
                )
        }
    CdActions(actions)
}

/** El valor anterior de un campo que cambia: "antes: 35,00 Bs" o "antes: vacío". */
@Composable
fun beforeText(
    change: FieldChange,
    wallets: List<FormWallet>,
    customCategories: List<FormCustomCategory> = emptyList(),
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
                    is CategoryRef.Custom ->
                        customCategories.firstOrNull { it.id == c.id }?.name
                            ?: stringResource(Res.string.spending_category_missing)
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
