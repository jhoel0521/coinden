# Tasks

> **Cierre (4-nov-2026): archivado sin completar, a propósito.** Se construyó y probó la parte
> que no depende de un modelo (grupos 1 y 3: contrato, validador, orquestador) y el adaptador a
> LiteRT-LM con descarga y pantalla (grupo 2 y 4). Se midió con el modelo real en emuladores de
> 4 y 6 GB (Qwen 3 0.6B y 1.7B, `.litertlm`, Apache 2.0): ~6 s un pago, ~8 s dos, 11 a 13 s un
> paquete; el 0,6B acierta "ambulante" y "taxi" pero pierde pagos en "comida y somó" e inventa
> montos; el 1,7B copió la nota del ejemplo del prompt y devolvió un solo pago. **No cumple el
> criterio de 5 s ni entiende mejor que las reglas deterministas**, que hoy funcionan mejor.
> Por eso el motor **salió del app y del AAB** (commit `cfe44df`; la versión con motor, descarga y
> pantalla está en `60bbe6b`, `2c956ed`, `98558f8` y `023f884` por si se retoma). El respaldo
> determinista **no se recorta** (grupo 6): las reglas siguen siendo la interpretación principal.
> Lo que queda en `core/domain` (puerto `IntentInterpreterPort`, `ProposalValidator`,
> `BundleInterpretation`, `IntentGrammar` con tope de 9 dígitos) sigue probado y es lo que usará el
> siguiente cambio, `interprete-gemini-nano`. Los deltas de specs de este cambio **no se
> sincronizaron**: describen un modelo activo que la app ya no incluye.

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
- [x] 2.5 Prueba de determinismo: la misma captura dos veces da la misma propuesta. _(Emulador Gama_baja_4GB, Qwen 3 0.6B real: 3 corridas idénticas de la misma captura, `LanguageModelDeviceTest#b_determinismoYTiempos`.)_

## 3. Los dos caminos y el desacuerdo

- [x] 3.1 Pruebas: coinciden en los hechos → Listo con la propuesta del modelo; no coinciden → Para revisar con las dos lecturas; el modelo falla → queda el respaldo.
- [x] 3.2 Orquestador que corre modelo y respaldo, compara los hechos y arma el borrador con su motivo.
- [x] 3.3 Motivos nuevos de revisión, como enum sin textos, traducidos en la presentación.

- [ ] 3.4 Los escenarios de `contexto-de-captura` que piden entender lenguaje, con el modelo:
  Con el modelo real (`LanguageModelDeviceTest#c_frasesDeContexto`), solo el modelo, sin el validador: **ambulante** 3 pagos (25, 60 y 25 en efectivo) ✓; **taxi entre varios** 25, 60 y 15 "taxi" ✓; **comida y somó** devuelve 2 de 4 pagos (pierde un 10) ✗; **comprobante equivocado** inventa "2500" donde el comprobante dice 25.00 — el validador lo rechaza porque no está en el texto, y el borrador va a revisar ✓ (es justo para lo que está el validador). **No se marca**: falta correr estos casos de punta a punta (modelo + reglas + paquete) y ver el resultado en pantalla; el modelo de 0,6B solo no basta en el caso del somó.
  ambulante; comida y somó; modelo y reglas no coinciden; varias frases en un audio.

## 4. Descarga, verificación y pantalla

