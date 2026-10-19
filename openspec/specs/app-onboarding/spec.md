# app-onboarding Specification

## Purpose
Lleva a la persona, la primera vez que abre CoinDen, de la bienvenida a su Inicio en tres pasos (idioma, monedas que usa, billeteras), sin cuenta ni configuraciones que el sistema pueda deducir, y define el idioma de la app y la pantalla de Inicio.

## Requirements

### Requirement: Bienvenida Multilingüe con Elección de Idioma
La primera vez que se abre la app DEBE (MUST) mostrarse una bienvenida con el saludo en el idioma elegido, que permita elegir español, inglés o portugués y proponga el del teléfono si es uno de esos tres (si no, inglés). Los nombres de los idiomas se muestran en su propia lengua. Debe indicar que no hace falta cuenta ni registro.

#### Scenario: Teléfono en portugués
- **WHEN** Ana abre la app por primera vez con el teléfono en portugués
- **THEN** la bienvenida propone portugués y muestra su saludo en portugués

#### Scenario: Teléfono en un idioma no soportado
- **WHEN** Wei abre la app por primera vez con el teléfono en chino
- **THEN** la bienvenida propone inglés

#### Scenario: Cambiar el saludo sin perder la pantalla
- **WHEN** John elige inglés o portugués desde una pantalla de la app
- **THEN** los textos siguen su selección sin reiniciar la navegación y la bienvenida no conserva un título fijo en español

### Requirement: Idioma de la App
Todos los textos de la interfaz DEBEN (MUST) existir en español, inglés y portugués y mostrarse en el idioma elegido, que se guarda en el dispositivo y se mantiene al reabrir. El idioma elegido activa su paquete de idioma en el intérprete, junto con el inglés base.

#### Scenario: Elegir inglés
- **WHEN** John elige inglés en la bienvenida
- **THEN** los pasos siguientes, Inicio y la captura se muestran en inglés, y al reabrir la app sigue en inglés

#### Scenario: Elegir portugués activa su paquete
- **WHEN** Ana elige portugués y luego captura "paguei 30 reais no pix"
- **THEN** el intérprete entiende el gasto en portugués

### Requirement: Monedas que Usa la Persona
El segundo paso DEBE (MUST) pedir la moneda principal, sugerida según la región del teléfono y cambiable por cualquier moneda del catálogo mundial, y permitir marcar otras monedas que la persona usa: frecuentes (dólar, euro y las de su región), buscadas por nombre o código, o propias (código, nombre, símbolo y decimales). Con una sola moneda el paso termina sin pedir nada más.

#### Scenario: Solo bolivianos
- **WHEN** Carlos, con el teléfono en la región Bolivia, deja "Boliviano" como moneda principal y no marca otras
- **THEN** el paso termina sin pedir tasas y su perfil queda solo en bolivianos

#### Scenario: Buscar una moneda
- **WHEN** Wei busca "yuan" o "CNY"
- **THEN** encuentra el yuan con su nombre en el idioma de la app

#### Scenario: Moneda propia
- **WHEN** Carlos agrega la moneda propia "VALE", "Vale del súper", símbolo "vl", 0 decimales
- **THEN** "VALE" queda entre sus monedas y puede usarla en una billetera

### Requirement: Tasas de Referencia Opcionales
Por cada moneda marcada que no es la principal, el paso DEBE (MUST) ofrecer escribir la tasa a la que la persona realmente la cambia (guardada como tasa manual con su fecha) o elegir "La pongo después". La pantalla DEBE aclarar que la tasa solo sirve para mostrar totales y que cada cambio que se registre usa su propia tasa real.

#### Scenario: Carlos con su tasa del paralelo
- **WHEN** Carlos marca dólares y escribe que se los cambian a 10,20
- **THEN** su perfil tiene una tasa de referencia manual de 1 USD = 10.20 BOB con la fecha de hoy

#### Scenario: Tasa para después
- **WHEN** Carlos marca reales y elige "La pongo después"
- **THEN** su perfil incluye reales sin tasa de referencia

### Requirement: Billeteras Iniciales sin Configurar Apodos
El tercer paso DEBE (MUST) permitir crear una o más billeteras, cada una con nombre, tipo (efectivo, banco, digital, cripto o tarjeta), una de las monedas elegidas y saldo de hoy, y no puede terminar sin al menos una. No pide apodos ni billetera por defecto: al terminar, la billetera por defecto es el efectivo en la moneda local o, si no hay, la primera billetera creada.

#### Scenario: Carlos crea sus billeteras
- **WHEN** Carlos crea "Billetera" (efectivo, 100 Bs), "BNB Bs" (banco, 500 Bs) y "Dólares colchón" (efectivo, 289 USD) y termina
- **THEN** existen las tres billeteras con esos saldos y la billetera por defecto es "Billetera"

