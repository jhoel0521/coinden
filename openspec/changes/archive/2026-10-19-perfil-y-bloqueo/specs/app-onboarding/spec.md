## MODIFIED Requirements

### Requirement: Pantalla de Inicio
Inicio DEBE (MUST) mostrar el patrimonio total en la moneda principal (sumando cada otra moneda con su tasa de referencia, y mostrando aparte, sin sumar, las que no tienen tasa), cada billetera activa con su saldo y, si hay borradores pendientes, un aviso con cuántos esperan revisión que lleva a Revisar. Inicio no repite los accesos que ya están en la barra de pestañas: la captura, el historial, los gastos del mes, el idioma y la privacidad se abren desde la barra o desde Perfil.

#### Scenario: Inicio de Carlos
- **WHEN** Carlos, con 100 Bs, 500 Bs y 289 USD a 10,20 y dos borradores pendientes, abre Inicio
- **THEN** ve un patrimonio de 3.547,80 Bs, sus tres billeteras con su saldo y "Borradores por revisar: 2"

#### Scenario: Moneda sin tasa aparte
- **WHEN** Carlos tiene 600 Bs en bolivianos y 50 BRL sin tasa de referencia
- **THEN** ve un patrimonio de 600,00 Bs y, aparte, 50,00 BRL sin convertir

#### Scenario: Capturar desde Inicio
- **WHEN** Carlos toca Capturar y luego Escribir mientras está en Inicio
- **THEN** se abre el formulario completo y, al volver con el botón Atrás, regresa a Inicio

#### Scenario: Ir a revisar desde el aviso
- **WHEN** Carlos toca "Revisar" en el aviso de borradores por revisar de Inicio
- **THEN** pasa a la pestaña Revisar

#### Scenario: Inicio sin pendientes
- **WHEN** Carlos no tiene borradores pendientes
- **THEN** Inicio no muestra el aviso de revisión

### Requirement: Privacidad: Qué Sale del Teléfono
Después de la bienvenida, la app DEBE (MUST) mostrar una pantalla de privacidad que explique qué se queda en el teléfono (todos los datos de la persona, cifrados), qué sale solo cuando la persona lo pide (consultas públicas y descargas opcionales, sin datos suyos) y el lector de Google como opción apagada, con lo que recibe y lo que nunca recibe. La misma pantalla ofrece, apagado, el bloqueo con huella o PIN del teléfono. Se puede volver a abrir desde Perfil.

#### Scenario: Lo que explica
- **WHEN** Carlos lee la pantalla de privacidad
- **THEN** ve que sus gastos, fotos, audios y textos nunca salen del teléfono; que las cotizaciones que configure y las descargas de paquetes, plantillas y modelos no envían nada suyo; y que el lector de Google, si lo activa, solo recibe datos técnicos de uso

#### Scenario: Primera vez
- **WHEN** Carlos elige español en la bienvenida y continúa
- **THEN** ve la pantalla de privacidad con el lector de Google y el bloqueo apagados y, al continuar, pasa a elegir sus monedas

#### Scenario: Aceptar el lector de Google
- **WHEN** Carlos activa el lector de Google en la pantalla de privacidad y continúa
- **THEN** la elección queda guardada y sus fotos se leen también con ese lector

#### Scenario: Activar el bloqueo en el onboarding
- **WHEN** Carlos activa el bloqueo en la pantalla de privacidad y se autentica con su huella
- **THEN** el bloqueo queda activo al terminar el onboarding

#### Scenario: Persona que ya terminó el onboarding
- **WHEN** Carlos actualiza la app después de haber terminado el onboarding
- **THEN** al abrirla ve una vez la pantalla de privacidad antes de Inicio

#### Scenario: Revisar desde Inicio
- **WHEN** Carlos busca la privacidad estando en Inicio
- **THEN** la encuentra en la pestaña Perfil, porque Inicio ya no repite ese acceso

#### Scenario: Revisar desde Perfil
- **WHEN** Carlos toca "Privacidad" en Perfil
- **THEN** ve la misma pantalla y puede cambiar su elección del lector de Google
