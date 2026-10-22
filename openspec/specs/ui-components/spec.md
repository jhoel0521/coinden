# ui-components Specification

## Purpose
Asegura que todas las pantallas de CoinDen se vean y se comporten igual: cada control es un componente de CoinDen que fija su estética y se acomoda según tenga hermanos, padre o hijos, de modo que ningún grupo quede desparejo ni con opciones sueltas.

## Requirements

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
Un grupo de elección única DEBE (MUST) elegir su forma según sus opciones: hasta 4 que caben en una fila, segmentados del mismo ancho (caben si la etiqueta más larga entra entera en su parte, con la marca de la elegida); una lista abierta o que no cabe, una fila deslizable con la elegida visible; más de 8 fijas, las más usadas y "Más…" con una cuadrícula de columnas fijas. Ninguna forma deja una opción sola en una fila incompleta.

#### Scenario: Tipo de movimiento
- **WHEN** Carlos abre el formulario
- **THEN** "Gasto" e "Ingreso" se muestran como dos segmentados del mismo ancho en una fila

#### Scenario: Cinco billeteras
- **WHEN** Carlos tiene "Billetera", "BNB Bs", "Dolares", "Reais" y "Visa"
- **THEN** se muestran en una sola fila deslizable con la elegida visible, sin una billetera sola en otra fila

#### Scenario: Categorías
- **WHEN** Carlos abre el formulario de un gasto y suele usar comida, transporte y supermercado
- **THEN** ve esas 3 como mosaicos con su ícono y un cuarto mosaico "Más…"; al tocar "Más…" se abre una hoja titulada "Categoría" con todas las categorías como mosaicos con ícono en columnas iguales, y la elegida queda marcada

#### Scenario: Letra grande
- **WHEN** Carlos usa letra grande y las 4 categorías más usadas ya no caben en una fila
- **THEN** el grupo pasa a una fila deslizable en lugar de cortarse en dos filas

#### Scenario: Idiomas con la letra al máximo
- **WHEN** Carlos tiene la letra del sistema al máximo y abre la bienvenida
- **THEN** "Español", "English" y "Português" se ven enteros, sin cortarse; si no entran como segmentados del mismo ancho, pasan a una fila deslizable

### Requirement: Cuadrícula sin Opciones Sueltas
Cuando las opciones se muestran en varias filas (como "Más…" o los tipos de billetera), el sistema DEBE (MUST) usar columnas fijas del mismo ancho, de modo que cada opción tenga el mismo tamaño y la última fila quede alineada con las demás.

#### Scenario: Monedas que usa
- **WHEN** Carlos elige sus otras monedas entre las 10 frecuentes
- **THEN** se muestran en una cuadrícula de columnas del mismo ancho, no como chips de distinto tamaño que dejan uno solo abajo

#### Scenario: Cuatro opciones en tres columnas
- **WHEN** 4 opciones solo entrarían en 3 columnas
- **THEN** se ordenan en 2 x 2 en lugar de 3 + 1

#### Scenario: Ninguna cantidad de columnas evita la sola
- **WHEN** hay 13 opciones (las 12 categorías y "Sin categoría") en 3 columnas
- **THEN** las filas se reparten como 3, 3, 3, 2 y 2, con celdas del mismo ancho, en lugar de dejar una sola al final

#### Scenario: Solo entran 2 columnas
- **WHEN** Carlos usa letra grande y las 13 opciones de categoría solo entran en 2 columnas
- **THEN** la última ocupa la fila entera como cierre de la cuadrícula, en lugar de quedar sola a media fila

#### Scenario: Etiqueta larga
- **WHEN** una opción de la cuadrícula es "Tarjeta de crédito"
- **THEN** la cuadrícula usa menos columnas para que la etiqueta se lea entera, sin cortarse con "…"

### Requirement: Opciones con Ícono como Mosaicos
Cuando todas las opciones de una elección única fija tienen ícono, el sistema DEBE (MUST) mostrarlas como mosaicos del mismo ancho, con el ícono arriba y la etiqueta debajo (hasta 2 líneas), sin partir palabras. La elegida se distingue con relleno, borde e ícono de color, no solo por el color de fondo.

#### Scenario: Muchas opciones con ícono
- **WHEN** una elección fija con ícono tiene más de 5 opciones, como las categorías
- **THEN** la fila muestra las 3 más usadas (con la elegida siempre entre ellas) y un mosaico "Más…" que abre todas en una hoja

#### Scenario: Categoría propia
- **WHEN** Carlos creó la categoría "Mascotas"
- **THEN** aparece en la hoja de categorías con un ícono de etiqueta, igual que las categorías base

#### Scenario: Medio de pago
- **WHEN** Carlos abre el formulario de un gasto
- **THEN** ve "Sin indicar", "Efectivo", "Transferencia o QR" y "Tarjeta" en una sola fila de 4 mosaicos con su ícono, sin opciones fuera de la pantalla

#### Scenario: Tipos de billetera en un teléfono
- **WHEN** Carlos crea una billetera en un teléfono y los 5 tipos no dejan 64 dp por mosaico
- **THEN** ve los 5 tipos como mosaicos con ícono en una cuadrícula de 3 columnas iguales, y "Tarjeta de crédito" se lee entera

#### Scenario: Tipos de billetera en pantalla ancha
- **WHEN** a cada uno de los 5 tipos le quedan al menos 64 dp
- **THEN** se muestran en una sola fila de mosaicos

### Requirement: Componentes que Se Acomodan a su Padre
Un componente DEBE (MUST) acomodar su espaciado y su ancho a su padre. Dentro de una tarjeta o sección usa el ancho y el margen de ese contenedor, sin agregar el suyo. Una sección agrupa un título, su contenido y, si hay, el valor anterior ("antes: …") y su error, siempre juntos. Los avisos de error de un grupo se muestran junto a ese grupo, no lejos.

#### Scenario: Error de la moneda propia
- **WHEN** Carlos agrega una moneda propia con el código "AB"
- **THEN** el aviso "Revisa la moneda" aparece debajo del formulario de la moneda, a la vista

#### Scenario: Botón dentro de una tarjeta
- **WHEN** una tarjeta de la bandeja tiene su acción "Revisar"
- **THEN** la acción usa el ancho interior de la tarjeta y no agrega un margen propio

### Requirement: Tecla del Teclado en Cada Campo
Cada campo DEBE (MUST) darle al teclado una tecla que haga algo: si el campo tiene una acción propia, "Listo" la ejecuta y cierra el teclado; si no, "Siguiente" pasa al próximo campo y, en el último, cierra el teclado. Un buscador muestra "Buscar" y cierra el teclado dejando los resultados a la vista.

#### Scenario: Pasar de un campo al siguiente
- **WHEN** Carlos escribe el código de su moneda propia y toca "Siguiente" en el teclado
- **THEN** el cursor pasa al campo "Nombre"

#### Scenario: Último campo con acción
- **WHEN** Carlos escribe el saldo de hoy de una billetera y toca "Listo"
- **THEN** la billetera se agrega como si tocara "Agregar billetera" y el teclado se cierra

#### Scenario: Frase del formulario
- **WHEN** Carlos escribe "almuerzo 45 bnb" en "Escríbelo como te salga" y toca "Listo"
- **THEN** el formulario se llena como si tocara "Llenar"

#### Scenario: Último campo sin acción
- **WHEN** Carlos escribe la referencia, el último campo del formulario, y toca la tecla del teclado
- **THEN** el teclado se cierra y Carlos ve el botón "Guardar"
