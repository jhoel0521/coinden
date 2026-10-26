package com.coinden.core.data.packs

import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.interpret.LanguagePack
import com.coinden.core.domain.model.interpret.ReceiptTemplate
import com.coinden.core.domain.model.interpret.TemplateKind
import com.coinden.core.domain.model.wallet.PaymentMethod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** La versión de formato más alta que esta app entiende. Una mayor se ignora, no falla. */
private const val FORMAT_VERSION = 1

private val json =
    Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

/**
 * Lee los paquetes de idioma y las plantillas de comprobante desde su formato de datos.
 *
 * Un archivo que no se entiende —dañado, sin versión, de una versión más nueva o con un valor
 * que esta app no conoce— se descarta devolviendo `null`. Quien lo llama sigue con los demás:
 * un archivo malo nunca puede dejar a la persona sin capturar.
 */
object PackParser {
    fun languagePack(text: String): LanguagePack? {
        val dto = decode<LanguagePackFile>(text) ?: return null
        if (dto.formatVersion == null || dto.formatVersion > FORMAT_VERSION) return null
        if (dto.code.isBlank()) return null
        return LanguagePack(
            code = dto.code,
            amountLabels = dto.amountLabels.toSet(),
            counterpartyLabels = dto.counterpartyLabels.toSet(),
            accountLabels = dto.accountLabels.toSet(),
            noteLabels = dto.noteLabels.toSet(),
            expenseWords = dto.expenseWords.toSet(),
            incomeWords = dto.incomeWords.toSet(),
            withdrawalWords = dto.withdrawalWords.toSet(),
            exchangeWords = dto.exchangeWords.toSet(),
            notePrepositions = dto.notePrepositions.toSet(),
            conjunctions = dto.conjunctions.toSet(),
            rateMarkers = dto.rateMarkers.toSet(),
            months = dto.months,
            currencyWords = dto.currencyWords,
            categoryWords = dto.categoryWords.mapNotNullValues { BaseCategory.entries.firstOrNull { c -> c.name == it } },
            receivedMarkers = dto.receivedMarkers.toSet(),
            paymentMethodWords = dto.paymentMethodWords.mapNotNullValues { PaymentMethod.entries.firstOrNull { m -> m.name == it } },
            genericWalletWords = dto.genericWalletWords.toSet(),
            referenceLabels = dto.referenceLabels.toSet(),
        )
    }

    fun receiptTemplate(text: String): ReceiptTemplate? {
        val dto = decode<ReceiptTemplateFile>(text) ?: return null
        if (dto.formatVersion == null || dto.formatVersion > FORMAT_VERSION) return null
        if (dto.id.isBlank() || dto.markers.isEmpty()) return null
        // Sin marcas no se puede identificar, y un tipo que no existe haría proponer cualquier cosa.
        val kind = TemplateKind.entries.firstOrNull { it.name == dto.kind } ?: return null
        return ReceiptTemplate(
            id = dto.id,
            markers = dto.markers,
            kind = kind,
            amountLabel = dto.amountLabel,
            counterpartyLabel = dto.counterpartyLabel,
            noteLabel = dto.noteLabel,
            accountLabel = dto.accountLabel,
            issuerWalletHint = dto.issuerWalletHint,
            counterpartyAfterAmount = dto.counterpartyAfterAmount,
            referenceLabel = dto.referenceLabel,
            // Un medio que esta app no conoce se descarta: la plantilla sigue sirviendo sin él.
            method = PaymentMethod.entries.firstOrNull { it.name == dto.method },
        )
    }

    private inline fun <reified T> decode(text: String): T? = runCatching { json.decodeFromString<T>(text) }.getOrNull()

    /** Una clave cuyo valor esta app no conoce se descarta; el resto del archivo sigue sirviendo. */
    private fun <V> Map<String, String>.mapNotNullValues(convert: (String) -> V?): Map<String, V> =
        mapNotNull { (key, value) -> convert(value)?.let { key to it } }.toMap()
}

@Serializable
private data class LanguagePackFile(
    @SerialName("formatVersion") val formatVersion: Int? = null,
    val code: String = "",
    val amountLabels: List<String> = emptyList(),
    val counterpartyLabels: List<String> = emptyList(),
    val accountLabels: List<String> = emptyList(),
    val noteLabels: List<String> = emptyList(),
    val referenceLabels: List<String> = emptyList(),
    val expenseWords: List<String> = emptyList(),
    val incomeWords: List<String> = emptyList(),
    val withdrawalWords: List<String> = emptyList(),
    val exchangeWords: List<String> = emptyList(),
    val notePrepositions: List<String> = emptyList(),
    val conjunctions: List<String> = emptyList(),
    val rateMarkers: List<String> = emptyList(),
    val receivedMarkers: List<String> = emptyList(),
    val genericWalletWords: List<String> = emptyList(),
    val months: Map<String, Int> = emptyMap(),
    /** Palabra de moneda -> código ISO; null = la moneda del locale. */
    val currencyWords: Map<String, String?> = emptyMap(),
    val categoryWords: Map<String, String> = emptyMap(),
    val paymentMethodWords: Map<String, String> = emptyMap(),
)

@Serializable
private data class ReceiptTemplateFile(
    @SerialName("formatVersion") val formatVersion: Int? = null,
    val id: String = "",
    val markers: List<String> = emptyList(),
    val kind: String = "",
    val amountLabel: String? = null,
    val counterpartyLabel: String? = null,
    val noteLabel: String? = null,
    val accountLabel: String? = null,
    val issuerWalletHint: String? = null,
    val counterpartyAfterAmount: Boolean = false,
    val referenceLabel: String? = null,
    val method: String? = null,
)
