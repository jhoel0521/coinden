package com.coinden.app.i18n

import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** El idioma elegido activa su paquete en el intérprete. */
class LanguagePacksByChoiceTest {
    @Test
    fun `ana elige portugues y el interprete entiende pix`() =
        runTest {
            val container = testContainer(country = "BR", language = "en")
            container.chooseLanguage("pt")
            val owner = container.ownerId
            container.storage.profiles.saveProfile(UserCurrencyProfile(userId = owner, localCurrency = Currency.BRL))
            container.createWallet(owner, "Carteira", Currency.BRL, WalletType.CASH, Money.parseCanonical("100", Currency.BRL))
            container.createWallet(owner, "Nubank", Currency.BRL, WalletType.DIGITAL, Money.parseCanonical("500", Currency.BRL))

            val draft =
                assertIs<Draft.Ready>(container.captureDraft(owner, Evidence(EvidenceSource.TEXT, "paguei 30 reais no pix")).single())
            val proposal = assertIs<MovementProposal.Expense>(draft.proposal)
            assertEquals(PaymentMethod.TRANSFER, proposal.paymentMethod)
            assertEquals("BRL", proposal.amount?.currency?.code)
        }

    @Test
    fun `el idioma del telefono o ingles`() {
        assertEquals("pt", AppLanguage.proposedFor("pt"))
        assertEquals("en", AppLanguage.proposedFor("zh"))
        assertEquals("es", AppLanguage.proposedFor("ES"))
    }
}
