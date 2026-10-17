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
Inicio DEBE (MUST) mostrar el patrimonio total en la moneda principal (sumando cada otra moneda con su tasa de referencia, y mostrando aparte, sin sumar, las que no tienen tasa), cada billetera activa con su saldo, cuántos borradores esperan revisión y la barra de captura con los botones Dictar, Foto y Escribir.

#### Scenario: Inicio de Carlos
- **WHEN** Carlos, con 100 Bs, 500 Bs y 289 USD a 10,20 y dos borradores pendientes, abre Inicio
- **THEN** ve un patrimonio de 3.547,80 Bs, sus tres billeteras con su saldo y "2 borradores por revisar"

#### Scenario: Moneda sin tasa aparte
- **WHEN** Carlos tiene 600 Bs en bolivianos y 50 BRL sin tasa de referencia
- **THEN** ve un patrimonio de 600,00 Bs y, aparte, 50,00 BRL sin convertir

#### Scenario: Capturar desde Inicio
- **WHEN** Carlos toca Escribir en Inicio
- **THEN** se abre el formulario completo y, al volver con el botón Atrás, regresa a Inicio

### Requirement: Privacidad: Qué Sale del Teléfono
Después de la bienvenida, la app DEBE (MUST) mostrar una pantalla de privacidad que explique qué se queda en el teléfono (todos los datos de la persona, cifrados), qué sale solo cuando la persona lo pide (consultas públicas y descargas opcionales, sin datos suyos) y el lector de Google como opción apagada, con lo que recibe y lo que nunca recibe. Se puede volver a abrir desde Inicio.

#### Scenario: Lo que explica
- **WHEN** Carlos lee la pantalla de privacidad
- **THEN** ve que sus gastos, fotos, audios y textos nunca salen del teléfono; que las cotizaciones que configure y las descargas de paquetes, plantillas y modelos no envían nada suyo; y que el lector de Google, si lo activa, solo recibe datos técnicos de uso

#### Scenario: Primera vez
- **WHEN** Carlos elige español en la bienvenida y continúa
- **THEN** ve la pantalla de privacidad con el lector de Google apagado y, al continuar, pasa a elegir sus monedas

#### Scenario: Aceptar el lector de Google
- **WHEN** Carlos activa el lector de Google en la pantalla de privacidad y continúa
- **THEN** la elección queda guardada y sus fotos se leen también con ese lector

#### Scenario: Persona que ya terminó el onboarding
- **WHEN** Carlos actualiza la app después de haber terminado el onboarding
- **THEN** al abrirla ve una vez la pantalla de privacidad antes de Inicio

#### Scenario: Revisar desde Inicio
- **WHEN** Carlos toca "Privacidad" en Inicio
- **THEN** ve la misma pantalla y puede cambiar su elección del lector de Google
