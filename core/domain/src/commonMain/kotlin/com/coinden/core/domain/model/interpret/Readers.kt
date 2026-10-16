package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.ExchangeRate
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.model.draft.WalletSuggestion
import com.coinden.core.domain.model.draft.WalletSuggestionReason
import com.coinden.core.domain.model.wallet.PaymentMethod
import com.coinden.core.domain.model.wallet.WalletType

/** Todo lo que un lector necesita, además del texto. */
data class InterpretContext(
    val pack: LanguagePack,
    val locale: UserLocale,
    val utcOffsetMinutes: Int,
    val directory: WalletDirectory,
)

/** Lo que se leyó de un monto, listo para la propuesta, con los orígenes de monto y moneda. */
private data class AmountRead(
    val proposed: ProposedAmount,
    val amountOrigin: FieldOrigin,
    val currencyOrigin: FieldOrigin?,
)

private fun MoneyMatch.toRead(certain: FieldOrigin): AmountRead =
    AmountRead(
        proposed = ProposedAmount(amount.canonical, currency),
        amountOrigin = if (amount.ambiguous) FieldOrigin.INFERRED else certain,
        currencyOrigin = currency?.let { if (currencyInferred) FieldOrigin.INFERRED else certain },
    )

private fun originsOf(vararg pairs: Pair<DraftField, FieldOrigin?>): Map<DraftField, FieldOrigin> =
    pairs.mapNotNull { (field, origin) -> origin?.let { field to it } }.toMap()

/** ¿El texto normalizado contiene la palabra o frase clave como palabras completas? */
private fun String.containsWords(phrase: String): Boolean = " $this ".contains(" $phrase ")

private fun defaultWallet(directory: WalletDirectory): WalletRef? = directory.defaultWalletId?.let { WalletRef.ById(it) }

/**
 * Lee frases escritas o dictadas: "page 3 bs para trasmporte", "almuerzo 45 qr",
 * "vendi 50 dolares a 11.88", "I pay 3 dollars for transportation".
 */
object PhraseReader {
    fun read(
        text: String,
        ctx: InterpretContext,
    ): List<MovementProposal> = segments(TextNormalizer.words(text), ctx.pack).mapNotNull { readSegment(it, ctx) }

    /**
     * Divide por conectores ("y", "and", "e") solo si cada parte tiene un número. Un conector
     * seguido de lo recibido en un cambio ("... y me dieron 1190 bs") no divide.
     */
    private fun segments(
        words: List<Word>,
        pack: LanguagePack,
    ): List<List<Word>> {
        val parts = mutableListOf(mutableListOf<Word>())
        words.forEachIndexed { i, word ->
            val splits = word.normalized in pack.conjunctions && receivedMarkerEnd(words, i + 1, pack) == null
            if (splits) parts += mutableListOf<Word>() else parts.last() += word
        }
        val merged = mutableListOf<MutableList<Word>>()
        parts.filter { it.isNotEmpty() }.forEach { part ->
            if (part.none { it.normalized.any(Char::isDigit) } && merged.isNotEmpty()) merged.last() += part else merged += part
        }
        return merged
    }

    private enum class Kind { EXPENSE, INCOME, WITHDRAWAL, EXCHANGE }

