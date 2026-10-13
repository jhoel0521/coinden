# Tasks: Cambios de Moneda por Entrada y Salida

## 1. Paquetes de idioma

- [ ] 1.1 Agregar `receivedMarkers` a `LanguagePack` (con valor por defecto vacío y unión en `merged()`) y los marcadores de en, es y pt de D1; escribir la prueba de unión de paquetes y verificar que pasa junto a las existentes con `:core:domain:jvmTest`.

## 2. Lectura de frases de cambio

- [ ] 2.1 Escribir en `PhraseReaderTest` los escenarios de lectura (es: "vendí 11 dólares, me dieron 112 bs", "vendi 100 dolares y me dieron 1190 bs", "cambié 100 usd por 1190 bs", "vendí 100 dólares a 11.80, me dieron 1190 bs", "vendí 100 dólares, me dieron 1190"; en: "sold 100 usd, got 1190 bob"; pt: "vendi 100 dólares, recebi 530 reais") más las regresiones ("vendi 50 dolares a 11.88", "I pay 3 dollars for transportation", "recibí 500 bs" como ingreso, "almuerzo 45 y micro 3" como dos gastos), e implementar D2, D3 y D4 en `PhraseReader` hasta que pasen.
- [ ] 2.2 Escribir las pruebas de billeteras sugeridas por moneda (una billetera USD → origen sugerido; dos billeteras USD → origen vacío; destino por defecto o única billetera de la moneda recibida; billeteras archivadas no cuentan) e implementar D5 hasta que pasen.

## 3. Feature de punta a punta

- [ ] 3.1 Escribir la prueba de feature de Carlos en el librecambista: captura "vendí 11 dólares, me dieron 112 bs" con "Dólares colchón" y su billetera de efectivo por defecto; el borrador queda Listo con confianza baja, al confirmarlo el libro tiene la salida de 11.00 USD, la entrada de 112.00 BOB y la tasa derivada 112/11; con dos billeteras USD queda Incompleto. Verificar que pasa y que `npm test` y `npm run lint` siguen en verde.

## Workflow follow-up

- Archivar `exchange-phrases` con `opsx-archive` tras la revisión.
- Retomar `local-persistence` (propuesta ya creada).
