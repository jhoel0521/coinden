package com.coinden.app.capture

import kotlin.test.Test
import kotlin.test.assertEquals

/** La barra de captura nunca reparte más de tres botones por fila, para que el texto se lea entero. */
class CaptureBarLayoutTest {
    @Test
    fun `Ana con Ditar, Foto y Escrever ve los tres en una sola fila`() {
        assertEquals(listOf(listOf("Ditar", "Foto", "Escrever")), CaptureBarLayout.rows(listOf("Ditar", "Foto", "Escrever")))
    }

    @Test
    fun `con una acción adicional queda sola en su propia fila`() {
        val rows = CaptureBarLayout.rows(listOf("Ditar", "Foto", "Escrever", "Adicionar ao último"))
        assertEquals(listOf(listOf("Ditar", "Foto", "Escrever"), listOf("Adicionar ao último")), rows)
    }

    @Test
    fun `una sola acción, como escuchando, ocupa la fila`() {
        assertEquals(listOf(listOf("Escuchando")), CaptureBarLayout.rows(listOf("Escuchando")))
    }

    @Test
    fun `sin acciones no hay filas`() {
        assertEquals(emptyList(), CaptureBarLayout.rows(emptyList<String>()))
    }
}
