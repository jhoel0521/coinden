# Proposal

## Why

Google Play rechazó CoinDen cuatro veces (y una apelación) por la política de servicios financieros: "la declaración de funciones financieras es imprecisa", sin decir qué falla. El asistente de ayuda de Play señaló como causa probable los textos visibles de la app ("Banco", "Cripto", "Tarjeta de crédito", "Transferencia"), que suenan a servicios que CoinDen no ofrece: la app solo anota lo ya gastado.

Aparte del rechazo, quien usa CoinDen guarda en el teléfono fotos y PDF de comprobantes que traen números de cuenta y de transacción. Hoy esos datos están cifrados, pero la persona no tiene cómo borrarlos sin desinstalar la app, y la política de privacidad no explica con detalle qué se guarda, cómo se protege ni cómo se borra.

Es para quien usa CoinDen (que debe poder controlar y borrar sus datos) y para quien publica la app (que necesita que el texto de la app diga lo que la app es).

## What Changes

- **Nombres neutros en la interfaz.** Los tipos de billetera pasan a nombres de registro, no de producto financiero: "Banco" → "Cuenta", "Cripto" → "Personalizada", "Tarjeta de crédito" → "Tarjeta". El movimiento "Transferencia" entre billeteras pasa a "Traspaso". Cambia solo lo que se ve (es, en, pt); los códigos del dominio (`BANK`, `CRYPTO`, `CREDIT`, `TRANSFER`) no cambian.
- **Vocabulario visible sin términos financieros.** Además de los tipos, se retiran del texto de la app, del sitio y de la ficha las palabras que suenan a producto financiero: "billetera" pasa a "fuente", "patrimonio" y "saldo" a "total anotado" o "total", "tasa" y "cotización" a "equivalencia", "pago" a "gasto", y se quitan los nombres de bancos y billeteras reales de los ejemplos. Cambia solo lo que se ve (es, en, pt): el dominio, los nombres internos del código y los nombres que escribe la persona no cambian. "Transferencia o QR" como medio de pago se queda.
- **Borrar todos mis datos.** Perfil gana una acción "Borrar mis datos" que, tras una confirmación clara (y la autenticación del teléfono si el bloqueo está activo), elimina del dispositivo la base cifrada, su llave, las fotos y audios de comprobantes, la cola de procesamiento y las preferencias, y devuelve la app a la bienvenida.
- **Política de privacidad completa y por idioma.** La página del sitio (es, en, pt) explica qué se guarda (incluidas las fotos de comprobantes, que pueden traer números de cuenta), cómo se cifra, que Android no respalda los datos en la nube, qué pasa con el lector de Google opcional, cómo borrar todo, que no hay cuenta ni servidor y a quién escribir.
- **Sitio en el idioma de la persona.** La portada elige es, en o pt según el idioma del navegador, una sola vez y respetando la elección manual. No se detecta por país ni por IP.
- **Acotar la regla de solo-agregar.** `local-persistence` dice que el almacenamiento no ofrece forma de borrar eventos; se aclara que eso protege la historia contra ediciones, y que el borrado total del dispositivo por decisión de la persona es una excepción explícita.
- Las capturas de la ficha de Play se vuelven a tomar con los nombres nuevos, y la ficha y la guía de Play se actualizan.

## Capabilities

### New Capabilities
- `data-erasure`: la persona borra por completo los datos de CoinDen de su dispositivo, con confirmación, y la app queda como recién instalada.
- `project-site`: el sitio del proyecto muestra la política de privacidad completa y lleva a cada visita al idioma de su navegador.

### Modified Capabilities
- `local-persistence`: el borrado total del dispositivo, pedido por la persona, es una excepción a "no se pueden borrar eventos".
- `app-navigation`: Perfil incluye la acción "Borrar mis datos".
- `wallet-settings`: los tipos de billetera se muestran con nombres neutros y todo el texto visible usa el vocabulario sin términos financieros.
- `app-onboarding`: el paso de billeteras usa los nombres neutros y la pantalla de privacidad menciona el borrado.

## Non-Goals

- No se agrega servidor, cuenta, telemetría propia ni detección de país o IP.
- No se cambian los enums ni el modelo del dominio (`WalletType`, `MovementType`): solo los textos de la interfaz.
- No se elimina ninguna función: monedas, equivalencias entre monedas, traspasos entre fuentes y el tipo "Personalizada" siguen existiendo; solo cambia cómo se llaman en pantalla.
- No se renombran clases, enums, claves de recursos ni paquetes del código (`Wallet`, `ExchangeRate`, etc.), ni los alias que dice la persona ("bnb", "meru"): son datos o nombres internos, no texto de la app.
- No hay borrado selectivo por movimiento: las correcciones siguen siendo eventos nuevos (append-only).
- No se agrega exportación ni respaldo de datos; quedan para un cambio futuro.
- Los modelos de voz, de lenguaje y los paquetes descargados son datos públicos, no del usuario: el borrado total no los toca.
- No se garantiza que esto levante el rechazo de Play; se hace porque mejora la privacidad y la claridad, y es la pista concreta que dio Google.

## Impact

- **Código:** `composeApp` (recursos de texto es/en/pt, `ProfileScreen`, nuevo presentador y pantalla de borrado, `AppContainer`/`CoinDenApplication` para cerrar y borrar la base), `core/data-local` (borrar base y llave).
- **Specs:** las seis listadas arriba.
- **Sitio:** `site/` (privacidad en es, en, pt; portada con redirección). Se publica solo con `pages.yml` al fusionar.
- **Documentación y Play:** `docs/play/ficha.md`, `docs/play/README.md` (declaración de datos), capturas de la ficha y versión 0.2.1.
