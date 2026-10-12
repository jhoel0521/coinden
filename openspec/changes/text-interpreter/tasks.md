# Tasks: Intérprete de Texto Agnóstico (TDD: primero la prueba)

## 1. Confianza en draft-inbox

- [x] 1.1 Escribir `DraftConfidenceTest` (sin orígenes = STATED → ALTA; un INFERRED → MEDIA; un SUGGESTED → BAJA; la revisión marca CORRECTED y sube a ALTA). Luego agregar `FieldOrigin`, `Confidence`, `origins` en `MovementProposal`, `CURRENCY` y `OCCURRED_AT` en `DraftField`, `confidence` en `Draft`, y marcar CORRECTED en `ReviseDraftUseCase` hasta que pase; verificar que las pruebas existentes de draft-inbox siguen pasando.
- [x] 1.2 Ordenar la bandeja por confianza y luego por momento de captura; agregar el escenario "lo dudoso primero" a las pruebas de la bandeja y verificar que pasa.
- [x] 1.3 Implementar la billetera por defecto (`setDefaultWallet`/`defaultWalletFor` en el puerto y el fake, `defaultWalletId` en `WalletDirectory`, `SetDefaultWalletUseCase`) con pruebas de los tres escenarios: única de efectivo, elección explícita y sin defecto con dos de efectivo.

## 2. Lectores básicos

- [x] 2.1 Escribir `TextNormalizerTest` (minúsculas, sin tildes ni signos; distancia de edición ≤ 1 solo para palabras de 4 o más letras) e implementar `TextNormalizer`.
- [x] 2.2 Escribir `AmountReaderTest` ("45,50" y "45.50" en es-BO → 45.50; "12.500,00" en es-AR → 12500.00; "1,234.56" en en-US → 1234.56; "1.234" en es-BO → INFERRED; los dígitos nunca cambian) e implementar `UserLocale`, `AmountReader` y `CurrencyReader` ("Bs", "US$", "R$", "€", "S/", ISO y "$" según el país) hasta que pase.
- [x] 2.3 Escribir `DateTimeReaderTest` ("03 oct. 2026" + "12:54 p. m." en UTC-4 → 16:54 UTC; "10/Oct/2026 14:22:50"; "10/11/2026" en en-US → 11 de octubre, INFERRED; sin hora → no propone) e implementar `DateTimeReader` con conversión civil a epoch hasta que pase.

## 3. Paquetes y plantillas como datos

- [x] 3.1 Definir `LanguagePack` con `LanguagePacks.ENGLISH`, `LanguagePacks.SPANISH` y `LanguagePacks.PORTUGUESE`, y `ReceiptTemplate` con `YAPE_BO`, `BE_QR_PAYMENT`, `BE_ECOMMERCE_DEBIT` y `BE_CASH_WITHDRAWAL`; definir `InstalledPacksPort` y `RegionalSettingsPort` con sus fakes; verificar que compila y que todo el contenido lingüístico vive solo en los paquetes.

## 4. Intérpretes

- [x] 4.1 Escribir `PhraseReaderTest` ("page 3 bs para trasmporte", "gaste 2 para un semillita", "almuerzo 45 qr", "almuerzo 45 y micro 3", "vendi 50 dolares a 11.88", "paid 12 usd for lunch" e "I pay 3 dollars for transportation" con solo inglés, "paguei 15 reais de almoço" con "pt", frase en español sin paquete "es") con sus orígenes esperados, e implementar `PhraseReader` hasta que pase.
- [x] 4.2 Escribir `TemplateReaderTest` con comprobantes anonimizados de Yape (gasto 36.00 desde la billetera "yape") y de Banco Económico (Pago QR 25.00 y 31.70, débito por compra 118.71 con signo negativo en el texto, retiro 10.00 hacia la billetera por defecto), resolviendo la cuenta de origen por alias; implementar `TemplateReader` hasta que pase.
- [x] 4.3 Escribir `GenericReceiptReaderTest` con comprobantes inventados de bancos desconocidos (AR "Transferiste … Monto $ 8.000,00 … Para: …", BR "Pix enviado … Valor R$ 45,90 … Para …", US "You paid … Amount $25.00 … To …", retiro genérico) e implementar `GenericReceiptReader` hasta que pase; verificar que una cuenta desconocida deja la billetera faltante.
- [x] 4.4 Implementar `TextDraftInterpreter` (orden plantilla → comprobante genérico → frase; texto vacío → sin propuestas; mismas entradas → mismas propuestas) con pruebas de selección y de determinismo.

## 5. Pruebas de feature

- [x] 5.1 `CarlosReceiptsFeatureTest`: con `CaptureDraftUseCase` y el intérprete real, capturar las 2 frases y los 5 comprobantes anonimizados; verificar las confianzas (comprobantes ALTA, frases BAJA), el orden de la bandeja, y confirmar todo en la noche con saldos exactos (retiro incluido).
- [x] 5.2 `WorldReceiptsFeatureTest`: Facundo (es-AR) con un comprobante desconocido en pesos, John (en-US) con uno en dólares y una frase en inglés, una usuaria de Brasil (pt-BR) con un comprobante Pix leído con el paquete "pt", y el mismo comprobante sin el paquete "pt" (monto y moneda por símbolo; el resto con menor confianza).

## 6. Documentación y verificación

- [x] 6.1 Actualizar la regla 1 de AGENTS.md, el principio 1 del MANIFIESTO y el `context` de `openspec/config.yaml` para permitir descargar paquetes de idioma y plantillas sin enviar datos del usuario; marcar text-interpreter en el roadmap del README.
- [x] 6.2 Ejecutar `npm test` y `npm run lint` en verde, confirmar que las 148 pruebas previas siguen pasando y verificar con `grep` que las pruebas no contienen datos reales de los comprobantes compartidos.

## Workflow follow-up

- Archivar `text-interpreter` con `opsx-archive` tras la revisión.
- Siguientes: paquete "pt", detección de duplicados por número de transacción y `expense-categorization`.
