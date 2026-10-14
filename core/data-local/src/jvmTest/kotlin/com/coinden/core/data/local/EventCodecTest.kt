package com.coinden.core.data.local

import com.coinden.core.data.local.json.EventCodec
import com.coinden.core.data.local.json.StateCodec
import com.coinden.core.domain.model.AnchorCurrencies
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.ResponseFormat
import com.coinden.core.domain.model.SavedRateSelector
import com.coinden.core.domain.model.SelectorStatus
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryOrigin
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.category.CategoryRule
import com.coinden.core.domain.model.category.ClassificationEvent
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.NonMovementIntent
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.ReadyMovement
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.wallet.LedgerEvent
import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletStatus
import com.coinden.core.domain.model.wallet.WalletType
import kotlin.test.Test
import kotlin.test.assertEquals

/** Cada objeto guardado y vuelto a leer es igual al original, con dinero y tasas exactos. */
class EventCodecTest {
    private val vale = Currency(code = "VALE", symbol = "vl", decimals = 0, name = "Vale del súper", isCustom = true)

    private fun bs(v: String) = Money.parseCanonical(v, Currency.BOB)

    private fun usd(v: String) = Money.parseCanonical(v, Currency.USD)

    private fun btc(v: String) = Money.parseCanonical(v, Currency.BTC)

    private val t = 1_760_000_000_000L

    @Test
    fun `eventos del libro`() {
        val events =
            listOf(
                LedgerEvent.OpeningBalance("e1", "dev", t, t + 1, "w1", bs("100.50")),
                LedgerEvent.Adjustment("e2", "dev", t, t + 1, "w1", bs("-3.25")),
                LedgerEvent.Expense("e3", "dev", t, t + 1, "w1", Money.parseCanonical("25", vale)),
                LedgerEvent.Income("e4", "dev", t, t + 1, "w2", btc("0.00000001")),
                LedgerEvent.Transfer("e5", "dev", t, t + 1, "w3", "w1", usd("11"), bs("112")),
                LedgerEvent.Annulment("e6", "dev", t, t + 1, "e5", setOf("w3", "w1")),
            )
        events.forEach { event ->
            val stored = EventCodec.encodeLedger(event)
            assertEquals(event, EventCodec.decodeLedger(stored.type, stored.version, stored.payload))
        }
    }

    @Test
    fun `eventos de borradores con todas las propuestas`() {
        val origins = mapOf(DraftField.KIND to FieldOrigin.STATED, DraftField.WALLET to FieldOrigin.SUGGESTED)
        val proposals =
            listOf(
                MovementProposal.Expense(ProposedAmount("45", Currency.BOB), WalletRef.ByHint("bnb"), t, "almuerzo", origins),
                MovementProposal.Income(ProposedAmount("200.5"), WalletRef.ById("w1"), null, null, emptyMap()),
                MovementProposal.Expense(ProposedAmount("45"), WalletRef.ById("w2"), null, "almuerzo", origins, PaymentMethod.TRANSFER),
                MovementProposal.Expense(ProposedAmount("20", Currency.USD), null, null, null, emptyMap(), PaymentMethod.CASH),
                MovementProposal.Transfer(
                    ProposedAmount("11", Currency.USD),
                    WalletRef.ById("w3"),
                    ProposedAmount("112", Currency.BOB),
                    WalletRef.ById("w1"),
                    t,
                    "cambio",
                    origins,
                ),
                MovementProposal.AdjustTo(ProposedAmount("140"), WalletRef.ById("w1"), null, "conteo", origins),
                MovementProposal.NotAMovement(NonMovementIntent.LOAN, "le preste a Juan"),
                MovementProposal.Unknown("hola"),
            )
        val movements =
            listOf(
                ReadyMovement.Expense(bs("45"), "w1", t, "almuerzo"),
                ReadyMovement.Income(Money.parseCanonical("3", vale), "w1", t, null),
                ReadyMovement.Transfer(usd("11"), "w3", bs("112"), "w1", t, null),
                ReadyMovement.AdjustTo(bs("140"), "w1", t, "conteo"),
            )
        val events =
            listOf(
                DraftEvent.Captured("d1", "draft", "carlos", "dev", t, t, Evidence(EvidenceSource.PHOTO, "BNB 45,00", "img-1")),
                DraftEvent.Captured("d2", "draft", "carlos", "dev", t, t, Evidence(EvidenceSource.VOICE)),
                DraftEvent.Discarded("d3", "draft", "carlos", "dev", t, t),
            ) +
                proposals.mapIndexed { i, p -> DraftEvent.Interpreted("i$i", "draft", "carlos", "dev", t, t, p) } +
                proposals.mapIndexed { i, p -> DraftEvent.Revised("r$i", "draft", "carlos", "dev", t, t, p) } +
                movements.mapIndexed { i, m -> DraftEvent.Confirmed("c$i", "draft", "carlos", "dev", t, t, "e$i", m) }
        events.forEach { event ->
            val stored = EventCodec.encodeDraft(event)
            assertEquals(event, EventCodec.decodeDraft(stored.type, stored.version, stored.payload))
        }
    }

