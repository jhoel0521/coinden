package com.coinden.core.data.local

import com.coinden.core.data.memory.InMemoryCategorizationRepository
import com.coinden.core.data.memory.InMemoryCurrencyProfileRepository
import com.coinden.core.data.memory.InMemoryDraftRepository
import com.coinden.core.data.memory.InMemoryWalletAliasRepository
import com.coinden.core.data.memory.InMemoryWalletRepository
import com.coinden.core.domain.model.AnchorCurrencies
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryOrigin
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.CategoryRule
import com.coinden.core.domain.model.category.ClassificationEvent
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.CategorizationRepositoryPort
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort
import com.coinden.core.domain.port.output.DraftRepositoryPort
import com.coinden.core.domain.port.output.WalletAliasRepositoryPort
import com.coinden.core.domain.port.output.WalletRepositoryPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/*
 * Batería de contrato de los repositorios: cada clase abstracta tiene una implementación en
 * memoria y otra sobre la base local, y ambas deben pasar exactamente las mismas pruebas.
 */

private const val T = 1_760_000_000_000L

private fun bs(v: String) = Money.parseCanonical(v, Currency.BOB)

private fun usd(v: String) = Money.parseCanonical(v, Currency.USD)

/** Una base local nueva en memoria por cada prueba. */
internal fun freshDatabase() = JvmDatabase.open(JvmDatabase.driver())

abstract class WalletRepositoryContract {
    abstract fun repo(): WalletRepositoryPort

    private val billetera = Wallet("w1", "carlos", "Billetera", Currency.BOB, WalletType.CASH)
    private val colchon = Wallet("w2", "carlos", "Dólares colchón", Currency.USD, WalletType.CASH)

    @Test
    fun `guarda, reemplaza y lista billeteras por dueno en orden de creacion`() =
        runTest {
            val repo = repo()
            repo.saveWallet(billetera)
            repo.saveWallet(colchon)
            repo.saveWallet(Wallet("w3", "ana", "Carteira", Currency.BRL, WalletType.CASH))
            repo.saveWallet(billetera.archived())

            assertEquals(billetera.archived(), repo.getWallet("w1"))
            assertEquals(listOf(billetera.archived(), colchon), repo.listWallets("carlos"))
            assertNull(repo.getWallet("no-existe"))
        }

    @Test
    fun `los eventos se leen por billetera en orden de insercion, incluida la transferencia`() =
        runTest {
            val repo = repo()
            val opening = LedgerEvent.OpeningBalance("e1", "dev", T, T, "w1", bs("100"))
            val transfer = LedgerEvent.Transfer("e2", "dev", T + 1, T + 1, "w2", "w1", usd("11"), bs("112"))
            val expense = LedgerEvent.Expense("e3", "dev", T + 2, T + 2, "w1", bs("45"))
            val annul = LedgerEvent.Annulment("e4", "dev", T + 3, T + 3, "e2", setOf("w1", "w2"))
            listOf(opening, transfer, expense, annul).forEach { repo.appendEvent(it) }

            assertEquals(listOf(opening, transfer, expense, annul), repo.eventsForWallet("w1"))
            assertEquals(listOf(transfer, annul), repo.eventsForWallet("w2"))
            assertEquals(transfer, repo.findEvent("e2"))
            assertNull(repo.findEvent("e9"))
        }

    @Test
    fun `un id repetido en el libro se rechaza y el original queda intacto`() =
        runTest {
            val repo = repo()
            val original = LedgerEvent.Expense("e1", "dev", T, T, "w1", bs("45"))
            repo.appendEvent(original)

            assertFailsWith<IllegalStateException> { repo.appendEvent(LedgerEvent.Expense("e1", "dev", T, T, "w1", bs("99"))) }
            assertEquals(listOf(original), repo.eventsForWallet("w1"))
        }
}

abstract class DraftRepositoryContract {
    abstract fun repo(): DraftRepositoryPort

    private fun captured(
        id: String,
        draftId: String,
        owner: String = "carlos",
    ) = DraftEvent.Captured(id, draftId, owner, "dev", T, T, Evidence(EvidenceSource.TEXT, "almuerzo 45 qr"))

    @Test
    fun `eventos por borrador en orden y borradores por dueno sin repetir`() =
        runTest {
            val repo = repo()
            val a1 = captured("a1", "d1")
            val b1 = captured("b1", "d2")
            val a2 = DraftEvent.Discarded("a2", "d1", "carlos", "dev", T + 1, T + 1)
            listOf(a1, b1, a2, captured("c1", "d3", owner = "ana")).forEach { repo.appendEvent(it) }

            assertEquals(listOf(a1, a2), repo.eventsFor("d1"))
            assertEquals(listOf("d1", "d2"), repo.draftIdsFor("carlos"))
            assertEquals(listOf("d3"), repo.draftIdsFor("ana"))
        }

    @Test
    fun `un id repetido en los borradores se rechaza`() =
        runTest {
            val repo = repo()
            repo.appendEvent(captured("a1", "d1"))
            assertFailsWith<IllegalStateException> { repo.appendEvent(captured("a1", "d9")) }
            assertEquals(listOf("d1"), repo.draftIdsFor("carlos"))
        }
}

abstract class WalletAliasRepositoryContract {
    abstract fun repo(): WalletAliasRepositoryPort

