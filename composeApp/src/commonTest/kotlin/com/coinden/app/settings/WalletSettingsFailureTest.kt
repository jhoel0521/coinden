package com.coinden.app.settings

import com.coinden.app.AppStorage
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort
import com.coinden.core.domain.port.output.WalletRepositoryPort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class WalletSettingsFailureTest {
    @Test
    fun `una lectura fallida de Carlos informa error y permite reintentar`() =
        runTest {
            val storage = AppStorage.inMemory()
            var fail = false
            val profiles =
                object : CurrencyProfileRepositoryPort by storage.profiles {
                    override suspend fun getProfile(userId: String): UserCurrencyProfile? {
                        if (fail) error("disk")
                        return storage.profiles.getProfile(userId)
                    }
                }
            val c =
                testContainer(
                    storage =
                        AppStorage(
                            storage.wallets,
                            storage.drafts,
                            storage.aliases,
                            storage.categorization,
                            profiles,
                            storage.preferences,
                            storage.bundles,
                        ),
                )
            seedCarlos(c)
            val p = WalletSettingsPresenter(c)
            fail = true
            p.load()
            assertEquals(SettingsError.STORAGE, p.state.value.error)
            assertFalse(p.state.value.busy)
            fail = false
            p.load()
            assertEquals(2, p.state.value.wallets.size)
            assertEquals(null, p.state.value.error)
        }

    @Test
    fun `salir de la pantalla no interrumpe la creacion entre billetera y saldo inicial`() =
        runTest {
            val storage = AppStorage.inMemory()
            val saved = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var pause = false
            val wallets =
                object : WalletRepositoryPort by storage.wallets {
                    override suspend fun saveWallet(wallet: Wallet) {
                        storage.wallets.saveWallet(wallet)
                        if (pause) {
                            saved.complete(Unit)
                            release.await()
                        }
                    }
                }
            val c =
                testContainer(
                    storage =
                        AppStorage(
                            wallets,
                            storage.drafts,
                            storage.aliases,
                            storage.categorization,
                            storage.profiles,
                            storage.preferences,
                            storage.bundles,
                        ),
                )
            seedCarlos(c)
            val p = WalletSettingsPresenter(c).also { it.load() }
            pause = true
            val save = launch { p.addWallet("Cuenta nueva", WalletType.BANK, "BOB", "12") }
            saved.await()
            save.cancel()
            release.complete(Unit)
            save.join()
            val wallet = storage.wallets.listWallets(c.ownerId).single { it.name == "Cuenta nueva" }
            assertEquals(1, storage.wallets.eventsForWallet(wallet.id).size)
        }
}