#### Scenario: Sin billeteras no se termina
- **WHEN** la persona intenta terminar sin crear ninguna billetera
- **THEN** la app no avanza y le pide crear al menos una

#### Scenario: Sin efectivo en la moneda local
- **WHEN** John solo crea "Chase" (banco, USD) y termina
- **THEN** la billetera por defecto es "Chase"

### Requirement: Primer Inicio y Siguientes
El onboarding DEBE (MUST) mostrarse mientras la persona no tenga perfil de moneda y al menos una billetera; una vez terminado, la app DEBE abrir directamente en Inicio. La app no crea billeteras de ejemplo.

#### Scenario: Reabrir después del onboarding
- **WHEN** Carlos terminó el onboarding, cierra la app y la vuelve a abrir
- **THEN** la app abre en Inicio con sus billeteras

#### Scenario: Primera vez sin datos
- **WHEN** la app se abre por primera vez
- **THEN** muestra la bienvenida y no existe ninguna billetera

### Requirement: Pantalla de Inicio
Inicio DEBE (MUST) mostrar el patrimonio total en la moneda principal (sumando cada otra moneda con su tasa de referencia, y mostrando aparte, sin sumar, las que no tienen tasa), cada billetera activa con su saldo y, si hay borradores pendientes, un aviso con cuántos esperan revisión que lleva a Revisar. Inicio no repite los accesos que ya están en la barra de pestañas: la captura, el historial, los gastos del mes, el idioma y la privacidad se abren desde la barra o desde Perfil.

#### Scenario: Inicio de Carlos
- **WHEN** Carlos, con 100 Bs, 500 Bs y 289 USD a 10,20 y dos borradores pendientes, abre Inicio
- **THEN** ve un patrimonio de 3.547,80 Bs, sus tres billeteras con su saldo y "Borradores por revisar: 2"

#### Scenario: Moneda sin tasa aparte
- **WHEN** Carlos tiene 600 Bs en bolivianos y 50 BRL sin tasa de referencia
- **THEN** ve un patrimonio de 600,00 Bs y, aparte, 50,00 BRL sin convertir

#### Scenario: Capturar desde Inicio
- **WHEN** Carlos toca Capturar y luego Escribir mientras está en Inicio
- **THEN** se abre el formulario completo y, al volver con el botón Atrás, regresa a Inicio

#### Scenario: Ir a revisar desde el aviso
- **WHEN** Carlos toca "Revisar" en el aviso de borradores por revisar de Inicio
- **THEN** pasa a la pestaña Revisar

#### Scenario: Inicio sin pendientes
- **WHEN** Carlos no tiene borradores pendientes
- **THEN** Inicio no muestra el aviso de revisión

### Requirement: Privacidad: Qué Sale del Teléfono
Después de la bienvenida, la app DEBE (MUST) mostrar una pantalla de privacidad que explique qué se queda en el teléfono (todos los datos de la persona, cifrados), qué sale solo cuando la persona lo pide (consultas públicas y descargas opcionales, sin datos suyos) y el lector de Google como opción apagada, con lo que recibe y lo que nunca recibe. La misma pantalla ofrece, apagado, el bloqueo con huella o PIN del teléfono. Se puede volver a abrir desde Perfil.

#### Scenario: Lo que explica
- **WHEN** Carlos lee la pantalla de privacidad
- **THEN** ve que sus gastos, fotos, audios y textos nunca salen del teléfono; que las cotizaciones que configure y las descargas de paquetes, plantillas y modelos no envían nada suyo; y que el lector de Google, si lo activa, solo recibe datos técnicos de uso

#### Scenario: Primera vez
- **WHEN** Carlos elige español en la bienvenida y continúa
- **THEN** ve la pantalla de privacidad con el lector de Google y el bloqueo apagados y, al continuar, pasa a elegir sus monedas

#### Scenario: Aceptar el lector de Google
- **WHEN** Carlos activa el lector de Google en la pantalla de privacidad y continúa
- **THEN** la elección queda guardada y sus fotos se leen también con ese lector

#### Scenario: Activar el bloqueo en el onboarding
- **WHEN** Carlos activa el bloqueo en la pantalla de privacidad y se autentica con su huella
- **THEN** el bloqueo queda activo al terminar el onboarding

#### Scenario: Persona que ya terminó el onboarding
- **WHEN** Carlos actualiza la app después de haber terminado el onboarding
- **THEN** al abrirla ve una vez la pantalla de privacidad antes de Inicio

#### Scenario: Revisar desde Inicio
- **WHEN** Carlos busca la privacidad estando en Inicio
- **THEN** la encuentra en la pestaña Perfil, porque Inicio ya no repite ese acceso

#### Scenario: Revisar desde Perfil
- **WHEN** Carlos toca "Privacidad" en Perfil
- **THEN** ve la misma pantalla y puede cambiar su elección del lector de Google

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
