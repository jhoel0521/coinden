package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.draft.WalletDirectory

/**
 * La gramática con la que responde el modelo de lenguaje local.
 *
 * El motor de inferencia la aplica **durante la decodificación**, enmascarando en cada paso los
 * tokens que la violarían: el modelo no puede salirse del formato ni inventar una billetera, una
 * moneda o una categoría, porque solo están en la gramática las que la persona tiene.
 *
 * Es texto puro y determinista, así que vive en el dominio y se prueba sin motor ni modelo. El
 * modelo no ve esta gramática: el prompt de sistema le describe el formato aparte.
 *
 * El monto va como **cadena de dígitos**, nunca como número: el dinero es exacto (Manifiesto,
 * principio 6) y un flotante en la gramática sería la puerta de entrada al redondeo.
 */
object IntentGrammar {
    fun of(
        wallets: WalletDirectory,
        categories: Set<String>,
    ): String {
        // Solo las billeteras activas: ofrecerle una archivada sería ofrecerle algo que no usa.
        val activas = wallets.wallets.filter { it.isActive }
        val walletIds = activas.map { it.id }.sorted()
        val currencies = activas.map { it.currency.code }.distinct().sorted()

        return buildString {
            appendLine("""root ::= "{" ws "\"payments\"" ws ":" ws "[" ws payment ("," ws payment)* ws "]" ws "}"""")
            appendLine(
                """payment ::= "{" ws "\"kind\"" ws ":" ws kind ws "," ws "\"amount\"" ws ":" ws amount ws "," ws """ +
                    """"\"currency\"" ws ":" ws currency ws "," ws "\"wallet\"" ws ":" ws wallet ws "," ws """ +
                    """"\"category\"" ws ":" ws category ws "," ws "\"note\"" ws ":" ws note ws "}"""",
            )
            appendLine("""kind ::= "\"EXPENSE\"" | "\"INCOME\"" | "\"TRANSFER\""""")
            // El monto es una cadena: "25" o "25.00", nunca 25.0 ni 2.5e1.
            appendLine("""amount ::= "\"" [0-9]+ ("." [0-9][0-9]?)? "\"" """.trimEnd())
            appendLine(alternatives("currency", currencies))
            appendLine(alternatives("wallet", walletIds))
            appendLine(alternatives("category", categories.sorted()))
            appendLine("""note ::= "\"" char* "\""""")
            appendLine("""char ::= [^"\\] | "\\" ["\\/bfnrt]""")
            append("""ws ::= [ \t\n]*""")
        }
    }

    /**
     * Una regla con las opciones cerradas que la persona tiene, más `null`. Sin opciones, el
     * único valor posible es `null`: así el modelo no puede inventar una, solo dejarla vacía.
     */
    private fun alternatives(
        name: String,
        values: List<String>,
    ): String {
        val opciones = values.map { """"\"${escape(it)}\""""" } + "\"null\""
        return "$name ::= ${opciones.joinToString(" | ")}"
    }

    /** Una comilla o una barra dentro de un id rompería la gramática si entrara tal cual. */
    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
