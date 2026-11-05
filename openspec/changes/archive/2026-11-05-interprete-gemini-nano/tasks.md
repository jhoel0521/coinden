# Tasks

> **Cierre (0.2.2): desconectado.** Decisión de la persona: ningún modelo de lenguaje (ni propio ni
> del sistema) viaja en la 0.2.2, porque no entendieron mejor que las reglas deterministas; la app
> queda con reglas, OCR y voz a texto. Se retiró el adaptador `GeminiNanoEngine`, la dependencia
> `genai-prompt` y su cableado (con ellos desaparece el permiso de enlace con AICore). El adaptador
> existió en los commits b196239 y 3cff261. La 2.2 queda **no verificada** y 3.1 no aplica porque
> no se ofrece. Retomar en una versión futura solo con una medición en un teléfono compatible.

## 1. Adaptador al modelo del sistema (system-language-model)

- [x] 1.1 Pruebas del adaptador con un cliente falso: no disponible devuelve `null`; disponible devuelve el texto; las vallas de código se quitan; una excepción devuelve `null` sin romper la captura; usa temperatura 0 y topK 1 _(`GeminiNanoEngineTest`, 7 pruebas.)_
- [x] 1.2 Agregar `com.google.mlkit:genai-prompt` a `gradle/libs.versions.toml` e implementar `GeminiNanoEngine` como `ModelEngine`; verificar que las pruebas de 1.1 pasan y anotar cuánto crece el AAB _(Release universal: **62,8 MB** con esta librería, contra 111,7 MB cuando llevaba LiteRT-LM. `FeatureStatus` verificado en la librería: 0 no disponible, 1 descargable, 2 descargando, 3 disponible.)_
- [x] 1.3 Cablear en `CoinDenApplication` solo si la persona aceptó el motor de Google (`googleReaderEnabled()`), consultándolo en cada captura y no solo al abrir; prueba de que sin consentimiento no se consulta _(`CoinDenApplication` arma el intérprete con el motor y le pasa `googleReaderEnabled()` como puerta, que se mira en cada captura.)_

## 2. Verificación en el emulador

- [x] 2.1 Probar en el emulador la ruta "sin modelo del sistema → reglas" con el motor de Google activado y desactivado; anotar el estado que devuelve `checkStatus()` _(Emulador Gama_baja_4GB, API 34: `checkStatus()` = `UNAVAILABLE`, no existe el paquete de AICore, `generate` devuelve `null` en 2 ms y la prueba pasa: sin modelo mandan las reglas. `GeminiNanoDeviceTest`.)_
- [x] 2.2 Si el emulador (o un teléfono compatible) tiene el modelo, medir tiempo y calidad con las frases de siempre y con los casos de `contexto-de-captura`; anotar los números. Si no lo tiene, dejarlo anotado como no verificado _(**No verificado**: no hay un teléfono compatible (Pixel 10, Galaxy S26, algunos Xiaomi y vivo) ni el emulador trae el modelo. Falta medir tiempo y calidad en uno real. Con el motor de Google aceptado, en un teléfono compatible el adaptador **ya se activaría sin haberse medido**: ver 3.1.)_

## 3. Privacidad y cierre

- [x] 3.1 Si se decide ofrecerlo: actualizar la pantalla de privacidad, `site/privacidad` (es, en, pt) y la ficha de Play para decir que, con el motor de Google activado, el modelo del sistema también interpreta el texto en el teléfono _(**Pendiente y bloqueante para publicar**: antes de ofrecerlo hay que actualizar la pantalla de privacidad, el sitio y la ficha de Play, o desactivar el cableado en la versión que se suba.)_
- [x] 3.2 `npm test`, `npm run lint`, `npm run build:android` y `openspec validate --all --strict` en verde
