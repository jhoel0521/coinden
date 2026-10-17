package com.coinden.app.i18n

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AppIdentityTest {
    @Test
    fun `el saludo principal esta traducido y los iconos estan declarados`() {
        for ((folder, greeting) in listOf(
            "values" to "Tu guarida de monedas",
            "values-en" to "Your coin den",
            "values-pt" to "Sua toca de moedas",
        )) {
            val xml = File("src/commonMain/composeResources/$folder/strings.xml").readText()
            assertTrue(xml.contains("<string name=\"welcome_greeting\">$greeting</string>"))
        }
        val manifest = File("src/androidMain/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
        for (path in listOf(
            "mipmap-anydpi-v26/ic_launcher.xml",
            "mipmap-anydpi-v26/ic_launcher_round.xml",
            "mipmap-anydpi-v33/ic_launcher.xml",
            "mipmap-anydpi-v33/ic_launcher_round.xml",
            "mipmap-anydpi/ic_launcher.xml",
            "drawable/ic_launcher_foreground.xml",
            "drawable/ic_launcher_monochrome.xml",
        )) {
            assertTrue(File("src/androidMain/res/$path").exists(), path)
        }
        val monochrome = File("src/androidMain/res/drawable/ic_launcher_monochrome.xml").readText()
        assertTrue(monochrome.contains("M46 104 V95"))
        assertTrue(monochrome.contains("M47.5 50.5"))
        assertTrue(monochrome.contains("M74 49 V63"))
    }
}
