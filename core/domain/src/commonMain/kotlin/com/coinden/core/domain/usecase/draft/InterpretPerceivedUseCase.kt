package com.coinden.core.domain.usecase.draft

import com.coinden.core.domain.model.draft.Draft
import com.coinden.core.domain.model.draft.DraftEvent
import com.coinden.core.domain.port.output.DraftInterpreterPort

/**
 * Caso de Uso: incorporar el texto leído en el teléfono de una foto, un PDF o un audio.
 *
 * Solo cambia un borrador Capturado que todavía no se leyó: lo que la persona completó,
 * confirmó o descartó queda igual, y una lectura que llegó de otro dispositivo no se repite.
 * El texto pasa por el intérprete como cualquier captura; la primera propuesta queda en este
 * borrador y cada propuesta adicional crea otro borrador con la misma evidencia.
 * Devuelve los borradores resultantes, empezando por este.
 */
class InterpretPerceivedUseCase(
    private val ctx: DraftContext,
    private val interpreter: DraftInterpreterPort,
) {
    suspend operator fun invoke(
        draftId: String,
        text: String,
        engine: String,
    ): List<Draft> {
        val draft = ctx.load(draftId) ?: return emptyList()
        if (draft !is Draft.Captured || draft.header.perceivedText != null || text.isBlank()) return listOf(draft)
        val header = draft.header
        val owner = header.ownerId

        perceive(draftId, owner, text, engine)
        val evidence = header.evidence.copy(text = text)
        val proposals = runCatching { interpreter.interpret(evidence, ctx.directory(owner)) }.getOrDefault(emptyList())
        if (proposals.isEmpty()) return listOf(ctx.load(draftId)!!)

        return proposals.mapIndexed { index, proposal ->
            val target =
                if (index == 0) {
                    draftId
                } else {
                    ctx.ids.newId().also { newDraft ->
                        val now = ctx.clock.nowEpochMs()
                        ctx.drafts.appendEvent(
                            DraftEvent.Captured(
                                ctx.ids.newId(),
                                newDraft,
                                owner,
                                ctx.device.deviceId(),
                                header.capturedAtEpochMs,
                                now,
                                header.evidence,
                            ),
                        )
                        perceive(newDraft, owner, text, engine)
                    }
                }
            val now = ctx.clock.nowEpochMs()
            ctx.drafts.appendEvent(DraftEvent.Interpreted(ctx.ids.newId(), target, owner, ctx.device.deviceId(), now, now, proposal))
            ctx.load(target)!!
        }
    }

    private suspend fun perceive(
        draftId: String,
        owner: String,
        text: String,
        engine: String,
    ) {
        val now = ctx.clock.nowEpochMs()
        ctx.drafts.appendEvent(DraftEvent.Perceived(ctx.ids.newId(), draftId, owner, ctx.device.deviceId(), now, now, text, engine))
    }
}
