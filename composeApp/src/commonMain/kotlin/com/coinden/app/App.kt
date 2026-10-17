package com.coinden.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coinden.app.capture.CaptureNotice
import com.coinden.app.capture.CapturePlatform
import com.coinden.app.capture.CapturePresenter
import com.coinden.app.capture.DraftFormPresenter
import com.coinden.app.capture.DraftFormScreen
import com.coinden.app.capture.EvidencePresenter
import com.coinden.app.capture.EvidenceScreen
import com.coinden.app.capture.FormMode
import com.coinden.app.capture.SharedContent
import com.coinden.app.history.HistoryPresenter
import com.coinden.app.history.HistoryScreen
import com.coinden.app.home.HomePresenter
import com.coinden.app.home.HomeScreen
import com.coinden.app.i18n.ProvideAppLocale
import com.coinden.app.inbox.ReviewInboxPresenter
import com.coinden.app.inbox.ReviewInboxScreen
import com.coinden.app.insights.SpendingInsightsPresenter
import com.coinden.app.insights.SpendingInsightsScreen
import com.coinden.app.onboarding.CurrenciesScreen
import com.coinden.app.onboarding.OnboardingPresenter
import com.coinden.app.onboarding.OnboardingStep
import com.coinden.app.onboarding.WalletsScreen
import com.coinden.app.onboarding.WelcomeScreen
import com.coinden.app.perception.PerceptionService
import com.coinden.app.perception.PrivacyPresenter
import com.coinden.app.perception.PrivacyScreen
import com.coinden.app.perception.VoiceModelsPresenter
import com.coinden.app.perception.VoiceModelsScreen
import com.coinden.app.settings.WalletSettingsPresenter
import com.coinden.app.settings.WalletSettingsScreen
import com.coinden.app.theme.CoinDenTheme
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Rutas de la app. */
private object Routes {
    const val WELCOME = "welcome"
    const val CURRENCIES = "currencies"
    const val WALLETS = "wallets"
    const val HOME = "home"
    const val FORM = "form"
    const val CAMERA = "camera"
    const val PRIVACY = "privacy"
    const val VOICE_MODELS = "voice-models"
    const val INBOX = "inbox"
    const val HISTORY = "history"
    const val SPENDING = "spending"
    const val SETTINGS = "settings"
    const val REVIEW = "review/{draftId}"
    const val CORRECT = "correct/{draftId}/{source}"
    const val EVIDENCE = "evidence/{draftId}"

    /** Sin comprobante fuente: corrección a mano. */
    private const val NO_SOURCE = "-"

    fun review(draftId: String) = "review/$draftId"

    fun evidence(draftId: String) = "evidence/$draftId"

    fun form(mode: FormMode): String =
        when (mode) {
            FormMode.New -> FORM
            is FormMode.Review -> review(mode.draftId)
            is FormMode.Correct -> "correct/${mode.draftId}/${mode.sourceDraftId ?: NO_SOURCE}"
        }

    fun sourceOf(arg: String?): String? = arg?.takeIf { it != NO_SOURCE }
}

/**
 * Punto de entrada de la UI compartida (Android hoy; Desktop e iOS después). Abre el onboarding
 * mientras no haya perfil y billeteras; si no, Inicio. [shared] trae lo que otra app compartió;
 * se captura al llegar a Inicio y luego se avisa con [onSharedTaken].
 */
