package com.coinden.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.bricolage_grotesque
import coinden.composeapp.generated.resources.manrope
import org.jetbrains.compose.resources.Font

/** Colores propios de CoinDen que Material 3 no tiene: éxito y advertencia (confianza alta y media). */
@Immutable
data class CoinDenExtraColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

/** Estilos de montos: números tabulares y alineados. */
@Immutable
data class CoinDenAmountStyles(
    val amount: TextStyle,
    val amountLarge: TextStyle,
)

val LocalCoinDenColors = staticCompositionLocalOf { CoinDenPalette.Cobre.extraColors() }
val LocalCoinDenAmounts = staticCompositionLocalOf { CoinDenAmountStyles(TextStyle.Default, TextStyle.Default) }

/** Acceso corto desde las pantallas: `CoinDenTheme.colors.success`, `CoinDenTheme.amounts.amount`. */
object CoinDenTheme {
    val colors: CoinDenExtraColors @Composable get() = LocalCoinDenColors.current
    val amounts: CoinDenAmountStyles @Composable get() = LocalCoinDenAmounts.current
}

/** Tema "Cobre" en modo claro y "Cobre noche" en modo oscuro, según el sistema. */
@Composable
fun CoinDenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) CoinDenPalette.CobreNoche else CoinDenPalette.Cobre
    val display = displayFamily()
    val body = bodyFamily()
    val tabular = "tnum, lnum"
    val amounts =
        CoinDenAmountStyles(
            amount = TextStyle(fontFamily = body, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, fontFeatureSettings = tabular),
            amountLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, fontFeatureSettings = tabular),
        )
    CompositionLocalProvider(
        LocalCoinDenColors provides palette.extraColors(),
        LocalCoinDenAmounts provides amounts,
    ) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = typography(display, body),
            shapes = CoinDenShapes,
            content = content,
        )
    }
}

/** Radios "suaves" del diseño. */
val CoinDenShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

private val WEIGHTS = listOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)

/** Bricolage Grotesque (variable): títulos y logotipo. */
@Composable
private fun displayFamily(): FontFamily =
    FontFamily(
        WEIGHTS.map { w ->
            Font(Res.font.bricolage_grotesque, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
        },
    )

/** Manrope (variable): cuerpo, etiquetas y montos. */
@Composable
private fun bodyFamily(): FontFamily =
    FontFamily(WEIGHTS.map { w -> Font(Res.font.manrope, w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight))) })

private fun typography(
    display: FontFamily,
    body: FontFamily,
): Typography {
    fun style(
        family: FontFamily,
        size: Int,
        weight: FontWeight,
        line: Double,
    ) = TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp, lineHeight = (size * line).sp)
    return Typography(
        displayLarge = style(display, 44, FontWeight.ExtraBold, 1.15),
        displayMedium = style(display, 40, FontWeight.ExtraBold, 1.15),
        displaySmall = style(display, 36, FontWeight.ExtraBold, 1.15),
        headlineLarge = style(display, 30, FontWeight.Bold, 1.2),
        headlineMedium = style(display, 26, FontWeight.Bold, 1.2),
        headlineSmall = style(display, 22, FontWeight.Bold, 1.25),
        titleLarge = style(display, 20, FontWeight.Bold, 1.3),
        titleMedium = style(body, 17, FontWeight.ExtraBold, 1.3),
        titleSmall = style(body, 15, FontWeight.ExtraBold, 1.3),
        bodyLarge = style(body, 16, FontWeight.Normal, 1.45),
        bodyMedium = style(body, 14, FontWeight.Normal, 1.4),
        bodySmall = style(body, 12, FontWeight.SemiBold, 1.35),
        labelLarge = style(body, 14, FontWeight.ExtraBold, 1.3),
        labelMedium = style(body, 12, FontWeight.ExtraBold, 1.3),
        labelSmall = style(body, 11, FontWeight.Bold, 1.3),
    )
}

private fun color(argb: Long) = Color(argb.toInt())

internal fun CoinDenPalette.extraColors() =
    CoinDenExtraColors(
        success = color(success),
        successContainer = color(successContainer),
        onSuccessContainer = color(onSuccessContainer),
        warning = color(warning),
        warningContainer = color(warningContainer),
        onWarningContainer = color(onWarningContainer),
    )

internal fun CoinDenPalette.toColorScheme(): ColorScheme {
    val scheme = if (isDark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = color(primary),
        onPrimary = color(onPrimary),
        primaryContainer = color(primaryContainer),
        onPrimaryContainer = color(onPrimaryContainer),
        secondary = color(secondary),
        onSecondary = color(onSecondary),
        secondaryContainer = color(secondaryContainer),
        onSecondaryContainer = color(onSecondaryContainer),
        tertiary = color(tertiary),
        onTertiary = color(onTertiary),
        tertiaryContainer = color(tertiaryContainer),
        onTertiaryContainer = color(onTertiaryContainer),
        error = color(error),
        onError = color(onError),
        errorContainer = color(errorContainer),
        onErrorContainer = color(onErrorContainer),
        background = color(surface),
        onBackground = color(onSurface),
        surface = color(surface),
        onSurface = color(onSurface),
        surfaceVariant = color(surfaceContainerHighest),
        onSurfaceVariant = color(onSurfaceVariant),
        outline = color(outline),
        outlineVariant = color(outlineVariant),
        surfaceContainerLowest = color(surfaceContainerLowest),
        surfaceContainerLow = color(surfaceContainerLow),
        surfaceContainer = color(surfaceContainer),
        surfaceContainerHigh = color(surfaceContainerHigh),
        surfaceContainerHighest = color(surfaceContainerHighest),
        inverseSurface = color(inverseSurface),
        inverseOnSurface = color(inverseOnSurface),
        inversePrimary = color(inversePrimary),
    )
}
