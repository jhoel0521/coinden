package com.coinden.app

import android.app.Application
import com.coinden.core.data.memory.FixedDeviceIdentity
import com.coinden.core.data.memory.StaticRegionalSettings
import com.coinden.core.data.memory.SystemClock
import com.coinden.core.data.memory.UuidIdGenerator
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.TimeZone

/** Arma el contenedor una sola vez por proceso, así sobrevive a la rotación de pantalla. */
class CoinDenApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val locale = Locale.getDefault()
        val offsetMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
        container =
            AppContainer(
                regional = StaticRegionalSettings(RegionalSettings(UserLocale(locale.language, locale.country), offsetMinutes)),
                device = FixedDeviceIdentity(UuidIdGenerator().newId()),
                clock = SystemClock(),
            )
        // En memoria es instantáneo; desaparece con local-persistence y la pantalla de billeteras.
        runBlocking { DemoSeed.seed(container) }
    }
}
