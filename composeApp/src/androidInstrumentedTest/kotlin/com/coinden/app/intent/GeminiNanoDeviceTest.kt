package com.coinden.app.intent

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * En el dispositivo: qué estado devuelve el modelo del sistema y, si está disponible, qué responde.
 * Si no lo está (el caso normal, y el del emulador), el motor devuelve `null` y mandan las reglas.
 * Los resultados salen por el log, etiqueta `CoinDenGemini`.
 */
@RunWith(AndroidJUnit4::class)
class GeminiNanoDeviceTest {
    @Test
    fun estadoYRespuesta() {
        val client = MlKitSystemModelClient()
        val estado = runCatching { runBlocking { client.status() } }
        Log.i("CoinDenGemini", "estado=${estado.getOrNull()} error=${estado.exceptionOrNull()}")
        val motor = GeminiNanoEngine(client) { true }
        val t0 = System.nanoTime()
        val texto = runBlocking { motor.generate(AndroidLocalModel.SYSTEM_PROMPT, "[CONTEXT a] almuerzo 45 en efectivo", "", 0f) }
        Log.i("CoinDenGemini", "respuesta=$texto en ${(System.nanoTime() - t0) / 1_000_000} ms")
        if (estado.getOrNull() != SystemModelStatus.AVAILABLE) assertNull("sin modelo debe devolver null", texto)
    }
}
