package com.coinden.app.capture

import com.coinden.app.theme.CoinDenIcon
import com.coinden.core.domain.model.draft.Confidence
import com.coinden.core.domain.model.wallet.PaymentMethod

/** Tono semántico de un chip: se traduce a colores del tema al dibujarlo. */
enum class Tone { SUCCESS, WARNING, ERROR }

/** Cómo se ve un nivel de confianza: tono e ícono (el texto sale de [label]). */
data class ConfidenceStyle(
    val tone: Tone,
    val icon: CoinDenIcon,
)

fun Confidence.style(): ConfidenceStyle =
    when (this) {
        Confidence.HIGH -> ConfidenceStyle(Tone.SUCCESS, CoinDenIcon.CHECK_CIRCLE)
        Confidence.MEDIUM -> ConfidenceStyle(Tone.WARNING, CoinDenIcon.GAUGE)
        Confidence.LOW -> ConfidenceStyle(Tone.ERROR, CoinDenIcon.ALERT)
    }

fun PaymentMethod.icon(): CoinDenIcon =
    when (this) {
        PaymentMethod.CASH -> CoinDenIcon.CASH
        PaymentMethod.TRANSFER -> CoinDenIcon.QR
        PaymentMethod.CARD -> CoinDenIcon.CARD
    }
