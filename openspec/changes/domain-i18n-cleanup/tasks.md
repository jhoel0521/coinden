# Tasks: Dominio sin Idioma (TDD: primero la prueba)

## 1. Monedas sin textos

- [ ] 1.1 Actualizar `CurrencyTest`: el catálogo (USD, BOB, BTC, …) no tiene nombre ni símbolo, y `Currency.custom("VALE", "V", 0, "Vale comunitario")` conserva símbolo y nombre. Luego hacer `symbol` y `name` opcionales en `Currency` y limpiar el catálogo hasta que pase.

## 2. Montos canónicos

- [ ] 2.1 Reescribir `MoneyTest`: quitar las aserciones de `formatted()`, usar `toString()` canónico ("50.00 BOB", "75 VALE", "0.00050000 BTC", "-12.50 USD") y cubrir que `parseCanonical` acepte "35.50" y "-12.5" y rechace "35,50", "1.234,56", "1,234.56" y decimales de más. Luego implementar el renombre y la eliminación en `Money` hasta que pase.
- [ ] 2.2 Reemplazar `toFormattedString()` por `toString()` en `CurrencyConverterTest` y en las pruebas de feature, y eliminar el alias; verificar que esas pruebas pasan sin cambiar los valores esperados.

## 3. Fuentes y sello de proveedor estructurados

- [ ] 3.1 Quitar `description` de `RateSourceDefinition` y dejar en `name` solo el nombre propio del proveedor en `VerifiedSourceDirectory`; verificar con las pruebas de Facundo y Carlos que la configuración por lista blanca sigue funcionando.
- [ ] 3.2 Escribir las aserciones del sello estructurado en `FacundoArgentinaFeatureTest` (VERIFIED, "ar-dolar-blue") y en `CarlosBoliviaFeatureTest` (WEB_SELECTOR, URL, degradado tras el rediseño). Luego introducir `RateSourceKind`, cambiar `ProviderStamp` y adaptar `GetConsolidatedPortfolioUseCase` hasta que pasen.
- [ ] 3.3 Agregar a `JohnUsaFeatureTest` la aserción de tipo FIXED con tasa 1.0 y referencia nula (su estrategia es `Fixed(1.0)`), y verificar que pasa.

## 4. Verificación integral

- [ ] 4.1 Verificar con `grep` que `core/domain/src/commonMain` no contiene strings de usuario fuera de los mensajes de `require` y de los identificadores y URLs del catálogo; ejecutar `npm test` y `npm run lint` con todo en verde.

## Workflow follow-up

- Archivar `domain-i18n-cleanup` con `opsx-archive` tras la revisión.
- Siguiente cambio del roadmap: `draft-inbox`.
