package com.coinden.core.data.packs

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.interpret.UserLocale
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.InstalledPacksPort
import com.coinden.core.domain.port.output.RegionalSettingsPort
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * El primer arranque, que es el caso que el Manifiesto protege: John instala la app, la abre sin
 * conexión y no ha descargado nada. Tiene que poder registrar un gasto igual.
 *
 * Nada en esta prueba toca la red: los paquetes salen de los archivos que viajan dentro de la app.
 */
class PrimerArranqueTest {
    private val cuenta = Wallet("w1", "john", "Checking", Currency.USD, WalletType.BANK)
    private val directory = WalletDirectory(listOf(cuenta), emptyMap(), defaultWalletId = "w1")

    /** Lo que la app tiene recién instalada: lo que viaja con ella y nada más. */
    private val recienInstalada =
        object : InstalledPacksPort {
            override fun languagePacks(): List<LanguagePack> = BundledPacks.languagePacks(appLanguage = "en")

            override fun receiptTemplates(): List<ReceiptTemplate> = BundledPacks.receiptTemplates()
        }

    private val enEstadosUnidos =
        object : RegionalSettingsPort {
            override fun settings() =
                RegionalSettings(
                    UserLocale(language = "en", country = "US", currency = Currency.USD, decimalSeparator = '.'),
                    utcOffsetMinutes = -300,
                )
        }

    @Test
    fun `John registra un gasto el primer dia, sin descargar nada`() =
        runTest {
            val propuestas =
                TextDraftInterpreter(recienInstalada, enEstadosUnidos)
                    .interpret(Evidence(EvidenceSource.TEXT, "paid 12 usd for lunch"), directory)

            val gasto = assertIs<MovementProposal.Expense>(propuestas.single())
            assertEquals(ProposedAmount("12", Currency.USD), gasto.amount)
            assertEquals("lunch", gasto.note)
        }

    @Test
    fun `sin ningun archivo descargado igual hay paquetes`() {
        assertEquals(3, BundledPacks.languagePacks().size)
        assertEquals(4, BundledPacks.receiptTemplates().size)
    }
}
