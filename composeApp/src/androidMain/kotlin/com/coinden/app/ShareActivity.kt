package com.coinden.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.share_not_saved
import coinden.composeapp.generated.resources.share_saved
import com.coinden.app.capture.AndroidCapturePlatform
import com.coinden.app.capture.ContextRoute
import com.coinden.app.capture.QuickCapture
import com.coinden.app.capture.SharedIntents
import com.coinden.app.theme.CoinDenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.util.Locale

/**
 * Recibe lo que otra app comparte con CoinDen, uno o varios archivos a la vez. Lo guarda en un
 * paquete abierto y muestra la página de contexto para sumarle un audio, una nota o una foto.
 * Listo, Atrás o irse a otra app cierran el paquete, avisan "Guardado en CoinDen" y devuelven a la
 * otra app. Lo pesado (leer la foto, el PDF o el audio) queda para la cola. Si el onboarding no
 * terminó, abre la app para terminarlo.
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
            // Todo lo que la otra app mandó junto va a un mismo paquete, sean uno o varios archivos.
            val contents = withContext(Dispatchers.IO) { SharedIntents.readAll(applicationContext, intent) }
            val bundleId = QuickCapture(app.container, app.perception.queue).sharedBundle(contents)
            if (bundleId == null) {
                toast(saved = false)
                finish()
                return@launch
            }
            val platform = AndroidCapturePlatform(this@ShareActivity, app.perception, app.container.attachments)
            setContent {
                CoinDenTheme {
                    Surface(Modifier.fillMaxSize()) {
                        ContextRoute(app.container, bundleId, platform, lifecycle, app.scope) {
                            app.perception.queue.kick()
                            toast(saved = true)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private suspend fun toast(saved: Boolean) =
        Toast
            .makeText(
                applicationContext,
                getString(if (saved) Res.string.share_saved else Res.string.share_not_saved),
                Toast.LENGTH_SHORT,
            ).show()
}
