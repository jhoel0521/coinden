# Tasks

## 1. El contrato y el validador (sin modelo todavía)

- [x] 1.1 Pruebas del validador con Carlos: un monto que no está en ningún texto se rechaza; una billetera fuera del directorio se rechaza; una categoría que no existe se rechaza; cambiar la moneda de un comprobante se rechaza; una propuesta respaldada pasa.
- [x] 1.2 `ProposalValidator` puro en `core/domain`, con los motivos de rechazo como enum sin textos.
- [x] 1.3 Pruebas: el validador trata igual una propuesta del modelo y una del respaldo.
- [x] 1.4 Puerto `IntentInterpreterPort` con el contrato cerrado (entrada `BundleText`, salida `BundlePayment`, `null` si no hay modelo).

## 2. El modelo local

- [x] 2.1 Pruebas del traductor de salida: una respuesta válida se convierte en `BundlePayment`; una respuesta fuera de formato devuelve `null`; un `walletId` desconocido se descarta.
- [x] 2.2 Gramática GBNF generada del esquema de `BundlePayment`, con la lista cerrada de monedas, billeteras y categorías de la persona.
- [x] 2.3 Prompt de sistema fijo, en el adaptador y no en el dominio, con temperatura 0.
- [x] 2.4 Adaptador Android sobre **LiteRT-LM**, con `ResponseFormat.json(...)` y el *speculative
  decoding* apagado. Enchufado: la app usa el modelo si está descargado y el respaldo si no.
  **Falta** descargar un modelo real y probarlo en un teléfono (va con la 2.5).
- [ ] 2.5 Prueba de determinismo: la misma captura dos veces da la misma propuesta.

## 3. Los dos caminos y el desacuerdo

- [x] 3.1 Pruebas: coinciden en los hechos → Listo con la propuesta del modelo; no coinciden → Para revisar con las dos lecturas; el modelo falla → queda el respaldo.
- [x] 3.2 Orquestador que corre modelo y respaldo, compara los hechos y arma el borrador con su motivo.
- [x] 3.3 Motivos nuevos de revisión, como enum sin textos, traducidos en la presentación.

- [ ] 3.4 Los escenarios de `contexto-de-captura` que piden entender lenguaje, con el modelo:
  ambulante; comida y somó; modelo y reglas no coinciden; varias frases en un audio.

## 4. Descarga, verificación y pantalla

- [ ] 4.1 Pruebas de las reglas de oferta: 4 GB y arm64 ofrecen el pequeño; 6 GB o más, el mediano; 2 GB, 32 bits o sin espacio no ofrecen nada.
- [ ] 4.2 Pruebas: se verifica la huella; un archivo alterado se borra; la petición no lleva datos de la persona; el modelo se puede borrar; si no carga, se sigue con el respaldo y se avisa una vez.
- [ ] 4.3 Descarga del modelo sobre el mismo mecanismo que los de voz, sin duplicarlo.
- [ ] 4.4 Pantalla del modelo de lenguaje: qué hace, que corre en el teléfono, que no envía datos, que solo propone, su tamaño y el botón de borrar.
- [ ] 4.5 Textos en es, en y pt.
- [ ] 4.6 Decidir si la librería de LiteRT-LM va como **módulo bajo demanda** de Play en lugar de
  dentro del APK. Hoy `liblitertlm_jni.so` suma unos 21 MB por persona (arm64) aunque nunca descargue
  el modelo, y el AAB pasó de 41,6 MB en la 0.1.0 a 81 MB. Quien no use el modelo no debería pagar ese
  peso. Anotar la decisión y su motivo.

## 5. Medición en teléfono real (criterio de aceptación)

- [ ] 5.1 Medir en el teléfono de referencia (4 GB, arm64, tipo Galaxy A15 o Redmi 13C): una captura típica se interpreta en **menos de 5 s**, sin cierres por memoria. Anotar el modelo exacto, su tamaño y los tiempos.
- [ ] 5.2 Si no se cumple, bajar de modelo o recortar el prompt y volver a medir. No se acepta sin el número.

## 6. El respaldo mínimo determinista

> **Va al final a propósito.** Recortar `Readers.kt` quita capacidades que hoy funcionan sin
> modelo. Si se hiciera antes de que el modelo ande y esté medido, cualquier versión publicada
> entre medias dejaría a la gente con menos de lo que ya tenía y sin nada a cambio. Primero el
> modelo funcionando en un teléfono real; recién después se recorta lo que el modelo reemplaza.


- [ ] 6.1 Pruebas del respaldo con las frases de siempre: "page 3 bs para trasmporte", "almuerzo 45 bnb", "almuerzo 45 y micro 3", y un comprobante por plantilla.
- [ ] 6.2 Prueba: sin modelo, "estaba comiendo y luego un ambulante pasó y le di 25 en efectivo" da un gasto de 25.00 Bs en efectivo con el texto como nota y queda para revisar.
- [ ] 6.3 Recortar `Readers.kt`: se va lo que adivina lenguaje (verbos, conectores, preposiciones de nota, marcadores de lo recibido); quedan monto, moneda, fecha, alias, medio de pago y los lectores de comprobante.
- [ ] 6.4 Borrar de los archivos JSON las listas que ya no se usan, y de `LanguagePack` sus campos.
- [ ] 6.5 Prueba: la categorización sin modelo queda en las reglas aprendidas de la persona.

## 7. Cierre

- [ ] 7.1 Emulador: instalar la 0.1.0 con datos, **actualizar sin desinstalar**, comprobar que no se pierde nada y que todo se interpreta sin modelo; después, instalar el modelo y repetir.
- [ ] 7.2 `npm test`, `npm run lint`, `npm run build:android`, `bundleRelease` y `openspec validate --all` en verde.

## Workflow follow-up

- Depende de `paquetes-como-datos`: primero las palabras salen a archivos, después se borran las que el modelo reemplaza.
- El orden importa: el recorte del respaldo (7) va después de medir el modelo (6), nunca antes.
- Al terminar, actualizar `contexto-de-captura` para que su intérprete del paquete sea el modelo con reglas que validan.
- Un solo PR.
