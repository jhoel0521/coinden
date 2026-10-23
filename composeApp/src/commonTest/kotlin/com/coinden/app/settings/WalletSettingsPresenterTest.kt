package com.coinden.app.settings

import com.coinden.app.AppStorage
import com.coinden.app.home.HomePresenter
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.currentRate
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.WalletRepositoryPort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WalletSettingsPresenterTest {
    @Test
    fun `Carlos crea despues del onboarding archiva reactiva y conserva eventos y predeterminada`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            val p = WalletSettingsPresenter(c)
            p.load()
            val cash =
                p.state.value.wallets
                    .first { it.wallet.type == WalletType.CASH }
                    .wallet.id
            val bank =
                p.state.value.wallets
                    .first { it.wallet.type == WalletType.BANK }
                    .wallet.id
            val before = c.storage.wallets.eventsForWallet(cash)
            assertTrue(p.archive(cash))
            assertEquals(bank, p.state.value.defaultWalletId)
            assertFalse(p.archive(bank))
            assertEquals(SettingsError.LAST_WALLET, p.state.value.error)
            assertTrue(c.isOnboarded())
            assertTrue(p.reactivate(cash))
            assertTrue(p.makeDefault(cash))
            assertEquals(before, c.storage.wallets.eventsForWallet(cash))
            assertTrue(p.addCurrency("USD"))
            assertTrue(p.addWallet("  Meru  ", WalletType.DIGITAL, "USD", "12.50"))
            val meru =
                p.state.value.wallets
                    .single { it.wallet.name == "  Meru  " }
            assertEquals(Money.parseCanonical("12.50", Currency.USD), meru.balance)
            assertEquals(
                1,
                c.storage.wallets
                    .eventsForWallet(meru.wallet.id)
                    .size,
            )
            val reopened = WalletSettingsPresenter(testContainer(storage = c.storage, ownerId = c.ownerId))
            reopened.load()
            assertEquals(p.state.value.wallets, reopened.state.value.wallets)
            assertEquals(cash, reopened.state.value.defaultWalletId)
        }

    @Test
    fun `con la ultima activa Carlos ve el aviso al instante sin el dialogo de confirmar`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            val p = WalletSettingsPresenter(c)
            p.load()
            val wallets =
                p.state.value.wallets
                    .map { it.wallet.id }
            // Con dos activas, archivar pide confirmación.
            assertTrue(p.requestArchive(wallets.first()))
            assertTrue(p.archive(wallets.first()))

            // Con la última activa no hay diálogo: el aviso sale de inmediato.
            assertFalse(p.requestArchive(wallets.last()))
            assertEquals(SettingsError.LAST_WALLET, p.state.value.error)
        }

    @Test
    fun `Facundo permite deuda inicial solo en credito y Carlos rechaza datos invalidos`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            val p = WalletSettingsPresenter(c).also { it.load() }
            assertFalse(p.addWallet("", WalletType.CASH, "BOB", "1"))
            assertFalse(p.addWallet("Efectivo", WalletType.CASH, "BOB", "-10"))
            assertFalse(p.addWallet("Error", WalletType.CASH, "BOB", "abc"))
            assertFalse(p.addWallet("Error", WalletType.CASH, "ZZZ", "1"))
            assertEquals(2, p.state.value.wallets.size)
            assertTrue(p.addCurrency("ARS"))
            assertTrue(p.addWallet("Visa Galicia", WalletType.CREDIT, "ARS", "-25.00"))
            assertEquals(
                Money.parseCanonical("-25", Currency.ARS),
                p.state.value.wallets
                    .last()
                    .balance,
            )
        }

    @Test
    fun `no permite acciones sobre billeteras ajenas ni inexistentes`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            val other = testContainer(storage = c.storage, ownerId = "john")
            other.createWallet(other.ownerId, "Chase", Currency.USD, WalletType.BANK, Money.zero(Currency.USD))
            val foreign =
                other.storage.wallets
                    .listWallets(other.ownerId)
                    .single()
                    .id
            val p = WalletSettingsPresenter(c).also { it.load() }
            assertFalse(p.archive(foreign))
            assertFalse(p.reactivate(foreign))
            assertFalse(p.makeDefault(foreign))
            assertFalse(p.makeDefault("missing"))
            assertTrue(
                other.storage.wallets
                    .getWallet(foreign)!!
                    .isActive,
            )
        }

    @Test
    fun `Carlos actualiza y quita la referencia legada sin tocar el libro`() =
        runTest {
            val c = testContainer().also { seedCarlos(it) }
            c.storage.profiles.saveProfile(
                UserCurrencyProfile(
                    c.ownerId,
                    localCurrency = Currency.BOB,
                    currencies = listOf(Currency.USD),
                    strategy = ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.20")),
                ),
            )
            val p = WalletSettingsPresenter(c).also { it.load() }
            assertTrue(p.addWallet("Dolares", WalletType.CASH, "USD", "12"))
            val wallets = c.storage.wallets.listWallets(c.ownerId)
            val before = wallets.associate { it.id to c.storage.wallets.eventsForWallet(it.id) }
            assertTrue(p.saveRate("USD", "10.00"))
            assertEquals(
                "10/1",
                c.storage.profiles
                    .getProfile(c.ownerId)!!
                    .referenceRateFor(Currency.USD)!!
                    .currentRate
                    .toCanonical(),
            )
            val home = HomePresenter(c).also { it.load() }
            assertEquals(Money.parseCanonical("720", Currency.BOB), home.state.value.total)
            for (invalid in listOf("0", "-1", "abc")) assertFalse(p.saveRate("USD", invalid))
            assertTrue(p.removeRate("USD"))
            assertEquals(
                null,
                c.storage.profiles
                    .getProfile(c.ownerId)!!
                    .referenceRateFor(Currency.USD),
            )
            home.load()
            assertEquals(listOf(Money.parseCanonical("12", Currency.USD)), home.state.value.unconverted)
            assertTrue(p.addCurrency("BRL"))
            assertTrue(p.saveRate("BRL", "1.85"))
            assertEquals(wallets.associate { it.id to c.storage.wallets.eventsForWallet(it.id) }, before)
            assertFalse(p.addCurrency("XXXXXX"))
        }

    @Test
    fun `John no duplica una creacion mientras el almacenamiento esta ocupado`() =
        runTest {
            val original = AppStorage.inMemory()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var pause = false
            val wallets =
                object : WalletRepositoryPort by original.wallets {
                    override suspend fun saveWallet(wallet: Wallet) {
                        if (pause) {
                            entered.complete(Unit)
                            release.await()
                        }
                        original.wallets.saveWallet(wallet)
                    }
                }
            val c =
                testContainer(
                    storage =
                        AppStorage(
                            wallets,
                            original.drafts,
                            original.aliases,
                            original.categorization,
                            original.profiles,
                            original.preferences,
                            original.bundles,
                        ),
                )
            seedCarlos(c)
            val p = WalletSettingsPresenter(c).also { it.load() }
            pause = true
            val first = launch { assertTrue(p.addWallet("Cuenta", WalletType.BANK, "BOB", "0")) }
            entered.await()
            runCurrent()
            assertTrue(p.state.value.busy)
            assertFalse(p.addWallet("Cuenta", WalletType.BANK, "BOB", "0"))
            release.complete(Unit)
            first.join()
            assertEquals(
                1,
                p.state.value.wallets
                    .count { it.wallet.name == "Cuenta" },
            )
        }
}