    private fun readSegment(
        words: List<Word>,
        ctx: InterpretContext,
    ): MovementProposal? {
        val pack = ctx.pack
        val used = mutableSetOf<Int>()

        // Tipo: la primera palabra que coincide con un verbo; exacta = dicho, aproximada = inferido.
        var kind = Kind.EXPENSE
        var kindOrigin = FieldOrigin.INFERRED
        run search@{
            listOf(false, true).forEach { fuzzy ->
                words.forEachIndexed { i, w ->
                    val found =
                        when {
                            TextNormalizer.matches(w.normalized, pack.exchangeWords, fuzzy) -> Kind.EXCHANGE
                            TextNormalizer.matches(w.normalized, pack.withdrawalWords, fuzzy) -> Kind.WITHDRAWAL
                            TextNormalizer.matches(w.normalized, pack.incomeWords, fuzzy) -> Kind.INCOME
                            TextNormalizer.matches(w.normalized, pack.expenseWords, fuzzy) -> Kind.EXPENSE
                            else -> null
                        }
                    if (found != null) {
                        kind = found
                        kindOrigin = if (fuzzy) FieldOrigin.INFERRED else FieldOrigin.STATED
                        used += i
                        return@search
                    }
                }
            }
        }

        // Tasa de un cambio: "a 11.88" / "at 11.88".
        var rate: String? = null
        if (kind == Kind.EXCHANGE) {
            words.forEachIndexed { i, w ->
                if (rate == null && w.normalized in pack.rateMarkers && i + 1 < words.size) {
                    AmountReader.read(words[i + 1].original, ctx.locale)?.let {
                        rate = it.canonical
                        used += i
                        used += i + 1
                    }
                }
            }
        }

        val scan = { raw: String -> MoneyScanner.scan(raw, ctx.locale, pack.currencyWords).firstOrNull() }
        val isAmount = { i: Int -> i !in used && words[i].normalized.any(Char::isDigit) && scan(words[i].original) != null }

        // Monto y moneda en una posición: "3 bs", "12 usd", "Bs 36", "$20".
        fun amountAt(amountIndex: Int): AmountRead {
            val money = scan(words[amountIndex].original)!!
            used += amountIndex
            var currency = money.currency
            var currencyInferred = money.currencyInferred
            if (currency == null) {
                // "3 bs", "12 usd", "50 dolares": la moneda en la palabra siguiente.
                words.getOrNull(amountIndex + 1)?.takeIf { it.normalized.none(Char::isDigit) }?.let { next ->
                    scan("1 ${next.original}")?.takeIf { it.currency != null }?.let {
                        currency = it.currency
                        currencyInferred = it.currencyInferred
                        used += amountIndex + 1
                    }
                }
            }
            if (currency == null) {
                // "Bs 36", "$ 20": el símbolo en la palabra anterior.
                words.getOrNull(amountIndex - 1)?.takeIf { it.normalized.none(Char::isDigit) }?.let { prev ->
                    scan("${prev.original}1")?.takeIf { it.currency != null }?.let {
                        currency = it.currency
                        currencyInferred = it.currencyInferred
                        used += amountIndex - 1
                    }
                }
            }
            return AmountRead(
                proposed = ProposedAmount(money.amount.canonical, currency),
                amountOrigin = if (money.amount.ambiguous) FieldOrigin.INFERRED else FieldOrigin.STATED,
                currencyOrigin = currency?.let { if (currencyInferred) FieldOrigin.INFERRED else FieldOrigin.STATED },
            )
        }

        // Lo recibido en un cambio: "me dieron 112 bs", "got 1190 bob", "por 1190 bs". Manda sobre la tasa.
        var received: AmountRead? = null
        if (kind == Kind.EXCHANGE) {
            for (i in words.indices) {
                if (i in used) continue
                val end = receivedMarkerEnd(words, i, pack) ?: continue
                val amountIndex = (end until minOf(end + 2, words.size)).firstOrNull(isAmount) ?: continue
                (i until end).forEach { used += it }
                received = amountAt(amountIndex)
                break
            }
        }

        // Monto entregado o gastado: el primer número que no sea la tasa ni lo recibido ("3", "45,50", "$20", "Bs25").
        val amount = amountAt(words.indices.firstOrNull(isAmount) ?: return null)

        // Billetera por alias: nombra una cuenta concreta y gana sobre el medio de pago.
        val aliasIndex = words.indices.firstOrNull { it !in used && ctx.directory.byHint(words[it].normalized) != null }
        val wallet: WalletRef?
        val walletOrigin: FieldOrigin?
        val named = if (aliasIndex == null) walletByName(words, used, ctx) else null
        if (aliasIndex != null) {
            used += aliasIndex
            wallet = WalletRef.ByHint(words[aliasIndex].normalized)
            walletOrigin = FieldOrigin.STATED
        } else if (named != null) {
            // Una palabra del nombre de la billetera ("bnb" de "BNB Bs"): cuenta como dicha.
            used += named.first
            wallet = WalletRef.ById(named.second)
            walletOrigin = FieldOrigin.STATED
        } else {
            wallet = null
            walletOrigin = null
        }

        // Medio de pago ("qr", "pix", "efectivo"): no nombra billetera, filtra las candidatas.
        val methodIndex = words.indices.firstOrNull { it !in used && words[it].normalized in pack.paymentMethodWords }
        val method =
            methodIndex?.let {
                used += it
                pack.paymentMethodWords.getValue(words[it].normalized)
            }

        val note = note(words, used, pack)
        val default = defaultWallet(ctx.directory)

        return when (kind) {
            Kind.EXPENSE, Kind.INCOME -> {
                val suggested = if (wallet == null) suggestWallet(method, amount.proposed.currency, ctx) else null
                val w = wallet ?: suggested?.first
                val origins =
                    originsOf(
                        DraftField.KIND to kindOrigin,
                        DraftField.AMOUNT to amount.amountOrigin,
                        DraftField.CURRENCY to amount.currencyOrigin,
                        DraftField.WALLET to (walletOrigin ?: w?.let { FieldOrigin.SUGGESTED }),
                    )
                if (kind == Kind.EXPENSE) {
                    MovementProposal.Expense(
                        amount.proposed,
                        w,
                        note = note,
                        origins = origins,
                        paymentMethod = method,
                        walletSuggestion = suggested?.second,
                    )
                } else {
                    MovementProposal.Income(
                        amount.proposed,
                        w,
                        note = note,
                        origins = origins,
                        paymentMethod = method,
                        walletSuggestion = suggested?.second,
                    )
                }
            }
            Kind.WITHDRAWAL ->
                MovementProposal.Transfer(
                    out = amount.proposed,
                    from = wallet,
                    to = default,
                    note = note,
                    origins =
                        originsOf(
                            DraftField.KIND to kindOrigin,
                            DraftField.AMOUNT to amount.amountOrigin,
                            DraftField.CURRENCY to amount.currencyOrigin,
                            DraftField.WALLET to walletOrigin,
                            DraftField.TO_WALLET to default?.let { FieldOrigin.SUGGESTED },
                        ),
                )
            Kind.EXCHANGE -> {
                val into = received?.proposed ?: exchangeInto(amount.proposed, rate, default, ctx.directory)
                val intoOrigin = received?.amountOrigin ?: into?.let { FieldOrigin.INFERRED }
                // Sin alias, el origen es la única billetera activa en la moneda entregada.
                val from = wallet ?: soleWalletIn(amount.proposed.currency, ctx.directory)
                val fromOrigin = walletOrigin ?: from?.let { FieldOrigin.SUGGESTED }
                val to = exchangeDestination(into?.currency, default, ctx.directory)
                MovementProposal.Transfer(
                    out = amount.proposed,
                    from = from,
                    into = into,
                    to = to,
                    note = note,
                    paymentMethod = method,
                    origins =
                        originsOf(
                            DraftField.KIND to kindOrigin,
                            DraftField.AMOUNT to amount.amountOrigin,
                            DraftField.CURRENCY to amount.currencyOrigin,
                            DraftField.WALLET to fromOrigin,
                            DraftField.TO_AMOUNT to intoOrigin,
                            DraftField.TO_WALLET to to?.let { FieldOrigin.SUGGESTED },
                        ),
                )
            }
        }
    }

