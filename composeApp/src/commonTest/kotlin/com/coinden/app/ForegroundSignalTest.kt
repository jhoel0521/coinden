package com.coinden.app

import kotlin.test.Test
import kotlin.test.assertEquals

class ForegroundSignalTest {
    @Test
    fun `cada vuelta al frente cambia la senal para recargar lo que se ve`() {
        val signal = ForegroundSignal()
        val before = signal.returns.value
        signal.onForeground()
        signal.onForeground()
        assertEquals(before + 2, signal.returns.value)
    }
}
