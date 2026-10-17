package com.coinden.app.perception

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import coinden.composeapp.generated.resources.privacy_google_never
import coinden.composeapp.generated.resources.privacy_google_receives
import coinden.composeapp.generated.resources.privacy_models
import coinden.composeapp.generated.resources.privacy_public
import coinden.composeapp.generated.resources.privacy_stays
import coinden.composeapp.generated.resources.privacy_title
import com.coinden.app.theme.CoinDenIcon
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Qué sale del teléfono y la elección del lector de Google (apagado por defecto). Se ve en el
 * onboarding, una vez a quien ya lo terminó, y desde Inicio.
 */
@Composable
fun PrivacyScreen(
    presenter: PrivacyPresenter,
    onContinue: () -> Unit,
    onVoiceModels: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
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
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(Res.string.privacy_google),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = state.googleReader, onCheckedChange = presenter::onGoogleReaderChange)
                    }
                    Text(stringResource(Res.string.privacy_google_receives), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(Res.string.privacy_google_never), style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedButton(onClick = onVoiceModels, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.privacy_models))
            }
        }
        Button(
            onClick = {
                scope.launch {
                    presenter.onContinue()
                    onContinue()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 60.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) { Text(stringResource(Res.string.privacy_continue), style = MaterialTheme.typography.titleMedium) }
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

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(CoinDenIcon.CLOSE.vector, stringResource(Res.string.back)) }
            Text(stringResource(Res.string.models_title), style = MaterialTheme.typography.titleLarge)
        }
        Text(stringResource(Res.string.models_intro), style = MaterialTheme.typography.bodyMedium)
        if (state.recommendation == VoiceRecommendation.SystemRecognizer) {
            Text(
                stringResource(Res.string.models_system),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        state.models.forEach { row ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(
                            Res.string.models_size,
                            stringResource(row.model.label()),
                            (row.model.sizeBytes / 1_000_000).toInt(),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (row.recommended) Text(stringResource(Res.string.models_recommended), color = MaterialTheme.colorScheme.primary)
                    if (!row.recommended && row.model.sizeBytes > recommendedSize(state)) {
                        Text(stringResource(Res.string.models_slow), style = MaterialTheme.typography.bodySmall)
                    }
                    when {
                        state.downloading == row.model ->
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        row.installed ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(Res.string.models_installed), modifier = Modifier.weight(1f))
                                OutlinedButton(
                                    onClick = { presenter.delete(row.model) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text(stringResource(Res.string.models_delete)) }
                            }
                        else ->
                            Button(
                                onClick = { scope.launch { presenter.download(row.model) } },
                                enabled = state.downloading == null,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(stringResource(Res.string.models_download)) }
                    }
                    if (state.failed == row.model) Text(stringResource(Res.string.models_failed), color = MaterialTheme.colorScheme.error)
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