    /**
     * Si en [start] empieza un marcador de lo recibido del paquete ("me dieron", "got", "por"),
     * devuelve la posición siguiente al marcador; si no, null.
     */
    private fun receivedMarkerEnd(
        words: List<Word>,
        start: Int,
        pack: LanguagePack,
    ): Int? =
        pack.receivedMarkers
            .map { it.split(' ') }
            .filter { marker -> marker.indices.all { words.getOrNull(start + it)?.normalized == marker[it] } }
            .maxOfOrNull { start + it.size }

    /** Lo que sigue a la preposición de nota; si no hay, las palabras no usadas en su orden original. */
    private fun note(
        words: List<Word>,
        used: Set<Int>,
        pack: LanguagePack,
    ): String? {
        val free = words.indices.filter { it !in used }
        val prepositionIndex = free.firstOrNull { words[it].normalized in pack.notePrepositions }
        // Lo que sigue a la preposición; si no queda nada (era el medio, la moneda o la billetera),
        // las demás palabras de la frase sin preposiciones sueltas.
        val afterPreposition = prepositionIndex?.let { p -> free.filter { it > p } }.orEmpty()
        val noteIndices = afterPreposition.ifEmpty { free.filter { words[it].normalized !in pack.notePrepositions } }
        return noteIndices.joinToString(" ") { words[it].original }.ifBlank { null }
    }

