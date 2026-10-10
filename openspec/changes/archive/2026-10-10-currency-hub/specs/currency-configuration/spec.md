# Spec Delta

## Purpose
Proporciona la gestión autónoma, local y desacoplada de la moneda base del usuario, las monedas ancla internacionales con proveedores públicos abiertos desde el Día 1 (Frankfurter y CoinGecko), catálogo de fuentes recomendadas por país (Whitelist comunitaria), estrategias de tipo de cambio con ciclo de vida de selectores persistidos (HTML/JSON) y auto-recuperación ante fallos, visualización dual (unificada y desglosada) y operatividad 100% offline.

## ADDED Requirements

### Requirement: Anchor Currencies and Default Trusted Public Providers
El sistema DEBE proveer monedas ancla globales (USD, EUR, CNY, BTC) y resolver sus cotizaciones de fábrica mediante proveedores públicos abiertos sin costo ni API keys (Frankfurter API para divisas del BCE y CoinGecko Public para Bitcoin).

#### Scenario: Cotización automática de divisas ancla globales
- **WHEN** el sistema solicita la cotización actualizada entre las monedas ancla USD, EUR y CNY
- **THEN** consulta el endpoint público de Frankfurter API y almacena las tasas oficiales sin requerir credenciales de usuario ni servidores propios

#### Scenario: Cotización automática de Bitcoin
- **WHEN** el sistema consulta la cotización de Bitcoin (BTC) frente a USD o EUR
- **THEN** obtiene el precio spot del endpoint público de CoinGecko y lo almacena localmente como cotización activa

### Requirement: Onboarding Express con Moneda Ancla
El sistema DEBE permitir a usuarios cuya moneda local coincide con una moneda ancla completar su configuración inicial de forma inmediata mediante una estrategia de tipo de cambio identidad (1.0).

#### Scenario: Onboarding inmediato para usuario de Estados Unidos (John)
- **WHEN** el usuario selecciona "USD" como su divisa local principal
- **THEN** el sistema activa la estrategia de Identidad con factor 1.0 y finaliza la configuración sin requerir datos cambiarios adicionales

### Requirement: Lista Blanca de Fuentes Verificadas por País
El sistema DEBE incluir un catálogo integrado de fuentes y servidores públicos verificados por región (ej. DolarApi en Argentina, DolarBlueBolivia en Bolivia) para que el usuario pueda vincular su cotización local en un solo toque.

#### Scenario: Selección de fuente de la lista blanca para Argentina (Facundo)
- **WHEN** el usuario selecciona moneda "ARS" y elige la fuente verificada "DolarApi" del catálogo de su país
- **THEN** el sistema configura automáticamente el endpoint y el selector correspondiente sin requerir ingreso manual de URLs

### Requirement: Ciclo de Vida de Selectores Personalizados y Auto-Recuperación
El sistema DEBE permitir al usuario confirmar un selector de extracción (JSON o HTML de una página web), persistir la regla para consultas diarias directas, y en caso de que la web cambie su estructura, realizar fallback a la última tasa guardada y alertar al usuario para re-escanear.

#### Scenario: Confirmación y persistencia de selector web (Carlos con DolarBlueBolivia)
- **WHEN** el usuario ingresa la URL de una web de cotización y confirma el valor detectado (ej. 11.88 BOB)
- **THEN** el sistema almacena la regla del selector y la utiliza directamente en las consultas siguientes sin re-analizar la estructura completa

#### Scenario: Fallo de selector por rediseño de la web y fallback seguro
- **WHEN** una página web externa cambia su maquetación y la regla persistida no encuentra la cotización
- **THEN** el sistema preserva la última tasa conocida localmente, marca el selector en estado de atención y notifica al usuario para re-escanear o ajustar la tasa manualmente

### Requirement: Moneda Local con Estrategia Manual o Fija
El sistema DEBE permitir al usuario definir una divisa local propia y vincularla a una estrategia de tipo de cambio fija o manual periódica referenciada al Dólar (USD).

#### Scenario: Configuración en Bolivia con tasa manual fija o periódica
- **WHEN** el usuario selecciona moneda "BOB" (Bs) y define una tasa manual de 11.20 BOB por 1 USD
- **THEN** el sistema registra el perfil monetario y liquida conversiones de activos en USD multiplicando por 11.20 BOB de forma determinista y offline

#### Scenario: Configuración en China con tasa fija (Wei)
- **WHEN** el usuario selecciona moneda "CNY" (¥) con tasa fija de 7.15 CNY por 1 USD
- **THEN** el sistema aplica el factor 7.15 para convertir saldos entre USD y CNY sin requerir conexión a internet

### Requirement: Registro de Monedas Personalizadas y Criptos Desconocidas
El sistema DEBE permitir al usuario dar de alta monedas y tokens personalizados especificando código, símbolo y precisión decimal de 0 a 8 posiciones.

#### Scenario: Registro de token comunitario sin centavos (VALE)
- **WHEN** el usuario crea una divisa "VALE" con símbolo "V", 0 decimales y cotización manual de 10.00 BOB
- **THEN** el sistema permite registrar transacciones en unidades enteras de "VALE" y calcula su valor consolidado en la moneda base

#### Scenario: Registro de token con alta precisión decimal (MOON)
- **WHEN** el usuario crea una divisa "MOON" con símbolo "M", 4 decimales y cotización manual de 0.05 USD
- **THEN** el sistema preserva la precisión de 4 decimales y calcula la equivalencia monetaria correspondiente

### Requirement: Visualización Dual del Patrimonio (Unificada vs Desglosada) con Transparencia de Fuente
El sistema DEBE permitir alternar entre una vista unificada (consolidada en moneda local con indicación visible del proveedor y fecha de la cotización) y una vista desglosada con la lista total de todas las monedas y su equivalencia estimada.

#### Scenario: Visualización unificada con trazabilidad de proveedor
- **WHEN** el usuario visualiza su balance consolidado en modo unificado
- **THEN** el sistema muestra el valor total en la moneda local e indica visiblemente la fuente de conversión (ej. "Tasa Frankfurter al día", "CoinGecko hace 10m", "DolarBlueBolivia a 11.88" o "Tasa manual fijada por ti")

#### Scenario: Visualización desglosada por moneda individual
- **WHEN** el usuario elige el modo de visualización desglosado
- **THEN** el sistema muestra la lista completa de saldos en sus divisas nativas individuales junto a su equivalencia calculada en moneda base
