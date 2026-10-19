package com.coinden.app

import com.coinden.app.capture.DraftFormPresenter
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class FormPresentersTest {
    @Test
    fun `la misma entrada de navegacion recupera su formulario al recrear la pantalla`() {
        val container = testContainer()
        val forms = FormPresenters()
        val first = forms.getOrPut("entrada-1") { DraftFormPresenter(container) }

        // Android recrea la pantalla (rotar, tamaño de letra): la entrada conserva su id.
        val again = forms.getOrPut("entrada-1") { DraftFormPresenter(container) }

        assertSame(first, again)
    }

    @Test
    fun `al terminar el formulario se suelta y una entrada nueva empieza de cero`() {
        val container = testContainer()
        val forms = FormPresenters()
        val first = forms.getOrPut("entrada-1") { DraftFormPresenter(container) }
        forms.remove("entrada-1")

        assertNotSame(first, forms.getOrPut("entrada-1") { DraftFormPresenter(container) })
    }
}
