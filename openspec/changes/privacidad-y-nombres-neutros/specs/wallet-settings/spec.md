# Spec Delta

## ADDED Requirements

### Requirement: Nombres Neutros para los Tipos de Billetera
La app DEBE (MUST) mostrar los tipos de billetera con nombres de registro y no de producto financiero: Efectivo, Cuenta, Digital, Personalizada y Tarjeta (en español), con su equivalente en inglés y portugués. Cambiar los nombres no cambia el comportamiento: Tarjeta sigue admitiendo saldo inicial negativo, y la moneda y el saldo siguen siendo los mismos.

#### Scenario: Tipos al crear una fuente
- **WHEN** Carlos abre el formulario para agregar una fuente
- **THEN** ve los tipos Efectivo, Cuenta, Digital, Personalizada y Tarjeta, y no ve "Banco", "Cripto" ni "Tarjeta de crédito"

#### Scenario: Una billetera existente conserva su tipo
- **WHEN** Carlos actualiza la app y tenía una billetera de tipo banco
- **THEN** su billetera aparece como Cuenta con el mismo saldo y los mismos movimientos

#### Scenario: Tarjeta admite deuda inicial
- **WHEN** Facundo agrega una tarjeta con saldo inicial -25.00 ARS
- **THEN** se conserva la deuda inicial exacta, igual que antes del cambio de nombre

#### Scenario: Nombres en los tres idiomas
- **WHEN** Ana cambia la app a portugués y abre el formulario de fuente
- **THEN** los tipos aparecen como Dinheiro, Conta, Digital, Personalizada y Cartão

### Requirement: Movimiento entre Billeteras con Nombre Neutro
El movimiento que lleva dinero de una billetera a otra de la persona DEBE (MUST) mostrarse como "Traspaso" (en inglés "Move", en portugués "Repasse") y no como "Transferencia", sin cambiar sus montos, su tasa ni su efecto en los saldos.

#### Scenario: Traspaso entre monedas
- **WHEN** Carlos pasa 50.00 USD de "Meru" a "BNB Bs" y recibe 594.00 Bs
- **THEN** el historial lo muestra como Traspaso, con los mismos 50.00 USD y 594.00 Bs, y los saldos cambian como antes

### Requirement: Vocabulario Visible sin Términos Financieros
La app DEBE (MUST) mostrar, en pantalla, en el sitio y en la ficha de Play, el vocabulario de una libreta de gastos: "fuente" en lugar de "billetera" ("source", "fonte"), "total anotado" o "total" en lugar de "patrimonio" y "saldo", "equivalencia" en lugar de "tasa" y "cotización", y "gasto" en lugar de "pago". Los ejemplos NO DEBEN (MUST NOT) nombrar bancos ni billeteras reales.

#### Scenario: Inicio con total anotado
- **WHEN** John abre Inicio con una fuente en efectivo de 463.00 USD
- **THEN** ve "Total anotado" y la lista "Fuentes", y no ve "Patrimonio", "Saldo" ni "Billeteras"

#### Scenario: Paso de fuentes del onboarding
- **WHEN** Carlos llega al paso 3 de la bienvenida
- **THEN** ve "¿De dónde salen tus gastos?", los tipos Efectivo, Cuenta, Digital, Personalizada y Tarjeta, y la pista de nombre "Mi efectivo, Mi cuenta…" sin "BNB" ni "Yape"

#### Scenario: Formulario de movimiento
- **WHEN** Carlos abre "Escribir movimiento"
- **THEN** el selector se llama "Fuente", el campo de texto de contexto pregunta "¿De qué fue este gasto?" y el medio "Transferencia o QR" sigue disponible

#### Scenario: Perfil
- **WHEN** Carlos abre Perfil
- **THEN** la sección dice "Fuentes y equivalencias" y no "Billeteras y tasas"

#### Scenario: Equivalencias en lugar de tasas
- **WHEN** Carlos tiene BOB y USD y abre el paso de monedas
- **THEN** el texto que explica el total habla de "equivalencia", y cada traspaso guarda la equivalencia exacta de ese momento, igual que antes

#### Scenario: Vocabulario en portugués y en inglés
- **WHEN** Ana cambia la app a portugués
- **THEN** ve "Fontes", "Total anotado" y "Equivalência"; en inglés ve "Sources", "Recorded total" y "Equivalence"

#### Scenario: Ninguna palabra retirada vuelve
- **WHEN** corre la prueba de vocabulario sobre los textos de la app en español, inglés y portugués
- **THEN** ningún texto visible contiene billetera, patrimonio, saldo, tasa, cotización ni pago (ni sus equivalentes en inglés y portugués), salvo las excepciones listadas en el diseño

### Requirement: El Vocabulario Solo Cambia el Texto
Cambiar el vocabulario visible NO DEBE (MUST NOT) cambiar el comportamiento: los montos, las equivalencias, los totales y los traspasos se calculan igual, los nombres que escribe la persona no se modifican, y el medio de pago "Transferencia o QR" se mantiene.

#### Scenario: El comportamiento no cambia
- **WHEN** Facundo agrega una fuente de tipo Tarjeta con total inicial -25.00 ARS y confirma un gasto de 10.00 ARS
- **THEN** el total de esa fuente es -35.00 ARS, igual que antes del cambio de vocabulario

#### Scenario: Los nombres de la persona no se tocan
- **WHEN** Carlos tiene una fuente que él llamó "BNB Bs"
- **THEN** la app la sigue mostrando como "BNB Bs", y el alias "bnb" sigue funcionando al dictar
