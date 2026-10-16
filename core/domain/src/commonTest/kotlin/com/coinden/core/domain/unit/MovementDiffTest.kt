package com.coinden.core.domain.unit

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.category.CategoryRef
import com.coinden.core.domain.model.draft.CorrectionField
import com.coinden.core.domain.model.draft.FieldChange
import com.coinden.core.domain.model.draft.MovementDiff
import com.coinden.core.domain.model.draft.ReadyMovement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MovementDiffTest {
    private fun bs(canonical: String) = Money.parseCanonical(canonical, Currency.BOB)

    private val lunch = ReadyMovement.Expense(bs("35"), "bnb", occurredAtEpochMs = 1_000L, note = "almuerzo")

    @Test
    fun `las diferencias del almuerzo traen el valor anterior aunque estuviera vacio`() {
        val receipt = lunch.copy(amount = bs("32"), occurredAtEpochMs = 61_000L, reference = "400000001")

        val changes = MovementDiff.between(lunch, null, receipt, null)

        assertEquals(
            listOf(
                FieldChange.Amount(bs("35"), bs("32"), large = false),
                FieldChange.OccurredAt(1_000L, 61_000L),
                FieldChange.Reference(null, "400000001"),
            ),
            changes,
        )
        assertTrue(MovementDiff.touchesMoney(changes))
    }

    @Test
    fun `la nota igual no figura como cambio`() {
        assertEquals(emptyList(), MovementDiff.between(lunch, null, lunch.copy(), null))
    }

    @Test
    fun `cambiar nota, categoria o referencia no toca el dinero`() {
        val changes =
            MovementDiff.between(
                lunch,
                CategoryRef.Base(BaseCategory.FOOD),
                lunch.copy(note = "almuerzo con Ana", reference = "400000001"),
                CategoryRef.Base(BaseCategory.LEISURE),
            )

        assertEquals(
            listOf(CorrectionField.NOTE, CorrectionField.CATEGORY, CorrectionField.REFERENCE),
            changes.map { it.field },
        )
        assertFalse(MovementDiff.touchesMoney(changes))
    }

    @Test
    fun `otra billetera y otro tipo tocan el dinero`() {
        val income = ReadyMovement.Income(bs("35"), "billetera", 1_000L, "almuerzo")

        val changes = MovementDiff.between(lunch, null, income, null)

        assertEquals(listOf(CorrectionField.KIND, CorrectionField.WALLET), changes.map { it.field })
        assertTrue(MovementDiff.touchesMoney(changes))
    }

    @Test
    fun `el OCR que pierde la coma es una diferencia grande y la chica no`() {
        assertTrue(MovementDiff.isLargeAmountChange(bs("35"), bs("320")))
        assertFalse(MovementDiff.isLargeAmountChange(bs("35"), bs("32")))
    }

    @Test
    fun `el doble y la mitad exactos ya son grandes`() {
        assertTrue(MovementDiff.isLargeAmountChange(bs("35"), bs("70")))
        assertFalse(MovementDiff.isLargeAmountChange(bs("35"), bs("69.99")))
        assertTrue(MovementDiff.isLargeAmountChange(bs("35"), bs("17.50")))
        assertFalse(MovementDiff.isLargeAmountChange(bs("35"), bs("17.51")))
    }

    @Test
    fun `otra moneda no se compara como diferencia grande`() {
        assertFalse(MovementDiff.isLargeAmountChange(bs("35"), Money.parseCanonical("350", Currency.USD)))
    }
}