- [x] 4.1 Pruebas de las reglas de oferta: 4 GB y arm64 ofrecen el pequeño; 6 GB o más, el mediano; 2 GB, 32 bits o sin espacio no ofrecen nada. _(`LanguageModelRecommenderTest`: 4 GB y 64 bits → Qwen 3 0.6B; 6 GB o más → Qwen 3 1.7B; 2 GB, 32 bits o sin espacio → nada, con su motivo.)_
- [x] 4.2 Pruebas: se verifica la huella; un archivo alterado se borra; la petición no lleva datos de la persona; el modelo se puede borrar; si no carga, se sigue con el respaldo y se avisa una vez. _(`VerifiedDownloadTest` ya cubre la huella, el archivo alterado y la petición sin datos; `LanguageModelPresenterTest` cubre borrar y el aviso único; `InstalledLanguageModelTest`, el modelo que no carga.)_
- [x] 4.3 Descarga del modelo sobre el mismo mecanismo que los de voz (`VerifiedDownload`), sin duplicarlo. Catálogo del modelo de lenguaje con URL pública, tamaño, huella SHA-256 real y RAM mínima (Qwen 3 0.6B `.litertlm` de `litert-community`, tras verificar su licencia). La descarga de ~586 MB se puede reanudar o tiene un tiempo de espera acorde, con prueba. _(Catálogo en `LanguageModels.kt` con las huellas del repositorio `litert-community`, licencia Apache 2.0 verificada. **No se agregó reanudación**: `VerifiedDownload` reintenta desde cero; el tiempo de espera es por lectura, no total. Queda como mejora si la descarga de ~614 MB se corta seguido en la medición.)_
- [x] 4.3b La app reconoce un modelo recién descargado **sin reiniciar** (hoy `CoinDenApplication` lo evalúa una sola vez en `onCreate`) y libera el motor (`LiteRtModelEngine.release()`) al borrar el modelo; con prueba. _(`InstalledLanguageModel` mira el archivo en cada captura y libera el motor al borrar.)_
- [x] 4.4 Pantalla del modelo de lenguaje: qué hace, que corre en el teléfono, que no envía datos, que solo propone, su tamaño y el botón de borrar. _(Sección "Modelo de lenguaje" dentro de la pantalla de modelos de voz, para no cambiar la lista de Perfil que fija `app-navigation`.)_
- [x] 4.5 Textos en es, en y pt.
- [x] 4.6 **Decidido: dentro del APK, como hoy** (ver design.md, Riesgos). Motivo: sin riesgo de integración; el costo es el peso del AAB. Decidir si la librería de LiteRT-LM va como **módulo bajo demanda** de Play en lugar de
  dentro del APK. Hoy `liblitertlm_jni.so` suma unos 21 MB por persona (arm64) aunque nunca descargue
  el modelo, y el AAB pasó de 41,6 MB en la 0.1.0 a 81 MB. Quien no use el modelo no debería pagar ese
  peso. Anotar la decisión y su motivo.

## 5. Medición en teléfono real (criterio de aceptación)

- [x] 5.0 Crear un emulador de gama baja (4 GB de RAM, 4 núcleos, imagen `android-34` x86_64) y otro de 2 GB; verificar en el de 2 GB que el modelo **no se ofrece** y que la app funciona con el respaldo. No hay teléfono físico disponible: la medición en teléfono real queda como verificación pendiente de la persona. _(Gama_baja_4GB: x86_64, 4 GB, 4 núcleos, API 34. El de 2 GB (`Android14_API34`) muestra "Tu teléfono no tiene memoria suficiente para este modelo" y la app sigue con el respaldo.)_
- [ ] 5.1 Medir en el emulador de 4 GB (perfil de gama baja; el teléfono de referencia real es arm64 tipo Galaxy A15 o Redmi 13C): una captura típica se interpreta en **menos de 5 s**, sin cierres por memoria. Anotar el modelo exacto, su tamaño y los tiempos.
  **Medido, NO cumple**: en el emulador de 4 GB (x86_64), Qwen 3 0.6B tarda **~6 s** un pago ("almuerzo 45 en efectivo"), **~8 s** dos pagos ("almuerzo 45 y micro 3") y **11–13 s** un paquete de 2 comprobantes más audio; la primera carga del modelo, ~12 s. Memoria nativa ~1,07 GB, sin cierres. Es un emulador x86_64, no el teléfono de referencia arm64: el número real puede ser mejor o peor. Hallazgos al medir: sin tope de dígitos en el monto el modelo escribió un 1 seguido de cientos de ceros (corregido: tope de 9 dígitos, con prueba en `IntentGrammarTest`); un prompt más corto dio peor resultado ("page 3 bs para trasmporte" → monto 0).
- [ ] 5.3 Confirmar en ejecución que interpretar no genera tráfico de red (el motor es de Google y el Manifiesto prohíbe la telemetría); mirar el tráfico del emulador mientras interpreta y anotar el resultado. _(Se comprobó que interpretar **funciona sin red** (wifi y datos apagados, `ping` inalcanzable, la prueba pasa). **No se pudo medir** el conteo de bytes del proceso con `dumpsys netstats` desde adb: queda pendiente la captura de tráfico.)_
- [ ] 5.4 Probar el APK **release minificado** con el modelo: `proguard-rules.pro` solo protege `WhisperNative`; agregar las reglas `-keep` que LiteRT-LM necesite (JNI, gson, kotlin-reflect). _(Reglas `-keep` para `com.google.ai.edge.litertlm.**` en `proguard-rules.pro`. Verificado en el mapeo de R8: sin ellas `Engine` se renombra a `ja.e`; con ellas conserva su nombre. **No se probó el APK release en ejecución con el modelo** (la prueba instrumentada apunta al APK debug).)_
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
- El orden importa: el recorte del respaldo (6) va después de medir el modelo (5), nunca antes.
- El recorte (6.3 y 6.4) edita `core/domain` y los paquetes: se confirma con la persona al empezar esa fase.
- Al terminar, actualizar `contexto-de-captura` para que su intérprete del paquete sea el modelo con reglas que validan.
- Un solo PR.
