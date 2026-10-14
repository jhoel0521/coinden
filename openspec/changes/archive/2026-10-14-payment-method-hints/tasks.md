# Tasks: Medios de Pago como Pistas, no como Billeteras

## 1. Dominio

- [x] 1.1 Escribir pruebas de `PaymentMethod` y `WalletType.acceptedMethods` (tabla de D2) e implementarlos; verificar con `:core:domain:jvmTest`.
- [x] 1.2 Agregar `paymentMethodWords` a `LanguagePack` (valor por defecto vacío, unión en `merged()`) con las palabras de en, es y pt de D3; extender `LanguagePackTest` y verificar que pasa.
- [x] 1.3 Agregar `paymentMethod` opcional a las propuestas de gasto, ingreso y transferencia (D5) y verificar que las pruebas existentes del dominio siguen pasando.

## 2. Intérprete

- [x] 2.1 Reescribir los casos de `PhraseReaderTest` que usaban "qr" como alias (pasan a "bnb") y escribir los escenarios del delta: medio de pago (varias cuentas QR → primera recomendada; una sola; la por defecto acepta el medio; ninguna; Pix de Ana; el alias gana sobre el medio) y efectivo por moneda (efectivo sin moneda → Bs; efectivo en dólares; "pague 20 dolares" → efectivo en dólares; efectivo en una moneda que no tiene); más las regresiones de cambio de moneda; implementar D4 en `PhraseReader` hasta que pasen.
- [x] 2.2 Actualizar las pruebas de borradores y de feature de Carlos que usaban el alias "qr" (`DraftUseCasesTest`, `DraftEvaluatorTest`, `CarlosDraftDayFeatureTest` y las demás que fallen), dándole a Carlos varias cuentas de banco donde el escenario lo pida; verificar que `:core:domain:jvmTest` pasa completo.

## 3. Persistencia

- [x] 3.1 Agregar `paymentMethod` opcional a `ProposalDto`, ampliar la prueba de ida y vuelta con un medio y la lectura de un JSON versión 1 sin el campo; actualizar los usos de "qr" en las pruebas de `core/data-local` y verificar con `:core:data-local:jvmTest`.

## 4. App y documentación

- [x] 4.1 Cambiar el alias de la demo a "bnb", actualizar `AppContainerTest` y `CaptureDemoPresenterTest` con los escenarios del delta de `app-shell` ("almuerzo 45 qr" → banco sugerido, confianza baja; "almuerzo 45 bnb" → banco, confianza media) y verificar con `:composeApp:testDebugUnitTest`.
- [x] 4.2 Reescribir la regla 4 de `AGENTS.md` y el ejemplo de categoría y medio del README (D6); verificar que `npm test`, `npm run lint` y `npm run build:android` pasan.

## Workflow follow-up

- Archivar `payment-method-hints` con `opsx-archive` tras la revisión.
- Siguiente del MVP: `onboarding-wallets-ui` (incluye editar medios por billetera y el diseño visual nuevo).
