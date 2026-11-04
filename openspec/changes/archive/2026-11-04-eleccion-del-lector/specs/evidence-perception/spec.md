# evidence-perception (delta)

## MODIFIED Requirements

### Requirement: Lector de Google Solo con Consentimiento
El lector de Google DEBE (MUST) estar apagado hasta que la persona elija usarlo, tras un
aviso que diga qué recibe Google (modelo y sistema del teléfono, versión de la app, un
identificador de la instalación, tiempos, tamaños, eventos y errores) y qué nunca recibe
(imágenes, textos, montos ni datos del usuario). El aviso DEBE (MUST) ofrecer un enlace
público donde la persona compruebe esa lista en la fuente y no solo en el resumen de la app.
La elección DEBE (MUST) quedar registrada como tal: no alcanza con que la persona haya
pasado de largo. La persona puede apagarlo en cualquier momento y, desde entonces, no se
vuelve a iniciar.

#### Scenario: Aceptar el lector de Google
- **WHEN** Carlos elige usar el lector de Google en la pantalla de privacidad
- **THEN** sus fotos se leen con ambos lectores y la elección se recuerda al reabrir la app

#### Scenario: Elegir seguir sin él
- **WHEN** Carlos elige seguir sin el lector de Google
- **THEN** sus fotos se leen solo con el lector libre, el lector de Google nunca se inicia y la elección se recuerda al reabrir la app

#### Scenario: Comprobar qué recibe Google
- **WHEN** Carlos abre el enlace que ofrece el aviso
- **THEN** llega a la página pública de Google que lista los datos que recoge ese lector

#### Scenario: Apagarlo después
- **WHEN** Carlos apaga el lector de Google desde la pantalla de privacidad
- **THEN** las fotos siguientes se leen solo con el lector libre