@Composable
fun App(
    container: AppContainer,
    deviceLanguage: String,
    deviceCountry: String,
    platform: CapturePlatform,
    shared: StateFlow<SharedContent?>,
    onSharedTaken: () -> Unit,
) {
    val language by container.language.collectAsState()
    CoinDenTheme {
        ProvideAppLocale(language) {
            // Sin Surface: el fondo y el color del contenido se ponen aquí, para claro y oscuro.
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Quien ya terminó el onboarding ve una vez la pantalla de privacidad antes de Inicio.
                    val start by produceState<String?>(null) {
                        value =
                            when {
                                !container.isOnboarded() -> Routes.WELCOME
                                !container.storage.preferences.privacyNoticeSeen() -> Routes.PRIVACY
                                else -> Routes.HOME
                            }
                    }
                    start?.let { AppNavHost(container, it, deviceLanguage, deviceCountry, language, platform, shared, onSharedTaken) }
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    container: AppContainer,
    start: String,
    deviceLanguage: String,
    deviceCountry: String,
    language: String,
    platform: CapturePlatform,
    shared: StateFlow<SharedContent?>,
    onSharedTaken: () -> Unit,
) {
    val nav = rememberNavController()
    val onboarding = remember { OnboardingPresenter(container, deviceLanguage, deviceCountry) }
    val settings = container.regional.settings()
    val locale = settings.locale
    // La captura vive con la navegación: su resultado sigue visible al volver de la cámara o del formulario.
    val capture =
        remember {
            val service = PerceptionService(container, platform.perceptionReaders)
            CapturePresenter(container, platform.dictation, platform.processingQueue, service)
        }
    val scope = rememberCoroutineScope()
    // La evidencia sale del formulario sin destruir su edición; cada entrada conserva su presentador.
    val forms = remember { mutableMapOf<String, DraftFormPresenter>() }
    val pendingShare by shared.collectAsState()
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.WELCOME) {
            LaunchedEffect(Unit) { onboarding.show(OnboardingStep.WELCOME) }
            WelcomeScreen(onboarding) { nav.navigate(Routes.PRIVACY) }
        }
        composable(Routes.PRIVACY) {
            val privacy = remember { PrivacyPresenter(container) }
            PrivacyScreen(
                privacy,
                onContinue = {
                    scope.launch {
                        when {
                            !container.isOnboarded() -> nav.navigate(Routes.CURRENCIES)
                            nav.previousBackStackEntry != null -> nav.popBackStack()
                            else -> nav.navigate(Routes.HOME) { popUpTo(Routes.PRIVACY) { inclusive = true } }
                        }
                    }
                },
                onVoiceModels = { nav.navigate(Routes.VOICE_MODELS) },
            )
        }
        composable(Routes.VOICE_MODELS) {
            val models = remember { VoiceModelsPresenter(platform.voiceModels) }
            VoiceModelsScreen(models, onBack = { nav.popBackStack() })
        }
        composable(Routes.CURRENCIES) {
            LaunchedEffect(Unit) { onboarding.show(OnboardingStep.CURRENCIES) }
            CurrenciesScreen(
                onboarding,
                container.currencyNames,
                onNext = { nav.navigate(Routes.WALLETS) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.WALLETS) {
            LaunchedEffect(Unit) { onboarding.show(OnboardingStep.WALLETS) }
            WalletsScreen(
                onboarding,
                locale,
                onFinished = { nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } } },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.HOME) {
            val home = remember { HomePresenter(container) }
            val askMicrophone =
                platform.rememberMicrophoneRequest { granted ->
                    if (granted) scope.launch { capture.dictate() } else capture.permissionDenied(CaptureNotice.MICROPHONE_DENIED)
                }
            // Lo compartido se captura al estar en Inicio (si el onboarding no terminó, espera). Se
            // captura en el alcance de la navegación: tomarlo cambia la clave y cancela este efecto.
            LaunchedEffect(pendingShare) {
                pendingShare?.let {
                    onSharedTaken()
                    scope.launch { capture.shared(it) }
                }
            }
            val captureState by capture.state.collectAsState()
            // "Son el mismo" con un confirmado: se revisa en el formulario de corrección antes de aplicar nada.
            LaunchedEffect(captureState.correction) {
                captureState.correction?.let {
                    capture.correctionOpened()
                    nav.navigate(Routes.form(it))
                }
            }
            HomeScreen(
                home,
                locale,
                language,
                onChooseLanguage = container::chooseLanguage,
                capture = capture,
                onDictate = { if (capture.state.value.listening) capture.cancelDictation() else askMicrophone() },
                onPhoto = { nav.navigate(Routes.CAMERA) },
                onWrite = { nav.navigate(Routes.FORM) },
                utcOffsetMinutes = settings.utcOffsetMinutes,
                onPrivacy = { nav.navigate(Routes.PRIVACY) },
                onVoiceModels = { nav.navigate(Routes.VOICE_MODELS) },
                onReview = { nav.navigate(Routes.INBOX) },
                onHistory = { nav.navigate(Routes.HISTORY) },
                onSpending = { nav.navigate(Routes.SPENDING) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SPENDING) {
            val spending = remember { SpendingInsightsPresenter(container) }
            SpendingInsightsScreen(spending, locale, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            val settingsPresenter = remember { WalletSettingsPresenter(container) }
            WalletSettingsScreen(settingsPresenter, container, locale, onBack = { nav.popBackStack() })
        }
        composable(Routes.INBOX) {
            val inbox = remember { ReviewInboxPresenter(container) }
            ReviewInboxScreen(
                inbox,
                locale,
                settings.utcOffsetMinutes,
                onOpen = { nav.navigate(Routes.form(it)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.HISTORY) {
            val history = remember { HistoryPresenter(container) }
            HistoryScreen(
                history,
                locale,
                settings.utcOffsetMinutes,
                onCorrect = { nav.navigate(Routes.form(it)) },
                onEvidence = { nav.navigate(Routes.evidence(it)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.REVIEW) { entry ->
            val draftId = entry.savedStateHandle.get<String>("draftId").orEmpty()
            val form = forms.getOrPut(entry.id) { DraftFormPresenter(container, FormMode.Review(draftId)) }
            FormRoute(container, form, locale, settings.utcOffsetMinutes, nav::navigate) {
                forms.remove(entry.id)
                nav.popBackStack()
            }
        }
        composable(Routes.CORRECT) { entry ->
            val draftId = entry.savedStateHandle.get<String>("draftId").orEmpty()
            val source = Routes.sourceOf(entry.savedStateHandle.get<String>("source"))
            val form = forms.getOrPut(entry.id) { DraftFormPresenter(container, FormMode.Correct(draftId, source)) }
            FormRoute(container, form, locale, settings.utcOffsetMinutes, nav::navigate) {
                forms.remove(entry.id)
                nav.popBackStack()
            }
        }
        composable(Routes.EVIDENCE) { entry ->
            val draftId = entry.savedStateHandle.get<String>("draftId").orEmpty()
            val evidence = remember(draftId) { EvidencePresenter(container, draftId) }
            EvidenceScreen(evidence, platform.evidenceViewer, onBack = { nav.popBackStack() })
        }
        composable(Routes.CAMERA) {
            platform.Camera(
                onPhoto = { bytes, mime ->
                    nav.popBackStack()
                    scope.launch { capture.photo(bytes, mime) }
                },
                onDenied = { capture.permissionDenied(CaptureNotice.CAMERA_DENIED) },
                onClose = { nav.popBackStack() },
            )
        }
        composable(Routes.FORM) { entry ->
            val form = forms.getOrPut(entry.id) { DraftFormPresenter(container) }
            val done: () -> Unit = {
                forms.remove(entry.id)
                nav.popBackStack()
                Unit
            }
            DraftFormScreen(
                form,
                nowEpochMs = container.clock::nowEpochMs,
                utcOffsetMinutes = settings.utcOffsetMinutes,
                locale = locale,
                onSaved = { result ->
                    capture.showResult(result)
                    done()
                },
                onDone = done,
                onEvidence = { nav.navigate(Routes.evidence(it)) },
                onBack = done,
            )
        }
    }
    // Si algo llega por compartir con la cámara o el formulario abiertos, se vuelve a Inicio.
    LaunchedEffect(pendingShare) {
        val route = nav.currentDestination?.route
        if (pendingShare != null && route == Routes.CAMERA) nav.popBackStack(Routes.HOME, inclusive = false)
    }
}

/** El formulario para revisar un borrador o corregir un movimiento; al terminar vuelve atrás. */
@Composable
private fun FormRoute(
    container: AppContainer,
    form: DraftFormPresenter,
    locale: UserLocale,
    utcOffsetMinutes: Int,
    navigate: (String) -> Unit,
    onDone: () -> Unit,
) {
    DraftFormScreen(
        form,
        nowEpochMs = container.clock::nowEpochMs,
        utcOffsetMinutes = utcOffsetMinutes,
        locale = locale,
        onSaved = { onDone() },
        onDone = onDone,
        onEvidence = { navigate(Routes.evidence(it)) },
        onBack = onDone,
    )
}
