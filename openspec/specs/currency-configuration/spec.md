# currency-configuration Specification

## Purpose
Proporciona la gestión autónoma, local y desacoplada de la moneda base del usuario, las monedas ancla internacionales con proveedores públicos abiertos desde el Día 1 (Frankfurter y CoinGecko), catálogo de fuentes recomendadas por país (Whitelist comunitaria), estrategias de tipo de cambio con ciclo de vida de selectores persistidos (HTML/JSON) y auto-recuperación ante fallos, visualización dual (unificada y desglosada) y operatividad 100% offline.

## Requirements

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
El sistema DEBE permitir alternar entre una vista unificada (consolidada en moneda local con indicación visible del proveedor y fecha de la cotización) y una vista desglosada con la lista total de todas las monedas y su equivalencia estimada. La fuente de conversión DEBE (MUST) entregarse como datos estructurados (tipo de fuente, referencia, tasa, fecha y si es un respaldo degradado), nunca como un texto en un idioma.

#### Scenario: Visualización unificada con trazabilidad de proveedor
- **WHEN** el usuario visualiza su balance consolidado en modo unificado
- **THEN** el sistema muestra el valor total en la moneda local e indica visiblemente la fuente de conversión (ej. "Tasa Frankfurter al día", "CoinGecko hace 10m", "DolarBlueBolivia a 11.88" o "Tasa manual fijada por ti"), armada por la presentación en el idioma del usuario a partir de los datos estructurados de la fuente

#### Scenario: Fuente de conversión como datos estructurados
- **WHEN** Facundo consulta su consolidado con la estrategia de fuente verificada "ar-dolar-blue"
- **THEN** el sello de la fuente indica tipo VERIFICADA, referencia "ar-dolar-blue", la tasa aplicada y la fecha, sin ningún texto en español ni en otro idioma

#### Scenario: Fuente degradada por selector web caído
- **WHEN** Carlos consulta su consolidado y su selector web está en estado de reconfirmación
- **THEN** el sello indica tipo SELECTOR_WEB, la URL configurada como referencia, la última tasa conocida y la marca de respaldo degradado

#### Scenario: Visualización desglosada por moneda individual
- **WHEN** el usuario elige el modo de visualización desglosado
- **THEN** el sistema muestra la lista completa de saldos en sus divisas nativas individuales junto a su equivalencia calculada en moneda base

### Requirement: Representación de Monedas y Montos Neutral al Idioma
El dominio DEBE (MUST) identificar las monedas del catálogo solo por su código y su precisión decimal, sin nombres ni símbolos, y conservar nombre y símbolo únicamente en las monedas personalizadas tal como los escribió el usuario. Los montos se representan y se leen en una forma canónica neutral (punto decimal y código), y el formato regional queda a cargo de la presentación.

#### Scenario: Moneda del catálogo sin textos
- **WHEN** el sistema resuelve la moneda "BOB" del catálogo
- **THEN** obtiene el código BOB con 2 decimales, sin nombre ni símbolo asociados

#### Scenario: Moneda personalizada conserva lo que escribió el usuario
- **WHEN** Carlos registra la moneda personalizada "VALE" con símbolo "V", nombre "Vale comunitario" y 0 decimales
- **THEN** el sistema conserva exactamente ese símbolo y ese nombre para que la presentación los muestre

#### Scenario: Representación canónica de un monto
- **WHEN** el sistema representa 1188 Bs (118800 centavos de BOB) para pruebas o registros
- **THEN** la representación es "1188.00 BOB", sin símbolo ni separadores de miles

#### Scenario: Lectura canónica rechaza formatos regionales
- **WHEN** el sistema recibe el monto "35,50" o "1.234,56" para leerlo en forma canónica
- **THEN** rechaza la lectura con un error de formato, porque interpretar formatos regionales corresponde a la presentación o al parser de borradores

### Requirement: Aritmética Monetaria Segura
El dominio DEBE (MUST) operar montos solo mediante operaciones de dinero: sumar, restar y comparar montos de la misma moneda y en su unidad mínima exacta. Operar montos de monedas distintas sin una tasa de cambio es un error. Si un resultado no cabe en la representación del monto, la operación DEBE fallar con un error explícito en lugar de producir un valor incorrecto.

#### Scenario: Suma exacta en la misma moneda
- **WHEN** Carlos suma 0.10 Bs y 0.20 Bs
- **THEN** el resultado es exactamente 0.30 Bs

#### Scenario: Monedas distintas sin tasa de cambio
- **WHEN** el sistema intenta sumar 10.00 USD y 35.50 Bs directamente
- **THEN** la operación falla con un error de monedas distintas

#### Scenario: Desbordamiento explícito
- **WHEN** una suma o una conversión produce un monto mayor al máximo representable
- **THEN** la operación falla con un error de desbordamiento y no devuelve ningún monto

### Requirement: Conversión Exacta mediante Tasas de Cambio
El sistema DEBE (MUST) convertir montos entre monedas únicamente aplicando una tasa de cambio, definida como "una cantidad de una moneda equivale a una cantidad de otra". La conversión es exacta y redondea una sola vez, al final, a la unidad mínima de la moneda destino con redondeo half-even. Las tasas pueden invertirse y componerse sin redondeo, de modo que una conversión encadenada (por ejemplo BTC → USD → BOB) redondea una única vez.

#### Scenario: Conversión con la tasa del paralelo
- **WHEN** Carlos convierte 50.00 USD con la tasa "1.00 USD = 11.88 BOB"
- **THEN** el resultado es exactamente 594.00 Bs

#### Scenario: Conversión encadenada con un solo redondeo
- **WHEN** el sistema convierte 0.15 BTC a BOB componiendo "1 BTC = 62,200.00 USD" con "1.00 USD = 11.88 BOB"
- **THEN** el resultado es exactamente 110,840.40 Bs, sin redondear el paso intermedio en USD

#### Scenario: Redondeo half-even al centavo
- **WHEN** el sistema convierte 0.05 USD con la tasa "1.00 USD = 0.25 EUR", cuyo valor exacto es 0.0125 EUR
- **THEN** el resultado es 0.01 EUR, porque half-even redondea el empate al dígito par

#### Scenario: Tasa definida por una operación real
- **WHEN** Carlos vende 50.00 USD y recibe 594.00 Bs
- **THEN** la tasa de esa operación es "50.00 USD = 594.00 BOB" y, aplicada a 1.00 USD, da 11.88 Bs

#### Scenario: Tasa inversa
- **WHEN** el sistema invierte la tasa "1.00 USD = 11.20 BOB" y convierte 1,120.00 Bs
- **THEN** el resultado es exactamente 100.00 USD

#### Scenario: Cotización leída de una fuente web sin coma flotante
- **WHEN** el selector de Facundo extrae el texto "1420.50" de DolarApi
- **THEN** el sistema construye la tasa "1.00 USD = 1,420.50 ARS" a partir del texto decimal, sin pasar por números de coma flotante

#### Scenario: Consolidado igual a la suma de su desglose
- **WHEN** el usuario consulta su patrimonio consolidado
- **THEN** cada posición se convierte con un único redondeo y el total unificado es exactamente la suma de los montos convertidos que muestra la vista desglosada
