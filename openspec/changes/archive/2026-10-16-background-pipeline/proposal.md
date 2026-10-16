# Proposal: Procesamiento en Segundo Plano (Background Pipeline)

## Why
CoinDen es para gastos hormiga: la persona manda una foto, un audio o un texto y se va; revisa en la noche. Hoy la lectura de fotos, PDF y audios corre solo mientras la app sigue abierta: si la persona sale y Android cierra la app, lo pendiente espera hasta que vuelva a abrirla. Compartir desde WhatsApp, además, abre CoinDen y deja a la persona mirando una hoja de resultado. Hace falta una cola que siga trabajando sola, que no sobrecargue el teléfono y sobre la que después se sume la IA (`local-llm-interpreter`) como un paso más.

## What Changes
- **Cola en segundo plano**: cada captura se guarda al instante y lo que falta procesar (leer la foto, el PDF o el audio) sigue haciéndose aunque la persona cierre la app, y se retoma solo si Android corta el trabajo o el teléfono se reinicia.
- **Lo pendiente sale de los datos**: no hay una lista aparte; la cola procesa los borradores Capturados con archivo que todavía no se leyeron, así nada se duplica ni se pierde, también con varios dispositivos.
- **De a uno y liberando memoria**: un trabajo a la vez; cuando la cola queda vacía se descargan los modelos (voz, lector de Google) y se libera la memoria. Con la app abierta y sin uso, también.
- **Compartir y volver**: compartir desde otra app guarda la captura, muestra un aviso corto ("Guardado en CoinDen") y devuelve a la otra app al instante, sin abrir CoinDen. Si el onboarding no terminó, sí abre la app para terminarlo.
- **Lo pendiente se lee solo**: las capturas guardadas antes de la lectura y los audios que esperaban un modelo de voz se procesan en la cola sin tocar ningún botón (al abrir la app o al descargar un modelo).
- **Reintentos**: si un trabajo falla, se reintenta más tarde; si sigue fallando, el borrador queda Capturado para revisarlo a mano.
- **Sin notificaciones**: lo procesado se ve en Inicio y, después, en la bandeja de la noche.

### Non-Goals
- La bandeja de la noche (`review-inbox-ui`).
- La IA que interpreta frases (`local-llm-interpreter`); la cola queda preparada para sumarla como un paso.
- Volver a interpretar texto ya interpretado (frases y comprobantes de texto se interpretan al capturar, en milisegundos).
- Procesar con condiciones de batería o carga: la cola corre apenas puede.

## Capabilities

### New Capabilities
- `background-processing`: cola que procesa en segundo plano lo capturado (sobrevive al cierre de la app y al reinicio), de a un trabajo por vez, con reintentos, y libera la memoria de los modelos al terminar.

### Modified Capabilities
- `capture-ui`: compartir desde otra app guarda y devuelve a la otra app con un aviso corto; las capturas pendientes se leen solas en la cola en lugar de con un botón.

## Impact
- **`composeApp`**: trabajador de la cola (Android), receptor de "compartir" sin interfaz, liberación de modelos al vaciarse la cola, Inicio sin el botón "Leer capturas guardadas"; textos en es, en y pt.
- **Dependencia nueva**: WorkManager (`androidx.work`), parte de Android Jetpack.
- **Sin cambios** en el dominio ni en la persistencia: lo pendiente ya se deduce de los eventos del borrador.
