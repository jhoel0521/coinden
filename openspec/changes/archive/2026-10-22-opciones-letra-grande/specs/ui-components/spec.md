## MODIFIED Requirements

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