    /**
     * Billetera nombrada por una palabra de su nombre (ver [WalletNameHints]): primero exacta y
     * después con la tolerancia de tipeo de los verbos. Devuelve la posición de la palabra y el id.
     */
    private fun walletByName(
        words: List<Word>,
        used: Set<Int>,
        ctx: InterpretContext,
    ): Pair<Int, String>? {
        val hints = WalletNameHints.derive(ctx.directory.wallets, ctx.pack, ctx.locale)
        if (hints.isEmpty()) return null
        val free = words.indices.filter { it !in used }
        free.firstOrNull { words[it].normalized in hints }?.let { return it to hints.getValue(words[it].normalized) }
        for (i in free) {
            val key = hints.keys.firstOrNull { TextNormalizer.matches(words[i].normalized, setOf(it), fuzzy = true) } ?: continue
            return i to hints.getValue(key)
        }
        return null
    }

    /**
     * Billetera sugerida cuando la frase no nombra ninguna:
     * - con medio de pago: candidatas = activas que lo aceptan (y la moneda dicha);
     * - sin medio, con una moneda que la billetera por defecto no acepta: las de esa moneda, primero efectivo;
     * - si no: la billetera por defecto.
     * Entre candidatas: la por defecto, si no la de la moneda local, si no la primera. Siempre sugerida.
     */
    private fun suggestWallet(
        method: PaymentMethod?,
        currency: Currency?,
        ctx: InterpretContext,
    ): Pair<WalletRef, WalletSuggestion>? {
        val directory = ctx.directory
        val default = directory.defaultWalletId?.let(directory::byId)
        val active = directory.wallets.filter { it.isActive }
        val candidates =
            when {
                method != null -> active.filter { method in it.type.acceptedMethods && (currency == null || it.accepts(currency)) }
                currency != null && default != null && !default.accepts(currency) ->
                    active.filter { it.accepts(currency) }.let { same -> same.filter { it.type == WalletType.CASH }.ifEmpty { same } }
                else -> return default?.let { WalletRef.ById(it.id) to WalletSuggestion(WalletSuggestionReason.DEFAULT_WALLET, 1) }
            }
        val chosen =
            candidates.firstOrNull { it.id == default?.id }
                ?: candidates.firstOrNull { it.currency.isSameAs(ctx.locale.currency) }
                ?: candidates.firstOrNull()
                ?: return null
        val reason =
            when {
                candidates.size == 1 -> WalletSuggestionReason.ONLY_CANDIDATE
                chosen.id == default?.id -> WalletSuggestionReason.DEFAULT_WALLET
                else -> WalletSuggestionReason.RECOMMENDED_AMONG_SEVERAL
            }
        return WalletRef.ById(chosen.id) to WalletSuggestion(reason, candidates.size)
    }

