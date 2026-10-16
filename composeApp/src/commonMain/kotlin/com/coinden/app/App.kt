package com.coinden.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import com.coinden.app.capture.SharedContent
import com.coinden.app.home.HomePresenter
import com.coinden.app.home.HomeScreen
import com.coinden.app.i18n.ProvideAppLocale
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
import com.coinden.app.theme.CoinDenTheme
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
            Surface(Modifier.fillMaxSize()) {
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
            )
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
        composable(Routes.FORM) {
            val form = remember { DraftFormPresenter(container) }
            DraftFormScreen(
                form,
                nowEpochMs = container.clock::nowEpochMs,
                utcOffsetMinutes = settings.utcOffsetMinutes,
                onSaved = { result ->
                    capture.showResult(result)
                    nav.popBackStack()
                },
                onBack = { nav.popBackStack() },
            )
        }
    }
    // Si algo llega por compartir con la cámara o el formulario abiertos, se vuelve a Inicio.
    LaunchedEffect(pendingShare) {
        val route = nav.currentDestination?.route
        if (pendingShare != null && (route == Routes.CAMERA || route == Routes.FORM)) nav.popBackStack(Routes.HOME, inclusive = false)
    }
}
