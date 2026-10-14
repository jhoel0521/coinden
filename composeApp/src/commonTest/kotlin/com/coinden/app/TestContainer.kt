package com.coinden.app

import com.coinden.app.currency.CurrencyNames
import com.coinden.core.data.memory.FixedDeviceIdentity
import com.coinden.core.data.memory.StaticRegionalSettings
import com.coinden.core.domain.model.Currency
import com.coinden.core.domain.model.Money
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.interpret.RegionalSettings
import com.coinden.core.domain.model.wallet.WalletOperationResult
import com.coinden.core.domain.model.wallet.WalletType
import com.coinden.core.domain.port.output.ClockPort
import com.coinden.core.domain.port.output.CurrencyCatalogPort

/** Reloj de prueba que avanza un segundo en cada lectura (los eventos nunca empatan). */
class SteppingClock(
    private var now: Long = 1_760_000_000_000,
) : ClockPort {
    override fun nowEpochMs(): Long = now.also { now += 1_000 }
}

/** Recorte del catálogo ISO 4217 (en la app los datos salen de la plataforma). */
object FakeCatalog : CurrencyCatalogPort {
    private val byCode =
        listOf(
            Currency.USD,
            Currency.EUR,
            Currency.BOB,
            Currency.ARS,
            Currency.BRL,
            Currency.CLP,
            Currency.PEN,
            Currency.CNY,
            Currency("PYG", decimals = 0),
            Currency("UYU", decimals = 2),
            Currency("COP", decimals = 2),
            Currency("VES", decimals = 2),
            Currency("MXN", decimals = 2),
            Currency("JPY", decimals = 0),
        ).associateBy { it.code }
    private val byRegion =
        mapOf(
            "BO" to "BOB",
            "AR" to "ARS",
            "BR" to "BRL",
            "CL" to "CLP",
            "PE" to "PEN",
            "PY" to "PYG",
            "UY" to "UYU",
            "CO" to "COP",
            "VE" to "VES",
            "EC" to "USD",
            "US" to "USD",
            "MX" to "MXN",
            "FR" to "EUR",
            "ES" to "EUR",
            "CN" to "CNY",
            "JP" to "JPY",
        )

    override fun all(): List<Currency> = byCode.values.toList()

    override fun find(code: String): Currency? = byCode[code.uppercase()]

    override fun forRegion(countryCode: String): Currency? = byRegion[countryCode.uppercase()]?.let(byCode::get)
}

/** Nombres de prueba de algunas monedas por idioma (en la app los da la plataforma). */
val FakeCurrencyNames =
    CurrencyNames { code, language ->
        mapOf(
            "es" to mapOf("BOB" to "Boliviano", "USD" to "Dólar estadounidense", "CNY" to "Yuan", "BRL" to "Real brasileño"),
            "en" to mapOf("BOB" to "Bolivian Boliviano", "USD" to "US Dollar", "CNY" to "Chinese Yuan"),
        )[language]?.get(code)
    }

/** Contenedor como el de un teléfono de Carlos en Bolivia (es-BO, UTC-4), en memoria. */
fun testContainer(
    storage: AppStorage = AppStorage.inMemory(),
    ownerId: String = "carlos-${storage.hashCode()}",
    country: String = "BO",
    language: String = "es",
): AppContainer =
    AppContainer(
        storage = storage,
        ownerId = ownerId,
        regional = StaticRegionalSettings(RegionalSettings(testLocale(language, country), utcOffsetMinutes = -240)),
        device = FixedDeviceIdentity("device-carlos"),
        clock = SteppingClock(),
        catalog = FakeCatalog,
        currencyNames = FakeCurrencyNames,
        initialLanguage = language,
    )

/** Las billeteras de Carlos como las deja el onboarding: efectivo en Bs (por defecto) y BNB Bs. */
suspend fun seedCarlos(container: AppContainer) {
    val owner = container.ownerId
    container.storage.profiles.saveProfile(UserCurrencyProfile(userId = owner, localCurrency = Currency.BOB))

    fun created(result: WalletOperationResult) = (result as WalletOperationResult.WalletCreated).wallet.id
    val cash = created(container.createWallet(owner, "Billetera", Currency.BOB, WalletType.CASH, Money.parseCanonical("100", Currency.BOB)))
    created(container.createWallet(owner, "BNB Bs", Currency.BOB, WalletType.BANK, Money.parseCanonical("500", Currency.BOB)))
    container.setDefaultWallet(owner, cash)
}
