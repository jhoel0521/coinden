# Proposal

## Why

`interprete-por-modelo` midió dos modelos propios (Qwen 3 0.6B y 1.7B con LiteRT-LM) y no
entendieron mejor que las reglas, tardaron 6 a 13 s y sumaban unos 40 MB al AAB. Se archivó y el
motor salió de la app. Queda por probar la otra mitad de la idea: el **modelo del sistema**
(Gemini Nano, que ya viene en el teléfono y lo gestiona Google, sin descargar nada de nuestra
parte ni sumar un motor propio). Es una opción que el Manifiesto ya permite (principio 1: "los
modelos on-device del sistema operativo").

Para quién: quien tiene un teléfono compatible (hoy gama alta: Pixel 10, Galaxy S26, algunos
Xiaomi y vivo) y quiere que CoinDen entienda frases más enredadas. El resto de las personas no
ve ningún cambio.

## What Changes

- Un adaptador del modelo del sistema (Gemini Nano por la API de ML Kit GenAI) detrás del puerto
  que ya existe (`IntentInterpreterPort` / `ModelEngine`). Solo propone; las reglas y el
  validador verifican todo, y nada toca saldos sin confirmar.
- Solo se usa si el teléfono lo tiene disponible **y** la persona aceptó el motor de Google en la
  pantalla de privacidad (el mismo consentimiento del lector de Google, porque ML Kit envía
  métricas técnicas de uso). Sin esas dos cosas, mandan las reglas, sin avisos de error.
- No se agrega ninguna descarga propia, pantalla de modelos ni motor en el APK: el sistema
  gestiona su modelo.
- Se prueba en el emulador (con las limitaciones que tenga: el modelo del sistema puede no estar
  disponible allí) y queda anotado qué se pudo verificar.

## Capabilities

### New Capabilities
- `system-language-model`: el modelo de lenguaje del sistema como adaptador opcional del intérprete de intenciones.

## Non-Goals

- Recortar las reglas deterministas: siguen siendo la interpretación principal.
- Modelos propios, descargas de modelos o motores de inferencia en el APK.
- Un modelo en la nube. El modelo del sistema corre en el teléfono.
- Cambiar el Manifiesto (principio 4): el hallazgo de que las reglas rinden mejor que un LLM chico se anota, pero no se reescribe el principio sin decisión de la persona.

## Impact

- `composeApp`: dependencia `com.google.mlkit:genai-prompt` (1.0.0-beta5, API en beta sin SLA), el permiso de enlace con AICore que declara la librería, un adaptador en `androidMain` y su cableado en `CoinDenApplication`.
- Privacidad: la pantalla, el sitio y la ficha deben decir que, con el motor de Google activado, el modelo del sistema también puede interpretar el texto en el teléfono.
- `core/domain`: sin cambios.
