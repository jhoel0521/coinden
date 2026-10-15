# Tasks: Primer Inicio (Onboarding)

## 1. Dominio

- [x] 1.1 Agregar `genericWalletWords` a `LanguagePack` (en/es/pt) y escribir pruebas de `WalletNameHints.derive` ("BNB Bs" → bnb; "Banco Económico" → economico, sin "banco"; "Dólares colchón" y "Dólares banco" → colchon, sin "dolares"; billeteras archivadas no cuentan; palabras de moneda y de medio de pago descartadas); implementarlo hasta que pase con `:core:domain:jvmTest`.
- [x] 1.2 Escribir en `PhraseReaderTest` los escenarios de billeteras reconocidas por su nombre (sigla, palabra genérica, error de tipeo "economco", palabra compartida, "bs" sigue siendo moneda) e integrarlos en `PhraseReader` (D1) hasta que pasen junto a todas las pruebas del dominio.
- [x] 1.3 Agregar `PreferencesRepositoryPort` al dominio y su implementación en memoria con prueba.
- [x] 1.4 Agregar `CurrencyCatalogPort` y un catálogo falso de prueba; quitar de `UserLocale` las tablas de países (D5) pasando la moneda de la región y el separador decimal como datos; agregar el escenario "pesos" en República Dominicana; verificar que todas las pruebas del dominio pasan sin cambiar sus resultados.
- [x] 1.5 Escribir pruebas del perfil con varias monedas (Carlos con USD a 10.20 y BRL a 1.85 → 1020.00 + 92.50 BOB; BRL sin tasa → aparte; un perfil viejo con solo `strategy` se lee como referencia del USD; un cambio real no usa ni modifica la referencia) e implementar D6 en el perfil, `ProfileRates`, el conversor y el consolidado.

## 2. Persistencia

- [x] 2.0 Implementar `PlatformCurrencyCatalog` (D4) y la batería de contrato del catálogo (falso y plataforma: decimales de BOB, CLP y BHD; Bolivia, Francia, China y República Dominicana); agregar `currencies` y `referenceRates` opcionales a `ProfileDto` con su ida y vuelta y la lectura de un perfil viejo; verificar con `:core:data-local:jvmTest`.
- [x] 2.1 Crear la migración `1.sqm` con `app_settings` (esquema 2) y `LocalPreferencesRepository`; agregar la preferencia a la batería de contrato (memoria y base local) y una prueba que migra una base del esquema 1 conservando sus datos; verificar con `:core:data-local:jvmTest`.

## 3. Presentadores

- [x] 3.1 Escribir pruebas de `OnboardingPresenter` (idioma propuesto según el teléfono, con inglés si no es es/en/pt; moneda principal sugerida por la región; Carlos solo con bolivianos termina sin tasas; Carlos con USD a "10,20" → tasa manual con fecha; BRL con "La pongo después" → sin tasa; buscar "yuan" encuentra CNY; moneda propia "VALE" con 0 decimales; frecuentes de Bolivia; las billeteras solo ofrecen las monedas elegidas; no termina sin billeteras; Carlos crea tres billeteras y la por defecto es "Billetera"; John solo con "Chase" → por defecto "Chase") e implementarlo hasta que pasen.
- [x] 3.2 Escribir pruebas de `HomePresenter` (patrimonio 3.547,80 Bs de Carlos, sus billeteras con saldo y 2 borradores pendientes; 50 BRL sin tasa se muestran aparte) y de `OnboardingStatus` (sin datos → bienvenida; con perfil y billetera → Inicio); implementarlos hasta que pasen.

## 4. Idioma

- [x] 4.1 Agregar `values-en` y `values-pt` con todas las claves, una prueba que verifica que los tres idiomas tienen las mismas claves, el locale de la app (D2) y `StaticInstalledPacks` según el idioma (prueba: con portugués el intérprete entiende "paguei 30 reais no pix"); verificar con `:composeApp:testDebugUnitTest`.

## 5. Pantallas y navegación

- [x] 5.1 Agregar navigation-compose al catálogo; implementar las pantallas de bienvenida, monedas (principal, frecuentes, búsqueda, moneda propia y tasas con la aclaración de que son solo para totales), billeteras e Inicio según el lienzo y la navegación de D8; eliminar `DemoSeed` y su prueba; actualizar `CoinDenApplication`; verificar que compila, que no hay textos fijos en las pantallas y que `npm test` y `npm run lint` pasan.

## 6. Verificación en el dispositivo

- [x] 6.1 Con los datos de la app borrados, recorrer el onboarding de Carlos en el emulador (español, bolivianos y dólares a 10,20, sus tres billeteras), comprobar Inicio, capturar "almuerzo 45 bnb" (BNB Bs, dicha) y "page 3 bs para trasmporte", cerrar y reabrir (abre en Inicio); repetir la bienvenida eligiendo portugués; guardar capturas como evidencia.

## Workflow follow-up

- Archivar `onboarding` con `opsx-archive` tras la revisión.
- Siguiente del MVP: `capture-ui` + compartir a CoinDen.
