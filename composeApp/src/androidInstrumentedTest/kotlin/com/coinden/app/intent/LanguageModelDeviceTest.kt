package com.coinden.app.intent

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coinden.app.perception.AndroidLanguageModels
import com.coinden.app.perception.LanguageModels
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.bundle.EvidenceRole
import com.coinden.core.domain.model.draft.EvidenceSource
import com.coinden.core.domain.model.draft.MovementProposal
import com.coinden.core.domain.model.draft.WalletDirectory
import com.coinden.core.domain.model.interpret.ModelIntentInterpreter
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.BundlePayment
import com.coinden.core.domain.port.output.BundleText
import com.coinden.core.domain.port.output.LocalModelPort
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Con el modelo de lenguaje **real** descargado en el teléfono (tareas 2.5, 3.4, 5.1 y 5.3):
 * la misma captura da la misma propuesta, las frases del cambio `contexto-de-captura` producen
 * propuestas del dominio, y se mide el tiempo y la memoria. Se salta si el modelo no está
 * descargado (se baja desde la pantalla de modelos). Los números salen por el log, etiqueta
 * `CoinDenLM`.
 */
@RunWith(AndroidJUnit4::class)
class LanguageModelDeviceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val file = File(context.filesDir, "models/${LanguageModels.LOCAL_FILE_NAME}")

    private companion object {
        const val AMBULANTE = "estaba comiendo y luego un ambulante pasó y le di 25 en efectivo"
        const val SOMO = "estaba comiendo 60 y 10, pasó el somosero y le compré somó 10"
    }

    private val cash = Wallet("efectivo", "carlos", "Efectivo", Currency.BOB, WalletType.CASH)
    private val bnb = Wallet("bnb", "carlos", "BNB Bs", Currency.BOB, WalletType.BANK)
    private val directory =
        WalletDirectory(listOf(cash, bnb), mapOf("efectivo" to "efectivo", "bnb" to "bnb"), defaultWalletId = "efectivo")
    private val categories = setOf("comida", "transporte", "otros")

    private fun qr(
        id: String,
        amount: String,
    ) = BundleText(id, EvidenceRole.RECEIPT, EvidenceSource.PHOTO, "Pago QR otros bancos Procesada Importe Bs$amount")

    private fun voice(text: String) = BundleText("audio", EvidenceRole.CONTEXT, EvidenceSource.VOICE, text)

    /** Deja en el log lo que se le pidió al modelo y lo que respondió, sin cambiar nada. */
    private class Recorder(
        private val inner: LocalModelPort,
    ) : LocalModelPort {
        override suspend fun complete(
            input: String,
            grammar: String,
        ): String? {
            val out = inner.complete(input, grammar)
            Log.i("CoinDenLM", "ENTRADA: ${input.take(600)}")
            Log.i("CoinDenLM", "RESPUESTA CRUDA: ${out?.take(800)}")
            return out
        }
    }

    private fun newInterpreter() = InstalledLanguageModel(file) { LiteRtModelEngine(it) }.let { it to ModelIntentInterpreter(Recorder(it)) }

    private fun describe(pagos: List<BundlePayment>?) =
        pagos?.joinToString(" | ") { p ->
            val e = p.proposal
            if (e is MovementProposal.Expense) {
                "gasto ${e.amount} wallet=${e.wallet} medio=${e.paymentMethod} nota=${e.note}"
            } else {
                e
                    .toString()
            }
        } ?: "null"

    private fun memoryMb(): Long {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        return Debug.getNativeHeapAllocatedSize() / 1_000_000
    }

    /** Baja el modelo real por la misma ruta que la pantalla y comprueba su huella SHA-256. */
    @Test
    fun a_descargaYVerificaElModelo() {
        assumeTrue("Ya está descargado", !file.isFile)
        val manager = AndroidLanguageModels(context, InstalledLanguageModel(file) { LiteRtModelEngine(it) })
        var ultimo = 0L
        val ok =
            runBlocking {
                manager.download(LanguageModels.SMALL) { hecho, _ ->
                    if (hecho - ultimo > 50_000_000) {
                        ultimo = hecho
                        Log.i("CoinDenLM", "descargado ${hecho / 1_000_000} MB")
                    }
                }
            }
        assertTrue("la descarga debe verificar la huella", ok)
        assertEquals(LanguageModels.SMALL.sizeBytes, file.length())
    }

    @Test
    fun b_determinismoYTiempos() {
        assumeTrue("El modelo no está descargado", file.isFile)
        val textos = listOf(qr("e1", "25.00"), qr("e2", "60.00"), voice(AMBULANTE))
        val (modelo, interprete) = newInterpreter()
        val tiempos = mutableListOf<Long>()
        val resultados = mutableListOf<String>()
        runBlocking {
            repeat(3) { i ->
                val t0 = System.nanoTime()
                val pagos = interprete.interpret(textos, directory, categories)
                val ms = (System.nanoTime() - t0) / 1_000_000
                tiempos += ms
                resultados += describe(pagos)
                Log.i("CoinDenLM", "corrida $i: $ms ms, nativeHeap=${memoryMb()} MB -> ${describe(pagos)}")
            }
            modelo.release()
        }
        val frias = tiempos.first()
        val calientes = tiempos.drop(1)
        Log.i(
            "CoinDenLM",
            "primera(carga)=$frias ms, siguientes: ${calientes.joinToString()} ms, mediana=${calientes.sorted()[calientes.size / 2]} ms",
        )
        assertEquals("la misma captura debe dar la misma propuesta", 1, resultados.toSet().size)
        assertNotNull(resultados.first())
    }

    /** Capturas típicas de una frase: es lo que mide el criterio de los 5 s. */
    @Test
    fun d_capturaTipica() {
        assumeTrue("El modelo no está descargado", file.isFile)
        val frases = listOf("almuerzo 45 en efectivo", "page 3 bs para trasmporte", "almuerzo 45 y micro 3")
        val (modelo, interprete) = newInterpreter()
        runBlocking {
            interprete.interpret(listOf(voice("hola")), directory, categories) // calienta: carga el modelo
            for (frase in frases) {
                val tiempos = mutableListOf<Long>()
                var ultimo: List<BundlePayment>? = null
                repeat(3) {
                    val t0 = System.nanoTime()
                    ultimo = interprete.interpret(listOf(voice(frase)), directory, categories)
                    tiempos += (System.nanoTime() - t0) / 1_000_000
                }
                Log.i("CoinDenLM", "TIPICA '$frase': ${tiempos.joinToString()} ms -> ${describe(ultimo)}")
            }
            modelo.release()
        }
    }

    @Test
    fun c_frasesDeContexto() {
        assumeTrue("El modelo no está descargado", file.isFile)
        val casos =
            mapOf(
                "ambulante" to listOf(qr("e1", "25.00"), qr("e2", "60.00"), voice(AMBULANTE)),
                "comida y somó" to listOf(qr("e1", "60.00"), qr("e2", "10.00"), qr("e3", "10.00"), voice(SOMO)),
                "comprobante equivocado" to listOf(qr("e1", "25.00"), voice("gasté 1500, acá te paso el comprobante")),
                "taxi entre varios" to listOf(qr("e1", "25.00"), qr("e2", "60.00"), voice("y le pagué 15 al taxi")),
            )
        val (modelo, interprete) = newInterpreter()
        runBlocking {
            for ((nombre, textos) in casos) {
                val pagos = interprete.interpret(textos, directory, categories)
                Log.i("CoinDenLM", "caso '$nombre' -> ${describe(pagos)}")
                assertTrue("el caso '$nombre' no devolvió propuesta", pagos != null)
            }
            modelo.release()
        }
    }
}
