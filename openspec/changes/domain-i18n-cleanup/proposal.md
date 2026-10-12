# Proposal: Dominio sin Idioma (Domain i18n Cleanup)

## Why
El principio 9 del manifiesto dice que el dominio no contiene textos de idioma ni formatos regionales, pero el código heredado de currency-hub lo rompe: el sello de proveedor del consolidado devuelve frases en español ("Manual del Usuario", "Tasa Fija (BOB)"), el catálogo de monedas y fuentes trae nombres y descripciones en español ("Dólar Estadounidense", "Cotización paralela del Dólar…"), `Money` arma textos para pantalla con símbolo y punto decimal fijo ("Bs 35.50"), y el parser de montos acepta coma o punto según le convenga. Conviene corregirlo ahora, antes de que `draft-inbox` y la UI empiecen a depender de esos textos.

## What Changes
- **BREAKING** El sello de proveedor del consolidado deja de exponer un texto: expone el **tipo de fuente** (base, fija, manual, verificada, selector web), su **referencia** (identificador de fuente o URL), la tasa, la fecha y si es un respaldo degradado. La presentación arma la frase en el idioma del usuario.
- **BREAKING** Las monedas del catálogo (USD, EUR, BOB, BTC, …) se identifican solo por código y decimales. El nombre y el símbolo quedan únicamente para las monedas personalizadas, porque los escribe el usuario.
- **BREAKING** Las fuentes verificadas pierden su descripción en español y su nombre pasa a ser solo el nombre propio del proveedor (DolarApi, DolarBlueBolivia, Frankfurter, CoinGecko).
- **BREAKING** `Money` deja de ofrecer un formato para pantalla. Conserva una representación **canónica** neutral (`"1188.00 BOB"`: punto decimal y código ISO) para pruebas, registros y serialización.
- **BREAKING** El parser de montos acepta solo la forma canónica (punto decimal). Interpretar "35,50" o "1.234,56" según el idioma del usuario es responsabilidad de la presentación o del futuro parser de borradores.

### Non-Goals
- Reemplazar las tasas `Double` por aritmética exacta (queda para un cambio `exact-rates`).
- Construir la capa de presentación o los archivos de traducción.
- Traducir los mensajes de `require` dirigidos a desarrolladores: no se muestran al usuario.

## Capabilities

### New Capabilities
<!-- Ninguna -->

### Modified Capabilities
- `currency-configuration`: la vista unificada identifica la fuente de conversión con datos estructurados en lugar de un texto, y se agrega el requisito de representar montos y monedas sin idioma.

## Impact
- **Código**: `Currency`, `Money`, `RateSourceDefinition` y `VerifiedSourceDirectory`, `ProviderStamp` y `GetConsolidatedPortfolioUseCase` en `core/domain`.
- **Pruebas**: se ajustan `MoneyTest`, `CurrencyTest`, `CurrencyConverterTest` y las pruebas de feature de John, Carlos, Facundo, Sophie y Wei que comparan textos. `wallet-management` no se ve afectado porque nació sin textos.
- **Sin consumidores externos**: todavía no hay UI ni persistencia, así que los cambios incompatibles no tienen costo de migración.
