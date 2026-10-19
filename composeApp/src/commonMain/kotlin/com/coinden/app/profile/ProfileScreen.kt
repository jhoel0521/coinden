package com.coinden.app.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.home_language
import coinden.composeapp.generated.resources.home_privacy
import coinden.composeapp.generated.resources.language_en
import coinden.composeapp.generated.resources.language_es
import coinden.composeapp.generated.resources.language_pt
import coinden.composeapp.generated.resources.models_title
import coinden.composeapp.generated.resources.profile_about
import coinden.composeapp.generated.resources.profile_about_text
import coinden.composeapp.generated.resources.profile_security
import coinden.composeapp.generated.resources.profile_settings
import coinden.composeapp.generated.resources.profile_title
import coinden.composeapp.generated.resources.settings_title
import com.coinden.app.lock.AppLockController
import com.coinden.app.lock.LockSwitch
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdChoice
import com.coinden.app.ui.components.CdListOption
import com.coinden.app.ui.components.CdOption
import com.coinden.app.ui.components.CdSection
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val LANGUAGES = listOf("es" to Res.string.language_es, "en" to Res.string.language_en, "pt" to Res.string.language_pt)

/** Mi perfil: seguridad, idioma, configuración y acerca de. Sin cuenta: todo vive en el teléfono. */
@Composable
fun ProfileScreen(
    lock: AppLockController,
    language: String,
    appVersion: String,
    onChooseLanguage: suspend (String) -> Unit,
    onWallets: () -> Unit,
    onPrivacy: () -> Unit,
    onVoiceModels: () -> Unit,
) {
    val lockState by lock.state.collectAsState()
    val availability by lock.availability.collectAsState()
    val scope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Text(stringResource(Res.string.profile_title), style = MaterialTheme.typography.headlineSmall) }
        item {
            CdSection(stringResource(Res.string.profile_security)) {
                CdCard {
                    LockSwitch(
                        lockState,
                        availability,
                        { enabled -> scope.launch { lock.setEnabled(enabled) } },
                        lock::openScreenLockSettings,
                    )
                }
            }
        }
        item {
            CdSection(stringResource(Res.string.home_language)) {
                CdChoice(
                    LANGUAGES.map { (code, name) -> CdOption(code, stringResource(name)) },
                    selected = language,
                    onSelect = { code -> scope.launch { onChooseLanguage(code) } },
                )
            }
        }
        item {
            CdSection(stringResource(Res.string.profile_settings)) {
                CdListOption(stringResource(Res.string.settings_title), onWallets)
                CdListOption(stringResource(Res.string.home_privacy), onPrivacy)
                CdListOption(stringResource(Res.string.models_title), onVoiceModels)
            }
        }
        item {
            CdSection(stringResource(Res.string.profile_about)) {
                Text(
                    stringResource(Res.string.profile_about_text, appVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
