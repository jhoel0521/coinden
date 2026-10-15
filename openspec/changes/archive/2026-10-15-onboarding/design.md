# Design: Primer Inicio (Onboarding)

## Context
Ver proposal.md y los specs `app-onboarding`, `text-interpreter` y `app-shell`. Hoy `CoinDenApplication` abre la base, siembra `DemoSeed` y muestra una sola pantalla (`CaptureDemoScreen`). El dominio ya tiene lo necesario: `ConfigureLocalCurrencyUseCase`, `CreateWalletUseCase`, `SetDefaultWalletUseCase`, `GetActiveWalletBalancesUseCase`, `GetConsolidatedPortfolioUseCase` y `GetDraftInboxUseCase`. `StaticInstalledPacks` activa español y portugués siempre. Los textos de la UI están solo en `composeResources/values` (español). La base local está en la versión 1 del esquema.

## Goals / Non-Goals

**Goals:**
- Que el dominio decida (apodos deducidos, regla de billetera por defecto) y la UI solo presente y recoja datos.
- Presentadores sin Compose, probados en la JVM, como `CaptureDemoPresenter`.

**Non-Goals:**
- Administrar billeteras, ajustes completos y barra inferior (ver proposal).

## Decisions

### D1. Apodos deducidos en el dominio
`WalletNameHints.derive(wallets: List<Wallet>, pack: LanguagePack): Map<String, String>` (palabra normalizada → id de billetera), pura, en `model/interpret`:
1. Para cada billetera activa, las palabras de `TextNormalizer.normalize(name)`.
2. Se descartan: palabras de moneda (`pack.currencyWords`, códigos y símbolos que `MoneyScanner` reconoce como moneda), de medio de pago (`pack.paymentMethodWords`) y genéricas (nuevo `pack.genericWalletWords`), y palabras de menos de 3 letras.
3. Una palabra que aparece en dos o más billeteras activas se descarta.

`genericWalletWords`: es `banco, cuenta, caja, mi, mis, ahorro, ahorros, sueldo`; en `bank, account, my, savings, checking`; pt `banco, conta, minha, meu, poupanca`.

En `PhraseReader`, la búsqueda de billetera hace primero lo que ya hace (alias configurado, `WalletRef.ByHint`) y, si no hay, busca una palabra no usada en el mapa deducido: exacta y después con la tolerancia de tipeo de los verbos (`TextNormalizer.matches`, palabras de 4+ letras). Si la encuentra, propone `WalletRef.ById(id)` con origen `STATED`. Al proponer por id, el evaluador no necesita conocer los apodos deducidos.

Alternativas descartadas: guardar los apodos deducidos con `SetWalletAliases` al crear la billetera (se desactualizan si cambia el conjunto de billeteras y las palabras compartidas cambian con cada billetera nueva) y deducirlos en la UI (lógica fuera del dominio).

### D2. Idioma: recursos, preferencia guardada y paquetes
- Textos en `composeResources/values` (es, por defecto), `values-en` y `values-pt`, con las mismas claves. Una prueba compara que los tres archivos tengan exactamente las mismas claves.
- Preferencia guardada mediante un puerto nuevo del dominio, `PreferencesRepositoryPort` (`languageCode(): String?`, `setLanguageCode(code)`), con el código ISO como dato sin idioma. Implementado en memoria (`core/data-memory`) y en la base local (tabla `app_settings`), con su prueba de contrato.
- Idioma efectivo: el guardado; si no hay, el del teléfono si es `es`, `en` o `pt`; si no, `en`.
- La UI aplica el idioma con un `CompositionLocal` de locale de la app: en Android se fija `Locale.setDefault` y la `Configuration` de los recursos al elegirlo (patrón documentado de Compose Multiplatform para cambiar el idioma de la app), sin reiniciar la actividad.
- `StaticInstalledPacks` pasa a recibir el idioma: paquete inglés siempre más el del idioma elegido (español o portugués). `UserLocale` usa el idioma elegido y el país del teléfono (el país sigue decidiendo separador decimal y moneda local sugerida).

### D3. Primera migración del esquema
`1.sqm` crea `app_settings(name TEXT PRIMARY KEY, value TEXT NOT NULL)`. El esquema pasa a la versión 2; SQLDelight migra las bases existentes al abrirlas. Una prueba abre una base creada con el esquema 1, la migra y comprueba que conserva sus datos y tiene la tabla nueva.

### D4. Catálogo mundial de monedas (puerto)
`CurrencyCatalogPort` en el dominio: `all(): List<Currency>` (ISO 4217 vigentes, con sus decimales), `find(code): Currency?` y `forRegion(countryCode): Currency?`. Sin nombres ni símbolos: el dominio sigue sin idioma.
- Adaptador `PlatformCurrencyCatalog` en `core/data-local` (`jvmMain` y `androidMain`, el mismo código): `java.util.Currency.getAvailableCurrencies()`, `defaultFractionDigits` (las de -1, como XAU, se omiten) y `Currency.getInstance(Locale("", país))`. iOS tendrá su adaptador con Foundation (`NSLocale.ISOCurrencyCodes`, `NumberFormatter`).
- Batería de contrato (BOB 2 decimales, CLP 0, BHD 3; Bolivia → BOB, Francia → EUR, China → CNY, República Dominicana → DOP) que corren el catálogo falso de las pruebas y el de la plataforma.
- Nombres y símbolos en la UI: función de presentación `currencyDisplayName(code, language)` con `java.util.Currency.getDisplayName(Locale(language))` en Android; si la plataforma no tiene nombre (moneda propia), el nombre que escribió la persona.
- Cripto: lista corta del dominio (BTC) más monedas propias; no son ISO.

