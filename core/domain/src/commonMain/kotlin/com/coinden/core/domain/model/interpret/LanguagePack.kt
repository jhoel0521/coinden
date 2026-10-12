package com.coinden.core.domain.model.interpret

/**
 * Paquete de idioma: las palabras clave que el intérprete reconoce, como datos. Agregar un idioma
 * es agregar un paquete, no código. Todas las palabras se guardan normalizadas (ver [TextNormalizer]).
 */
data class LanguagePack(
    val code: String,
    val amountLabels: Set<String>,
    val counterpartyLabels: Set<String>,
    val accountLabels: Set<String>,
    val noteLabels: Set<String>,
    val expenseWords: Set<String>,
    val incomeWords: Set<String>,
    val withdrawalWords: Set<String>,
    val exchangeWords: Set<String>,
    val notePrepositions: Set<String>,
    val conjunctions: Set<String>,
    /** Palabra que introduce la tasa en un cambio ("a 11.88", "at 11.88"). */
    val rateMarkers: Set<String>,
    val months: Map<String, Int>,
    /** Palabra de moneda -> código ISO; null = la moneda del locale ("pesos"). */
    val currencyWords: Map<String, String?>,
)

/** Une varios paquetes activos en uno solo para buscar en todos a la vez. */
fun List<LanguagePack>.merged(): LanguagePack =
    LanguagePack(
        code = joinToString("+") { it.code },
        amountLabels = flatMap { it.amountLabels }.toSet(),
        counterpartyLabels = flatMap { it.counterpartyLabels }.toSet(),
        accountLabels = flatMap { it.accountLabels }.toSet(),
        noteLabels = flatMap { it.noteLabels }.toSet(),
        expenseWords = flatMap { it.expenseWords }.toSet(),
        incomeWords = flatMap { it.incomeWords }.toSet(),
        withdrawalWords = flatMap { it.withdrawalWords }.toSet(),
        exchangeWords = flatMap { it.exchangeWords }.toSet(),
        notePrepositions = flatMap { it.notePrepositions }.toSet(),
        conjunctions = flatMap { it.conjunctions }.toSet(),
        rateMarkers = flatMap { it.rateMarkers }.toSet(),
        months = fold(emptyMap()) { acc, pack -> acc + pack.months },
        currencyWords = fold(emptyMap()) { acc, pack -> acc + pack.currencyWords },
    )

/**
 * Catálogo de paquetes. [ENGLISH] siempre está activo; los demás solo si están instalados.
 */
object LanguagePacks {
    private fun months(vararg names: Pair<List<String>, Int>): Map<String, Int> =
        names.flatMap { (words, n) -> words.map { it to n } }.toMap()

    val ENGLISH =
        LanguagePack(
            code = "en",
            amountLabels = setOf("amount", "total", "amount paid", "sum"),
            counterpartyLabels = setOf("to", "recipient", "payee", "merchant", "beneficiary", "paid to"),
            accountLabels = setOf("from", "account", "from account", "source account"),
            noteLabels = setOf("note", "memo", "description"),
            expenseWords = setOf("paid", "pay", "spent", "spend", "bought", "buy", "sent", "purchase", "debit"),
            incomeWords = setOf("received", "earned", "deposit", "deposited", "got paid"),
            withdrawalWords = setOf("withdrawal", "withdrew", "withdraw", "atm"),
            exchangeWords = setOf("sold", "exchanged", "changed"),
            notePrepositions = setOf("for", "on"),
            conjunctions = setOf("and"),
            rateMarkers = setOf("at"),
            months =
                months(
                    listOf("jan", "january") to 1, listOf("feb", "february") to 2, listOf("mar", "march") to 3,
                    listOf("apr", "april") to 4, listOf("may") to 5, listOf("jun", "june") to 6, listOf("jul", "july") to 7,
                    listOf("aug", "august") to 8, listOf("sep", "sept", "september") to 9, listOf("oct", "october") to 10,
                    listOf("nov", "november") to 11, listOf("dec", "december") to 12,
                ),
            currencyWords = mapOf("dollar" to "USD", "dollars" to "USD", "euro" to "EUR", "euros" to "EUR", "pesos" to null),
        )

    val SPANISH =
        LanguagePack(
            code = "es",
            amountLabels = setOf("importe", "monto", "total", "valor", "total pagado"),
            counterpartyLabels = setOf("beneficiario", "destinatario", "para", "comercio", "pagaste a"),
            accountLabels = setOf("cuenta origen", "cuenta", "desde", "cuenta de origen"),
            noteLabels = setOf("nota", "glosa", "concepto", "descripcion"),
            expenseWords =
                setOf(
                    "pague", "pago", "gaste", "compre", "compra", "yapeaste", "transferiste", "debito", "envie",
                    "enviaste", "pagaste",
                ),
            incomeWords = setOf("recibi", "cobre", "recibiste", "deposito", "me pagaron"),
            withdrawalWords = setOf("retiro", "retire", "saque", "cajero"),
            exchangeWords = setOf("vendi", "cambie"),
            notePrepositions = setOf("para", "de", "en"),
            conjunctions = setOf("y", "e"),
            rateMarkers = setOf("a"),
            months =
                months(
                    listOf("ene", "enero") to 1, listOf("feb", "febrero") to 2, listOf("mar", "marzo") to 3,
                    listOf("abr", "abril") to 4, listOf("may", "mayo") to 5, listOf("jun", "junio") to 6,
                    listOf("jul", "julio") to 7, listOf("ago", "agosto") to 8, listOf("sep", "set", "septiembre", "setiembre") to 9,
                    listOf("oct", "octubre") to 10, listOf("nov", "noviembre") to 11, listOf("dic", "diciembre") to 12,
                ),
            currencyWords =
                mapOf(
                    "dolar" to "USD",
                    "dolares" to "USD",
                    "boliviano" to "BOB",
                    "bolivianos" to "BOB",
                    "reales" to "BRL",
                    "euros" to "EUR",
                    "pesos" to null,
                    "peso" to null,
                ),
        )

    val PORTUGUESE =
        LanguagePack(
            code = "pt",
            amountLabels = setOf("valor", "total", "quantia", "montante", "valor pago"),
            counterpartyLabels = setOf("para", "destinatario", "favorecido", "recebedor", "quem recebeu"),
            accountLabels = setOf("conta", "conta de origem", "de"),
            noteLabels = setOf("descricao", "mensagem", "observacao"),
            expenseWords = setOf("paguei", "pago", "gastei", "comprei", "compra", "enviado", "enviei", "pagamento", "pix enviado"),
            incomeWords = setOf("recebi", "recebido", "recebeu", "pix recebido"),
            withdrawalWords = setOf("saque", "saquei", "retirada"),
            exchangeWords = setOf("vendi", "troquei"),
            notePrepositions = setOf("de", "para", "com"),
            conjunctions = setOf("e"),
            rateMarkers = setOf("a"),
            months =
                months(
                    listOf("jan", "janeiro") to 1, listOf("fev", "fevereiro") to 2, listOf("mar", "marco") to 3,
                    listOf("abr", "abril") to 4, listOf("mai", "maio") to 5, listOf("jun", "junho") to 6,
                    listOf("jul", "julho") to 7, listOf("ago", "agosto") to 8, listOf("set", "setembro") to 9,
                    listOf("out", "outubro") to 10, listOf("nov", "novembro") to 11, listOf("dez", "dezembro") to 12,
                ),
            currencyWords = mapOf("real" to "BRL", "reais" to "BRL", "dolar" to "USD", "dolares" to "USD", "euros" to "EUR"),
        )
}