    /** La única billetera activa en [currency]; null si hay varias, ninguna o no se sabe la moneda. */
    private fun soleWalletIn(
        currency: Currency?,
        directory: WalletDirectory,
    ): WalletRef? {
        if (currency == null) return null
        return directory.wallets
            .filter { it.isActive && it.accepts(currency) }
            .singleOrNull()
            ?.let { WalletRef.ById(it.id) }
    }

    /** Destino de un cambio: la billetera por defecto si acepta lo recibido; si no, la única de esa moneda. */
    private fun exchangeDestination(
        received: Currency?,
        default: WalletRef?,
        directory: WalletDirectory,
    ): WalletRef? {
        val defaultWallet = (default as? WalletRef.ById)?.let { directory.byId(it.walletId) }
        if (received == null || defaultWallet?.accepts(received) == true) return default
        return soleWalletIn(received, directory)
    }

    /** "vendi 50 dolares a 11.88": lo que llega a la billetera por defecto, calculado con la tasa dicha. */
    private fun exchangeInto(
        out: ProposedAmount,
        rate: String?,
        default: WalletRef?,
        directory: WalletDirectory,
    ): ProposedAmount? {
        val fromCurrency = out.currency ?: return null
        val toWallet = (default as? WalletRef.ById)?.let { directory.byId(it.walletId) } ?: return null
        if (rate == null || fromCurrency == toWallet.currency) return null
        val outMoney = runCatching { Money.parseCanonical(out.value, fromCurrency) }.getOrNull() ?: return null
        val converted = outMoney * ExchangeRate.parse(fromCurrency, toWallet.currency, rate)
        return ProposedAmount(converted.toNumericString(), toWallet.currency)
    }
}

/** Líneas de un comprobante y búsqueda de valores por etiqueta. */
private class ReceiptLines(
    text: String,
) {
    val original: List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val normalized: List<String> = original.map { TextNormalizer.normalize(it) }

    /** Índice de la primera línea que empieza con la etiqueta. */
    fun labelLine(label: String): Int? = normalized.indices.firstOrNull { normalized[it] == label || normalized[it].startsWith("$label ") }

    /** Valor de una etiqueta: lo que sigue en la misma línea o, si no hay, la línea siguiente. */
    fun valueOf(label: String): Pair<String, Int>? {
        val index = labelLine(label) ?: return null
        val line = original[index]
        val inline =
            if (':' in line) {
                line.substringAfter(':')
            } else {
                line.split(Regex("""\s+""")).drop(label.split(' ').size).joinToString(" ")
            }
        val cleaned = inline.trim().trimEnd('-', '_', '—', ' ').trim()
        if (cleaned.isNotEmpty()) return cleaned to index
        return original.getOrNull(index + 1)?.let { it to index + 1 }
    }

    fun firstLabel(labels: Set<String>): Pair<String, Int>? = labels.sortedByDescending { it.length }.firstNotNullOfOrNull { valueOf(it) }
}

/**
 * Número de transacción del valor de una etiqueta: el primer token (sin "N°", "#" ni signos
 * iniciales) hecho de letras, dígitos y guiones, con al menos un dígito.
 */
internal fun referenceOf(value: String?): String? {
    val token =
        value
            ?.trim()
            ?.trimStart(':', '#', '°', 'º', ' ')
            ?.removePrefix("N°")
            ?.removePrefix("Nº")
            ?.trim()
            ?.split(Regex("""\s+"""))
            ?.firstOrNull()
            ?.trimEnd('.', ',', ';')
            ?: return null
    return token.takeIf { REFERENCE.matches(it) && it.any(Char::isDigit) }
}