    @Test
    fun `los alias de una billetera se reemplazan y la billetera por defecto se puede borrar`() =
        runTest {
            val repo = repo()
            repo.setAliases("carlos", "bnb", setOf("banco", "bnb"))
            repo.setAliases("carlos", "yape", setOf("yape"))
            repo.setAliases("carlos", "bnb", setOf("bnb"))
            repo.setAliases("ana", "nubank", setOf("pix"))

            assertEquals(mapOf("bnb" to "bnb", "yape" to "yape"), repo.aliasesFor("carlos"))

            assertNull(repo.defaultWalletFor("carlos"))
            repo.setDefaultWallet("carlos", "billetera")
            assertEquals("billetera", repo.defaultWalletFor("carlos"))
            repo.setDefaultWallet("carlos", null)
            assertNull(repo.defaultWalletFor("carlos"))
        }
}

abstract class CategorizationRepositoryContract {
    abstract fun repo(): CategorizationRepositoryPort

    private fun classified(
        id: String,
        movementId: String,
        owner: String = "carlos",
    ) = ClassificationEvent(id, movementId, owner, "dev", T, T, "almuerzo", CategoryRef.Base(BaseCategory.FOOD), CategoryOrigin.USER)

    @Test
    fun `clasificaciones por movimiento y por dueno, de solo agregar`() =
        runTest {
            val repo = repo()
            val k1 = classified("k1", "m1")
            val k2 = classified("k2", "m2")
            val k3 = classified("k3", "m1").copy(category = null, origin = null)
            listOf(k1, k2, k3, classified("k4", "m9", owner = "ana")).forEach { repo.appendClassification(it) }

            assertEquals(listOf(k1, k3), repo.classificationsFor("m1"))
            assertEquals(listOf(k1, k2, k3), repo.classificationsOf("carlos"))
            assertFailsWith<IllegalStateException> { repo.appendClassification(classified("k1", "m5")) }
        }

    @Test
    fun `reglas y categorias propias se reemplazan por su clave`() =
        runTest {
            val repo = repo()
            repo.saveRule(CategoryRule("carlos", "almuerzo", CategoryRef.Base(BaseCategory.FOOD)))
            repo.saveRule(CategoryRule("carlos", "micro", CategoryRef.Base(BaseCategory.TRANSPORT)))
            repo.saveRule(CategoryRule("carlos", "almuerzo", CategoryRef.Custom("cat-1")))
            repo.saveRule(CategoryRule("ana", "almoco", CategoryRef.Base(BaseCategory.FOOD)))

            assertEquals(
                listOf(
                    CategoryRule("carlos", "almuerzo", CategoryRef.Custom("cat-1")),
                    CategoryRule("carlos", "micro", CategoryRef.Base(BaseCategory.TRANSPORT)),
                ),
                repo.rulesOf("carlos"),
            )

            val mascotas = CustomCategory("cat-1", "carlos", "Mascotas")
            repo.saveCustomCategory(mascotas)
            repo.saveCustomCategory(mascotas.copy(archived = true))
            assertEquals(listOf(mascotas.copy(archived = true)), repo.customCategoriesOf("carlos"))
        }
}

abstract class CurrencyProfileRepositoryContract {
    abstract fun repo(): CurrencyProfileRepositoryPort

    @Test
    fun `el perfil se reemplaza por usuario`() =
        runTest {
            val repo = repo()
            assertNull(repo.getProfile("carlos"))
            val fixed = ExchangeRateStrategy.Fixed(ExchangeRate.parse(Currency.USD, Currency.BOB, "11.20"))
            repo.saveProfile(UserCurrencyProfile("carlos", AnchorCurrencies.USD, Currency.BOB, fixed))
            val manual = ExchangeRateStrategy.ManualDaily(ExchangeRate.of(usd("11"), bs("112")), T)
            repo.saveProfile(UserCurrencyProfile("carlos", AnchorCurrencies.USD, Currency.BOB, manual))

            assertEquals(UserCurrencyProfile("carlos", AnchorCurrencies.USD, Currency.BOB, manual), repo.getProfile("carlos"))
        }
}

// --- Implementaciones: memoria y base local ----------------------------------------

class InMemoryWalletRepositoryContractTest : WalletRepositoryContract() {
    override fun repo() = InMemoryWalletRepository()
}

class LocalWalletRepositoryContractTest : WalletRepositoryContract() {
    override fun repo() = LocalWalletRepository(freshDatabase(), Dispatchers.Unconfined)
}

class InMemoryDraftRepositoryContractTest : DraftRepositoryContract() {
    override fun repo() = InMemoryDraftRepository()
}

class LocalDraftRepositoryContractTest : DraftRepositoryContract() {
    override fun repo() = LocalDraftRepository(freshDatabase(), Dispatchers.Unconfined)
}

class InMemoryWalletAliasRepositoryContractTest : WalletAliasRepositoryContract() {
    override fun repo() = InMemoryWalletAliasRepository()
}

class LocalWalletAliasRepositoryContractTest : WalletAliasRepositoryContract() {
    override fun repo() = LocalWalletAliasRepository(freshDatabase(), Dispatchers.Unconfined)
}

class InMemoryCategorizationRepositoryContractTest : CategorizationRepositoryContract() {
    override fun repo() = InMemoryCategorizationRepository()
}

class LocalCategorizationRepositoryContractTest : CategorizationRepositoryContract() {
    override fun repo() = LocalCategorizationRepository(freshDatabase(), Dispatchers.Unconfined)
}

class InMemoryCurrencyProfileRepositoryContractTest : CurrencyProfileRepositoryContract() {
    override fun repo() = InMemoryCurrencyProfileRepository()
}

class LocalCurrencyProfileRepositoryContractTest : CurrencyProfileRepositoryContract() {
    override fun repo() = LocalCurrencyProfileRepository(freshDatabase(), Dispatchers.Unconfined)
}
