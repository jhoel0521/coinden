package com.coinden.app.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Las pantallas solo usan componentes de CoinDen (`ui/components`): ningún botón, chip, campo,
 * superficie, tarjeta, interruptor ni diálogo de Material suelto. Así la estética y la
 * agrupación se deciden en un solo lugar. `theme/` y `ui/components` son el sistema de diseño.
 */
class RawMaterialUsageTest {
    private val allowed =
        setOf(
            "Text",
            "Icon",
            "MaterialTheme",
            "LocalContentColor",
            "HorizontalDivider",
            "CircularProgressIndicator",
            "LinearProgressIndicator",
            "ExperimentalMaterial3Api",
            "DatePicker",
            "DatePickerDialog",
            "rememberDatePickerState",
        )

    /** Archivos que todavía no pasan por los componentes; la lista solo puede achicarse. */
    private val pendingMigration =
        setOf(
            "App.kt",
            "history/HistoryScreen.kt",
            "home/HomeScreen.kt",
            "inbox/ReviewInboxScreen.kt",
            "insights/SpendingInsightsScreen.kt",
            "onboarding/OnboardingScreens.kt",
            "perception/PerceptionScreens.kt",
            "settings/WalletSettingsScreen.kt",
            "ui/LoadNotice.kt",
        )

    private fun offenders(): Map<String, List<String>> {
        val root = File("src/commonMain/kotlin/com/coinden/app")
        return root
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.replace('\\', '/').let { path -> "/ui/components/" in path || "/theme/" in path } }
            .associate { file ->
                val name = file.relativeTo(root).path.replace('\\', '/')
                name to
                    Regex("""^import androidx\.compose\.material3\.(\w+)""", RegexOption.MULTILINE)
                        .findAll(file.readText())
                        .map { it.groupValues[1] }
                        .filter { it !in allowed }
                        .toList()
            }.filterValues { it.isNotEmpty() }
    }

    @Test
    fun `las pantallas no usan controles de Material sueltos`() {
        val found = offenders()
        assertEquals(pendingMigration, found.keys, "Usar componentes de ui/components en lugar de: $found")
    }
}
