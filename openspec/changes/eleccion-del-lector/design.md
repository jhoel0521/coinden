# Design

## Context

Ver [proposal.md](proposal.md) — Why.

Lo que ya existe y condiciona el diseño:

- `PreferencesRepositoryPort` guarda dos banderas independientes: `googleReaderEnabled()` y
  `privacyNoticeSeen()`. Ya hay precedente de "todavía no eligió" en ese puerto:
  `languageCode()` devuelve `null`.
- `App.kt` decide el arranque con esas banderas: sin onboarding va a `WELCOME`, sin aviso
  visto va a `PRIVACY`, si no a `HOME`. `WelcomeScreen` navega a `PRIVACY`.
- `PrivacyScreen` muestra hoy dos interruptores (lector de Google y bloqueo) y un botón de
  acuse que llama a un único `onContinue`.
- `PerceptionReaders.googleImageReader` es una función que se invoca solo con
  consentimiento, y crear `MlKitImageReader` ya inicia ML Kit: el consentimiento tiene que
  decidirse **antes** de tocar ese lector, no después.

Restricción dura del proyecto: actualizar nunca borra datos. Quien viene de la 0.1.0 ya
tiene sus dos banderas escritas y no puede perder nada.

## Goals / Non-Goals

**Goals**

- Que la elección sobre el lector de Google quede registrada como una decisión, y no como la
  ausencia de una.
- Que no haga falta ningún campo nuevo en la base ni ninguna migración.
- Que la pantalla siga sirviendo para las dos situaciones en que se abre: la primera vez y
  cuando se vuelve desde Perfil.

**Non-Goals**

- Cambiar el momento en que aparece la pantalla: sigue entre la bienvenida y las monedas.
- Tocar cómo se eligen los lectores al leer una foto: eso ya está en `evidence-perception` y
  no cambia.
- Convertir el bloqueo con huella o PIN en una elección obligatoria: sigue siendo un
  interruptor opcional.

## Decisions

### El tri-estado sale de las dos banderas que ya existen

"Todavía no eligió" no necesita un valor nuevo: es exactamente
`privacyNoticeSeen() == false`. Una vez que la persona elige, se escriben las dos banderas
juntas, y a partir de ahí `googleReaderEnabled()` dice qué eligió.

| `privacyNoticeSeen` | `googleReaderEnabled` | Significa |
|---|---|---|
| `false` | cualquiera | Todavía no eligió: la pantalla pide elegir |
| `true` | `false` | Eligió seguir sin el lector |
| `true` | `true` | Eligió usar el lector |

*Alternativa descartada:* volver `googleReaderEnabled()` nullable (`Boolean?`). Habría
obligado a tocar el puerto, sus dos adaptadores y la batería de contrato, y a migrar la
bandera de quien viene de la 0.1.0, todo para expresar algo que el par de banderas ya
expresa. Más superficie y más riesgo sobre una base publicada, a cambio de nada.

### La pantalla tiene dos modos según quién la abre

El mismo componente sirve a dos situaciones, y la que manda es si la persona ya eligió:

- **Todavía no eligió** (llega desde la bienvenida, o actualizó sin haber visto el aviso):
  el lector de Google se presenta como dos acciones, "seguir con él" y "seguir sin él",
  ninguna preseleccionada, y no hay botón de continuar. Elegir una es lo que avanza.
- **Ya eligió** (vuelve desde Perfil): el lector es un interruptor con su elección actual,
  como hoy, y la pantalla se cierra cuando quiere.

*Alternativa descartada:* dejar el interruptor también la primera vez y limitarse a exigir
que lo toque. Un interruptor nace en una posición, así que no hay forma de distinguir "lo
dejó apagado a propósito" de "no lo miró": es justo la distinción que este cambio existe
para crear.

### El enlace de verificación vive en el texto, no en el código

La URL de la página de datos de Google va en los textos traducidos, junto al párrafo que
explica qué recibe. Así cambia como dato cuando Google mueva la página, y cada idioma puede
apuntar a su versión si alguna vez la hay.

## Risks / Trade-offs

- **Quien ya terminó el onboarding en la 0.1.0 vuelve a elegir una vez.** → Es el
  comportamiento que ya existía (`privacyNoticeSeen` hace que vea el aviso una vez tras
  actualizar); ahora en vez de acusar recibo, elige. Su elección anterior se muestra ya
  aplicada en la pantalla, así que confirmarla es un toque.
- **Dos botones ocupan más que un interruptor en pantallas chicas.** → La pantalla ya hace
  scroll y el piso de hardware declarado es un teléfono de gama baja; las acciones van al
  final, donde el pulgar llega.
- **Quien elige seguir sin el lector lee peor las fotos difíciles.** → Es el precio de la
  decisión y la pantalla lo dice: sin él se lee con el lector libre, que no envía nada. El
  resto de la app no cambia, que es lo que exige el Manifiesto.

## Migration Plan

No hay migración: ningún campo nuevo, ningún cambio de esquema, ninguna conversión de datos.
`UpdateFromReleaseTest` y `verifySqlDelightMigration` deben seguir en verde sin tocarlos.

Quien viene de la 0.1.0 conserva sus dos banderas: si ya vio el aviso, no vuelve a verlo; si
no, lo ve una vez y elige.

Para volver atrás alcanza con revertir la versión: los datos escritos son las mismas dos
banderas que la 0.1.0 ya entiende.
