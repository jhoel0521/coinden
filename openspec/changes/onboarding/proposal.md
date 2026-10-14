# Proposal: Primer Inicio (Onboarding)

## Why
Hoy la app arranca con billeteras de ejemplo inventadas ("Billetera" y "BNB Bs"): nadie puede usarla con su dinero real. El primer contacto debe dejar a la persona lista en un minuto, en su idioma, con su moneda, la tasa que de verdad usa y sus billeteras, sin pedirle nada que el sistema pueda deducir solo. En particular, los apodos para dictar ("bnb", "yape") no deben configurarse: salen del nombre de cada billetera.

## What Changes
- **Bienvenida multilingüe con elección de idioma**: la primera vez la app saluda en español, inglés y portugués y deja elegir el idioma (propone el del teléfono). Toda la interfaz pasa a tener sus textos en los tres idiomas. El idioma elegido se guarda y activa su paquete de idioma en el intérprete.
- **Las monedas que usas**: la moneda principal (sugerida por la región del teléfono, cambiable) y, si quiere, otras: frecuentes (USD, EUR y las de la región), búsqueda entre todas las monedas del mundo y un formulario para monedas propias o cripto. Con una sola moneda no se pide nada más.
- **Catálogo mundial de monedas sin datos propios**: las ~160 monedas oficiales, sus decimales, la moneda de cada país y sus nombres en el idioma de la app salen del sistema operativo mediante un puerto del dominio (Android hoy; iOS y Desktop con su propio adaptador). Se elimina la tabla de 18 países escrita en el dominio.
- **Tasas de referencia opcionales, solo para totales**: por cada moneda que no es la principal, la tasa a la que realmente se cambia (manual con fecha) o "La pongo después". Sirve únicamente para mostrar un total en una moneda; cada operación entre monedas guarda su propia tasa real. Una moneda sin tasa se muestra aparte, sin sumarla.
- **Billeteras**: crear una o más billeteras (efectivo, banco, digital, cripto, tarjeta), cada una en una de las monedas elegidas y con su saldo de hoy. El efectivo en cada moneda es una billetera aparte.
- **Apodos automáticos (sin configurar)**: el intérprete reconoce una billetera por las palabras de su nombre ("BNB Bs" → "bnb", "Banco Económico" → "economico", "Dólares colchón" → "colchon"), ignorando monedas, medios de pago y palabras genéricas ("bs", "qr", "banco", "cuenta"), sin usar una palabra que comparten dos billeteras y tolerando errores de tipeo.
- **Billetera por defecto elegida sola**: al terminar, el efectivo en la moneda local (o, si no hay, la primera billetera creada).
- **Pantalla de Inicio**: patrimonio total, billeteras con su saldo, borradores por revisar y el botón para capturar (lleva a la captura que ya existe).
- **Navegación** entre bienvenida, pasos, Inicio y captura, con el botón Atrás del sistema.
- **Sin billeteras de ejemplo**: se elimina la siembra de demostración. El onboarding aparece mientras no haya perfil ni billeteras; después, la app abre en Inicio.

### Non-Goals
- Editar o archivar billeteras, apodos manuales y medios de pago por billetera ("Administrar billeteras").
- Barra de navegación inferior, Bandeja, Resumen y Ajustes completos (solo el cambio de idioma queda accesible).
- Más idiomas que español, inglés y portugués; traducción del contenido que escribe el usuario.
- Cotizaciones por red y estrategias fija o por API en la pantalla (`rate-fetch-adapter`); el dominio ya las soporta.
- Agregar monedas o cambiar tasas después del onboarding (llega con Ajustes).
- Sugerir la tasa a partir de los cambios reales que la persona ya registró.

## Capabilities

### New Capabilities
- `app-onboarding`: primer inicio en tres pasos (bienvenida con idioma, moneda y tasa, billeteras), idioma de la app y pantalla de Inicio.

### Modified Capabilities
- `text-interpreter`: reconoce billeteras por las palabras de su nombre, sin apodos configurados; la moneda de la región ("$", "pesos") sale del catálogo y no de una tabla fija.
- `currency-configuration`: catálogo mundial de monedas por puerto; perfil con las monedas que se usan y una tasa de referencia opcional por moneda, solo para totales.
- `app-shell`: deja de sembrar billeteras de ejemplo; la captura se abre desde Inicio.

## Impact
- **`core/domain`**: deducción pura de apodos desde el nombre (con palabras genéricas por paquete de idioma) y su uso en el lector de frases; `CurrencyCatalogPort`; perfil con monedas y tasas de referencia por moneda; patrimonio con monedas sin tasa aparte.
- **Adaptadores**: catálogo de monedas con los datos del sistema (Android/JVM: `java.util.Currency` y `Locale`), con batería de contrato; perfil persistido con los campos nuevos (formato compatible).
- **`core/data-local`**: primera migración del esquema para guardar la configuración de la app (idioma).
- **`composeApp`**: navegación, pantallas de bienvenida, moneda y tasa, billeteras e Inicio; textos en `values` (es), `values-en` y `values-pt`; se elimina `DemoSeed`.
- **Dependencias**: la librería de navegación de JetBrains para Compose Multiplatform.
