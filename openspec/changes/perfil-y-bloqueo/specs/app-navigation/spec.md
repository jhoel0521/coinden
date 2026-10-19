## ADDED Requirements

### Requirement: Barra de Pestañas con Capturar al Centro
Después del onboarding, la app DEBE (MUST) mostrar abajo una barra con cinco destinos, en este orden: Inicio, Revisar, Capturar, Movimientos y Perfil. Capturar va al centro y se destaca del resto. Cada destino muestra su ícono y su nombre, con un área táctil de al menos 48 dp, y el elegido se distingue con algo más que el color. Revisar muestra cuántos borradores esperan revisión. La barra no aparece en el formulario, la evidencia ni la cámara.

#### Scenario: La barra en Inicio
- **WHEN** Carlos termina el onboarding
- **THEN** ve abajo Inicio, Revisar, Capturar, Movimientos y Perfil, con Inicio elegido

#### Scenario: Pendientes en Revisar
- **WHEN** Carlos tiene 2 borradores pendientes
- **THEN** la pestaña Revisar muestra 2

#### Scenario: Formulario sin barra
- **WHEN** Carlos abre el formulario para escribir un gasto
- **THEN** el formulario ocupa toda la pantalla, sin la barra de pestañas

### Requirement: Capturar desde la Pestaña Central
Tocar Capturar DEBE (MUST) ofrecer Dictar, Foto y Escribir sin cambiar de pestaña. Dictar y Foto empiezan a capturar con un toque más y Escribir abre el formulario completo. Al cerrar el resultado o el formulario, la persona vuelve a la pestaña en la que estaba.

#### Scenario: Escribir desde Movimientos
- **WHEN** Carlos está en Movimientos, toca Capturar y luego Escribir
- **THEN** se abre el formulario y, al salir, vuelve a Movimientos

### Requirement: Movimientos Reúne Historial y Gastos del Mes
La pestaña Movimientos DEBE (MUST) mostrar el historial de movimientos confirmados y los gastos del mes, y permitir pasar de uno a otro sin salir de la pestaña.

#### Scenario: Pasar al resumen del mes
- **WHEN** Carlos abre Movimientos y elige "Gastos del mes"
- **THEN** ve el resumen del mes actual, y al elegir "Historial" vuelve a la lista de movimientos

### Requirement: Perfil de la Persona
La pestaña Perfil DEBE (MUST) reunir la configuración de la persona: Seguridad (el bloqueo con huella o PIN), Idioma, Billeteras y tasas, Privacidad, Modelos de voz y Acerca de (versión de la app y que no hay cuenta ni servidores). Cambiar el idioma desde Perfil cambia toda la app al momento.

#### Scenario: Lo que hay en Perfil
- **WHEN** Carlos abre Perfil
- **THEN** ve Seguridad, Idioma, Billeteras y tasas, Privacidad, Modelos de voz y Acerca de

#### Scenario: Cambiar el idioma
- **WHEN** Carlos elige English en Perfil
- **THEN** la barra de pestañas y Perfil se muestran en inglés
