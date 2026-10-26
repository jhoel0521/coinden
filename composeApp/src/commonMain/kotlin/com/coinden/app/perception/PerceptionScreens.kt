package com.coinden.app.perception

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.model_base
import coinden.composeapp.generated.resources.model_large
import coinden.composeapp.generated.resources.model_small
import coinden.composeapp.generated.resources.models_delete
import coinden.composeapp.generated.resources.models_download
import coinden.composeapp.generated.resources.models_failed
import coinden.composeapp.generated.resources.models_installed
import coinden.composeapp.generated.resources.models_intro
import coinden.composeapp.generated.resources.models_recommended
import coinden.composeapp.generated.resources.models_size
import coinden.composeapp.generated.resources.models_slow
import coinden.composeapp.generated.resources.models_system
import coinden.composeapp.generated.resources.models_title
import coinden.composeapp.generated.resources.privacy_continue
import coinden.composeapp.generated.resources.privacy_downloads
import coinden.composeapp.generated.resources.privacy_google
import coinden.composeapp.generated.resources.privacy_google_choose_no
import coinden.composeapp.generated.resources.privacy_google_choose_yes
import coinden.composeapp.generated.resources.privacy_google_link
import coinden.composeapp.generated.resources.privacy_google_never
import coinden.composeapp.generated.resources.privacy_google_receives
import coinden.composeapp.generated.resources.privacy_models
import coinden.composeapp.generated.resources.privacy_public
import coinden.composeapp.generated.resources.privacy_stays
import coinden.composeapp.generated.resources.privacy_title
import com.coinden.app.lock.AppLockController
import com.coinden.app.lock.LockSwitch
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.BadgeTone
import com.coinden.app.ui.components.CardTone
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdBadge
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdErrorText
import com.coinden.app.ui.components.CdHeaderAction
import com.coinden.app.ui.components.CdNotice
import com.coinden.app.ui.components.CdSwitchRow
import com.coinden.app.ui.components.CdTopBar
import com.coinden.app.ui.components.NoticeKind
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** La lista que publica Google de lo que recoge su lector: la persona lo comprueba en la fuente. */
private const val GOOGLE_DATA_URL = "https://developers.google.com/ml-kit/android-data-disclosure"

/**
 * Qué sale del teléfono y la elección del lector de Google (apagado por defecto). Se ve en el
 * onboarding, una vez a quien ya lo terminó, y desde Inicio.
 */
@Composable
fun PrivacyScreen(
    presenter: PrivacyPresenter,
    onContinue: () -> Unit,
    onVoiceModels: () -> Unit,
    lock: AppLockController? = null,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(presenter) { presenter.load() }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(Res.string.privacy_title), style = MaterialTheme.typography.headlineSmall)
            Point(CoinDenIcon.LOCK, Res.string.privacy_stays)
            Point(CoinDenIcon.BANK, Res.string.privacy_public)
            Point(CoinDenIcon.SHIELD, Res.string.privacy_downloads)
            CdCard(tone = CardTone.Raised) {
                // Quien ya eligió lo cambia con el interruptor de siempre. Quien todavía no,
                // elige abajo: un interruptor no distingue "lo dejó apagado" de "no lo miró".
                if (state.chosen) {
                    CdSwitchRow(
                        stringResource(Res.string.privacy_google),
                        state.googleReader,
                        { enabled -> scope.launch { presenter.chooseGoogleReader(enabled) } },
                    )
                } else {
                    Text(stringResource(Res.string.privacy_google), style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(Res.string.privacy_google_receives), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(Res.string.privacy_google_never), style = MaterialTheme.typography.bodyMedium)
                CdActions(
                    listOf(
                        CdActionSpec(
                            stringResource(Res.string.privacy_google_link),
                            { uriHandler.openUri(GOOGLE_DATA_URL) },
                            ActionRole.Tertiary,
                        ),
                    ),
                )
            }
            lock?.let {
                val lockState by it.state.collectAsState()
                val availability by it.availability.collectAsState()
                CdCard(tone = CardTone.Raised) {
                    LockSwitch(lockState, availability, { enabled -> scope.launch { it.setEnabled(enabled) } }, it::openScreenLockSettings)
                }
            }
            CdActions(listOf(CdActionSpec(stringResource(Res.string.privacy_models), onVoiceModels)))
        }
        // Sin elección no se avanza: las dos salidas de esta pantalla son elegir.
        CdActions(
            if (state.chosen) {
                listOf(CdActionSpec(stringResource(Res.string.privacy_continue), onContinue, ActionRole.Primary))
            } else {
                listOf(
                    CdActionSpec(
                        stringResource(Res.string.privacy_google_choose_yes),
                        {
                            scope.launch {
                                presenter.chooseGoogleReader(true)
                                onContinue()
                            }
                        },
                        ActionRole.Primary,
                    ),
                    CdActionSpec(
                        stringResource(Res.string.privacy_google_choose_no),
                        {
                            scope.launch {
                                presenter.chooseGoogleReader(false)
                                onContinue()
                            }
                        },
                        ActionRole.Secondary,
                    ),
                )
            },
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun Point(
    icon: CoinDenIcon,
    text: StringResource,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon.vector, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Modelos de voz: el recomendado para este teléfono, los otros con su tamaño, descargar y borrar. */
@Composable
fun VoiceModelsScreen(
    presenter: VoiceModelsPresenter,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(presenter) { presenter.load() }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CdTopBar(stringResource(Res.string.models_title), stringResource(Res.string.back), onBack)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.models_intro), style = MaterialTheme.typography.bodyMedium)
            if (state.recommendation == VoiceRecommendation.SystemRecognizer) {
                CdNotice(NoticeKind.Info, stringResource(Res.string.models_system))
            }
            state.models.forEach { row ->
                CdCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(
                                Res.string.models_size,
                                stringResource(row.model.label()),
                                (row.model.sizeBytes / 1_000_000).toInt(),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (row.recommended) CdBadge(stringResource(Res.string.models_recommended), BadgeTone.Success)
                    }
                    if (!row.recommended && row.model.sizeBytes > recommendedSize(state)) {
                        Text(stringResource(Res.string.models_slow), style = MaterialTheme.typography.bodySmall)
                    }
                    when {
                        state.downloading == row.model ->
                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                        row.installed ->
                            CdHeaderAction(
                                stringResource(Res.string.models_installed),
                                CdActionSpec(stringResource(Res.string.models_delete), { presenter.delete(row.model) }, ActionRole.Danger),
                            )
                        else ->
                            CdActions(
                                listOf(
                                    CdActionSpec(
                                        stringResource(Res.string.models_download),
                                        { scope.launch { presenter.download(row.model) } },
                                        enabled = state.downloading == null,
                                    ),
                                ),
                            )
                    }
                    if (state.failed == row.model) CdErrorText(stringResource(Res.string.models_failed))
                }
            }
        }
    }
}

/** Nombre del modelo para la persona (el id técnico no se muestra). */
private fun VoiceModel.label(): StringResource =
    when (this) {
        VoiceModels.BASE -> Res.string.model_base
        VoiceModels.SMALL -> Res.string.model_small
        else -> Res.string.model_large
    }

private fun recommendedSize(state: VoiceModelsState): Long = (state.recommendation as? VoiceRecommendation.Download)?.model?.sizeBytes ?: 0L
