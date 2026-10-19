package com.coinden.app.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.lock_lost
import coinden.composeapp.generated.resources.lock_no_screen_lock
import coinden.composeapp.generated.resources.lock_open_settings
import coinden.composeapp.generated.resources.lock_switch
import coinden.composeapp.generated.resources.lock_switch_note
import coinden.composeapp.generated.resources.lock_unsupported
import coinden.composeapp.generated.resources.locked_note
import coinden.composeapp.generated.resources.locked_title
import coinden.composeapp.generated.resources.locked_unlock
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdSwitchRow
import org.jetbrains.compose.resources.stringResource

/** Lo que se ve en una pestaña protegida mientras la app está bloqueada: ningún monto. */
@Composable
fun LockedPanel(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(CoinDenIcon.LOCK.vector, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(48.dp))
        Text(stringResource(Res.string.locked_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            stringResource(Res.string.locked_note),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        CdActions(
            listOf(CdActionSpec(stringResource(Res.string.locked_unlock), onUnlock, ActionRole.Primary, icon = CoinDenIcon.LOCK.vector)),
        )
    }
}

/** El interruptor del bloqueo, con su explicación y por qué no se puede usar si es el caso. */
@Composable
fun LockSwitch(
    state: AppLockState,
    availability: AuthAvailability,
    onChange: (Boolean) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val available = availability == AuthAvailability.Available
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CdSwitchRow(
            stringResource(Res.string.lock_switch),
            checked = state.enabled,
            onCheckedChange = { if (available) onChange(it) },
            text = stringResource(Res.string.lock_switch_note),
        )
        val problem =
            when {
                state.lostScreenLock -> Res.string.lock_lost
                availability == AuthAvailability.NoScreenLock -> Res.string.lock_no_screen_lock
                availability == AuthAvailability.Unsupported -> Res.string.lock_unsupported
                else -> null
            }
        problem?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = scheme.error) }
        if (availability == AuthAvailability.NoScreenLock) {
            CdActions(listOf(CdActionSpec(stringResource(Res.string.lock_open_settings), onOpenSettings, icon = CoinDenIcon.LOCK.vector)))
        }
    }
}
