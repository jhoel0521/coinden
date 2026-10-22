package com.coinden.app.profile

/** El sitio del proyecto (GitHub Pages): inicio, política de privacidad y cómo apoyar. */
object ProjectSite {
    private const val BASE = "https://jhoel0521.github.io/coinden/"

    /** Crédito en Acerca de: el nombre no se traduce; al tocarlo se abre el repositorio. */
    const val AUTHOR = "@jhoel0521"
    const val REPOSITORY = "https://github.com/jhoel0521/coinden"

    /** La página de inicio en el idioma de la app; sin página propia, la versión en inglés. */
    fun url(language: String): String =
        when (language) {
            "es" -> BASE
            "pt" -> BASE + "pt/"
            else -> BASE + "en/"
        }
}
