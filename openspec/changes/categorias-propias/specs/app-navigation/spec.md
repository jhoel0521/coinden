## MODIFIED Requirements

### Requirement: Perfil de la Persona
La pestaña Perfil DEBE (MUST) reunir la configuración de la persona: Seguridad (el bloqueo con huella o PIN), Idioma, Billeteras y tasas, Categorías propias, Privacidad, Modelos de voz y Acerca de (versión de la app, que no hay cuenta ni servidores y un enlace al sitio del proyecto con su política de privacidad, en el idioma de la app). El enlace abre el navegador; la app no cobra ni pide donaciones. Cambiar el idioma desde Perfil cambia toda la app al momento.

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
