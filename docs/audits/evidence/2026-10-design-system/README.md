# Evidencia: sistema de componentes y marca (2026-10-17)

Emulador `Android14_API34` (1080×2400), APK de depuración de la rama `feat/sistema-de-componentes`.
Datos inventados: Billetera 100 BOB, BNB Bs 500 BOB, Dolares 100 USD, Reais 50 BRL; USD a 10,20 y BRL sin tasa.

| Captura | Qué verifica |
|---------|--------------|
| 01, 40 | Bienvenida con el logo nuevo en placa oscura, en claro y oscuro; idioma como segmentado. |
| 02, 41 | Privacidad: interruptor del lector de Google en una tarjeta; acción principal al pie. |
| 03 | Modelos de voz: barra superior, insignia "Recomendado" y acciones del mismo ancho. |
| 04, 05, 42 | Monedas: cuadrícula de 4 columnas iguales; "Atrás" y "Continuar" con la misma altura. |
| 06 | F1: al buscar con el teclado abierto, el buscador sube y sus resultados quedan a la vista. |
| 07 | F2: el error de la moneda propia aparece dentro de su formulario. |
| 08, 09, 43 | Billeteras, antes de los mosaicos (ver 54): los tipos en 2 columnas porque "Tarjeta de crédito" no entraba en 3 sin cortarse. |
| 10, 21, 30 | Inicio en claro, oscuro y letra 1,3. |
| 11, 12, 23, 32 | Formulario: segmentados, filas deslizables, "Más…" y "Más detalles" plegable. |
| 13 | Hoja de resultado con la tarjeta del borrador. |
| 14, 22, 31 | Bandeja compacta con "Confirmar los N listos". |
| 15, 16, 17, 20, 33, 34 | Ajustes: tarjetas por billetera, diálogo de archivar, tasas; la acción de peligro deshabilitada apaga también su borde. |
| 45 | Ícono adaptable nuevo en el lanzador. |
| 51, 52 | Medio de pago como 4 mosaicos con ícono en una fila (claro y oscuro); la elegida con relleno, borde e ícono de color. |
| 53 | Medio de pago con letra 1,3: la palabra "Transferencia" no entra en 4 columnas y los mosaicos pasan a 2 x 2. |
| 54 | Tipos de billetera como mosaicos con ícono en 3 + 2 columnas iguales. |
| 55, 57, 58 | Categoría como mosaicos con ícono: las 3 más usadas (con la elegida siempre visible) y "Más…", en claro y oscuro. |
| 56 | Hoja "Categoría" con las 13 opciones como mosaicos en 3, 3, 3, 2, 2: ninguna queda sola. |
| 59, 60 | Letra 1,3: categoría en 2 x 2 y la hoja en 2 columnas; la última ocupa la fila entera. |
| 61, 62 | Teclado: la frase muestra "Listo" (✓) y al tocarlo llena el formulario como "Llenar"; Enter del teclado físico hace lo mismo. |
| 63 | "Siguiente" en el nombre de una billetera salta los mosaicos y deja el cursor en "Saldo de hoy". |

Corregido durante esta verificación:

- Acciones hermanas en fila con distinta altura (la principal medía 56 dp y la otra 48 dp).
- "Tarjeta de …" cortado en la cuadrícula de tipos.
- Títulos negros sobre fondo oscuro tras quitar la `Surface` raíz: ahora `App` fija el color del contenido.
- Borde rojo en una acción de peligro deshabilitada.
- En cada billetera, "Usar como predeterminada" va antes que "Archivar".
- Los mosaicos no cambian a negrita al elegirse (partía "Transferencia") y la cuadrícula evita dejar una opción sola (3 + 1 pasa a 2 x 2).

Las capturas 51 a 63 usan los datos que había en el emulador; no se guardó nada.

El splash (#1C140F con el ícono) dura menos que una captura por adb y no quedó registrado.
