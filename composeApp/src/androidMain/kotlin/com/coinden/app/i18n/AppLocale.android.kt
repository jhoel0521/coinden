package com.coinden.app.i18n

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Contexto de recursos con el idioma elegido. Conserva la región y el árbol de navegación;
 * no modifica los recursos globales del teléfono ni reinicia los formularios al traducir.
 */
@Composable
actual fun ProvideAppLocale(
    language: String,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base = LocalConfiguration.current
    val configuration =
        remember(language, base) {
            val region = base.locales[0]?.country.orEmpty()
            val locale =
                Locale
                    .Builder()
                    .setLanguage(language)
                    .setRegion(region)
                    .build()
            // Compose Resources también consulta Locale.current; actualizar el proceso antes de componer.
            Locale.setDefault(locale)
            Configuration(base).apply { setLocale(locale) }
        }
    val localized =
        remember(context, configuration) {
            // Preservar la cadena hasta Activity para permisos, cámara y ActivityResultRegistry.
            ContextThemeWrapper(context, 0).apply {
                applyOverrideConfiguration(configuration)
                theme.setTo(context.theme)
            }
        }
    CompositionLocalProvider(LocalConfiguration provides configuration, LocalContext provides localized) {
        content()
    }
}
