package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.draft.Evidence
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.port.output.DraftInterpreterPort
import com.coinden.core.domain.port.output.InstalledPacksPort
import com.coinden.core.domain.port.output.RegionalSettingsPort

/**
 * Intérprete de texto agnóstico y determinista: todo origen (texto, voz transcrita, OCR de una foto)
 * llega como texto y se convierte en propuestas, sin red ni modelos.
 *
 * Orden de decisión:
 * 1. Texto vacío -> sin propuestas (el borrador queda Capturado).
 * 2. Una plantilla instalada coincide -> [TemplateReader] (campos de evidencia).
 * 3. Parece un comprobante -> [GenericReceiptReader] (bancos desconocidos).
 * 4. Si no -> [PhraseReader] (frases escritas o dictadas).
 *
 * El inglés está siempre activo; los demás paquetes solo si están instalados.
 */
class TextDraftInterpreter(
    private val packs: InstalledPacksPort,
    private val regional: RegionalSettingsPort,
) : DraftInterpreterPort {
    override suspend fun interpret(
        evidence: Evidence,
        wallets: WalletDirectory,
    ): List<MovementProposal> {
        val text = evidence.text?.takeIf { it.isNotBlank() } ?: return emptyList()
        val settings = regional.settings()
        val pack = (listOf(LanguagePacks.ENGLISH) + packs.languagePacks().filter { it.code != LanguagePacks.ENGLISH.code }).merged()
        val ctx = InterpretContext(pack, settings.locale, settings.utcOffsetMinutes, wallets)

        packs.receiptTemplates().firstOrNull { TemplateReader.matches(it, text) }?.let { return TemplateReader.read(it, text, ctx) }
        if (GenericReceiptReader.looksLikeReceipt(text, evidence.source == EvidenceSource.PHOTO, pack)) {
            return GenericReceiptReader.read(text, ctx)
        }
        return PhraseReader.read(text, ctx)
    }
}
