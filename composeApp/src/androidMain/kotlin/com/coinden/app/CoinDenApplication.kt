package com.coinden.app

import android.app.Application
import com.coinden.app.capture.EncryptedAttachmentStore
import com.coinden.app.currency.CurrencyNames
import com.coinden.app.i18n.AppLanguage
import com.coinden.core.data.local.AndroidDatabase
import com.coinden.core.data.local.LocalCategorizationRepository
import com.coinden.core.data.local.LocalCurrencyProfileRepository
import com.coinden.core.data.local.LocalDraftRepository
import com.coinden.core.data.local.LocalIdentity
import com.coinden.core.data.local.LocalPreferencesRepository
import com.coinden.core.data.local.LocalWalletAliasRepository
import com.coinden.core.data.local.LocalWalletRepository
import com.coinden.core.data.local.PlatformCurrencyCatalog
import com.coinden.core.data.memory.FixedDeviceIdentity
import com.coinden.core.data.memory.StaticRegionalSettings
import com.coinden.core.data.memory.SystemClock
import com.coinden.core.data.memory.UuidIdGenerator
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.interpret.UserLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.TimeZone

/** Abre la base cifrada y arma el contenedor una sola vez por proceso (sobrevive a la rotación). */
class CoinDenApplication : Application() {
    lateinit var container: AppContainer
        private set

    /** Idioma y región del teléfono al abrir la app (la app puede cambiar luego el locale por defecto). */
    var deviceLanguage: String = "en"
        private set
    var deviceCountry: String = ""
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
                preferences = LocalPreferencesRepository(db, io),
            )
        val catalog = PlatformCurrencyCatalog()
        val device = Locale.getDefault()
        deviceLanguage = device.language
        deviceCountry = device.country
        // La región del teléfono decide la moneda de "$" o "pesos" y el separador decimal (datos de la plataforma).
        val locale =
            UserLocale(
                language = device.language,
                country = device.country,
                currency = catalog.forRegion(device.country) ?: Currency.USD,
                decimalSeparator = DecimalFormatSymbols.getInstance(device).decimalSeparator,
            )
        val offsetMinutes = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
        // Lectura única y local al iniciar (milisegundos); pasa a carga asíncrona cuando haga falta.
        val savedLanguage = runBlocking { storage.preferences.languageCode() }
        container =
            AppContainer(
                storage = storage,
                ownerId = identity.ownerId(),
                regional = StaticRegionalSettings(RegionalSettings(locale, offsetMinutes)),
                device = FixedDeviceIdentity(identity.deviceId()),
                clock = SystemClock(),
                catalog = catalog,
                currencyNames = PlatformCurrencyNames,
                initialLanguage = savedLanguage ?: AppLanguage.proposedFor(device.language),
                attachments = EncryptedAttachmentStore(filesDir.resolve("attachments")),
            )
    }
}

/** Nombres de monedas en el idioma de la app, con los datos de la plataforma. */
object PlatformCurrencyNames : CurrencyNames {
    override fun name(
        code: String,
        language: String,
    ): String? =
        runCatching {
            java.util.Currency
                .getInstance(code)
                .getDisplayName(Locale.Builder().setLanguage(language).build())
        }.getOrNull()
            ?.replaceFirstChar { it.uppercase() }
}
