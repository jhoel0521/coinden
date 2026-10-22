# app-navigation Specification

## Purpose
Organiza la app en cinco pestañas con Capturar al centro, de modo que Inicio solo muestre el patrimonio y cada cosa (revisar, movimientos, perfil) tenga su lugar, y reúne en Mi perfil la configuración de la persona.

## Requirements

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
La pestaña Perfil DEBE (MUST) reunir la configuración de la persona: Seguridad (el bloqueo con huella o PIN), Idioma, Billeteras y tasas, Categorías propias, Privacidad, Modelos de voz y Acerca de (versión de la app, que no hay cuenta ni servidores, un enlace al sitio del proyecto con su política de privacidad en el idioma de la app, las licencias de código abierto con su texto completo, y en letra pequeña el crédito de quien hace CoinDen, con su usuario de GitHub y un enlace al repositorio). El enlace abre el navegador; la app no cobra ni pide donaciones. Cambiar el idioma desde Perfil cambia toda la app al momento.

#### Scenario: Lo que hay en Perfil
- **WHEN** Carlos abre Perfil
- **THEN** ve Seguridad, Idioma, Billeteras y tasas, Categorías propias, Privacidad, Modelos de voz y Acerca de

#### Scenario: Cambiar el idioma
- **WHEN** Carlos elige English en Perfil
- **THEN** la barra de pestañas y Perfil se muestran en inglés

#### Scenario: Abrir la política de privacidad
- **WHEN** Carlos toca "Sitio y política de privacidad" en Acerca de con la app en español
- **THEN** el navegador abre la página del proyecto en español, desde donde llega a la política de privacidad

#### Scenario: Sitio en el idioma de la app
- **WHEN** Ana usa la app en portugués y toca el enlace de Acerca de
- **THEN** el navegador abre la página del proyecto en portugués

#### Scenario: Categorías propias desde Perfil
- **WHEN** Carlos toca "Categorías propias" en Perfil
- **THEN** ve sus categorías activas y archivadas y puede crear, renombrar, archivar o reactivar

#### Scenario: Licencias de código abierto
- **WHEN** Carlos toca "Licencias de código abierto" en Acerca de
- **THEN** ve cada librería que usa CoinDen con su licencia, y puede leer el texto completo de cada licencia sin conexión

#### Scenario: Crédito del autor
- **WHEN** Carlos abre Acerca de
- **THEN** ve en letra pequeña "Hecho por @jhoel0521", y al tocarlo se abre el repositorio de CoinDen en GitHub en el navegador
