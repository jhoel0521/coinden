# Proposal

## Why

La pantalla de privacidad explica bien qué sale del teléfono, pero se cierra con un solo
botón de acuse ("Entendido, continuar") y el lector de Google queda apagado por omisión, no
por decisión. Quien pasa de largo nunca eligió nada: ni activarlo ni rechazarlo. Para un
producto cuyo segundo principio es la transparencia total, la diferencia entre "no lo
activó" y "decidió no activarlo" es justo la que importa.

Una auditoría en el emulador midió lo que ese lector envía cuando está activo: 20 eventos,
2.364 bytes en total, el mayor de 133 bytes, hacia `firebaselogging.googleapis.com`. Llevan
el nombre del paquete, la versión del SDK, el idioma del teléfono, un identificador por
instalación y tiempos. Se buscaron dentro los textos reales de las capturas de prueba
(nombres de comercios, montos, números de transacción y cabeceras de imagen) y no aparece
ninguno. Es poco y es técnico, pero sale del teléfono, así que la persona merece decidirlo
mirándolo y poder verificarlo por su cuenta.

## What Changes

- La pantalla de privacidad deja de cerrarse con un botón de acuse y pasa a pedir una
  **elección entre dos acciones**: seguir con el lector de Google o seguir sin él. No se
  avanza sin elegir una.
- La pantalla ofrece un **enlace a la página oficial de Google** con la lista de datos que
  recoge ML Kit, para que la persona verifique en la fuente y no solo en nuestra palabra.
- Ambos caminos dejan la app completa: sin el lector de Google las fotos se leen con el
  lector libre, que no envía nada. **No hay bloqueo ni cierre de la app.**
- La elección se guarda igual que hoy y se puede cambiar cuando se quiera en
  Perfil → Privacidad.
- Textos nuevos en español, inglés y portugués.

No es **BREAKING**: quien ya terminó el onboarding conserva su elección guardada, y quien ya
vio el aviso no vuelve a verlo.

## Capabilities

### New Capabilities

Ninguna. El lector de Google, su consentimiento y la pantalla de privacidad ya existen como
comportamiento especificado; lo que cambia es cómo se toma la decisión.

### Modified Capabilities

- `app-onboarding`: la pantalla de privacidad pide una elección explícita entre dos acciones
  en lugar de un acuse, y muestra el enlace de verificación. Cambian los escenarios de
  primera vez y de aceptación.
- `evidence-perception`: el aviso que precede al consentimiento debe ofrecer el enlace
  público donde la persona comprueba qué recibe Google, y el consentimiento pasa a ser una
  elección registrada, no la ausencia de una acción.

## Impact

- `composeApp/.../perception/PerceptionScreens.kt`: la pantalla de privacidad y sus acciones.
- `composeApp/.../perception/PerceptionPresenters.kt`: el estado deja de alcanzar con un
  interruptor y necesita distinguir "todavía no eligió" de "eligió que no".
- `composeApp/.../App.kt`: la navegación desde la pantalla de privacidad, hoy un único
  `onContinue`.
- `composeApp/src/commonMain/composeResources/values*/strings.xml`: textos en es, en y pt.
- Sin cambios en `core/domain`, sin esquema de base nuevo y sin dependencias nuevas.
- Fuera de alcance: bloquear la app si no se acepta (rompería el principio 2 del
  Manifiesto); apagar la telemetría de ML Kit por configuración (Google no documenta ningún
  opt-out, el control real es usar o no el lector); y las páginas de privacidad del sitio en
  es, en y pt, que ya se actualizaron con el enlace por ser documentación.
