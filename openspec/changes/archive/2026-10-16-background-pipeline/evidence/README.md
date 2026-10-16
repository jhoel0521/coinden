# Evidencia de background-pipeline (emulador Android 14, API 34)

Archivos de prueba **inventados**. Carlos con "Billetera" (100 Bs) y "BNB Bs" (500 Bs) y el modelo de voz pequeño descargado.

| Prueba | Resultado |
|---|---|
| Compartir una nota de voz desde Archivos (01) | Aviso "Guardado en CoinDen" y el teléfono sigue en Archivos (`FilesActivity` en primer plano); CoinDen no abre su pantalla |
| Compartir un PDF escaneado sin abrir CoinDen (02) | WorkManager corre `PerceptionWorker` en segundo plano: `Worker result SUCCESS` mientras se sigue en Archivos |
| Matar el proceso (`kill -9`) mientras Whisper transcribe (05) | El trabajo en curso se pierde ("unexpectedly disconnected"); al volver a arrancar el proceso, WorkManager registra "Found unfinished work, scheduling it" y la cola termina con `SUCCESS` |
| Memoria (06) | ~260 MB de PSS mientras Whisper transcribe → ~120 MB al vaciarse la cola (modelos liberados) |
| Compartir sin haber terminado el onboarding (07) | Se abre CoinDen en la bienvenida para terminarlo |

Pruebas instrumentadas: el trabajador lee una foto pendiente y libera los modelos (`PerceptionWorkerTest`), y el registro de fallas sobrevive en archivo.

Observación: tras un `kill -9`, Android no reprograma por su cuenta el trabajo cortado; WorkManager lo recupera la próxima vez que arranca el proceso (al abrir la app, compartir algo o reiniciar el teléfono). La captura nunca se pierde: queda Capturada con su archivo hasta entonces.
