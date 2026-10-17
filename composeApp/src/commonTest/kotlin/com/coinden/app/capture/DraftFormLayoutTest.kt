package com.coinden.app.capture

import com.coinden.app.AppContainer
import com.coinden.app.seedCarlos
import com.coinden.app.testContainer
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** El formulario más corto: la moneda sale de la billetera, "Más detalles" y las categorías más usadas. */
class DraftFormLayoutTest {
    private suspend fun carlos(): AppContainer = testContainer().also { seedCarlos(it) }

    private suspend fun AppContainer.form() = DraftFormPresenter(this).also { it.load() }

    @Test
    fun `la moneda es la de la billetera elegida`() =
        runTest {
            val container = carlos()
            assertIs<WalletOperationResult.WalletCreated>(
                container.createWallet(
                    container.ownerId,
                    "Dolares",
                    Currency.USD,
                    WalletType.CASH,
                    Money.parseCanonical("100", Currency.USD),
                ),
            )
            val form = container.form()
            assertEquals(
                "BOB",
                form.state.value.walletCurrency
                    ?.code,
            )

            form.onWalletChange(
                form.state.value.wallets
                    .single { it.name == "Dolares" }
                    .id,
            )

            assertEquals(
                "USD",
                form.state.value.walletCurrency
                    ?.code,
            )
        }

    @Test
    fun `mas detalles se abre si el comprobante trae referencia`() =
        runTest {
            val form = carlos().form()
            assertFalse(form.state.value.hasDetails)

            form.onPhraseChange("Banco ECONOMICO\nPago QR otros bancos\nImporte\nBs25.00\nNro. de transacción: 400000001")
            form.fillFromPhrase()

            assertTrue(form.state.value.hasDetails)
            assertEquals("400000001", form.state.value.reference)
        }

    @Test
    fun `las categorias mas usadas salen de lo que la persona ya clasifico`() =
        runTest {
            val container = carlos()

            suspend fun confirmed(
                phrase: String,
                category: BaseCategory,
            ) {
                val draft = container.captureDraft(container.ownerId, Evidence(EvidenceSource.VOICE, phrase)).single()
                assertIs<DraftResult.Ok>(container.confirmDraft(draft.id, CategoryRef.Base(category)))
            }
            confirmed("pagué 10 de taxi bnb", BaseCategory.TRANSPORT)
            confirmed("pagué 12 de micro bnb", BaseCategory.TRANSPORT)
            confirmed("pagué 25 de almuerzo bnb", BaseCategory.FOOD)

            val usage =
                container
                    .form()
                    .state.value.categoryUsage

            assertEquals(2, usage[CategoryRef.Base(BaseCategory.TRANSPORT)])
            assertEquals(1, usage[CategoryRef.Base(BaseCategory.FOOD)])
        }
}
