package com.coinden.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.coinden_icon
import coinden.composeapp.generated.resources.coinden_logo
import coinden.composeapp.generated.resources.logo_description
import coinden.composeapp.generated.resources.welcome_greeting
import coinden.composeapp.generated.resources.wordmark_coin
import coinden.composeapp.generated.resources.wordmark_den
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Logo completo de la bienvenida: la ilustración transparente (búho, cofre y monedas, con sus
 * contornos oscuros) y, debajo, "CoinDen" y el lema como texto real y traducido.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painterResource(Res.drawable.coinden_logo),
            contentDescription = stringResource(Res.string.logo_description),
            modifier = Modifier.size(size),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Wordmark(MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(Res.string.welcome_greeting),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Marca chica para cabeceras: el ícono de la app y "CoinDen" en texto. */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Image(
            painterResource(Res.drawable.coinden_icon),
            contentDescription = stringResource(Res.string.logo_description),
            modifier = Modifier.size(40.dp),
        )
        Wordmark(MaterialTheme.typography.headlineSmall)
    }
}

/** "Coin" + "Den" con "Den" en el color principal: texto real con la tipografía de la app. */
@Composable
private fun Wordmark(style: androidx.compose.ui.text.TextStyle) {
    val primary = MaterialTheme.colorScheme.primary
    val coin = stringResource(Res.string.wordmark_coin)
    val den = stringResource(Res.string.wordmark_den)
    Text(
        buildAnnotatedString {
            append(coin)
            withStyle(SpanStyle(color = primary)) { append(den) }
        },
        style = style,
    )
}
