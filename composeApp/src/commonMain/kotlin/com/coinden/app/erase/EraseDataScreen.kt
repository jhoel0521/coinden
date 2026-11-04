package com.coinden.app.erase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.dialog_cancel
import coinden.composeapp.generated.resources.erase_authenticating
import coinden.composeapp.generated.resources.erase_confirm
import coinden.composeapp.generated.resources.erase_device
import coinden.composeapp.generated.resources.erase_done
import coinden.composeapp.generated.resources.erase_erasing
import coinden.composeapp.generated.resources.erase_failed
import coinden.composeapp.generated.resources.erase_kept
import coinden.composeapp.generated.resources.erase_retry
import coinden.composeapp.generated.resources.erase_title
import coinden.composeapp.generated.resources.erase_undo
import coinden.composeapp.generated.resources.erase_what
import com.coinden.app.capture.FormBackHandler
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdNotice
import com.coinden.app.ui.components.CdTopBar
import com.coinden.app.ui.components.NoticeKind
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Lo que dice la confirmación, en orden: qué se borra, que no se deshace, dónde, y qué se conserva. */
object EraseTexts {
    val confirmation = listOf(Res.string.erase_what, Res.string.erase_undo, Res.string.erase_device, Res.string.erase_kept)
}

/**
 * Confirmación de "Borrar mis datos". Cancelar vuelve a Perfil sin borrar nada. Si el borrado se
 * corta, la pantalla no deja volver a la app con datos a medias: solo ofrece reintentar.
 */
@Composable
fun EraseDataScreen(
    presenter: EraseDataPresenter,
    onBack: () -> Unit,
) {
    val step by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(presenter) { presenter.open() }
    val cancel: () -> Unit = {
        presenter.cancel()
        onBack()
    }
    // Atrás cancela solo mientras se puede cancelar; durante y después del borrado no hace nada.
    FormBackHandler(enabled = true) { if (step == EraseStep.Confirming) cancel() }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CdTopBar(
            stringResource(Res.string.erase_title),
            stringResource(Res.string.back),
            onBack = cancel.takeIf { step == EraseStep.Confirming },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (step) {
                EraseStep.Idle, EraseStep.Confirming, EraseStep.Authenticating -> {
                    EraseTexts.confirmation.forEachIndexed { index, text ->
                        // El "no se puede deshacer" va como aviso: es lo que más importa leer.
                        if (index == 1) {
                            CdNotice(NoticeKind.Warning, stringResource(text))
                        } else {
                            Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (step == EraseStep.Authenticating) {
                        Text(stringResource(Res.string.erase_authenticating), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                EraseStep.Erasing -> Text(stringResource(Res.string.erase_erasing), style = MaterialTheme.typography.bodyLarge)
                EraseStep.Failed ->
                    CdNotice(
                        NoticeKind.Error,
                        stringResource(Res.string.erase_failed),
                        action =
                            CdActionSpec(
                                stringResource(Res.string.erase_retry),
                                { scope.launch { presenter.retry() } },
                                ActionRole.Danger,
                            ),
                    )
                EraseStep.Done -> Text(stringResource(Res.string.erase_done), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (step == EraseStep.Confirming || step == EraseStep.Authenticating) {
            val ready = step == EraseStep.Confirming
            CdActions(
                listOf(
                    CdActionSpec(stringResource(Res.string.dialog_cancel), cancel, ActionRole.Tertiary, enabled = ready),
                    CdActionSpec(
                        stringResource(Res.string.erase_confirm),
                        { scope.launch { presenter.confirm() } },
                        ActionRole.Danger,
                        enabled = ready,
                    ),
                ),
                Modifier.padding(16.dp),
            )
        }
    }
}
