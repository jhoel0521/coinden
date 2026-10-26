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
import com.coinden.core.domain.model.category.CustomCategory
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
import com.coinden.core.domain.usecase.category.CategorizationResult
import com.coinden.core.domain.usecase.category.CreateCustomCategoryUseCase
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
import kotlin.test.assertNotNull

/**
 * Actualizar nunca borra datos (local-persistence: Actualizar sin Perder Datos).
 *
 * `releases/coinden-0.1.0.db` es una base guardada por la versión 0.1.0, publicada en Google Play,
 * con el día de Carlos. Cada versión nueva tiene que abrirla y leer todo igual. La base se generó
 * una sola vez con `COINDEN_WRITE_RELEASE_FIXTURE=<ruta>` sobre el código de la 0.1.0; no se
 * vuelve a generar: si esta prueba falla, la versión nueva rompería los datos de alguien.
 */
class UpdateFromReleaseTest {
    private val file = File.createTempFile("coinden-update-", ".db").also { it.delete() }

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

    /** La app abierta sobre la base, con los repositorios persistentes. */
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
        private val categoriesRepo = LocalCategorizationRepository(db, io)
        val categorization = CategorizationContext(categoriesRepo, walletRepo, packs, ids, clock, device)
        private val ledger = LedgerContext(walletRepo, ids, clock, device)
        private val drafts =
            DraftContext(LocalDraftRepository(db, io), walletRepo, LocalWalletAliasRepository(db, io), ids, clock, device, categorization)
        private val regional = StaticRegionalSettings(RegionalSettings(UserLocale("es", "BO", Currency.BOB, '.'), -240))

        val wallets = walletRepo
        val profiles = LocalCurrencyProfileRepository(db, io)
        val preferences = LocalPreferencesRepository(db, io)
        val createWallet = CreateWalletUseCase(ledger)
        val setAliases = SetWalletAliasesUseCase(drafts)
        val setDefault = SetDefaultWalletUseCase(drafts)
        val createCategory = CreateCustomCategoryUseCase(categorization)
        val capture = CaptureDraftUseCase(drafts, TextDraftInterpreter(packs, regional))
        val confirm = ConfirmDraftUseCase(drafts, ledger)
        val inbox = GetDraftInboxUseCase(drafts)
        val status = GetWalletStatusUseCase(walletRepo)

        suspend fun customCategories(): List<CustomCategory> = categoriesRepo.customCategoriesOf(owner)
    }

    private fun bs(v: String) = Money.parseCanonical(v, Currency.BOB)

    /** Solo para generar la base de la 0.1.0 una vez: nunca corre en el CI. */
    @Test
    fun `generar la base de la version publicada`() =
        runTest {
            val target = System.getenv("COINDEN_WRITE_RELEASE_FIXTURE") ?: return@runTest
            File(target).delete()
            val driver = JvmDatabase.driver(target)
            val app = App(JvmDatabase.open(driver))
            val owner = app.owner
            val billetera =
                assertIs<WalletOperationResult.WalletCreated>(
                    app.createWallet(owner, "Billetera", Currency.BOB, WalletType.CASH, bs("300")),
                ).wallet
            val bnb =
                assertIs<WalletOperationResult.WalletCreated>(app.createWallet(owner, "BNB Bs", Currency.BOB, WalletType.BANK, bs("2500")))
                    .wallet
            app.createWallet(owner, "Dolares", Currency.USD, WalletType.CASH, Money.parseCanonical("200", Currency.USD))
            app.setDefault(owner, billetera.id)
            app.setAliases(owner, bnb.id, setOf("bnb"))
            ConfigureLocalCurrencyUseCase(app.profiles)(
                owner,
                Currency.BOB,
                ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.20")),
            )
            val pets = assertIs<CategorizationResult.Ok<CustomCategory>>(app.createCategory(owner, "Mascotas")).value

            val lunch = app.capture(owner, Evidence(EvidenceSource.TEXT, "almuerzo 45 bnb")).single()
            assertIs<DraftResult.Ok>(app.confirm(lunch.id, CategoryRef.Base(BaseCategory.FOOD)))
            val vet = app.capture(owner, Evidence(EvidenceSource.TEXT, "veterinario 30 bnb")).single()
            assertIs<DraftResult.Ok>(app.confirm(vet.id, CategoryRef.Custom(pets.id)))
            app.capture(owner, Evidence(EvidenceSource.VOICE, "page 3 bs para trasmporte"))
            app.capture(owner, Evidence(EvidenceSource.PHOTO, attachmentRef = "attachment-0-1-0", fingerprint = "huella-0-1-0"))
            app.preferences.setLanguageCode("es")
            driver.close()
        }

    @Test
    fun `la version nueva abre la base de la 0-1-0 sin perder nada`() =
        runTest {
            val release = javaClass.getResourceAsStream("/releases/coinden-0.1.0.db")
            assertNotNull(release, "falta la base de la versión publicada")
            release.use { input -> file.outputStream().use { input.copyTo(it) } }

            val app = App(JvmDatabase.open(JvmDatabase.driver(file.path)))
            val owner = app.owner

            val byName = app.wallets.listWallets(owner).associateBy { it.name }
            assertEquals(setOf("Billetera", "BNB Bs", "Dolares"), byName.keys)
            assertEquals(bs("300"), app.status(byName.getValue("Billetera").id)!!.balance)
            assertEquals(bs("2425"), app.status(byName.getValue("BNB Bs").id)!!.balance)
            assertEquals(Money.parseCanonical("200", Currency.USD), app.status(byName.getValue("Dolares").id)!!.balance)

            val pending = app.inbox(owner).pending
            assertEquals(setOf("page 3 bs para trasmporte", null), pending.map { it.header.evidence.text }.toSet())
            val photo = pending.single { it.header.evidence.source == EvidenceSource.PHOTO }.header.evidence
            assertEquals("attachment-0-1-0", photo.attachmentRef)
            assertEquals("huella-0-1-0", photo.fingerprint)

            assertEquals(listOf("Mascotas"), app.customCategories().map { it.name })
            assertEquals(Currency.BOB, app.profiles.getProfile(owner)!!.localCurrency)
            assertEquals("es", app.preferences.languageCode())
        }
}
