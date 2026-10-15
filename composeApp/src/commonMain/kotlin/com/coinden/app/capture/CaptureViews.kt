package com.coinden.app.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.bar_dictate
import coinden.composeapp.generated.resources.bar_listening
import coinden.composeapp.generated.resources.bar_photo
import coinden.composeapp.generated.resources.bar_write
import coinden.composeapp.generated.resources.notice_camera_denied
import coinden.composeapp.generated.resources.notice_dictation_unavailable
import coinden.composeapp.generated.resources.notice_dismiss
import coinden.composeapp.generated.resources.notice_microphone_denied
import coinden.composeapp.generated.resources.result_confirm
import coinden.composeapp.generated.resources.result_confirmed
import coinden.composeapp.generated.resources.result_done
import coinden.composeapp.generated.resources.result_not_ready
import coinden.composeapp.generated.resources.result_rejected
import coinden.composeapp.generated.resources.result_saved
import coinden.composeapp.generated.resources.result_shortfall
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.theme.CoinDenIcon
import com.coinden.core.domain.model.interpret.UserLocale
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Los tres botones de captura, siempre visibles al pie de Inicio, al alcance del pulgar. */
@Composable
fun CaptureBar(
    listening: Boolean,
    onDictate: () -> Unit,
    onPhoto: () -> Unit,
    onWrite: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (listening) {
            Button(onClick = onDictate, modifier = Modifier.weight(1f).heightIn(min = 64.dp), shape = MaterialTheme.shapes.large) {
                Icon(CoinDenIcon.STOP.vector, contentDescription = null, modifier = Modifier.size(22.dp))
                Text(stringResource(Res.string.bar_listening), modifier = Modifier.padding(start = 8.dp))
            }
        } else {
            BarButton(CoinDenIcon.MIC, Res.string.bar_dictate, onDictate, primary = true, modifier = Modifier.weight(1f))
            BarButton(CoinDenIcon.CAMERA, Res.string.bar_photo, onPhoto, primary = true, modifier = Modifier.weight(1f))
            BarButton(CoinDenIcon.PENCIL, Res.string.bar_write, onWrite, primary = false, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BarButton(
    icon: CoinDenIcon,
    label: StringResource,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier,
) {
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon.vector, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        }
    }
    val sized = modifier.heightIn(min = 64.dp)
    if (primary) {
        Button(onClick = onClick, modifier = sized, shape = MaterialTheme.shapes.large) { content() }
    } else {
        FilledTonalButton(onClick = onClick, modifier = sized, shape = MaterialTheme.shapes.large) { content() }
    }
}

/** Aviso de captura (dictado no disponible, permiso negado) con el porqué. */
@Composable
fun CaptureNoticeCard(
    notice: CaptureNotice,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.medium,
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(notice.label()), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.notice_dismiss)) }
        }
    }
}

/**
 * Después de capturar: "Guardado como borrador", la tarjeta y la opción de confirmarlo ya.
 * Cerrar la hoja lo deja pendiente para la revisión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureResultSheet(
    result: CaptureResult,
    locale: UserLocale,
    onConfirm: (draftId: String) -> Unit,
    onDone: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDone, sheetState = sheet) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(CoinDenIcon.CHECK_CIRCLE.vector, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(Res.string.result_saved), style = MaterialTheme.typography.titleLarge)
            }
            result.cards.forEach { card ->
                DraftCardView(card, locale)
                val confirmed = card.draftId in result.confirmed
                when {
                    confirmed -> Outcome(stringResource(Res.string.result_confirmed))
                    else -> result.problems[card.draftId]?.let { Outcome(problemText(it, locale)) }
                }
                if (!confirmed && card.status == DraftStatusView.READY) {
                    Button(
                        onClick = { onConfirm(card.draftId) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.large,
                    ) { Text(stringResource(Res.string.result_confirm), style = MaterialTheme.typography.titleMedium) }
                }
            }
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(bottom = 8.dp),
                shape = MaterialTheme.shapes.large,
            ) { Text(stringResource(Res.string.result_done), style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun Outcome(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun problemText(
    problem: ConfirmProblem,
    locale: UserLocale,
): String =
    when (problem) {
        is ConfirmProblem.InsufficientFunds -> stringResource(Res.string.result_shortfall, MoneyFormatter.format(problem.shortfall, locale))
        ConfirmProblem.NotReady -> stringResource(Res.string.result_not_ready)
        ConfirmProblem.Rejected -> stringResource(Res.string.result_rejected)
    }

private fun CaptureNotice.label(): StringResource =
    when (this) {
        CaptureNotice.DICTATION_UNAVAILABLE -> Res.string.notice_dictation_unavailable
        CaptureNotice.MICROPHONE_DENIED -> Res.string.notice_microphone_denied
        CaptureNotice.CAMERA_DENIED -> Res.string.notice_camera_denied
    }