private val REFERENCE = Regex("""[\p{L}\p{N}-]+""")

/** Resuelve la cuenta de un comprobante con el alias que el usuario le puso (el número de cuenta). */
private fun accountWallet(
    accountText: String?,
    directory: WalletDirectory,
): WalletRef? {
    val digits = accountText?.let { Regex("""\d{4,}""").findAll(it).maxByOrNull { m -> m.value.length }?.value } ?: return null
    val alias =
        directory.aliases.keys.firstOrNull { key ->
            key.length >= 4 && key.all(Char::isDigit) && digits.contains(key)
        } ?: return null
    return WalletRef.ByHint(alias)
}

private fun readDate(
    text: String,
    ctx: InterpretContext,
): Pair<Long?, FieldOrigin?> {
    val read = DateTimeReader.read(text, ctx.pack.months, ctx.locale, ctx.utcOffsetMinutes) ?: return null to null
    return read.epochMs to if (read.ambiguous) FieldOrigin.INFERRED else FieldOrigin.EVIDENCE
}

/** Lee comprobantes que coinciden con una plantilla conocida: sus campos son evidencia. */
object TemplateReader {
    fun matches(
        template: ReceiptTemplate,
        text: String,
    ): Boolean {
        val normalized = TextNormalizer.normalize(text)
        return template.markers.all { normalized.contains(it) }
    }

    fun read(
        template: ReceiptTemplate,
        text: String,
        ctx: InterpretContext,
    ): List<MovementProposal> {
        val lines = ReceiptLines(text)
        val scan = { line: String -> MoneyScanner.scan(line, ctx.locale, ctx.pack.currencyWords).firstOrNull { it.currency != null } }

        val (moneyMatch, amountLine) =
            template.amountLabel?.let { label ->
                lines.valueOf(label)?.let { (value, index) -> scan(value)?.let { it to index } }
            } ?: lines.original.indices.firstNotNullOfOrNull { i -> scan(lines.original[i])?.let { it to i } }
                ?: return emptyList()
        val amount = moneyMatch.toRead(FieldOrigin.EVIDENCE)

        val counterparty =
            when {
                template.counterpartyAfterAmount -> lines.original.getOrNull(amountLine + 1)
                template.counterpartyLabel != null -> lines.valueOf(template.counterpartyLabel)?.first
                else -> null
            }
        val note = counterparty ?: template.noteLabel?.let { lines.valueOf(it)?.first }
        val account = template.accountLabel?.let { accountWallet(lines.valueOf(it)?.first, ctx.directory) }
        val wallet = template.issuerWalletHint?.let { WalletRef.ByHint(it) } ?: account
        val (occurredAt, dateOrigin) = readDate(text, ctx)
        val reference = template.referenceLabel?.let { referenceOf(lines.valueOf(it)?.first) }

        val origins =
            originsOf(
                DraftField.KIND to FieldOrigin.EVIDENCE,
                DraftField.AMOUNT to amount.amountOrigin,
                DraftField.CURRENCY to amount.currencyOrigin,
                DraftField.WALLET to wallet?.let { FieldOrigin.EVIDENCE },
                DraftField.OCCURRED_AT to dateOrigin,
            )
        return listOf(
            when (template.kind) {
                TemplateKind.EXPENSE -> MovementProposal.Expense(amount.proposed, wallet, occurredAt, note, origins, reference = reference)
                TemplateKind.INCOME -> MovementProposal.Income(amount.proposed, wallet, occurredAt, note, origins, reference = reference)
                TemplateKind.WITHDRAWAL -> {
                    val default = defaultWallet(ctx.directory)
                    MovementProposal.Transfer(
                        out = amount.proposed,
                        from = wallet,
                        to = default,
                        occurredAtEpochMs = occurredAt,
                        note = note,
                        origins = origins + originsOf(DraftField.TO_WALLET to default?.let { FieldOrigin.SUGGESTED }),
                        reference = reference,
                    )
                }
            },
        )
    }
}

