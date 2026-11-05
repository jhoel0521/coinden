# app-onboarding (delta)

## MODIFIED Requirements

### Requirement: Privacidad: Qué Sale del Teléfono
Después de la bienvenida, la app DEBE (MUST) mostrar una pantalla de privacidad que explique
qué se queda en el teléfono (todos los datos de la persona, cifrados), qué sale solo cuando
la persona lo pide (consultas públicas y descargas opcionales, sin datos suyos) y el lector
de Google, con lo que recibe y lo que nunca recibe, junto a un enlace público donde la
persona puede verificarlo. Mientras la persona no haya elegido, la pantalla DEBE (MUST)
pedir una elección explícita sobre el lector de Google —seguir con él o seguir sin él— y no
DEBE (MUST NOT) dejar avanzar sin que elija una de las dos. Las dos elecciones dejan la app
completa: sin el lector de Google las fotos se leen con el lector libre. La misma pantalla
ofrece, apagado, el bloqueo con huella o PIN del teléfono. Se puede volver a abrir desde
Perfil, donde la elección ya tomada se cambia cuando se quiera.

#### Scenario: Lo que explica
- **WHEN** Carlos lee la pantalla de privacidad
- **THEN** ve que sus gastos, fotos, audios y textos nunca salen del teléfono; que las cotizaciones que configure y las descargas de paquetes, plantillas y modelos no envían nada suyo; y que el lector de Google, si lo activa, solo recibe datos técnicos de uso

#### Scenario: Puede comprobarlo por su cuenta
- **WHEN** Carlos quiere saber exactamente qué recibe Google y no conformarse con el resumen
- **THEN** la pantalla le ofrece un enlace a la página pública de Google con la lista de datos que recoge ese lector

#### Scenario: Primera vez
- **WHEN** Carlos elige español en la bienvenida y continúa
- **THEN** ve la pantalla de privacidad con el bloqueo apagado y las dos elecciones del lector de Google a la vista, sin ninguna marcada

#### Scenario: No se avanza sin elegir
- **WHEN** Carlos intenta pasar de la pantalla de privacidad sin elegir sobre el lector de Google
- **THEN** la app no avanza y la pantalla le sigue pidiendo que elija

#### Scenario: Aceptar el lector de Google
- **WHEN** Carlos elige seguir con el lector de Google
- **THEN** la elección queda guardada, pasa a elegir sus monedas y sus fotos se leen también con ese lector

#### Scenario: Seguir sin el lector de Google
- **WHEN** Carlos elige seguir sin el lector de Google
- **THEN** la elección queda guardada, pasa a elegir sus monedas, sus fotos se leen solo con el lector libre y el resto de la app funciona igual

#### Scenario: Activar el bloqueo en el onboarding
- **WHEN** Carlos activa el bloqueo en la pantalla de privacidad y se autentica con su huella
- **THEN** el bloqueo queda activo al terminar el onboarding

#### Scenario: Persona que ya terminó el onboarding
- **WHEN** Carlos actualiza la app después de haber terminado el onboarding
- **THEN** al abrirla ve una vez la pantalla de privacidad antes de Inicio, con su elección anterior ya aplicada, y elige de nuevo para continuar

#### Scenario: Revisar desde Inicio
- **WHEN** Carlos busca la privacidad estando en Inicio
- **THEN** la encuentra en la pestaña Perfil, porque Inicio ya no repite ese acceso

#### Scenario: Revisar desde Perfil
- **WHEN** Carlos toca "Privacidad" en Perfil
- **THEN** ve la misma pantalla, con el lector de Google como interruptor en su elección actual, y puede cambiarla
