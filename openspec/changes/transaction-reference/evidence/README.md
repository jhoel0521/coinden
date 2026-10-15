# Evidencia de transaction-reference (emulador Android 14, API 34)

Carlos (Bolivia) con "Billetera" en efectivo (100 Bs, por defecto) y "BNB Bs" en banco (500 Bs). El emulador
no tiene dictado local, así que las frases se escribieron en el formulario (mismo intérprete).

| Captura | Escenario |
|---|---|
| 01 | Escribe "gaste 10 en power" y comparte el comprobante QR de 10 Bs con "Nro. de transacción: 400000123": la tarjeta muestra el Nro. y pregunta "¿Es el mismo gasto que «power» de 10,00 BOB a las 18:33?" con "Son el mismo" y "Son distintos" |
| 02 | "Son el mismo": queda un borrador con la frase de Carlos, el Nro. 400000123 y "Unido con 1 captura más" |
| 03 | Dos "gaste 10 bs por qr": el segundo pregunta si es el mismo gasto |
| 04 | "Son distintos": la pregunta pasa al otro gasto de 10 Bs del día (el power); la app pregunta uno por uno |
| 05 | Compartir otra vez el mismo comprobante: "Ya enviaste este comprobante." |
| 06–07 | "Son el mismo": los pendientes siguen siendo 3 (power, taxi y el segundo de 10 Bs) y los saldos no cambiaron |

Observaciones para decidir fuera de este cambio:
- El lector de frases deja "por" en la nota de "gaste 10 bs por qr" (la pregunta muestra «por») y de "gasté 10 por power" ("por power"), porque "por" no está entre las preposiciones del paquete español. Los escenarios usan "gasté 10 en power".
- Una pregunta que queda sin responder al cerrar la hoja de resultado solo se vuelve a ver cuando exista la bandeja de revisión (`review-inbox-ui`).
