package com.coinden.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.bar_listening
import coinden.composeapp.generated.resources.capture_sheet_title
import coinden.composeapp.generated.resources.tab_capture
import coinden.composeapp.generated.resources.tab_home
import coinden.composeapp.generated.resources.tab_movements
import coinden.composeapp.generated.resources.tab_profile
import coinden.composeapp.generated.resources.tab_review
import com.coinden.app.capture.CaptureBar
import com.coinden.app.capture.CaptureNoticeCard
import com.coinden.app.capture.CapturePresenter
import com.coinden.app.capture.CaptureResultSheet
import com.coinden.app.capture.FormMode
import com.coinden.app.home.HomePresenter
import com.coinden.app.home.HomeScreen
import com.coinden.app.inbox.ReviewInboxPresenter
import com.coinden.app.inbox.ReviewInboxScreen
import com.coinden.app.lock.AppLock
import com.coinden.app.lock.AppLockController
import com.coinden.app.lock.LockedDestination
import com.coinden.app.lock.LockedPanel
import com.coinden.app.movements.MovementsScreen
import com.coinden.app.profile.ProfileScreen
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.ui.components.CdNavBar
import com.coinden.app.ui.components.CdNavItem
import com.coinden.app.ui.components.CdSheet
import com.coinden.app.ui.components.NavTab
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Qué protege el bloqueo en cada pestaña. */
private fun NavTab.destination(): LockedDestination =
    when (this) {
        NavTab.HOME -> LockedDestination.HOME
        NavTab.REVIEW -> LockedDestination.REVIEW
        NavTab.CAPTURE -> LockedDestination.CAPTURE
        NavTab.MOVEMENTS -> LockedDestination.MOVEMENTS
        NavTab.PROFILE -> LockedDestination.PROFILE
    }

/**
 * La app después del onboarding: la pestaña elegida y la barra de abajo. Capturar abre Dictar,
 * Foto y Escribir sin cambiar de pestaña y funciona aunque la app esté bloqueada; las demás
 * pestañas, bloqueada, muestran [LockedPanel] y no crean sus presentadores (no leen saldos).
 */
@Composable
fun MainScreen(
    container: AppContainer,
    lock: AppLockController,
    capture: CapturePresenter,
    locale: UserLocale,
    utcOffsetMinutes: Int,
    language: String,
    appVersion: String,
    onDictate: () -> Unit,
    onPhoto: () -> Unit,
    onWrite: () -> Unit,
    openForm: (FormMode) -> Unit,
    openEvidence: (String) -> Unit,
    onSettings: () -> Unit,
    onCategories: () -> Unit,
    onPrivacy: () -> Unit,
    onVoiceModels: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var choosingCapture by remember { mutableStateOf(false) }
    val lockState by lock.state.collectAsState()
    val captureState by capture.state.collectAsState()
    val scope = rememberCoroutineScope()
    val unlock: () -> Unit = { scope.launch { lock.ensureUnlocked() } }
    // Mientras la app está abierta, lo que la cola procesa actualiza la tarjeta del resultado.
    LaunchedEffect(capture) { capture.followProcessing() }

    // Inicio solo existe desbloqueada; también da el número de pendientes de la barra.
    val home = if (lockState.locked) null else remember { HomePresenter(container) }
    val homeState = home?.state?.collectAsState()?.value
    LaunchedEffect(home, captureState.result, tab) { if (tab != NavTab.HOME) home?.load() }

    // La raíz respeta las barras del sistema: el contenido no queda bajo la hora ni la barra de abajo bajo los gestos.
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (lockState.locked && AppLock.requiresUnlock(tab.destination())) {
                LockedPanel(onUnlock = unlock)
            } else {
                when (tab) {
                    NavTab.HOME, NavTab.CAPTURE ->
                        home?.let { HomeScreen(it, locale, onReview = { tab = NavTab.REVIEW }, reload = captureState.result) }
                    NavTab.REVIEW -> {
                        val inbox = remember { ReviewInboxPresenter(container) }
                        ReviewInboxScreen(inbox, locale, utcOffsetMinutes, onOpen = openForm)
                    }
                    NavTab.MOVEMENTS ->
                        MovementsScreen(
                            container,
                            locale,
                            utcOffsetMinutes,
                            onCorrect = openForm,
                            onEvidence = openEvidence,
                        )
                    NavTab.PROFILE ->
                        ProfileScreen(
                            lock,
                            language,
                            appVersion,
                            onChooseLanguage = container::chooseLanguage,
                            onWallets = onSettings,
                            onCategories = onCategories,
                            onPrivacy = onPrivacy,
                            onVoiceModels = onVoiceModels,
                        )
                }
            }
        }
        captureState.notice?.let { CaptureNoticeCard(it, onDismiss = capture::dismissNotice) }
        val pending = homeState?.takeIf { it.loaded }?.pendingDrafts
        CdNavBar(
            items =
                listOf(
                    CdNavItem(NavTab.HOME, stringResource(Res.string.tab_home), CoinDenIcon.HOME_TAB.vector),
                    CdNavItem(NavTab.REVIEW, stringResource(Res.string.tab_review), CoinDenIcon.INBOX.vector, badge = pending),
                    if (captureState.listening) {
                        CdNavItem(NavTab.CAPTURE, stringResource(Res.string.bar_listening), CoinDenIcon.STOP.vector)
                    } else {
                        CdNavItem(NavTab.CAPTURE, stringResource(Res.string.tab_capture), CoinDenIcon.PLUS.vector)
                    },
                    CdNavItem(NavTab.MOVEMENTS, stringResource(Res.string.tab_movements), CoinDenIcon.LIST.vector),
                    CdNavItem(NavTab.PROFILE, stringResource(Res.string.tab_profile), CoinDenIcon.USER.vector),
                ),
            selected = tab,
            onSelect = { tab = it },
            onCapture = { if (captureState.listening) onDictate() else choosingCapture = true },
        )
    }
    if (choosingCapture) {
        CdSheet(onDismiss = { choosingCapture = false }, title = stringResource(Res.string.capture_sheet_title)) {
            CaptureBar(
                listening = captureState.listening,
                onDictate = {
                    choosingCapture = false
                    onDictate()
                },
                onPhoto = {
                    choosingCapture = false
                    onPhoto()
                },
                onWrite = {
                    choosingCapture = false
                    onWrite()
                },
            )
        }
    }
    captureState.result?.let { result ->
        CaptureResultSheet(
            result,
            locale,
            utcOffsetMinutes,
            // Confirmar cambia saldos: con la app bloqueada, primero pide la autenticación.
            onConfirm = { draftId -> scope.launch { if (lock.ensureUnlocked()) capture.confirm(draftId) } },
            onResolve = { draftId, same -> scope.launch { capture.resolve(draftId, same) } },
            onDone = capture::dismissResult,
            reading = captureState.reading,
            onVoiceModels = onVoiceModels,
        )
    }
}
