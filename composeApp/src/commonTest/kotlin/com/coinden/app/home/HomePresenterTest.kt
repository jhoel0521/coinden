package com.coinden.app.home

import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Inicio de Carlos: patrimonio, billeteras y borradores por revisar. */
class HomePresenterTest {
    private fun bs(v: String) = Money.parseCanonical(v, Currency.BOB)

    private suspend fun carlos(withBrl: Boolean = false): com.coinden.app.AppContainer {
        val container = testContainer()
        val owner = container.ownerId
        val usd = ExchangeRateStrategy.ManualDaily(ExchangeRate.parse(Currency.USD, Currency.BOB, "10.20"), 1)
        container.storage.profiles.saveProfile(
            UserCurrencyProfile(
                userId = owner,
                localCurrency = Currency.BOB,
                currencies = listOfNotNull(Currency.USD, Currency.BRL.takeIf { withBrl }),
                referenceRates = mapOf("USD" to usd),
            ),
        )
        container.createWallet(owner, "Billetera", Currency.BOB, WalletType.CASH, bs("100"))
        container.createWallet(owner, "BNB Bs", Currency.BOB, WalletType.BANK, bs("500"))
        container.createWallet(owner, "Dólares colchón", Currency.USD, WalletType.CASH, Money.parseCanonical("289", Currency.USD))
        if (withBrl) container.createWallet(owner, "Reais", Currency.BRL, WalletType.CASH, Money.parseCanonical("50", Currency.BRL))
        return container
    }

    @Test
    fun `patrimonio, billeteras y borradores pendientes de Carlos`() =
        runTest {
            val container = carlos()
            container.captureDraft(container.ownerId, Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb"))
            container.captureDraft(container.ownerId, Evidence(EvidenceSource.TEXT, "page 3 bs para trasmporte"))

            val home = HomePresenter(container).also { it.load() }.state.value

            // 100 + 500 + 289 x 10.20 = 3547.80 Bs
            assertEquals(bs("3547.80"), home.total)
            assertEquals(listOf("Billetera", "BNB Bs", "Dólares colchón"), home.wallets.map { it.name })
            assertEquals(Money.parseCanonical("289", Currency.USD), home.wallets.last().balance)
            assertEquals(2, home.pendingDrafts)
            assertEquals(emptyList(), home.unconverted)
        }

    @Test
    fun `los reales sin tasa se muestran aparte, sin sumarlos`() =
        runTest {
            val home = HomePresenter(carlos(withBrl = true)).also { it.load() }.state.value
            assertEquals(bs("3547.80"), home.total)
            assertEquals(listOf(Money.parseCanonical("50", Currency.BRL)), home.unconverted)
        }

    @Test
    fun `el onboarding se muestra hasta tener perfil y una billetera`() =
        runTest {
            val container = testContainer()
            assertFalse(container.isOnboarded())
            container.storage.profiles.saveProfile(UserCurrencyProfile(userId = container.ownerId, localCurrency = Currency.BOB))
            assertFalse(container.isOnboarded())
            container.createWallet(container.ownerId, "Billetera", Currency.BOB, WalletType.CASH, bs("0"))
            assertTrue(container.isOnboarded())
        }
}
