# Spec Delta

## MODIFIED Requirements

### Requirement: Billeteras Iniciales sin Configurar Apodos
El tercer paso DEBE (MUST) permitir crear una o más billeteras, cada una con nombre, tipo (efectivo, cuenta, digital, personalizada o tarjeta), una de las monedas elegidas y saldo de hoy, y no puede terminar sin al menos una. No pide apodos ni billetera por defecto: al terminar, la billetera por defecto es el efectivo en la moneda local o, si no hay, la primera billetera creada.

#### Scenario: Carlos crea sus billeteras
- **WHEN** Carlos crea "Billetera" (efectivo, 100 Bs), "BNB Bs" (cuenta, 500 Bs) y "Dólares colchón" (efectivo, 289 USD) y termina
- **THEN** existen las tres billeteras con esos saldos y la billetera por defecto es "Billetera"

#### Scenario: Sin billeteras no se termina
- **WHEN** la persona intenta terminar sin crear ninguna billetera
- **THEN** la app no avanza y le pide crear al menos una

#### Scenario: Sin efectivo en la moneda local
- **WHEN** John solo crea "Chase" (cuenta, USD) y termina
- **THEN** la billetera por defecto es "Chase"

#### Scenario: Tipos con nombres neutros
- **WHEN** Carlos llega al tercer paso
- **THEN** los tipos que se ofrecen son Efectivo, Cuenta, Digital, Personalizada y Tarjeta

### Requirement: Privacidad: Qué Sale del Teléfono
Después de la bienvenida, la app DEBE (MUST) mostrar una pantalla de privacidad que explique qué se queda en el teléfono (todos los datos de la persona, cifrados), qué sale solo cuando la persona lo pide (consultas públicas y descargas opcionales, sin datos suyos) y el lector de Google como opción apagada, con lo que recibe y lo que nunca recibe. También DEBE decir que la persona puede borrar todos sus datos desde Perfil en cualquier momento. La misma pantalla ofrece, apagado, el bloqueo con huella o PIN del teléfono. Se puede volver a abrir desde Perfil.

#### Scenario: Lo que explica
- **WHEN** Carlos lee la pantalla de privacidad
- **THEN** ve que sus gastos, fotos, audios y textos nunca salen del teléfono; que las cotizaciones que configure y las descargas de paquetes, plantillas y modelos no envían nada suyo; y que el lector de Google, si lo activa, solo recibe datos técnicos de uso

#### Scenario: Cómo borrar todo
- **WHEN** Carlos lee la pantalla de privacidad
- **THEN** ve que puede borrar todos sus datos desde Perfil y que esa acción solo afecta a este teléfono

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