    @Test
    fun `clasificaciones`() {
        val events =
            listOf(
                ClassificationEvent(
                    "k1", "m1", "carlos", "dev", t, t, "almuerzo",
                    CategoryRef.Base(
                        BaseCategory.FOOD,
                    ),
                    CategoryOrigin.PACK,
                ),
                ClassificationEvent("k2", "m1", "carlos", "dev", t, t, null, CategoryRef.Custom("cat-1"), CategoryOrigin.USER),
                ClassificationEvent("k3", "m1", "carlos", "dev", t, t, null, null, null),
            )
        events.forEach { event ->
            val stored = EventCodec.encodeClassification(event)
            assertEquals(event, EventCodec.decodeClassification(stored.type, stored.version, stored.payload))
        }
    }

    @Test
    fun `estado configurable - billetera, regla, categoria propia y perfil con cada estrategia`() {
        val wallet =
            Wallet(
                "w1",
                "carlos",
                "Dólares colchón",
                Currency.USD,
                WalletType.CREDIT,
                allowsNegative = false,
                status = WalletStatus.ARCHIVED,
            )
        assertEquals(wallet, StateCodec.decodeWallet(StateCodec.encodeWallet(wallet)))

        val rule = CategoryRule("carlos", "almuerzo", CategoryRef.Base(BaseCategory.FOOD))
        assertEquals(rule, StateCodec.decodeRule(StateCodec.encodeRule(rule)))

        val custom = CustomCategory("cat-1", "carlos", "Mascotas 🐶", archived = true)
        assertEquals(custom, StateCodec.decodeCustomCategory(StateCodec.encodeCustomCategory(custom)))

        val usdToBs = ExchangeRate.of(usd("11"), bs("112"))
        // 1 USD = 1/3 de VALE: una tasa sin decimal exacto
        val usdToVale = ExchangeRate.of(usd("3"), Money.parseCanonical("1", vale))
        val selector =
            SavedRateSelector(
                "s1",
                Currency.BOB,
                "https://example.org/rates",
                ResponseFormat.HTML,
                "#usd .sell",
                usdToBs,
                t,
                SelectorStatus.NEEDS_RECONFIRMATION,
            )
        val strategies =
            listOf(
                ExchangeRateStrategy.Fixed(usdToBs),
                ExchangeRateStrategy.ManualDaily(usdToVale, t),
                ExchangeRateStrategy.Verified("dolarapi-blue", usdToBs),
                ExchangeRateStrategy.CustomSelector(selector),
            )
        strategies.forEach { strategy ->
            val profile = UserCurrencyProfile("carlos", AnchorCurrencies.USD, Currency.BOB, strategy)
            assertEquals(profile, StateCodec.decodeProfile(StateCodec.encodeProfile(profile)))
        }
    }

    @Test
    fun `el dinero se guarda como texto exacto, nunca como numero flotante`() {
        val stored = EventCodec.encodeLedger(LedgerEvent.Income("e1", "dev", t, t, "w1", btc("0.00000001")))
        assertEquals(true, "\"0.00000001\"" in stored.payload, stored.payload)
        val transfer = EventCodec.encodeLedger(LedgerEvent.Transfer("e2", "dev", t, t, "w3", "w1", usd("11"), bs("112")))
        assertEquals(false, "10.18" in transfer.payload, transfer.payload)
    }

    @Test
    fun `un tipo desconocido o una version futura se omiten al leer sin fallar`() {
        val stored = EventCodec.encodeLedger(LedgerEvent.Expense("e1", "dev", t, t, "w1", bs("3")))

        assertEquals(null, EventCodec.decodeLedger("ledger.commitment_paid", 1, "{\"lo\":\"que sea\"}"))
        assertEquals(null, EventCodec.decodeLedger(stored.type, EventCodec.VERSION + 1, stored.payload))
        assertEquals(null, EventCodec.decodeDraft("draft.voice_note", 1, "{}"))
        assertEquals(null, EventCodec.decodeClassification("classification.split", 1, "{}"))
    }

    @Test
    fun `una propuesta guardada antes de existir el medio de pago se sigue leyendo`() {
        // JSON versión 1 tal como lo guardaba local-persistence, sin el campo paymentMethod
        val payload =
            """{"draft":{"id":"i1","draftId":"draft","ownerId":"carlos","deviceId":"dev","occurredAt":1,"recordedAt":1},""" +
                """"proposal":{"kind":"expense","amount":{"value":"45"},"wallet":{"hint":"qr"},"note":"almuerzo"}}"""
        val event = EventCodec.decodeDraft("draft.interpreted", 1, payload) as DraftEvent.Interpreted
        val proposal = event.proposal as MovementProposal.Expense
        assertEquals(null, proposal.paymentMethod)
        assertEquals(WalletRef.ByHint("qr"), proposal.wallet)
    }
}
