package com.coinden.core.domain.model.interpret

import com.coinden.core.domain.model.draft.DraftField
import com.coinden.core.domain.model.draft.FieldOrigin
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.ProposedAmount
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.draft.WalletRef
import com.coinden.core.domain.port.output.BundlePayment

/**
 * Convierte la respuesta del modelo de lenguaje en propuestas del dominio.
 *
 * El formato es cerrado y lo impone [IntentGrammar], así que leerlo es lógica pura: no hace falta
 * una librería, y el dominio sigue sin depender de ningún framework.
 *
 * Este lector **no confía** en que la gramática se haya aplicado. Si la respuesta no cumple,
 * devuelve `null` y manda el respaldo determinista; si un pago suelto viene mal, lo descarta en
 * lugar de completarlo con algo inventado. Lo que devuelve todavía pasa por [ProposalValidator]:
 * acá solo se comprueba la forma, no que lo dicho esté respaldado.
 */
object IntentResponseReader {
    fun read(
        text: String,
        wallets: WalletDirectory,
        categories: Set<String>,
    ): List<BundlePayment>? {
        val cuerpo = text.trim()
        // Nada de relleno conversacional alrededor: el formato cerrado es todo lo que puede decir.
        if (!cuerpo.startsWith("{") || !cuerpo.endsWith("}")) return null
        val lista = arrayBody(cuerpo, "payments") ?: return null

        return objectsIn(lista).mapIndexedNotNull { index, objeto -> payment(objeto, index, wallets, categories) }
    }

    private fun payment(
        fields: Map<String, String?>,
        index: Int,
        wallets: WalletDirectory,
        categories: Set<String>,
    ): BundlePayment? {
        val monto = fields["amount"] ?: return null
        if (!AMOUNT.matches(monto)) return null

        // La moneda, la billetera y la categoría que no existen se descartan: el modelo elige de
        // listas cerradas, así que si llega otra cosa es señal de que algo falló, no un dato.
        val moneda = fields["currency"]?.let { codigo -> wallets.wallets.firstOrNull { it.currency.code == codigo }?.currency }
        val billetera = fields["wallet"]?.takeIf { wallets.byId(it) != null }?.let { WalletRef.ById(it) }
        val nota = fields["note"]

        val amount = ProposedAmount(monto, moneda)
        val origins =
            mapOf(
                DraftField.AMOUNT to FieldOrigin.INFERRED,
                DraftField.KIND to FieldOrigin.INFERRED,
            )
        val proposal =
            when (fields["kind"]) {
                "INCOME" -> MovementProposal.Income(amount, billetera, note = nota, origins = origins)
                "EXPENSE" -> MovementProposal.Expense(amount, billetera, note = nota, origins = origins)
                "TRANSFER" -> MovementProposal.Transfer(out = amount, from = billetera, note = nota, origins = origins)
                else -> return null
            }
        // La categoría propuesta se valida aparte (va al borrador, no al pago), así que acá solo
        // se comprueba que sea una de las de la persona; si no lo es, se ignora.
        fields["category"]?.takeIf { it !in categories }?.let { return null }

        // La clave identifica el pago entre interpretaciones del mismo paquete.
        return BundlePayment(paymentKey = "model:$index:$monto", proposal = proposal, evidenceIds = emptySet())
    }

    /** El contenido del arreglo de [name], o null si no está o no cierra. */
    private fun arrayBody(
        text: String,
        name: String,
    ): String? {
        val marca = "\"$name\""
        val inicio = text.indexOf(marca).takeIf { it >= 0 } ?: return null
        val abre = text.indexOf('[', inicio).takeIf { it >= 0 } ?: return null
        var profundidad = 0
        var enTexto = false
        var escapado = false
        for (i in abre until text.length) {
            val c = text[i]
            when {
                escapado -> escapado = false
                c == '\\' && enTexto -> escapado = true
                c == '"' -> enTexto = !enTexto
                enTexto -> Unit
                c == '[' -> profundidad++
                c == ']' -> {
                    profundidad--
                    if (profundidad == 0) return text.substring(abre + 1, i)
                }
            }
        }
        return null
    }

    /** Cada objeto `{...}` del arreglo, ya leído como campos. Respeta las llaves dentro de un texto. */
    private fun objectsIn(body: String): List<Map<String, String?>> {
        val objetos = mutableListOf<Map<String, String?>>()
        var inicio = -1
        var profundidad = 0
        var enTexto = false
        var escapado = false
        for (i in body.indices) {
            val c = body[i]
            when {
                escapado -> escapado = false
                c == '\\' && enTexto -> escapado = true
                c == '"' -> enTexto = !enTexto
                enTexto -> Unit
                c == '{' -> {
                    if (profundidad == 0) inicio = i
                    profundidad++
                }
                c == '}' -> {
                    profundidad--
                    if (profundidad == 0 && inicio >= 0) objetos += fieldsOf(body.substring(inicio, i + 1))
                }
            }
        }
        return objetos
    }

    /** `"clave": "valor"` o `"clave": null`, con los escapes del texto ya resueltos. */
    private fun fieldsOf(objeto: String): Map<String, String?> =
        FIELD
            .findAll(objeto)
            .associate { match ->
                val clave = match.groupValues[1]
                val texto = match.groups[2]?.value
                clave to texto?.let { unescape(it) }
            }

    private fun unescape(value: String): String =
        buildString {
            var escapado = false
            value.forEach { c ->
                when {
                    escapado -> {
                        append(
                            when (c) {
                                'n' -> '\n'
                                't' -> '\t'
                                'r' -> '\r'
                                else -> c
                            },
                        )
                        escapado = false
                    }
                    c == '\\' -> escapado = true
                    else -> append(c)
                }
            }
        }

    private val FIELD = Regex(""""([a-z]+)"\s*:\s*(?:"((?:[^"\\]|\\.)*)"|null)""")
    private val AMOUNT = Regex("""\d+(?:\.\d{1,2})?""")
}
