package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.Currency

/**
 * Locale del usuario como datos, sin tablas de países: el idioma elegido, el país de la región,
 * la moneda de esa región (la usa "$" o "pesos") y su separador decimal. Los llena el adaptador
 * de configuración regional con el catálogo de monedas y el formato numérico de la plataforma.
 */
data class UserLocale(
    val language: String,
    val country: String,
    val currency: Currency,
    val decimalSeparator: Char,
)

/** Configuración regional del dispositivo: locale y desplazamiento horario en minutos (UTC-4 = -240). */
data class RegionalSettings(
    val locale: UserLocale,
    val utcOffsetMinutes: Int,
)

/** Monto leído como decimal canónico; [ambiguous] si se eligió una lectura por el locale. */
data class ReadAmount(
    val canonical: String,
    val ambiguous: Boolean,
)

/**
 * Lee números escritos con separadores de cualquier país sin alterar sus dígitos: solo
 * normaliza los separadores al decimal canónico ("12.500,00" -> "12500.00").
 */
object AmountReader {
    private val NUMBER = Regex("""^\d+([.,]\d+)*$""")

    fun read(
        raw: String,
        locale: UserLocale,
    ): ReadAmount? {
        val text = raw.trim()
        if (!NUMBER.matches(text)) return null
        val dots = text.count { it == '.' }
        val commas = text.count { it == ',' }

        if (dots > 0 && commas > 0) {
            // Ambos separadores: el último es el decimal.
            val decimal = if (text.lastIndexOf('.') > text.lastIndexOf(',')) '.' else ','
            return split(text, decimal)
        }
        val separator =
            when {
                dots > 0 -> '.'
                commas > 0 -> ','
                else -> return ReadAmount(text, ambiguous = false)
            }
        if (dots + commas > 1) return ReadAmount(text.filter { it.isDigit() }, ambiguous = false)

        val integer = text.substringBefore(separator)
        val fraction = text.substringAfter(separator)
        val looksLikeThousands = fraction.length == 3 && !integer.startsWith("0")
        return if (looksLikeThousands) {
            ReadAmount(integer + fraction, ambiguous = separator == locale.decimalSeparator)
        } else {
            ReadAmount("$integer.$fraction", ambiguous = false)
        }
    }

    private fun split(
        text: String,
        decimal: Char,
    ): ReadAmount {
        val integer = text.substringBeforeLast(decimal).filter { it.isDigit() }
        val fraction = text.substringAfterLast(decimal)
        return ReadAmount("$integer.$fraction", ambiguous = false)
    }
}

/** Un monto encontrado en el texto con su moneda (si se reconoció) y su signo. */
data class MoneyMatch(
    val amount: ReadAmount,
    val currency: Currency?,
    /** La moneda salió de un símbolo compartido ("$") o de "pesos": se resolvió con el locale. */
    val currencyInferred: Boolean,
    val negative: Boolean,
    val start: Int,
    val end: Int,
)

/**
 * Busca montos en una línea y su moneda por símbolo ("Bs", "US$", "R$", "€", "S/", "$"),
 * código ISO o palabra del paquete de idioma ("dolares", "reais"; null = moneda del locale).
 */
object MoneyScanner {
    private val NUMBER = Regex("""\d[\d.,]*\d|\d""")
    private val SYMBOL_BEFORE = Regex("""(US[$]|U[$]S|R[$]|Bs\.?|S/\.?|€|£|[$])\s*$""", RegexOption.IGNORE_CASE)
    private val SYMBOLS =
        mapOf(
            "us$" to "USD",
            "u\$s" to "USD",
            "r$" to "BRL",
            "bs" to "BOB",
            "bs." to "BOB",
            "s/" to "PEN",
            "s/." to "PEN",
            "€" to "EUR",
            "£" to "GBP",
        )
    private val ISO_CODES =
        setOf("USD", "EUR", "BOB", "BRL", "ARS", "CLP", "COP", "PEN", "MXN", "UYU", "PYG", "GBP", "CNY", "BTC", "USDT")

    fun scan(
        line: String,
        locale: UserLocale,
        currencyWords: Map<String, String?>,
    ): List<MoneyMatch> =
        NUMBER
            .findAll(line)
            .mapNotNull { number ->
                val amount = AmountReader.read(number.value, locale) ?: return@mapNotNull null
                val before = line.substring(0, number.range.first)
                val after = line.substring(number.range.last + 1)

                var currency: Currency? = null
                var inferred = false
                var start = number.range.first
                SYMBOL_BEFORE.find(before)?.let { symbol ->
                    val key = symbol.groupValues[1].lowercase()
                    if (key == "$") {
                        currency = locale.currency
                        inferred = true
                    } else {
                        currency = Currency.of(SYMBOLS.getValue(key))
                    }
                    start = symbol.range.first
                }
                val nextWord = Regex("""^\s*([\p{L}$]+)""").find(after)?.groupValues?.get(1)
                if (currency == null && nextWord != null) {
                    val normalized = TextNormalizer.normalize(nextWord)
                    when {
                        nextWord.uppercase() in ISO_CODES -> currency = Currency.of(nextWord.uppercase())
                        normalized in SYMBOLS -> currency = Currency.of(SYMBOLS.getValue(normalized))
                        normalized in currencyWords -> {
                            val code = currencyWords[normalized]
                            currency = if (code != null) Currency.of(code) else locale.currency
                            inferred = code == null
                        }
                    }
                }
                val negative =
                    before
                        .trimEnd()
                        .removeSuffix(SYMBOL_BEFORE.find(before)?.value?.trim() ?: "")
                        .trimEnd()
                        .endsWith("-")
                MoneyMatch(amount, currency, inferred, negative, start, number.range.last + 1)
            }.toList()
}
