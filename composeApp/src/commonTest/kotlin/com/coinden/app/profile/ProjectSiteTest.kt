package com.coinden.app.profile

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectSiteTest {
    @Test
    fun `el sitio se abre en el idioma de la app`() {
        assertEquals("https://jhoel0521.github.io/coinden/", ProjectSite.url("es"))
        assertEquals("https://jhoel0521.github.io/coinden/en/", ProjectSite.url("en"))
        assertEquals("https://jhoel0521.github.io/coinden/pt/", ProjectSite.url("pt"))
    }

    @Test
    fun `un idioma sin pagina propia abre la version en ingles`() {
        assertEquals("https://jhoel0521.github.io/coinden/en/", ProjectSite.url("fr"))
    }

    @Test
    fun `el credito nombra al autor por su usuario de GitHub y enlaza el repositorio`() {
        assertEquals("@jhoel0521", ProjectSite.AUTHOR)
        assertEquals("https://github.com/jhoel0521/coinden", ProjectSite.REPOSITORY)
    }
}