/**
 * Lee comprobantes de bancos desconocidos solo por su estructura: monto con moneda junto a una
 * etiqueta de importe, contraparte, cuenta, fecha y hora, y el sentido por las palabras del paquete.
 */
object GenericReceiptReader {
    fun looksLikeReceipt(
        text: String,
        fromPhoto: Boolean,
        pack: LanguagePack,
    ): Boolean {
        if (fromPhoto) return true
        val lines = ReceiptLines(text)
        val labels = pack.amountLabels + pack.counterpartyLabels + pack.accountLabels + pack.noteLabels
        return lines.original.size >= 2 && labels.count { lines.labelLine(it) != null } >= 2
    }

    fun read(
        text: String,
        ctx: InterpretContext,
    ): List<MovementProposal> {
        val pack = ctx.pack
        val lines = ReceiptLines(text)
        val all = MoneyScanner.scan(text, ctx.locale, pack.currencyWords)

        val labeled =
            lines
                .firstLabel(
                    pack.amountLabels,
                )?.first
                ?.let { MoneyScanner.scan(it, ctx.locale, pack.currencyWords).firstOrNull() }
        val amount =
            when {
                labeled != null -> labeled.toRead(FieldOrigin.EVIDENCE)
                else -> {
                    val largest = all.filter { it.currency != null }.maxWithOrNull(BY_VALUE) ?: return emptyList()
                    largest.toRead(FieldOrigin.EVIDENCE).let { it.copy(amountOrigin = FieldOrigin.INFERRED) }
                }
            }

        val normalized = TextNormalizer.normalize(text)
        val kind =
            when {
                pack.withdrawalWords.any { normalized.containsWords(it) } -> TemplateKind.WITHDRAWAL
                pack.incomeWords.any { normalized.containsWords(it) } -> TemplateKind.INCOME
                else -> TemplateKind.EXPENSE
            }
        val note = lines.firstLabel(pack.counterpartyLabels)?.first ?: lines.firstLabel(pack.noteLabels)?.first
        val account = accountWallet(lines.firstLabel(pack.accountLabels)?.first, ctx.directory)
        val (occurredAt, dateOrigin) = readDate(text, ctx)
        val reference = referenceOf(lines.firstLabel(pack.referenceLabels)?.first)

        val origins =
            originsOf(
                DraftField.KIND to FieldOrigin.INFERRED,
                DraftField.AMOUNT to amount.amountOrigin,
                DraftField.CURRENCY to amount.currencyOrigin,
                DraftField.WALLET to account?.let { FieldOrigin.EVIDENCE },
                DraftField.OCCURRED_AT to dateOrigin,
            )
        return listOf(
            when (kind) {
                TemplateKind.EXPENSE -> MovementProposal.Expense(amount.proposed, account, occurredAt, note, origins, reference = reference)
                TemplateKind.INCOME -> MovementProposal.Income(amount.proposed, account, occurredAt, note, origins, reference = reference)
                TemplateKind.WITHDRAWAL -> {
                    val default = defaultWallet(ctx.directory)
                    MovementProposal.Transfer(
                        out = amount.proposed,
                        from = account,
                        to = default,
                        occurredAtEpochMs = occurredAt,
                        note = note,
                        origins = origins + originsOf(DraftField.TO_WALLET to default?.let { FieldOrigin.SUGGESTED }),
                        reference = reference,
                    )
                }
            },
        )
    }

    /** Orden por valor de un decimal canónico sin coma flotante: parte entera y luego decimales. */
    private val BY_VALUE: Comparator<MoneyMatch> =
        compareBy<MoneyMatch>(
            {
                it.amount.canonical
                    .substringBefore('.')
                    .toLong()
            },
            {
                it.amount.canonical
                    .substringAfter('.', "")
                    .padEnd(8, '0')
            },
        )
}
