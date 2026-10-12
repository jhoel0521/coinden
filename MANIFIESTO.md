# 📜 Manifiesto SDD de CoinDen

Este es el contrato cultural entre las personas y los agentes de IA que construyen CoinDen.
Es corto a propósito: si una regla no cabe aquí, pertenece a un spec.

## 1. La especificación es la fuente de verdad

- Ningún comportamiento entra al código sin estar en un spec de `openspec/specs/` o en un cambio activo de `openspec/changes/`.
- Si el código y el spec no coinciden, el bug está en uno de los dos, y se decide **explícitamente** cuál se corrige.
- Ante una duda, se vuelve al spec, no a la memoria del agente ni a una conversación anterior.

## 2. Los specs están vivos

- Un spec se versiona junto al código, en el mismo repo y con el mismo historial.
- Cambiar de idea es normal. Se hace con `opsx-update` o con un nuevo cambio, nunca editando el código a escondidas.
- Al terminar un cambio se archiva (`opsx-archive`) y sus deltas pasan al spec principal.

## 3. Primero el qué y el por qué, después el cómo

- `proposal.md` dice **por qué** y **para quién**.
- `specs/` dice **qué** debe pasar, en escenarios WHEN/THEN que se pueden probar.
- `design.md` dice **cómo**, y es el único lugar donde se nombran clases, librerías o patrones.

## 4. La IA propone, la persona decide

- Los agentes exploran, redactan y programan dentro de los límites del cambio activo.
- Para proponer, aplicar o archivar un cambio, o hacer un commit, hace falta la aceptación explícita de la persona.
- Un agente que descubre algo fuera de alcance lo reporta. No lo resuelve por su cuenta.

## 5. Todo se puede rastrear

- Cada tarea de `tasks.md` apunta a un requisito, y cada requisito tiene al menos una prueba.
- Las pruebas de feature llevan nombre de persona (John, Carlos, Facundo, Sophie, Wei): son los escenarios del spec ejecutándose.
- Los commits siguen Conventional Commits en español y nombran el área que tocan.

## 6. Límites claros, no planos exhaustivos

- Cada cambio declara qué queda **dentro** y qué queda **fuera** (Non-Goals).
- Los principios del producto (abajo) son límites duros: un spec que los contradiga se rechaza.

## Principios del producto que ningún spec puede romper

1. **Local-first:** los datos viven en los dispositivos del usuario. Solo se mueven entre ellos (sincronización P2P) o hacia la nube *propia* del usuario (respaldo), y siempre por decisión explícita suya y cifrados de extremo a extremo. La app puede descargar paquetes de idioma y plantillas de comprobantes, que son datos públicos: nunca se envía nada del usuario para obtenerlos.
2. **Cero servidores de CoinDen:** no hay backend propio, cuenta obligatoria ni API keys pagas. La app funciona completa sin registrarse.
3. **Tasas reales:** se usa la tasa que el usuario realmente usa (paralelo, Meru, manual), no la oficial impuesta.
4. **Inteligencia determinista y local:** la categorización y la interpretación son reglas que se pueden probar y explicar. Los modelos (voz a texto, OCR) solo *perciben*, corren on-device y nunca deciden.
5. **Capturar rápido, confirmar con calma:** registrar toma segundos y genera borradores. La precisión llega en la revisión.
6. **Dinero exacto:** los montos se guardan como enteros en unidades mínimas. Nunca se usan flotantes para guardar dinero.
7. **Listo para sincronizar desde el día 1:** IDs globales (UUID), movimientos append-only y correcciones como nuevos eventos. Ninguna decisión de hoy puede impedir la sincronización P2P ni el respaldo de mañana.
8. **Sostenible sin traicionar al usuario:** el núcleo es gratis para siempre y el proyecto vive de donaciones. Lo premium solo agrega comodidad (por ejemplo, respaldo en tu nube), nunca encierra tus datos. Sin publicidad ni venta de datos.
9. **Mundial desde el día 1:** el dominio no contiene textos de idioma ni formatos regionales. Devuelve enums, códigos e IDs, y la presentación traduce y formatea según el locale. La app empieza en español y luego suma idiomas; el proyecto (specs, commits, docs) se escribe en español.
