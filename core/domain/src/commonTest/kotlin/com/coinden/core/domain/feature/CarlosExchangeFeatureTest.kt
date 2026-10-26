package com.coinden.core.domain.feature

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.DraftResult
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.interpret.TextDraftInterpreter
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.support.TestPacks
import com.coinden.core.domain.usecase.draft.CaptureDraftUseCase
import com.coinden.core.domain.usecase.draft.ConfirmDraftUseCase
import com.coinden.core.domain.usecase.draft.DraftContext
import com.coinden.core.domain.usecase.draft.SetDefaultWalletUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Escenario de Feature: Carlos (Bolivia) en el librecambista.
 *
 * El cambista anuncia "a 11 y algo", pero el trato se cierra con montos: Carlos dicta
 * "vendí 11 dólares, me dieron 112 bs". El intérprete propone la transferencia con lo que
 * entregó y lo que recibió, sugiere sus billeteras por moneda y, al confirmar, el libro
 * guarda la tasa derivada exacta (112/11), sin redondear a 10.18.
 */
class CarlosExchangeFeatureTest {
    private val wallets = InMemoryWalletRepository()
    private val ids = SequentialIdGenerator("carlos")
    private val clock = FixedClock(at(10))
    private val device = FixedDevice("celular-carlos")
    private val ledger = LedgerContext(wallets, ids, clock, device)
    private val ctx = DraftContext(InMemoryDraftRepository(), wallets, InMemoryWalletAliasRepository(), ids, clock, device)

    // es-BO, UTC-4
    private val interpreter =
        TextDraftInterpreter(FakeInstalledPacks(listOf(TestPacks.spanish), TestPacks.templates), FixedRegionalSettings())
    private val capture = CaptureDraftUseCase(ctx, interpreter)
    private val confirm = ConfirmDraftUseCase(ctx, ledger)
    private val status = GetWalletStatusUseCase(wallets)

    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private fun usd(canonical: String) = Money.parseCanonical(canonical, Currency.USD)

    private suspend fun wallet(
        name: String,
        opening: Money,
        type: WalletType,
    ): Wallet =
        assertIs<WalletOperationResult.WalletCreated>(
            CreateWalletUseCase(ledger)("carlos", name, opening.currency, type, opening),
        ).wallet

    private suspend fun dictate(text: String): Draft = capture("carlos", Evidence(EvidenceSource.VOICE, text)).single()

    @Test
    fun `vende dolares en el librecambista y el libro guarda la tasa exacta`() =
        runTest {
            val billetera = wallet("Billetera", bs("20"), WalletType.CASH)
            val colchon = wallet("Dólares colchón", usd("300"), WalletType.CASH)
            SetDefaultWalletUseCase(ctx)("carlos", billetera.id)

            val draft = assertIs<Draft.Ready>(dictate("vendí 11 dólares, me dieron 112 bs"))
            assertEquals(Confidence.LOW, draft.confidence)

            val confirmed = assertIs<Draft.Confirmed>(assertIs<DraftResult.Ok>(confirm(draft.id)).draft)
            val event = assertIs<LedgerEvent.Transfer>(wallets.findEvent(confirmed.ledgerEventId))
            assertEquals(colchon.id, event.fromWalletId)
            assertEquals(billetera.id, event.toWalletId)
            assertEquals(usd("11"), event.amountOut)
            assertEquals(bs("112"), event.amountIn)
            assertEquals(ExchangeRate.of(usd("11"), bs("112")), event.derivedRate())

            assertEquals(usd("289"), status(colchon.id)!!.balance)
            assertEquals(bs("132"), status(billetera.id)!!.balance)
        }

    @Test
    fun `con dos billeteras en dolares el borrador espera que elija el origen`() =
        runTest {
            val billetera = wallet("Billetera", bs("20"), WalletType.CASH)
            wallet("Dólares colchón", usd("300"), WalletType.CASH)
            wallet("Dólares banco", usd("500"), WalletType.BANK)
            SetDefaultWalletUseCase(ctx)("carlos", billetera.id)

            val draft = assertIs<Draft.Incomplete>(dictate("vendí 11 dólares, me dieron 112 bs"))
            assertEquals(setOf(DraftField.WALLET), draft.missing)
        }
}
