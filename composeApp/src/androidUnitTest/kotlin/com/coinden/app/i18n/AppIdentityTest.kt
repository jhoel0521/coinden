package com.coinden.app.i18n

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppIdentityTest {
    @Test
    fun `el saludo principal esta traducido y los iconos de la marca estan declarados`() {
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
        )) {
            val icon = File("src/androidMain/res/$path").readText()
            assertTrue(icon.contains("@mipmap/ic_launcher_foreground"), path)
            if ("v33" in path) assertTrue(icon.contains("@mipmap/ic_launcher_monochrome"), path)
        }
        // La marca nueva: primer plano y silueta en cada densidad, sobre el fondo #1C140F.
        for (density in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")) {
            for (name in listOf("ic_launcher_foreground.png", "ic_launcher_monochrome.png")) {
                assertTrue(File("src/androidMain/res/mipmap-$density/$name").exists(), "$density/$name")
            }
            assertTrue(File("src/androidMain/res/drawable-$density/splash_icon.png").exists(), "$density/splash_icon.png")
        }
        assertTrue(File("src/androidMain/res/values/colors.xml").readText().contains("#1C140F"))
        // Splash de la marca en MainActivity.
        assertTrue(manifest.contains("android:theme=\"@style/Theme.CoinDen.Splash\""))
        val themes = File("src/androidMain/res/values/themes.xml").readText()
        assertTrue(themes.contains("@drawable/splash_icon") && themes.contains("@color/ic_launcher_background"))
        // No queda el búho-moneda anterior.
        for (old in listOf("drawable/ic_launcher_foreground.xml", "drawable/ic_launcher_monochrome.xml", "mipmap-anydpi/ic_launcher.xml")) {
            assertFalse(File("src/androidMain/res/$old").exists(), old)
        }
    }
}
