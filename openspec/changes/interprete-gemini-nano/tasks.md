# Tasks

## 1. Adaptador al modelo del sistema (system-language-model)

- [ ] 1.1 Pruebas del adaptador con un cliente falso: no disponible devuelve `null`; disponible devuelve el texto; las vallas de código se quitan; una excepción devuelve `null` sin romper la captura; usa temperatura 0 y topK 1
- [ ] 1.2 Agregar `com.google.mlkit:genai-prompt` a `gradle/libs.versions.toml` e implementar `GeminiNanoEngine` como `ModelEngine`; verificar que las pruebas de 1.1 pasan y anotar cuánto crece el AAB
- [ ] 1.3 Cablear en `CoinDenApplication` solo si la persona aceptó el motor de Google (`googleReaderEnabled()`), consultándolo en cada captura y no solo al abrir; prueba de que sin consentimiento no se consulta

## 2. Verificación en el emulador

- [ ] 2.1 Probar en el emulador la ruta "sin modelo del sistema → reglas" con el motor de Google activado y desactivado; anotar el estado que devuelve `checkStatus()`
- [ ] 2.2 Si el emulador (o un teléfono compatible) tiene el modelo, medir tiempo y calidad con las frases de siempre y con los casos de `contexto-de-captura`; anotar los números. Si no lo tiene, dejarlo anotado como no verificado

## 3. Privacidad y cierre

- [ ] 3.1 Si se decide ofrecerlo: actualizar la pantalla de privacidad, `site/privacidad` (es, en, pt) y la ficha de Play para decir que, con el motor de Google activado, el modelo del sistema también interpreta el texto en el teléfono
- [ ] 3.2 `npm test`, `npm run lint`, `npm run build:android` y `openspec validate --all --strict` en verde
