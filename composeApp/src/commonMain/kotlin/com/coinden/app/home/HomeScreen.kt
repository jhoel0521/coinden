package com.coinden.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import coinden.composeapp.generated.resources.home_default
import coinden.composeapp.generated.resources.home_partial
import coinden.composeapp.generated.resources.home_pending
import coinden.composeapp.generated.resources.home_pending_open
import coinden.composeapp.generated.resources.home_total
import coinden.composeapp.generated.resources.home_unconverted
import coinden.composeapp.generated.resources.home_wallets
import com.coinden.app.format.MoneyFormatter
import com.coinden.app.onboarding.icon
import com.coinden.app.onboarding.label
import com.coinden.app.theme.CoinDenTheme
import com.coinden.app.ui.LoadNotice
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.BrandMark
import com.coinden.app.ui.components.CardTone
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdNotice
import com.coinden.app.ui.components.CdSection
import com.coinden.app.ui.components.NoticeKind
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Inicio: el patrimonio, cuántos borradores esperan revisión y las billeteras. Nada más. */
@Composable
fun HomeScreen(
    presenter: HomePresenter,
    locale: UserLocale,
    onReview: () -> Unit,
    reload: Any? = null,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    // Cada captura o confirmación cambia [reload]: Inicio se recarga (pendientes y saldos).
    LaunchedEffect(presenter, reload) { presenter.load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { BrandMark() }
        item { LoadNotice(state.load, state.loaded, onRetry = { scope.launch { presenter.load() } }) }
        if (state.loaded) {
            item {
                CdCard(tone = CardTone.Accent) {
                    Text(
                        stringResource(if (state.partial) Res.string.home_partial else Res.string.home_total),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(state.total?.let { MoneyFormatter.format(it, locale) }.orEmpty(), style = CoinDenTheme.amounts.amountLarge)
                    if (state.partial) {
                        val apart = state.unconverted.joinToString(" · ") { MoneyFormatter.format(it, locale) }
                        Text(stringResource(Res.string.home_unconverted, apart), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (state.pendingDrafts > 0) {
                item {
                    CdNotice(
                        NoticeKind.Info,
                        stringResource(Res.string.home_pending, state.pendingDrafts),
                        action = CdActionSpec(stringResource(Res.string.home_pending_open), onReview, ActionRole.Primary),
                    )
                }
            }
            item { CdSection(stringResource(Res.string.home_wallets)) {} }
            items(state.wallets, key = { it.id }) { wallet -> WalletRow(wallet, locale) }
        }
    }
}

@Composable
private fun WalletRow(
    wallet: HomeWallet,
    locale: UserLocale,
) {
    val scheme = MaterialTheme.colorScheme
    CdCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(scheme.secondaryContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    wallet.type.icon().vector,
                    contentDescription = null,
                    tint = scheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(wallet.name, style = MaterialTheme.typography.titleMedium)
                val sub =
                    stringResource(wallet.type.label()) + if (wallet.isDefault) " · " + stringResource(Res.string.home_default) else ""
                Text(sub, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Text(MoneyFormatter.format(wallet.balance, locale), style = CoinDenTheme.amounts.amount)
        }
    }
}
