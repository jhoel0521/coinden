package com.coinden.app.onboarding

import com.coinden.app.AppStorage
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El primer inicio de Carlos (Bolivia), John (EE. UU.), Ana (Brasil) y Wei (China). */
class OnboardingPresenterTest {
    private fun presenter(
        country: String = "BO",
        deviceLanguage: String = "es",
        storage: AppStorage = AppStorage.inMemory(),
    ): Pair<OnboardingPresenter, com.coinden.app.AppContainer> {
        val container = testContainer(storage = storage, country = country, language = deviceLanguage)
        return OnboardingPresenter(container, deviceLanguage = deviceLanguage, deviceCountry = country) to container
    }

    @Test
    fun `propone el idioma del telefono y si no lo tiene, ingles`() {
        assertEquals(
            "pt",
            presenter(country = "BR", deviceLanguage = "pt")
                .first.state.value.language,
        )
        assertEquals(
            "en",
            presenter(country = "CN", deviceLanguage = "zh")
                .first.state.value.language,
        )
    }

    @Test
    fun `elegir idioma lo guarda y lo aplica`() =
        runTest {
            val (onboarding, container) = presenter()
            onboarding.chooseLanguage("en")
            assertEquals("en", container.languageCode)
            assertEquals("en", container.storage.preferences.languageCode())
        }

    @Test
    fun `la moneda principal sale de la region y las frecuentes de sus vecinos`() {
        val state = presenter().first.state.value
        assertEquals(Currency.BOB, state.localCurrency)
        val frequent = state.frequent.map { it.code }
        assertTrue(frequent.containsAll(listOf("USD", "EUR", "ARS", "BRL", "CLP", "PEN", "PYG")), "$frequent")
        assertFalse("BOB" in frequent)
    }

    @Test
    fun `buscar por nombre en el idioma de la app o por codigo`() {
        val (onboarding, _) = presenter()
        onboarding.search("yuan")
        assertEquals(
            listOf("CNY"),
            onboarding.state.value.searchResults
                .map { it.code },
        )
        onboarding.search("cn")
        assertEquals(
            listOf("CNY"),
            onboarding.state.value.searchResults
                .map { it.code },
        )
    }

    @Test
    fun `solo bolivianos termina sin tasas`() =
        runTest {
            val (onboarding, container) = presenter()
            onboarding.next()
            assertTrue(onboarding.next())
            assertEquals(OnboardingStep.WALLETS, onboarding.state.value.step)
            onboarding.addWallet("Billetera", WalletType.CASH, Currency.BOB, "100")
            assertTrue(onboarding.finish())

            val profile = container.storage.profiles.getProfile(container.ownerId)!!
            assertEquals(Currency.BOB, profile.localCurrency)
            assertEquals(emptyList(), profile.currencies)
            assertEquals(emptyMap(), profile.referenceRates)
        }

    @Test
    fun `dolares a la tasa del paralelo y reales para despues`() =
        runTest {
            val (onboarding, container) = presenter()
            onboarding.next()
            onboarding.toggleCurrency(Currency.USD)
            onboarding.setRate("USD", "10,20")
            onboarding.toggleCurrency(Currency.BRL)
            onboarding.setRateLater("BRL")
            assertTrue(onboarding.next())
            onboarding.addWallet("Billetera", WalletType.CASH, Currency.BOB, "100")
            onboarding.finish()

            val profile = container.storage.profiles.getProfile(container.ownerId)!!
            assertEquals(listOf("USD", "BRL"), profile.currencies.map { it.code })
            val usd = assertIs<ExchangeRateStrategy.ManualDaily>(profile.referenceRates["USD"])
            assertEquals(ExchangeRate.parse(Currency.USD, Currency.BOB, "10.20"), usd.rate)
            assertTrue(usd.updatedAtEpochMs > 0)
            assertNull(profile.referenceRates["BRL"])
        }

