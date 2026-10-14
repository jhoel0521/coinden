package com.coinden.app.theme

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** El texto de CoinDen se lee bien en los dos temas: contraste WCAG AA (4.5:1) en cada par. */
class CoinDenPaletteTest {
    private fun luminance(argb: Long): Double {
        val channels = listOf(16, 8, 0).map { ((argb shr it) and 0xFF) / 255.0 }
        val linear = channels.map { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]
    }

    private fun contrast(
        a: Long,
        b: Long,
    ): Double {
        val (light, dark) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private fun pairs(p: CoinDenPalette) =
        mapOf(
            "texto sobre fondo" to (p.onSurface to p.surface),
            "texto secundario sobre fondo" to (p.onSurfaceVariant to p.surface),
            "texto secundario sobre contenedor alto" to (p.onSurfaceVariant to p.surfaceContainerHigh),
            "principal sobre fondo" to (p.primary to p.surface),
            "sobre principal" to (p.onPrimary to p.primary),
            "contenedor principal" to (p.onPrimaryContainer to p.primaryContainer),
            "sobre secundario" to (p.onSecondary to p.secondary),
            "contenedor secundario" to (p.onSecondaryContainer to p.secondaryContainer),
            "sobre terciario" to (p.onTertiary to p.tertiary),
            "contenedor terciario" to (p.onTertiaryContainer to p.tertiaryContainer),
            "sobre error" to (p.onError to p.error),
            "contenedor de error" to (p.onErrorContainer to p.errorContainer),
            "error sobre fondo" to (p.error to p.surface),
            "éxito sobre fondo" to (p.success to p.surface),
            "contenedor de éxito" to (p.onSuccessContainer to p.successContainer),
            "advertencia sobre fondo" to (p.warning to p.surface),
            "contenedor de advertencia" to (p.onWarningContainer to p.warningContainer),
        )

    @Test
    fun `cobre claro cumple contraste AA en todos sus pares`() {
        pairs(CoinDenPalette.Cobre).forEach { (name, pair) ->
            val ratio = contrast(pair.first, pair.second)
            assertTrue(ratio >= 4.5, "$name: $ratio")
        }
    }

    @Test
    fun `cobre noche cumple contraste AA en todos sus pares`() {
        pairs(CoinDenPalette.CobreNoche).forEach { (name, pair) ->
            val ratio = contrast(pair.first, pair.second)
            assertTrue(ratio >= 4.5, "$name: $ratio")
        }
    }

    @Test
    fun `los colores son los del diseno`() {
        assertEquals(0xFF9A4A22, CoinDenPalette.Cobre.primary)
        assertEquals(0xFFFFF8F3, CoinDenPalette.Cobre.surface)
        assertEquals(0xFFFFB693, CoinDenPalette.CobreNoche.primary)
        assertEquals(0xFF1A1210, CoinDenPalette.CobreNoche.surface)
    }
}
