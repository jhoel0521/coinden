## ADDED Requirements

### Requirement: Patrimonio Parcial Explícito
Cuando Inicio no pueda convertir todos los saldos por faltar referencias, DEBE (MUST) identificar la cifra unificada como patrimonio parcial, mostrar aparte los importes no convertidos y explicar que no están incluidos. Con todas las referencias necesarias, DEBE mostrar patrimonio total. Esto no modifica los saldos ni exige consultar internet.

#### Scenario: Moneda sin referencia
- **WHEN** Carlos tiene 600 BOB y 50 BRL sin referencia
- **THEN** Inicio muestra 600 BOB como patrimonio parcial y 50 BRL aparte como no incluidos

#### Scenario: Referencia completa
- **WHEN** Carlos agrega la referencia manual de BRL y vuelve a Inicio
- **THEN** ve el patrimonio total con la equivalencia incluida sin modificar los movimientos

#### Scenario: Traducción del total parcial
- **WHEN** John o Facundo elige inglés, portugués o español con un saldo sin referencia
- **THEN** la etiqueta parcial y su explicación aparecen en el idioma elegido

### Requirement: Carga Recuperable de Inicio
Inicio DEBE (MUST) mostrar un estado de carga antes de obtener sus datos y un error con reintento si la lectura falla. No DEBE presentar valores iniciales como saldos reales. Si conserva valores de una carga anterior tras un fallo, DEBE aclarar que no están actualizados. Una respuesta anterior no DEBE reemplazar una carga más reciente.

#### Scenario: Primer inicio cargando
- **WHEN** Carlos abre Inicio y aún no se obtuvieron los saldos
- **THEN** ve que los datos están cargando y no un patrimonio vacío presentado como definitivo

#### Scenario: Fallo de lectura y recuperación
- **WHEN** falla la lectura de Inicio y Carlos toca Reintentar
- **THEN** permanece en Inicio, ve la carga y después sus datos cuando la lectura se recupera

#### Scenario: Datos anteriores durante un fallo
- **WHEN** falla una actualización después de haber mostrado saldos
- **THEN** los datos conservados se identifican como no actualizados y se ofrece reintento

#### Scenario: Cargas fuera de orden
- **WHEN** una carga vieja de Inicio termina después de una actualización más reciente
- **THEN** se conserva la información de la actualización más reciente
