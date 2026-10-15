# Spec Delta

## ADDED Requirements

### Requirement: Privacidad: Qué Sale del Teléfono
Después de la bienvenida, la app DEBE (MUST) mostrar una pantalla de privacidad que explique qué se queda en el teléfono (todos los datos de la persona, cifrados), qué sale solo cuando la persona lo pide (consultas públicas y descargas opcionales, sin datos suyos) y el lector de Google como opción apagada, con lo que recibe y lo que nunca recibe. Se puede volver a abrir desde Inicio.

#### Scenario: Lo que explica
- **WHEN** Carlos lee la pantalla de privacidad
- **THEN** ve que sus gastos, fotos, audios y textos nunca salen del teléfono; que las cotizaciones que configure y las descargas de paquetes, plantillas y modelos no envían nada suyo; y que el lector de Google, si lo activa, solo recibe datos técnicos de uso

#### Scenario: Primera vez
- **WHEN** Carlos elige español en la bienvenida y continúa
- **THEN** ve la pantalla de privacidad con el lector de Google apagado y, al continuar, pasa a elegir sus monedas

#### Scenario: Aceptar el lector de Google
- **WHEN** Carlos activa el lector de Google en la pantalla de privacidad y continúa
- **THEN** la elección queda guardada y sus fotos se leen también con ese lector

#### Scenario: Persona que ya terminó el onboarding
- **WHEN** Carlos actualiza la app después de haber terminado el onboarding
- **THEN** al abrirla ve una vez la pantalla de privacidad antes de Inicio

#### Scenario: Revisar desde Inicio
- **WHEN** Carlos toca "Privacidad" en Inicio
- **THEN** ve la misma pantalla y puede cambiar su elección del lector de Google
