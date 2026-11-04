# Design

## Context

- El puerto ya existe y está probado: `IntentInterpreterPort` → `ModelIntentInterpreter` →
  `LocalModelPort`, con `AndroidLocalModel` como adaptador a un `ModelEngine`, el esquema
  (`IntentGrammar`, con tope de 9 dígitos), el validador (`ProposalValidator`) y el orquestador
  (`BundleInterpretation`). Todo vive en `core/domain` o ya es independiente del motor.
- La API `com.google.mlkit:genai-prompt:1.0.0-beta5` expone `Generation.getClient()`,
  `checkStatus()` (disponibilidad), `download()` (lo gestiona el sistema), `warmup()` y
  `generateContent(request)` con `temperature`, `topK`, `seed`, `maxOutputTokens` y prompt de
  sistema. Su `minSdk` es 26, igual que la app.
- Restricciones documentadas por Google: la inferencia solo se permite con la app en primer plano
  y hay una cuota por app. La interpretación en la cola de segundo plano puede quedar sin modelo y
  caer en las reglas, que es el comportamiento previsto.

## Decisions

### 1. Se conecta como `ModelEngine`, sin tocar el dominio

`GeminiNanoEngine : ModelEngine` en `androidMain`: comprueba `checkStatus()`; si no está
disponible devuelve `null` (el caso normal de casi todos los teléfonos); si lo está, envía
`SYSTEM_PROMPT` + entrada con `temperature = 0` y `topK = 1`, y devuelve el texto.

### 2. Sin formato forzado: lectura tolerante y validación

A diferencia de LiteRT-LM, esta API no recibe el JSON Schema como restricción de decodificación
(la salida tipada exige un compilador de esquemas propio de Google). Se pide el JSON en el prompt,
se quitan las vallas de código y `IntentResponseReader` descarta lo que no cumple. Una respuesta
fuera de formato es `null` y manda el respaldo. El validador sigue siendo quien decide.

### 3. Consentimiento: el mismo que el lector de Google

El modelo del sistema pasa por ML Kit, que envía métricas técnicas de uso (nunca textos ni
montos; ver la pantalla de privacidad). Por la restricción dura 1 de `AGENTS.md`, un SDK de
terceros solo se usa si la persona lo acepta tras un aviso claro. Se reutiliza
`googleReaderEnabled()`: sin él, el adaptador no se consulta.

### 4. Qué se puede probar y qué no

El emulador de Google APIs normalmente **no** trae AICore ni el modelo; es probable que
`checkStatus()` dé "no disponible". Eso también es un resultado válido: se prueba la ruta
"sin soporte → reglas" en el emulador y la ruta "con soporte" con un motor falso en pruebas
unitarias. La verificación con un teléfono compatible queda como paso de la persona.

## Risks / Trade-offs

- **API beta sin SLA**: Google avisa que puede romper compatibilidad. Se aísla detrás de `ModelEngine`.
- **Hoy solo gama alta**: la gente a la que apunta CoinDen (4 GB) casi seguro no lo tiene. Es un extra, no una mejora general.
- **Rendimiento no medido**: la calidad frente a las reglas debe medirse en un teléfono compatible antes de ofrecerlo como bueno. Hallazgo previo: las reglas y el OCR nativo rinden mejor que los modelos chicos.
- **Texto de privacidad**: hay que actualizar pantalla, sitio y ficha de Play antes de publicar, o el adaptador no se activa.
