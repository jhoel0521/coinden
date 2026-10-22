package com.coinden.app.profile

/**
 * Una librería que va dentro de la app. [textFile] es el texto completo de su licencia en
 * `files/licenses/`; sin texto (no es software libre), [url] lleva a sus términos.
 */
data class OpenSourceLibrary(
    val name: String,
    val license: String,
    val url: String,
    val textFile: String? = null,
)

/** Las librerías y fuentes que se distribuyen con CoinDen, con su licencia. */
object OpenSourceLicenses {
    private const val APACHE = "apache-2.0.txt"

    val all: List<OpenSourceLibrary> =
        listOf(
            OpenSourceLibrary("whisper.cpp (ggml)", "MIT", "https://github.com/ggml-org/whisper.cpp", "whisper.txt"),
            OpenSourceLibrary(
                "Tesseract (tesseract4android, tessdata)",
                "Apache-2.0",
                "https://github.com/tesseract-ocr/tesseract",
                APACHE,
            ),
            OpenSourceLibrary("Leptonica", "Leptonica", "http://www.leptonica.org", "leptonica.txt"),
            OpenSourceLibrary("SQLCipher", "BSD (Zetetic)", "https://www.zetetic.net/sqlcipher/", "sqlcipher.txt"),
            OpenSourceLibrary("SQLDelight", "Apache-2.0", "https://github.com/sqldelight/sqldelight", APACHE),
            OpenSourceLibrary("PdfBox-Android", "Apache-2.0", "https://github.com/TomRoush/PdfBox-Android", APACHE),
            OpenSourceLibrary("AndroidX", "Apache-2.0", "https://developer.android.com/jetpack/androidx", APACHE),
            OpenSourceLibrary("Compose Multiplatform", "Apache-2.0", "https://github.com/JetBrains/compose-multiplatform", APACHE),
            OpenSourceLibrary("Kotlin, kotlinx.coroutines, kotlinx.serialization", "Apache-2.0", "https://kotlinlang.org", APACHE),
            OpenSourceLibrary("Manrope", "OFL-1.1", "https://github.com/sharanda/manrope", "ofl_manrope.txt"),
            OpenSourceLibrary("Bricolage Grotesque", "OFL-1.1", "https://github.com/ateliertriay/bricolage", "ofl_bricolage_grotesque.txt"),
            OpenSourceLibrary("ML Kit (Google)", "ML Kit Terms", "https://developers.google.com/ml-kit/terms"),
        )

    /** Une las líneas de cada párrafo: los archivos cortan a 80 columnas y en el teléfono se ve partido. */
    fun paragraphs(text: String): String =
        text
            .replace("\r\n", "\n")
            .trim()
            .split(Regex("\n\\s*\n"))
            .joinToString("\n\n") { paragraph -> paragraph.lines().joinToString(" ") { it.trim() } }
}
