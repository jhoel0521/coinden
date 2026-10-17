# Spec Delta

## Purpose

Asegura que todas las pantallas de CoinDen se vean y se comporten igual: cada control es un componente de CoinDen que fija su estética y se acomoda según tenga hermanos, padre o hijos, de modo que ningún grupo quede desparejo ni con opciones sueltas.

## ADDED Requirements

### Requirement: Solo Componentes de CoinDen en las Pantallas
Las pantallas NO DEBEN (MUST NOT) usar directamente botones, chips, campos de texto, superficies, tarjetas, interruptores ni diálogos de Material. Deben usar los componentes de CoinDen, que fijan tamaño, forma, color, densidad y área táctil de al menos 48 dp en los temas claro y oscuro.

#### Scenario: Un control suelto
- **WHEN** una pantalla usa un botón de Material en lugar de un componente de CoinDen
- **THEN** la verificación automática del proyecto falla e indica el archivo

#### Scenario: Mismo aspecto en todas partes
- **WHEN** Carlos ve la acción principal de Inicio, del formulario y de los ajustes
- **THEN** tienen la misma altura, forma y estilo

### Requirement: Grupo de Acciones según sus Hermanos
Un grupo de acciones DEBE (MUST) acomodarse según cuántas tenga. Una sola ocupa todo el ancho. Dos van lado a lado y del mismo ancho. Tres o más se apilan a todo el ancho, con la principal al final. Hay como mucho una acción principal por grupo, y una acción que destruye o descarta se distingue sin depender solo del color.

#### Scenario: Dos acciones
- **WHEN** el formulario de revisión muestra "Descartar" y "Guardar sin confirmar"
- **THEN** quedan en una fila, cada una con la mitad del ancho

#### Scenario: Tres acciones
- **WHEN** un cambio pendiente muestra "Son distintos", "Descartar cambios" y "Confirmar"
- **THEN** se apilan a todo el ancho y "Confirmar" queda al final como principal

### Requirement: Elección Única según la Cantidad de Opciones
Un grupo de elección única DEBE (MUST) elegir su forma según sus opciones: hasta 4 que caben en una fila, segmentados del mismo ancho; una lista abierta o que no cabe, una fila deslizable con la elegida visible; más de 8 fijas, las más usadas y "Más…" con una cuadrícula de columnas fijas. Ninguna forma deja una opción sola en una fila incompleta.

#### Scenario: Tipo de movimiento
- **WHEN** Carlos abre el formulario
- **THEN** "Gasto" e "Ingreso" se muestran como dos segmentados del mismo ancho en una fila

#### Scenario: Cinco billeteras
- **WHEN** Carlos tiene "Billetera", "BNB Bs", "Dolares", "Reais" y "Visa"
- **THEN** se muestran en una sola fila deslizable con la elegida visible, sin una billetera sola en otra fila

#### Scenario: Categorías
- **WHEN** Carlos abre el formulario de un gasto y suele usar comida, transporte, supermercado y salud
- **THEN** ve esas 4 y "Más…"; al tocar "Más…" se abren todas en una cuadrícula de 3 columnas y la elegida queda marcada

#### Scenario: Letra grande
- **WHEN** Carlos usa letra grande y los 4 medios de pago ya no caben en una fila
- **THEN** el grupo pasa a una fila deslizable en lugar de cortarse en dos filas

### Requirement: Cuadrícula sin Opciones Sueltas
Cuando las opciones se muestran en varias filas (como "Más…" o los tipos de billetera), el sistema DEBE (MUST) usar columnas fijas del mismo ancho, de modo que cada opción tenga el mismo tamaño y la última fila quede alineada con las demás.

#### Scenario: Tipos de billetera
- **WHEN** Carlos crea una billetera y ve los 5 tipos
- **THEN** se muestran en una cuadrícula de columnas del mismo ancho, no como chips de distinto tamaño que dejan uno solo abajo

### Requirement: Componentes que Se Acomodan a su Padre
Un componente DEBE (MUST) acomodar su espaciado y su ancho a su padre. Dentro de una tarjeta o sección usa el ancho y el margen de ese contenedor, sin agregar el suyo. Una sección agrupa un título, su contenido y, si hay, el valor anterior ("antes: …") y su error, siempre juntos. Los avisos de error de un grupo se muestran junto a ese grupo, no lejos.

#### Scenario: Error de la moneda propia
- **WHEN** Carlos agrega una moneda propia con el código "AB"
- **THEN** el aviso "Revisa la moneda" aparece debajo del formulario de la moneda, a la vista

#### Scenario: Botón dentro de una tarjeta
- **WHEN** una tarjeta de la bandeja tiene su acción "Revisar"
- **THEN** la acción usa el ancho interior de la tarjeta y no agrega un margen propio
