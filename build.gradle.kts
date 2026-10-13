plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    // El código generado (recursos de Compose, etc.) vive en build/ y no se revisa.
    // En KMP el filtro de la extensión no llega a las tareas por source set: se excluye en cada tarea.
    tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
        exclude { element -> element.file.invariantSeparatorsPath.contains("/build/") }
    }
}
