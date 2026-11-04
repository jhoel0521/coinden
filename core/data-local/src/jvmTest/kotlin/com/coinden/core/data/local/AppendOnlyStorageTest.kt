package com.coinden.core.data.local

import com.coinden.core.domain.port.output.CategorizationRepositoryPort
import com.coinden.core.domain.port.output.DraftRepositoryPort
import com.coinden.core.domain.port.output.WalletRepositoryPort
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Ningún evento suelto se borra ni se modifica: solo se agrega, y corregir es un evento nuevo. */
class AppendOnlyStorageTest {
    private val schema = File("src/commonMain/sqldelight").walkTopDown().filter { it.extension in setOf("sq", "sqm") }.toList()

    @Test
    fun `sobre los eventos solo hay INSERT y SELECT`() {
        assertTrue(schema.isNotEmpty())
        val touching =
            schema.flatMap { file ->
                file
                    .readText()
                    .split(';')
                    .map { it.lines().filterNot { line -> line.trimStart().startsWith("--") }.joinToString(" ") }
                    .filter {
                        Regex(
                            """\b(DELETE\s+FROM|UPDATE|REPLACE\s+INTO|DROP\s+TABLE|ALTER\s+TABLE)\s+(events|event_refs)\b""",
                            RegexOption.IGNORE_CASE,
                        ).containsMatchIn(it)
                    }
            }
        assertEquals(emptyList(), touching)
    }

    @Test
    fun `los puertos de eventos no ofrecen borrar ni modificar uno`() {
        val forbidden = Regex("(delete|remove|update|replace|modify|edit|clear)", RegexOption.IGNORE_CASE)
        for (port in listOf(WalletRepositoryPort::class.java, DraftRepositoryPort::class.java, CategorizationRepositoryPort::class.java)) {
            val offending = port.methods.map { it.name }.filter { forbidden.containsMatchIn(it) }
            assertEquals(emptyList(), offending, port.simpleName)
        }
    }
}
