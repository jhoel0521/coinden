# app-theme Specification

## Purpose
Define la identidad visual de CoinDen: los temas claro y oscuro "Cobre", las tipografías incluidas en la app, el contraste accesible y cómo se presentan los borradores (lo sugerido, la confianza y el medio de pago) para que todas las pantallas compartan un mismo estilo.

## Requirements

### Requirement: Tema Claro y Oscuro según el Sistema
La app DEBE (MUST) usar el tema "Cobre" cuando el teléfono está en modo claro y el tema "Cobre noche" cuando está en modo oscuro, cambiando al momento si el usuario cambia el modo del sistema, sin pedir ninguna configuración.

#### Scenario: Teléfono en modo claro
- **WHEN** el teléfono está en modo claro y se abre la app
- **THEN** la app usa el fondo crema y el color principal cobre del tema "Cobre"

#### Scenario: Teléfono en modo oscuro
- **WHEN** el teléfono está en modo oscuro y se abre la app
- **THEN** la app usa el fondo oscuro y el color principal cobre claro del tema "Cobre noche"

### Requirement: Contraste Accesible
En ambos temas, el texto principal sobre su fondo y el texto sobre cada color de contenedor DEBEN (MUST) cumplir un contraste de al menos 4.5:1 (WCAG AA), y los elementos interactivos DEBEN tener un área táctil de al menos 48 dp.

#### Scenario: Pares de color de los temas
- **WHEN** se mide el contraste de cada par texto/fondo de los temas "Cobre" y "Cobre noche" (superficie, principal, secundario, terciario, error, éxito y advertencia, y sus contenedores)
- **THEN** todos alcanzan al menos 4.5:1

### Requirement: Tipografías Incluidas
La app DEBE (MUST) mostrar los títulos con Bricolage Grotesque y el resto del texto con Manrope, incluidas dentro de la app sin descargarlas, y los montos DEBEN mostrarse con números tabulares para que las cifras queden alineadas.

#### Scenario: Sin conexión
- **WHEN** el teléfono no tiene conexión y se abre la app
- **THEN** los textos se muestran con las tipografías de CoinDen

### Requirement: Presentación de Borradores
La tarjeta de un borrador DEBE (MUST) mostrar su estado, el monto, la billetera y el medio de pago por separado, la categoría con su motivo y la confianza. Un campo sugerido DEBE distinguirse sin depender solo del color (borde punteado y la palabra "sugerida"), y la confianza DEBE mostrarse con color, ícono y texto (alta, media o baja). En la bandeja la tarjeta es compacta: nota, monto, billetera y, en "Para revisar", el motivo por el que hay que revisarla en lugar del estado; el detalle completo se ve en el formulario.

#### Scenario: Borrador por QR sugerido
- **WHEN** se muestra el borrador de "almuerzo 45 qr" con la billetera "BNB Bs" sugerida y confianza baja
- **THEN** la tarjeta muestra "BNB Bs" con borde punteado y la palabra "sugerida", el medio de pago QR aparte, la categoría comida con su motivo y la confianza baja con su ícono y su texto

#### Scenario: Borrador con alias
- **WHEN** se muestra el borrador de "almuerzo 45 bnb" con confianza media
- **THEN** la billetera "BNB Bs" se muestra sin borde punteado y la confianza media con su ícono y su texto

#### Scenario: Borrador capturado sin propuesta
- **WHEN** se muestra el borrador de "hola"
- **THEN** la tarjeta indica que quedó capturado sin propuesta y muestra la evidencia original, sin campos vacíos

#### Scenario: Tarjeta compacta en Para revisar
- **WHEN** la bandeja muestra "page 3 bs para trasmporte" con la billetera sugerida
- **THEN** la tarjeta ocupa dos líneas con la nota, 3,00 Bs, "Billetera" sugerida y el motivo "Billetera sugerida", y no dice "Listo para confirmar"

### Requirement: Logo de CoinDen en la App
La app DEBE (MUST) mostrar el logo de CoinDen (el búho con el cofre y las monedas) como imagen dentro de una placa oscura #1C140F de esquinas redondeadas, igual en los temas claro y oscuro. Debajo van "CoinDen" y "Tu guarida de monedas" como texto real con la tipografía de la app y traducido, nunca dentro de la imagen. No queda ningún rastro del logo anterior.

#### Scenario: Bienvenida en tema claro
- **WHEN** Carlos abre la bienvenida con el teléfono en modo claro
- **THEN** ve el logo sobre la placa oscura y debajo "CoinDen" y "Tu guarida de monedas" como texto

#### Scenario: Bienvenida en inglés
- **WHEN** Carlos elige English en la bienvenida
- **THEN** el texto debajo del logo dice "Your coin den" y el logo no cambia
