package com.coinden.app.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.wordmark_coin
import coinden.composeapp.generated.resources.wordmark_den
import org.jetbrains.compose.resources.stringResource

/** Isotipo del diseño (la "guarida": orejas, cara y ojos con la C y la D) en los colores del tema. */
@Composable
fun CoinDenLogo(
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val vector =
        remember(scheme.primary, scheme.surfaceContainerLowest, scheme.onPrimaryContainer) {
            logo(scheme.primary, scheme.surfaceContainerLowest, scheme.onPrimaryContainer)
        }
    Image(vector, contentDescription = contentDescription, modifier = modifier.size(size))
}

/** Logotipo: isotipo y "Coin" + "Den" en el color principal. */
@Composable
fun CoinDenWordmark(contentDescription: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CoinDenLogo(contentDescription)
        val primary = MaterialTheme.colorScheme.primary
        val coin = stringResource(Res.string.wordmark_coin)
        val den = stringResource(Res.string.wordmark_den)
        Text(
            buildAnnotatedString {
                append(coin)
                withStyle(SpanStyle(color = primary)) { append(den) }
            },
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

private fun logo(
    primary: Color,
    eyes: Color,
    mouth: Color,
): ImageVector {
    val fill = SolidColor(primary)
    return ImageVector
        .Builder(name = "CoinDenLogo", defaultWidth = 36.dp, defaultHeight = 36.dp, viewportWidth = 120f, viewportHeight = 120f)
        .addPath(addPathNodes("M22 40 L27 10 L46 25 Z"), fill = fill)
        .addPath(addPathNodes("M98 40 L93 10 L74 25 Z"), fill = fill)
        .addPath(addPathNodes("M14 64 a46 46 0 1 0 92 0 a46 46 0 1 0 -92 0 Z"), fill = fill)
        .addPath(addPathNodes("M28 56 a14 14 0 1 0 28 0 a14 14 0 1 0 -28 0 Z"), fill = SolidColor(eyes))
        .addPath(addPathNodes("M64 56 a14 14 0 1 0 28 0 a14 14 0 1 0 -28 0 Z"), fill = SolidColor(eyes))
        .addPath(
            addPathNodes("M47.5 50.5 A8 8 0 1 0 47.5 61.5"),
            stroke = fill,
            strokeLineWidth = 5f,
            strokeLineCap = StrokeCap.Round,
        ).addPath(
            addPathNodes("M74 49 V63 H76 A7 7 0 0 0 76 49 Z"),
            stroke = fill,
            strokeLineWidth = 4.5f,
            strokeLineJoin = StrokeJoin.Round,
        ).addPath(addPathNodes("M46 104 V95 A14 14 0 0 1 74 95 V104 Z"), fill = SolidColor(mouth))
        .build()
}
