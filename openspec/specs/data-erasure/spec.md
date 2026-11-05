# data-erasure Specification

## Purpose
Permite a la persona borrar por completo, desde la app y sin desinstalarla, todos los datos de CoinDen que viven en su dispositivo, con una confirmación clara y sin que quede nada a medias.

## Requirements

### Requirement: Borrar Mis Datos desde Perfil
La pestaña Perfil DEBE (MUST) ofrecer la acción "Borrar mis datos". Al tocarla, la app DEBE explicar qué se borra, avisar que no se puede deshacer y pedir una confirmación explícita antes de borrar nada.

#### Scenario: Abrir el borrado
- **WHEN** Carlos toca "Borrar mis datos" en Perfil
- **THEN** ve qué se borrará (billeteras, movimientos, borradores, fotos, audios y preferencias), que no se puede deshacer, y las opciones Cancelar y Borrar todo

#### Scenario: Cancelar no borra nada
- **WHEN** Carlos toca "Borrar mis datos" y luego Cancelar
- **THEN** todos sus datos siguen igual y la app permanece en Perfil

### Requirement: Qué se Borra
Al confirmar, la app DEBE (MUST) eliminar del dispositivo la base de datos, su llave de cifrado, las fotos, PDF y audios de comprobantes, la cola de procesamiento y las preferencias de la persona. Tras borrar, no debe poder recuperarse ningún monto, nota, nombre de billetera ni comprobante desde el almacenamiento de la app.

#### Scenario: Borrado completo
- **WHEN** Carlos, con 3 billeteras, 40 movimientos y 5 fotos de comprobantes, confirma "Borrar todo"
- **THEN** ya no existen su base, su llave, sus fotos ni sus preferencias, y la app no conserva ningún texto de sus capturas

#### Scenario: Datos públicos no se tocan
- **WHEN** Ana confirma el borrado teniendo descargado un modelo de voz
- **THEN** el modelo y los paquetes de idioma siguen disponibles, porque no son datos suyos

### Requirement: Autenticación Antes de Borrar
Si el bloqueo de la app está activo, la app DEBE (MUST) pedir la autenticación del teléfono (huella o PIN) antes de ejecutar el borrado. Si la autenticación falla o se cancela, no se borra nada.

#### Scenario: Bloqueo activo y autenticación correcta
- **WHEN** Carlos tiene el bloqueo activo, confirma "Borrar todo" y se autentica con su huella
- **THEN** sus datos se borran

#### Scenario: Autenticación cancelada
- **WHEN** Carlos tiene el bloqueo activo, confirma "Borrar todo" y cancela la autenticación
- **THEN** no se borra nada y sus datos siguen intactos

#### Scenario: Sin bloqueo
- **WHEN** John no tiene el bloqueo activo y confirma "Borrar todo"
- **THEN** sus datos se borran sin pedir autenticación adicional

### Requirement: La App Queda como Recién Instalada
Terminado el borrado, la app DEBE (MUST) volver a la bienvenida, sin sesión ni datos de la persona, y permitir empezar de nuevo creando una base nueva con una llave nueva.

#### Scenario: Volver a empezar
- **WHEN** Facundo termina de borrar sus datos
- **THEN** ve la bienvenida y, al completar el onboarding, tiene una base vacía con una llave distinta a la anterior

### Requirement: El Borrado No Deja Datos a Medias
Si el borrado falla antes de terminar, la app DEBE (MUST) avisar con claridad, conservar la posibilidad de reintentar y no abrir la app con datos parciales. Reintentar completa el borrado.

#### Scenario: Falla a mitad del borrado
- **WHEN** el borrado de Carlos se interrumpe después de eliminar la base pero antes de eliminar las fotos
- **THEN** la app le avisa que el borrado no terminó, no le muestra datos parciales y al reintentar elimina lo que faltaba

### Requirement: El Borrado Solo Afecta Este Dispositivo
El borrado DEBE (MUST) afectar únicamente al dispositivo donde se confirma y no enviar ninguna orden ni dato a otros dispositivos de la persona ni a ningún servidor. La pantalla de confirmación lo dice.

#### Scenario: Aviso en la confirmación
- **WHEN** Carlos abre la confirmación de "Borrar mis datos"
- **THEN** lee que solo se borra este teléfono y que no se envía nada a ningún lado
