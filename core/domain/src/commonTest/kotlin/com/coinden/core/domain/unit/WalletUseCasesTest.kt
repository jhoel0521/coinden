package com.coinden.core.domain.unit

import com.coinden.core.domain.feature.FixedClock
import com.coinden.core.domain.feature.FixedDevice
import com.coinden.core.domain.feature.InMemoryWalletRepository
import com.coinden.core.domain.feature.SequentialIdGenerator
import com.coinden.core.domain.feature.at
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.LedgerProjection
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletError
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletStatus
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.usecase.wallet.AnnulMovementUseCase
import com.coinden.core.domain.usecase.wallet.ArchiveWalletUseCase
import com.coinden.core.domain.usecase.wallet.CreateWalletUseCase
import com.coinden.core.domain.usecase.wallet.GetActiveWalletBalancesUseCase
import com.coinden.core.domain.usecase.wallet.GetWalletStatusUseCase
import com.coinden.core.domain.usecase.wallet.LedgerContext
import com.coinden.core.domain.usecase.wallet.MovementKind
import com.coinden.core.domain.usecase.wallet.ReactivateWalletUseCase
import com.coinden.core.domain.usecase.wallet.RegisterMovementUseCase
import com.coinden.core.domain.usecase.wallet.RegisterTransferUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WalletUseCasesTest {
    private val repo = InMemoryWalletRepository()
    private val clock = FixedClock(at(21))
    private val ctx = LedgerContext(repo, SequentialIdGenerator(), clock, FixedDevice("celular"))
    private val create = CreateWalletUseCase(ctx)
    private val register = RegisterMovementUseCase(ctx)
    private val transfer = RegisterTransferUseCase(ctx)
    private val annul = AnnulMovementUseCase(ctx)
    private val archive = ArchiveWalletUseCase(repo)
    private val reactivate = ReactivateWalletUseCase(repo)
    private val status = GetWalletStatusUseCase(repo)
    private val balances = GetActiveWalletBalancesUseCase(repo)

    private fun bs(cents: Long) = Money(cents, Currency.BOB)

    private fun usd(cents: Long) = Money(cents, Currency.USD)

    private suspend fun newWallet(
        name: String,
        opening: Money,
        type: WalletType = WalletType.CASH,
        occurred: Long = at(8),
    ): Wallet {
        val result = create("carlos", name, opening.currency, type, opening, occurredAtEpochMs = occurred)
        return assertIs<WalletOperationResult.WalletCreated>(result).wallet
    }

    private suspend fun balanceOf(walletId: String) = status(walletId)!!.balance

    private fun recorded(result: WalletOperationResult) = assertIs<WalletOperationResult.EventRecorded>(result).event

    private fun rejected(result: WalletOperationResult) = assertIs<WalletOperationResult.Rejected>(result).error

    // --- 4.1 Creación ---

    @Test
    fun `crear billetera registra el saldo inicial con identidad y fechas`() =
        runTest {
            val result = create("carlos", "Billetera", Currency.BOB, WalletType.CASH, bs(15_000), occurredAtEpochMs = at(8))
            val created = assertIs<WalletOperationResult.WalletCreated>(result)
            assertEquals(Money(15_000, Currency.BOB), created.openingBalance.amount)
            assertEquals("celular", created.openingBalance.deviceId)
            assertEquals(at(8), created.openingBalance.occurredAtEpochMs)
            assertEquals(at(21), created.openingBalance.recordedAtEpochMs)
            assertEquals(WalletStatus.ACTIVE, created.wallet.status)
            assertFalse(created.wallet.allowsNegative)
            assertEquals(bs(15_000), balanceOf(created.wallet.id))
        }

    @Test
    fun `billetera BTC guarda satoshis exactos`() =
        runTest {
            val ledger = newWallet("Ledger", Money(5_000_000, Currency.BTC), WalletType.CRYPTO)
            assertEquals(Money(5_000_000, Currency.BTC), balanceOf(ledger.id))
            assertEquals("0.05000000", balanceOf(ledger.id).toNumericString())
        }

    @Test
    fun `nombre vacio es rechazado sin registrar nada`() =
        runTest {
            assertEquals(WalletError.InvalidName, rejected(create("carlos", "  ", Currency.BOB, WalletType.CASH, bs(0))))
            assertTrue(repo.listWallets("carlos").isEmpty())
            assertTrue(repo.allEvents.isEmpty())
        }

    @Test
    fun `saldo inicial en otra moneda es rechazado`() =
        runTest {
            assertEquals(WalletError.CurrencyMismatch, rejected(create("carlos", "Billetera", Currency.BOB, WalletType.CASH, usd(100))))
        }

    // --- 4.2 Archivado ---

    @Test
    fun `archivar conserva historial y rechaza movimientos nuevos`() =
        runTest {
            val bnb = newWallet("BNB Bs", bs(20_000), WalletType.BANK)
            recorded(register(bnb.id, MovementKind.EXPENSE, bs(1_000), at(12)))
            archive(bnb.id)

            assertEquals(2, repo.eventsForWallet(bnb.id).size)
            assertEquals(WalletError.WalletArchived, rejected(register(bnb.id, MovementKind.EXPENSE, bs(100))))
            assertTrue(balances("carlos").none { it.walletId == bnb.id })
        }

    @Test
    fun `reactivar mantiene el saldo y vuelve a aceptar movimientos`() =
        runTest {
            val bnb = newWallet("BNB Bs", bs(20_000), WalletType.BANK)
            archive(bnb.id)
            val updated = assertIs<WalletOperationResult.WalletUpdated>(reactivate(bnb.id))
            assertEquals(WalletStatus.ACTIVE, updated.wallet.status)
            assertEquals(bs(20_000), balanceOf(bnb.id))
            recorded(register(bnb.id, MovementKind.EXPENSE, bs(100)))
        }

    // --- 4.3 Movimientos simples y control cronológico ---

    @Test
    fun `gasto, ingreso y ajuste actualizan el saldo derivado`() =
        runTest {
            val cash = newWallet("Billetera", bs(15_000))
            recorded(register(cash.id, MovementKind.EXPENSE, bs(300), at(12)))
            recorded(register(cash.id, MovementKind.INCOME, bs(5_000), at(18)))
            recorded(register(cash.id, MovementKind.ADJUSTMENT, bs(-700), at(20)))
            assertEquals(bs(19_000), balanceOf(cash.id))
        }

    @Test
    fun `moneda distinta y montos invalidos son rechazados`() =
        runTest {
            val cash = newWallet("Billetera", bs(15_000))
            assertEquals(WalletError.CurrencyMismatch, rejected(register(cash.id, MovementKind.EXPENSE, usd(1_200))))
            assertEquals(WalletError.InvalidAmount, rejected(register(cash.id, MovementKind.EXPENSE, bs(0))))
            assertEquals(WalletError.InvalidAmount, rejected(register(cash.id, MovementKind.EXPENSE, bs(-500))))
            assertEquals(WalletError.InvalidAmount, rejected(register(cash.id, MovementKind.ADJUSTMENT, bs(0))))
        }

    @Test
    fun `olvido el cajero - el gasto se rechaza con faltante y fecha`() =
        runTest {
            val cash = newWallet("Billetera", bs(1_000))
            val error = rejected(register(cash.id, MovementKind.EXPENSE, bs(4_500), at(13)))
            assertEquals(WalletError.InsufficientFunds(cash.id, bs(3_500), at(13)), error)
            assertEquals(bs(1_000), balanceOf(cash.id))
        }

    @Test
    fun `gasto con fecha pasada que deja un hueco es rechazado aunque el final sea positivo`() =
        runTest {
            val cash = newWallet("Billetera", bs(1_000))
            recorded(register(cash.id, MovementKind.INCOME, bs(20_000), at(14)))
            val error = rejected(register(cash.id, MovementKind.EXPENSE, bs(4_500), at(13)))
            assertEquals(WalletError.InsufficientFunds(cash.id, bs(3_500), at(13)), error)
        }

    @Test
    fun `credito admite saldo negativo`() =
        runTest {
            val visa = newWallet("Visa Galicia", Money(0, Currency.ARS), WalletType.CREDIT)
            recorded(register(visa.id, MovementKind.EXPENSE, Money(2_500_000, Currency.ARS), at(12)))
            assertEquals(Money(-2_500_000, Currency.ARS), balanceOf(visa.id))
            assertFalse(status(visa.id)!!.isUnbalanced)
        }

    // --- 4.4 Transferencias ---

    @Test
    fun `venta de dolares al paralelo con tasa derivada`() =
        runTest {
            val colchon = newWallet("Dólares colchón", usd(50_000))
            val cash = newWallet("Billetera", bs(1_000))
            val event = recorded(transfer(colchon.id, cash.id, usd(5_000), bs(59_400), at(10)))
            assertEquals(usd(45_000), balanceOf(colchon.id))
            assertEquals(bs(60_400), balanceOf(cash.id))
            assertEquals(bs(1_188), assertIs<LedgerEvent.Transfer>(event).derivedRate())
        }

    @Test
    fun `transferencias invalidas son rechazadas`() =
        runTest {
            val bnb = newWallet("BNB Bs", bs(20_000), WalletType.BANK)
            val cash = newWallet("Billetera", bs(1_000))
            assertEquals(WalletError.SameWallet, rejected(transfer(cash.id, cash.id, bs(100), bs(100))))
            assertEquals(WalletError.InconsistentTransferAmounts, rejected(transfer(bnb.id, cash.id, bs(20_000), bs(19_500))))
            assertEquals(WalletError.CurrencyMismatch, rejected(transfer(bnb.id, cash.id, usd(100), bs(100))))
            assertEquals(WalletError.InvalidAmount, rejected(transfer(bnb.id, cash.id, bs(0), bs(0))))
        }

    @Test
    fun `transferencia sin fondos en el origen es rechazada`() =
        runTest {
            val bnb = newWallet("BNB Bs", bs(10_000), WalletType.BANK)
            val cash = newWallet("Billetera", bs(0))
            val error = rejected(transfer(bnb.id, cash.id, bs(20_000), bs(20_000), at(9)))
            assertEquals(WalletError.InsufficientFunds(bnb.id, bs(10_000), at(9)), error)
        }

    // --- 4.5 Anulaciones ---

    @Test
    fun `corregir un gasto por anulacion conserva el historial`() =
        runTest {
            val cash = newWallet("Billetera", bs(15_000))
            val wrong = recorded(register(cash.id, MovementKind.EXPENSE, bs(5_400), at(13)))
            recorded(annul(wrong.id))
            recorded(register(cash.id, MovementKind.EXPENSE, bs(4_500), at(13)))
            assertEquals(4, repo.eventsForWallet(cash.id).size)
            assertEquals(bs(10_500), balanceOf(cash.id))
        }

    @Test
    fun `anular una transferencia revierte ambos lados`() =
        runTest {
            val colchon = newWallet("Dólares colchón", usd(50_000))
            val cash = newWallet("Billetera", bs(1_000))
            val t = recorded(transfer(colchon.id, cash.id, usd(5_000), bs(59_400), at(10)))
            recorded(annul(t.id))
            assertEquals(usd(50_000), balanceOf(colchon.id))
            assertEquals(bs(1_000), balanceOf(cash.id))
        }

    @Test
    fun `doble anulacion y anular una anulacion son rechazadas`() =
        runTest {
            val cash = newWallet("Billetera", bs(15_000))
            val g = recorded(register(cash.id, MovementKind.EXPENSE, bs(100), at(12)))
            val a = recorded(annul(g.id))
            assertEquals(WalletError.AlreadyAnnulled, rejected(annul(g.id)))
            assertEquals(WalletError.CannotAnnulAnnulment, rejected(annul(a.id)))
            assertEquals(WalletError.NotFound, rejected(annul("no-existe")))
        }

    @Test
    fun `anular un ingreso del que dependen gastos es rechazado`() =
        runTest {
            val cash = newWallet("Billetera", bs(0))
            val sueldo = recorded(register(cash.id, MovementKind.INCOME, bs(20_000), at(9)))
            recorded(register(cash.id, MovementKind.EXPENSE, bs(18_000), at(12)))
            val error = rejected(annul(sueldo.id))
            assertEquals(WalletError.InsufficientFunds(cash.id, bs(18_000), at(12)), error)
        }

    // --- 4.6 Estado, descuadre y saldos activos ---

    @Test
    fun `movimientos fusionados de otro dispositivo dejan la billetera descuadrada`() =
        runTest {
            val cash = newWallet("Billetera", bs(5_000))
            // Llegan por fusión sin pasar por la validación: celular 30 Bs a las 12:00, PC 40 Bs a las 13:00
            repo.appendEvent(LedgerEvent.Expense("cel-1", "celular", at(12), at(12), cash.id, bs(3_000)))
            repo.appendEvent(LedgerEvent.Expense("pc-1", "pc", at(13), at(13), cash.id, bs(4_000)))

            val state = status(cash.id)!!
            assertEquals(3, repo.eventsForWallet(cash.id).size)
            assertTrue(state.isUnbalanced)
            assertEquals(bs(2_000), state.shortfall)
            assertEquals(at(13), state.firstNegativeAtEpochMs)

            // Un ajuste a las 21:00 no se bloquea, pero no cuadra el hueco de las 13:00
            recorded(register(cash.id, MovementKind.ADJUSTMENT, bs(2_000), at(21)))
            assertTrue(status(cash.id)!!.isUnbalanced)

            // Un gasto que agrava el hueco sí se rechaza
            assertIs<WalletError.InsufficientFunds>(rejected(register(cash.id, MovementKind.EXPENSE, bs(100), at(13, 30))))

            // Un ajuste justo antes del faltante la vuelve a cuadrar
            recorded(register(cash.id, MovementKind.ADJUSTMENT, bs(2_000), at(12, 59)))
            assertFalse(status(cash.id)!!.isUnbalanced)
        }

    @Test
    fun `saldos activos excluyen billeteras archivadas`() =
        runTest {
            val cash = newWallet("Billetera", bs(16_500))
            val bnb = newWallet("BNB Bs", bs(2_000_000), WalletType.BANK)
            val colchon = newWallet("Dólares colchón", usd(45_000))
            val viejo = newWallet("Cuenta vieja", bs(30_000), WalletType.BANK)
            archive(viejo.id)

            val result = balances("carlos").associate { it.walletId to it.balance }
            assertEquals(mapOf(cash.id to bs(16_500), bnb.id to bs(2_000_000), colchon.id to usd(45_000)), result)
        }

    @Test
    fun `orden determinista con misma hora de ocurrencia`() =
        runTest {
            val cash = newWallet("Billetera", bs(1_000))
            clock.nowEpochMs = at(20)
            recorded(register(cash.id, MovementKind.EXPENSE, bs(100), at(12)))
            clock.nowEpochMs = at(21)
            recorded(register(cash.id, MovementKind.EXPENSE, bs(100), at(12)))
            val ordered =
                LedgerProjection(cash, repo.eventsForWallet(cash.id))
                    .effectiveEvents
                    .map { it.recordedAtEpochMs }
            assertEquals(listOf(at(21), at(20), at(21)), ordered)
        }
}
