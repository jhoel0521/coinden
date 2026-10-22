package com.coinden.app.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.licenses_hint
import coinden.composeapp.generated.resources.licenses_terms
import coinden.composeapp.generated.resources.licenses_title
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdTopBar
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

/** Licencias de código abierto: cada librería con su licencia; el texto completo se lee sin conexión. */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var reading by remember { mutableStateOf<OpenSourceLibrary?>(null) }
    val open = reading
    val file = open?.textFile
    if (open != null && file != null) {
        var text by remember(open) { mutableStateOf("") }
        LaunchedEffect(open) { text = OpenSourceLicenses.paragraphs(Res.readBytes("files/licenses/$file").decodeToString()) }
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            CdTopBar(open.name, stringResource(Res.string.back), { reading = null })
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                item { Text(text, style = MaterialTheme.typography.bodySmall) }
            }
        }
        return
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CdTopBar(stringResource(Res.string.licenses_title), stringResource(Res.string.back), onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    stringResource(Res.string.licenses_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(OpenSourceLicenses.all, key = { it.name }) { library ->
                // Con texto se lee aquí mismo; sin texto (no es software libre) se abren sus términos.
                CdCard(onClick = { if (library.textFile != null) reading = library else uriHandler.openUri(library.url) }) {
                    Text(library.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (library.textFile != null) library.license else stringResource(Res.string.licenses_terms, library.license),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
