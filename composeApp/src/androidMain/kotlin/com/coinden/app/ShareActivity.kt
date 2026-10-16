package com.coinden.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.share_not_saved
import coinden.composeapp.generated.resources.share_saved
import com.coinden.app.capture.QuickCapture
import com.coinden.app.capture.SharedIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.util.Locale

/**
 * Recibe lo que otra app comparte con CoinDen sin mostrar ninguna pantalla: lo guarda, avisa
 * "Guardado en CoinDen" y devuelve a la otra app. Lo pesado (leer la foto, el PDF o el audio)
 * queda para la cola. Si el onboarding no terminó, abre la app para terminarlo.
 */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CoinDenApplication
        // El aviso sale en el idioma elegido en la app, no en el del teléfono.
        Locale.setDefault(Locale.forLanguageTag(app.container.languageCode))
        lifecycleScope.launch {
            if (!app.container.isOnboarded()) {
                startActivity(
                    Intent(intent).setClass(this@ShareActivity, MainActivity::class.java).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
                finish()
                return@launch
            }
            val content = withContext(Dispatchers.IO) { SharedIntents.read(applicationContext, intent) }
            val saved = content?.let { QuickCapture(app.container, app.perception.queue).shared(it) }.orEmpty().isNotEmpty()
            Toast
                .makeText(
                    applicationContext,
                    getString(if (saved) Res.string.share_saved else Res.string.share_not_saved),
                    Toast.LENGTH_SHORT,
                ).show()
            finish()
        }
    }
}
