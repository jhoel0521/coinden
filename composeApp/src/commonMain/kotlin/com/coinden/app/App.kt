package com.coinden.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.coinden.app.capture.CaptureDemoPresenter
import com.coinden.app.capture.CaptureDemoScreen
import com.coinden.app.home.HomePresenter
import com.coinden.app.home.HomeScreen
import com.coinden.app.i18n.ProvideAppLocale
import com.coinden.app.onboarding.CurrenciesScreen
import com.coinden.app.onboarding.OnboardingPresenter
import com.coinden.app.onboarding.OnboardingStep
import com.coinden.app.onboarding.WalletsScreen
import com.coinden.app.onboarding.WelcomeScreen
import com.coinden.app.theme.CoinDenTheme

/** Rutas de la app. */
private object Routes {
    const val WELCOME = "welcome"
    const val CURRENCIES = "currencies"
    const val WALLETS = "wallets"
    const val HOME = "home"
    const val CAPTURE = "capture"
}

/**
 * Punto de entrada de la UI compartida (Android hoy; Desktop e iOS después). Abre el onboarding
 * mientras no haya perfil y billeteras; si no, Inicio.
 */
@Composable
fun App(
    container: AppContainer,
    deviceLanguage: String,
    deviceCountry: String,
) {
    val language by container.language.collectAsState()
    CoinDenTheme {
        ProvideAppLocale(language) {
            Surface(Modifier.fillMaxSize()) {
                val start by produceState<String?>(null) { value = if (container.isOnboarded()) Routes.HOME else Routes.WELCOME }
                start?.let { AppNavHost(container, it, deviceLanguage, deviceCountry, language) }
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
) {
    val nav = rememberNavController()
    val onboarding = remember { OnboardingPresenter(container, deviceLanguage, deviceCountry) }
    val locale = container.regional.settings().locale
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.WELCOME) {
            LaunchedEffect(Unit) { onboarding.show(OnboardingStep.WELCOME) }
            WelcomeScreen(onboarding) { nav.navigate(Routes.CURRENCIES) }
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
            HomeScreen(home, locale, language, onChooseLanguage = container::chooseLanguage, onCapture = { nav.navigate(Routes.CAPTURE) })
        }
        composable(Routes.CAPTURE) {
            val capture = remember { CaptureDemoPresenter(container) }
            LaunchedEffect(capture) { capture.load() }
            CaptureDemoScreen(capture, locale)
        }
    }
}
