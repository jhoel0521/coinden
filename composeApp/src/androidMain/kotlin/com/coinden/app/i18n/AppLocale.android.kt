package com.coinden.app.i18n

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Patrón de Compose Multiplatform para cambiar el idioma de la app en Android: se fija el locale
 * por defecto y una configuración con ese idioma para los recursos.
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
            val locale = Locale.forLanguageTag(language)
            Locale.setDefault(locale)
            Configuration(base).apply { setLocale(locale) }.also {
                @Suppress("DEPRECATION")
                context.resources.updateConfiguration(it, context.resources.displayMetrics)
            }
        }
    CompositionLocalProvider(LocalConfiguration provides configuration) {
        key(language) { content() }
    }
}
