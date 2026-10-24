package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.draft.WalletDirectory

/**
 * El formato cerrado con el que responde el modelo de lenguaje local, como JSON Schema.
 *
 * No es una sugerencia: el motor lo aplica **durante la decodificación**, así que el modelo no
 * puede salirse del formato ni inventar una billetera, una moneda o una categoría, porque solo
 * están las que la persona tiene. Es lo que hace cumplible el principio 4 del Manifiesto sin
 * confiar en su buena voluntad.
 *
 * Es texto puro y determinista, así que vive en el dominio y se prueba sin motor ni modelo. El
 * modelo no ve este esquema: el prompt de sistema le describe el formato aparte.
 *
 * El monto va como **cadena de dígitos**, nunca como número: el dinero es exacto (Manifiesto,
 * principio 6) y un flotante acá sería la puerta de entrada al redondeo.
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

        return """
            {
              "type": "object",
              "properties": {
                "payments": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "kind": ${enumOf(listOf("EXPENSE", "INCOME", "TRANSFER"), nullable = false)},
                      "amount": { "type": "string", "pattern": "^[0-9]+(\\\\.[0-9]{1,2})?${'$'}" },
                      "currency": ${enumOf(currencies)},
                      "wallet": ${enumOf(walletIds)},
                      "category": ${enumOf(categories.sorted())},
                      "note": { "type": ["string", "null"] }
                    },
                    "required": ["kind", "amount"],
                    "additionalProperties": false
                  }
                }
              },
              "required": ["payments"],
              "additionalProperties": false
            }
            """.trimIndent()
    }

    /**
     * Una lista cerrada de valores, más `null`. Sin valores, el único posible es `null`: así el
     * modelo no puede inventar uno, solo dejarlo vacío.
     */
    private fun enumOf(
        values: List<String>,
        nullable: Boolean = true,
    ): String {
        val opciones = values.map { "\"${escape(it)}\"" } + if (nullable) listOf("null") else emptyList()
        return """{ "enum": [${opciones.joinToString(", ")}] }"""
    }

    /** Una comilla o una barra dentro de un id rompería el esquema si entrara tal cual. */
    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}
