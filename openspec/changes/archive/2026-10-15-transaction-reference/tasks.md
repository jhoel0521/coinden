# Tasks: Número de Transacción y Gastos Repetidos

## 1. Referencia (dominio)

- [x] 1.1 Escribir las pruebas de lectura de la referencia con los comprobantes anonimizados (BE con plantilla "400000001", Yape sin dos puntos "900000001", banco argentino desconocido "123456", Pix "E00000000000000000001", pago en inglés "000111222", retiro con cédula y código de autorización → "8000001", frase "almuerzo 45 bnb" sin referencia) e implementar D1 y D2 hasta que pasen con `:core:domain:jvmTest`.
- [x] 1.2 Escribir las pruebas del borrador (la referencia sobrevive a la revisión, corregir la nota no la borra, llega al movimiento confirmado; efectivo sin referencia) e implementarlas.

## 2. Coincidencias (dominio)

- [x] 2.1 Escribir la prueba de feature de Carlos con los escenarios de la pregunta (taxi y power de 10 Bs, audio y comprobante del power, ya confirmado, otro día u otro monto, mismo comprobante, mismo archivo) e implementar D3 y D4.
- [x] 2.2 Escribir las pruebas de las respuestas (son distintos no vuelve a preguntar y el saldo baja 20 Bs; son el mismo con el comprobante repetido baja una vez; power con su comprobante queda con nota "power", "BNB Bs", 10:42 y referencia; la evidencia no se pierde; unir con el taxi confirmado no cambia el saldo; billetera distinta se rechaza) e implementar D5 y D6.

## 3. Persistencia

- [x] 3.1 Agregar los campos opcionales y los eventos nuevos al JSON (D7) con su ida y vuelta, la lectura de JSON guardado sin los campos y la conservación de los eventos en la batería de contrato de memoria y base local; verificar con `:core:data-local:jvmTest`.

## 4. App

- [x] 4.1 Escribir pruebas de presentadores (huella del archivo al guardarlo; formulario con referencia a mano y al pegar un comprobante; compartir el comprobante tras dictar el power muestra la pregunta; Son el mismo deja un pendiente menos; Son distintos quita la pregunta) e implementarlas.
- [x] 4.2 Referencia, evidencias y pregunta en la tarjeta, campo en el formulario; textos en es, en y pt; verificar que `npm test`, `npm run lint` y `npm run build:android` pasan.

## 5. Verificación en el dispositivo

- [x] 5.1 En el emulador: escribir "gasté 10 en power" y compartir el comprobante de 10 Bs (responder Son el mismo); capturar dos "gasté 10 bs por qr" (responder Son distintos); compartir dos veces el mismo comprobante; guardar capturas como evidencia.

## Workflow follow-up

- Archivar `transaction-reference` con `opsx-archive` tras la revisión.
- Siguiente del MVP: `perception-adapters` (leer fotos, PDF y audios con estas mismas etiquetas); después `statement-reconciliation`, que hará la misma pregunta para cada línea del extracto.
