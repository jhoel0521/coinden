package com.coinden.core.domain.unit

import com.coinden.core.domain.model.interpret.LanguagePacks
import com.coinden.core.domain.model.interpret.merged
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanguagePackTest {
    @Test
    fun `cada paquete trae sus marcadores de lo recibido en un cambio`() {
        assertEquals(setOf("got", "received", "for"), LanguagePacks.ENGLISH.receivedMarkers)
        assertEquals(setOf("me dieron", "me dio", "recibi", "por"), LanguagePacks.SPANISH.receivedMarkers)
        assertEquals(setOf("me deram", "recebi", "por"), LanguagePacks.PORTUGUESE.receivedMarkers)
    }

    @Test
    fun `al unir paquetes se unen sus marcadores de lo recibido`() {
        val merged = listOf(LanguagePacks.ENGLISH, LanguagePacks.SPANISH).merged()

        assertTrue(merged.receivedMarkers.containsAll(setOf("got", "for", "me dieron", "por")))
        assertEquals(7, merged.receivedMarkers.size)
    }
}
