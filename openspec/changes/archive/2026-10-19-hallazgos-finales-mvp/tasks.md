# Tasks

## 1. Consejo solo en frases (F4, capture-coaching)

- [x] 1.1 Prueba de `CaptureCoach`: foto o PDF con billetera sugerida y texto de varias líneas sin consejo; frase dictada sigue con consejo. Implementar.

## 2. Pendientes al volver (F3, capture-ui: Compartir a CoinDen)

- [x] 2.1 Señal de primer plano (prueba pura del contador) que la Application avisa en `ON_START`; `MainScreen` recarga los pendientes con ella.

## 3. Plural de confirmar (O4)

- [x] 3.1 `inbox_confirm_all` como plural en es, en y pt ("Confirmar el listo" / "Confirmar los N listos").

## 4. Última billetera (O5, wallet-settings)

- [x] 4.1 Prueba del presentador: pedir archivar la única activa da `LAST_WALLET` sin pedir confirmación; con otra activa pide confirmar. Implementar en presentador y pantalla.

## 5. Formulario al recrear la pantalla (capture-ui: Formulario que Sobrevive a Recrear la Pantalla)

- [ ] 5.1 Los presentadores de formulario viven fuera de la pantalla (por entrada de navegación), así una recreación los encuentra; probar con el emulador rotando y cambiando la letra.

## 6. Cierre

- [x] 6.1 `npm test`, `npm run lint`, `npm run build:android` y `openspec validate --all` en verde.

## Workflow follow-up

- O1 se cierra en la issue #2 sin cambios (códigos para todas las monedas).
- Archivar tras la unión.