    @Test
    fun `una tasa invalida no deja avanzar`() {
        val (onboarding, _) = presenter()
        onboarding.next()
        onboarding.toggleCurrency(Currency.USD)
        onboarding.setRate("USD", "diez")
        assertFalse(onboarding.next())
        assertEquals(setOf<OnboardingError>(OnboardingError.InvalidRate("USD")), onboarding.state.value.errors)
    }

    @Test
    fun `moneda propia sin decimales`() =
        runTest {
            val (onboarding, container) = presenter()
            onboarding.next()
            assertTrue(onboarding.addCustomCurrency("vale", "Vale del súper", "vl", "0"))
            onboarding.setRateLater("VALE")
            onboarding.next()
            val vale =
                onboarding.state.value.walletCurrencies
                    .single { it.code == "VALE" }
            assertEquals(0, vale.decimals)
            onboarding.addWallet("Vales", WalletType.DIGITAL, vale, "25")
            onboarding.addWallet("Billetera", WalletType.CASH, Currency.BOB, "100")
            onboarding.finish()
            assertTrue(
                container.storage.profiles
                    .getProfile(container.ownerId)!!
                    .currencies
                    .any { it.code == "VALE" && it.isCustom },
            )
        }

    @Test
    fun `las billeteras solo ofrecen las monedas elegidas`() {
        val (onboarding, _) = presenter()
        onboarding.next()
        onboarding.toggleCurrency(Currency.USD)
        onboarding.setRateLater("USD")
        onboarding.next()
        assertEquals(
            listOf("BOB", "USD"),
            onboarding.state.value.walletCurrencies
                .map { it.code },
        )
    }

    @Test
    fun `sin billeteras no se termina`() =
        runTest {
            val (onboarding, container) = presenter()
            onboarding.next()
            onboarding.next()
            assertFalse(onboarding.finish())
            assertEquals(setOf<OnboardingError>(OnboardingError.NoWallets), onboarding.state.value.errors)
            assertFalse(container.isOnboarded())
        }

    @Test
    fun `carlos crea sus tres billeteras y la por defecto es su efectivo en bolivianos`() =
        runTest {
            val (onboarding, container) = presenter()
            onboarding.next()
            onboarding.toggleCurrency(Currency.USD)
            onboarding.setRate("USD", "10,20")
            onboarding.next()
            onboarding.addWallet("BNB Bs", WalletType.BANK, Currency.BOB, "500")
            onboarding.addWallet("Billetera", WalletType.CASH, Currency.BOB, "100")
            onboarding.addWallet("Dólares colchón", WalletType.CASH, Currency.USD, "289")
            assertTrue(onboarding.finish())

            val directory = container.drafts.directory(container.ownerId)
            assertEquals(listOf("BNB Bs", "Billetera", "Dólares colchón"), directory.wallets.map { it.name })
            assertEquals("Billetera", directory.byId(directory.defaultWalletId!!)!!.name)
            val balances = container.walletBalances(container.ownerId).map { it.balance }
            assertEquals(listOf("500.00 BOB", "100.00 BOB", "289.00 USD"), balances.map { it.toString() })
            assertTrue(container.isOnboarded())
        }

    @Test
    fun `john solo con un banco en dolares lo tiene por defecto`() =
        runTest {
            val (onboarding, container) = presenter(country = "US", deviceLanguage = "en")
            onboarding.next()
            onboarding.next()
            onboarding.addWallet("Chase", WalletType.BANK, Currency.USD, "1200.50")
            onboarding.finish()
            val directory = container.drafts.directory(container.ownerId)
            assertEquals("Chase", directory.byId(directory.defaultWalletId!!)!!.name)
            assertEquals(Money.parseCanonical("1200.50", Currency.USD), container.walletBalances(container.ownerId).single().balance)
        }

    @Test
    fun `un saldo invalido no agrega la billetera`() {
        val (onboarding, _) = presenter()
        onboarding.next()
        onboarding.next()
        assertFalse(onboarding.addWallet("Billetera", WalletType.CASH, Currency.BOB, "cien"))
        assertFalse(onboarding.addWallet(" ", WalletType.CASH, Currency.BOB, "100"))
        assertEquals(emptyList(), onboarding.state.value.wallets)
    }
}
