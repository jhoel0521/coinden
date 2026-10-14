package com.coinden.core.data.local

import com.coinden.core.data.local.db.CoinDenDatabase
import com.coinden.core.data.memory.FixedDeviceIdentity
import com.coinden.core.data.memory.StaticInstalledPacks
import com.coinden.core.data.memory.StaticRegionalSettings
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.SuggestionReason
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.IdGeneratorPort
import com.coinden.core.domain.usecase.ConfigureLocalCurrencyUseCase
import com.coinden.core.domain.usecase.category.CategorizationContext
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.GetDraftInboxUseCase
import com.coinden.core.domain.usecase.draft.SetDefaultWalletUseCase
import com.coinden.core.domain.usecase.draft.SetWalletAliasesUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: el día de Carlos (Bolivia) con la base local.
 *
 * De mañana crea sus billeteras, configura su tasa fija, captura "almuerzo 45 bnb" (lo deja para
 * la noche) y "page 3 bs para trasmporte", que confirma como transporte. Cierra la app.
 * De noche la reabre: saldos, borrador pendiente, regla aprendida y perfil siguen ahí.
 */
class CarlosPersistentDayFeatureTest {
    private val file = File.createTempFile("carlos-", ".db").also { it.delete() }

    @AfterTest
    fun cleanUp() {
        file.delete()
    }

    private class SteppingClock : ClockPort {
        private var now = 1_760_000_000_000L

        override fun nowEpochMs(): Long = now.also { now += 1_000 }
    }

    private class UuidIds : IdGeneratorPort {
        override fun newId(): String = UUID.randomUUID().toString()
    }

    /** La app abierta sobre la base: todos los puertos de almacenamiento son los persistentes. */
    private class App(
        val db: CoinDenDatabase,
    ) {
        private val io = Dispatchers.Unconfined
        private val ids = UuidIds()
        private val clock = SteppingClock()
        private val identity = LocalIdentity(db) { ids.newId() }
        val owner = identity.ownerId()
        private val device = FixedDeviceIdentity(identity.deviceId())

        private val walletRepo = LocalWalletRepository(db, io)
        private val packs = StaticInstalledPacks()
        val categorization = CategorizationContext(LocalCategorizationRepository(db, io), walletRepo, packs, ids, clock, device)
        private val ledger = LedgerContext(walletRepo, ids, clock, device)
        private val drafts =
            DraftContext(LocalDraftRepository(db, io), walletRepo, LocalWalletAliasRepository(db, io), ids, clock, device, categorization)
        private val regional = StaticRegionalSettings(RegionalSettings(UserLocale("es", "BO"), -240))

        val profiles = LocalCurrencyProfileRepository(db, io)
        val createWallet = CreateWalletUseCase(ledger)
        val setAliases = SetWalletAliasesUseCase(drafts)
        val setDefault = SetDefaultWalletUseCase(drafts)
        val capture = CaptureDraftUseCase(drafts, TextDraftInterpreter(packs, regional))
        val confirm = ConfirmDraftUseCase(drafts, ledger)
        val inbox = GetDraftInboxUseCase(drafts)
        val status = GetWalletStatusUseCase(walletRepo)
    }

    private fun bs(v: String) = Money.parseCanonical(v, Currency.BOB)

    @Test
    fun `de manana captura y clasifica, cierra la app y de noche todo sigue ahi`() =
        runTest {
            // --- Mañana ---
            val morningDriver = JvmDatabase.driver(file.path)
            val morning = App(JvmDatabase.open(morningDriver))
            val owner = morning.owner
            val billetera =
                assertIs<WalletOperationResult.WalletCreated>(
                    morning.createWallet(owner, "Billetera", Currency.BOB, WalletType.CASH, bs("100")),
                ).wallet
            val bnb =
                assertIs<WalletOperationResult.WalletCreated>(
                    morning.createWallet(owner, "BNB Bs", Currency.BOB, WalletType.BANK, bs("500")),
                ).wallet
            morning.setDefault(owner, billetera.id)
            morning.setAliases(owner, bnb.id, setOf("bnb"))
            val fixed = ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.20"))
            val profile = ConfigureLocalCurrencyUseCase(morning.profiles)(owner, Currency.BOB, fixed)

            val almuerzo = assertIs<Draft.Ready>(morning.capture(owner, Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb")).single())
            val micro = assertIs<Draft.Ready>(morning.capture(owner, Evidence(EvidenceSource.VOICE, "page 3 bs para trasmporte")).single())
            assertIs<DraftResult.Ok>(morning.confirm(micro.id, CategoryRef.Base(BaseCategory.TRANSPORT)))
            morningDriver.close()

            // --- Noche: la app se vuelve a abrir sobre la misma base ---
            val night = App(JvmDatabase.open(JvmDatabase.driver(file.path)))
            assertEquals(owner, night.owner)

            assertEquals(bs("97"), night.status(billetera.id)!!.balance)
            assertEquals(bs("500"), night.status(bnb.id)!!.balance)

            val pending = night.inbox(owner).pending.single()
            assertEquals(almuerzo, pending)

            val suggestion = night.categorization.suggest(owner, "trasmporte")!!
            assertEquals(CategoryRef.Base(BaseCategory.TRANSPORT), suggestion.category)
            assertIs<SuggestionReason.UserRule>(suggestion.reason)

            assertEquals(profile, night.profiles.getProfile(owner))
        }
}
