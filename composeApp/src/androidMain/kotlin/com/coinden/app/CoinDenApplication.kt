package com.coinden.app

import android.app.Application
import com.coinden.core.data.local.AndroidDatabase
import com.coinden.core.data.local.LocalCategorizationRepository
import com.coinden.core.data.local.LocalCurrencyProfileRepository
import com.coinden.core.data.local.LocalDraftRepository
import com.coinden.core.data.local.LocalIdentity
import com.coinden.core.data.local.LocalWalletAliasRepository
import com.coinden.core.data.local.LocalWalletRepository
import com.coinden.core.data.memory.FixedDeviceIdentity
import com.coinden.core.data.memory.StaticRegionalSettings
import com.coinden.core.data.memory.SystemClock
import com.coinden.core.data.memory.UuidIdGenerator
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.TimeZone

/** Abre la base cifrada y arma el contenedor una sola vez por proceso (sobrevive a la rotación). */
class CoinDenApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AndroidDatabase.open(this).database
        val identity = LocalIdentity(db) { UuidIdGenerator().newId() }
        val io = Dispatchers.IO
        val storage =
            AppStorage(
                wallets = LocalWalletRepository(db, io),
                drafts = LocalDraftRepository(db, io),
                aliases = LocalWalletAliasRepository(db, io),
                categorization = LocalCategorizationRepository(db, io),
                profiles = LocalCurrencyProfileRepository(db, io),
            )
        val locale = Locale.getDefault()
        val offsetMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
        container =
            AppContainer(
                storage = storage,
                ownerId = identity.ownerId(),
                regional = StaticRegionalSettings(RegionalSettings(UserLocale(locale.language, locale.country), offsetMinutes)),
                device = FixedDeviceIdentity(identity.deviceId()),
                clock = SystemClock(),
            )
        // Base pequeña y local: abrir y sembrar toma milisegundos. Pasa a carga asíncrona con las pantallas reales.
        runBlocking { DemoSeed.seed(container) }
    }
}
