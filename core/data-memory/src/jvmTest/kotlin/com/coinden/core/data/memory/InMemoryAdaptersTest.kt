package com.coinden.core.data.memory

import com.coinden.core.data.packs.BundledPacks
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InMemoryAdaptersTest {
    @Test
    fun `el libro en memoria es de solo agregar`() =
        runTest {
            val repo = InMemoryWalletRepository()
            repo.saveWallet(Wallet("w", "carlos", "Billetera", Currency.BOB, WalletType.CASH))
            val event = LedgerEvent.Income("e-1", "d", 0, 0, "w", Money.ofMinorUnits(100, Currency.BOB))
            repo.appendEvent(event)
            assertFailsWith<IllegalStateException> { repo.appendEvent(event.copy(amount = Money.ofMinorUnits(999, Currency.BOB))) }
            assertEquals(listOf<LedgerEvent>(event), repo.eventsForWallet("w"))
            assertEquals(1, repo.listWallets("carlos").size)
        }

    @Test
    fun `el historial de borradores es de solo agregar`() =
        runTest {
            val repo = InMemoryDraftRepository()
            val event = DraftEvent.Captured("e-1", "d-1", "carlos", "celular", 0, 0, Evidence(EvidenceSource.TEXT, "x"))
            repo.appendEvent(event)
            assertFailsWith<IllegalStateException> { repo.appendEvent(event) }
            assertEquals(listOf("d-1"), repo.draftIdsFor("carlos"))
        }

    @Test
    fun `ids UUID unicos`() {
        val generator = UuidIdGenerator()
        val ids = List(1000) { generator.newId() }
        assertEquals(1000, ids.toSet().size)
        assertTrue(ids.all { Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$").matches(it) })
    }

    @Test
    fun `reloj del sistema y paquetes del catalogo`() {
        val before = System.currentTimeMillis()
        val now = SystemClock().nowEpochMs()
        assertTrue(now >= before)
        val packs = StaticInstalledPacks()
        val codigos = packs.languagePacks().map { it.code }
        // El puerto promete estas dos cosas, y el adaptador las cumple con los archivos de datos.
        assertTrue("en" in codigos, "el inglés viaja con la app, así que siempre está")
        assertEquals("es", codigos.last(), "el idioma de la app va al final para ganar al unirlos")
        assertEquals(BundledPacks.receiptTemplates(), packs.receiptTemplates())
    }

    @Test
    fun `alias y billetera por defecto`() =
        runTest {
            val repo = InMemoryWalletAliasRepository()
            repo.setAliases("carlos", "bnb", setOf("qr"))
            repo.setDefaultWallet("carlos", "billetera")
            assertEquals(mapOf("qr" to "bnb"), repo.aliasesFor("carlos"))
            assertEquals("billetera", repo.defaultWalletFor("carlos"))
        }

    @Test
    fun `el idioma elegido se guarda y se puede cambiar`() =
        runTest {
            val preferences = InMemoryPreferencesRepository()
            assertEquals(null, preferences.languageCode())
            preferences.setLanguageCode("pt")
            preferences.setLanguageCode("es")
            assertEquals("es", preferences.languageCode())
        }
}