### D5. La moneda de la región ya no es una tabla del dominio
`UserLocale` deja de tener las tablas `COUNTRY_CURRENCY` y `COMMA_DECIMAL_COUNTRIES`: recibe `currency` (la de la región) y `decimalSeparator` como datos, que llena el adaptador de configuración regional con el catálogo y con el formato numérico de la plataforma (`DecimalFormatSymbols`). Las pruebas del dominio construyen `UserLocale` con un ayudante de prueba que usa el catálogo falso; los resultados no cambian.

### D6. Perfil con varias monedas y tasas de referencia
`UserCurrencyProfile` gana `currencies: List<Currency>` (las que usa además de la local, propias incluidas) y `referenceRates: Map<String, ExchangeRateStrategy>` (código → estrategia hacia la moneda local). El `strategy` actual se conserva como la referencia del ancla (USD) por compatibilidad: al leer un perfil viejo, se toma como `referenceRates[USD]`.
- `ProfileRates` y `CurrencyConverter` convierten cada moneda con su propia estrategia; una moneda sin estrategia no se convierte.
- `GetConsolidatedPortfolioUseCase` devuelve, además del total, la lista de saldos sin tasa (que la presentación muestra aparte).
- Onboarding: tasa escrita con el formato del locale ("10,20") → `AmountReader` → `ExchangeRate.parse(moneda, local, canónica)` → `ExchangeRateStrategy.ManualDaily(rate, ahora)`. "La pongo después" → la moneda queda en `currencies` sin entrada en `referenceRates`. Una sola moneda: perfil con la local, sin otras monedas y con la identidad como referencia del ancla si la local es USD.
- Persistencia: `ProfileDto` agrega `currencies` y `referenceRates` como campos opcionales (versión 1 compatible; prueba con un JSON de perfil viejo).

### D6b. Frecuentes del paso de monedas
Frecuentes = USD, EUR y las monedas de los países de la misma región que el teléfono (por ejemplo, en Bolivia: ARS, BRL, CLP, PEN, PYG), calculadas con el catálogo y una lista de regiones (Sudamérica, Norteamérica, Europa, Asia oriental…) como datos de la app. La búsqueda recorre `all()` por código y por nombre en el idioma de la app.

### D7. Billeteras y billetera por defecto
El paso 3 junta las billeteras en el estado del presentador y solo al terminar las crea con `CreateWalletUseCase`, en el orden en que se agregaron. Luego elige la por defecto con una función pura de la app, `defaultWalletFor(wallets, localCurrency)`: la primera de efectivo en la moneda local; si no hay, la primera creada; y la guarda con `SetDefaultWalletUseCase`. La regla del dominio (`WalletDirectory.resolveDefault`) no cambia: la elección explícita tiene prioridad.

### D8. Navegación y pantallas
`org.jetbrains.androidx.navigation:navigation-compose` 2.9.2 (requiere Compose 1.8.2 o superior). Rutas: `welcome`, `currencies`, `wallets`, `home`, `capture`. El destino inicial lo decide `OnboardingStatus` (perfil y al menos una billetera → `home`; si no → `welcome`). Al terminar el onboarding se navega a `home` y se borra la pila del onboarding. Presentadores: `OnboardingPresenter` (los tres pasos y sus validaciones) y `HomePresenter` (patrimonio con `GetActiveWalletBalancesUseCase` + `GetConsolidatedPortfolioUseCase`, saldos y borradores pendientes con `GetDraftInboxUseCase`). Las pantallas siguen los tableros "Onboarding" e "Inicio" del lienzo con `CoinDenTheme`.

### D9. Sin datos de ejemplo
Se eliminan `DemoSeed` y su prueba. `CoinDenApplication` ya no siembra nada. Las pruebas de la app crean sus billeteras como lo haría el onboarding.

## Risks / Trade-offs
- [Una palabra del nombre coincide con una palabra común de una frase ("pague 20 a juan" con una billetera "Juan")] → es lo que la persona escribió como nombre; se propone como dicha y se puede corregir en la revisión.
- [Cambiar el idioma sin reiniciar depende de la plataforma] → hoy solo Android; Desktop e iOS tendrán su propia implementación del mismo `CompositionLocal`.
- [Traducciones hechas por el agente] → son textos cortos de interfaz; se revisan en las capturas de verificación.

- [El separador decimal ahora sale de la plataforma] → en es-BO la plataforma usa coma (CLDR), así que los montos se muestran "3.547,80 BOB" y no "3,547.80 BOB" como con la tabla anterior del dominio. El lector de montos sigue aceptando punto o coma; si en Bolivia se prefiere el punto, se decide como ajuste de la persona, no como tabla del dominio.

## Migration Plan
Las instalaciones de prueba con la demo anterior tienen billeteras de ejemplo y no tienen perfil: verán el onboarding. Para empezar limpio basta con borrar los datos de la app.
