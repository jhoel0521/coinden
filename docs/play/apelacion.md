# Apelación: "Política de servicios financieros — formulario impreciso"

Borrador para el formulario de apelación de Play Console. **Sin enviar: lo revisa Jhoel.**

- **App:** CoinDen — `com.coinden.app`
- **Motivo del rechazo:** Política de servicios financieros, declaración imprecisa
- **Declaración actual:** "Otro", con la descripción de [README.md](README.md)

---

## Español

**Asunto: CoinDen (com.coinden.app) — la declaración "Otro" es la que corresponde**

Apelo el rechazo por declaración imprecisa en el formulario de servicios financieros.
Creo que el formulario está respondido con precisión y que la confusión viene de la
categoría de la ficha, no de lo que la app hace.

**Qué es CoinDen.** Es una libreta personal de gastos: un cuaderno de anotaciones. La
persona registra a mano, dictando o fotografiando un comprobante lo que **ya pagó**, y la
app suma y resta esas anotaciones para mostrarle sus totales. Nada más.

**Por qué marqué "Otro".** Antes marqué "Mi app no ofrece ninguna función financiera" y
fue rechazada por imprecisa, entiendo porque la ficha está en la categoría Finanzas y
menciona billeteras y saldos. Cambié a "Otro" y describí la función exacta. No marqué
"Pagos móviles y billeteras digitales" ni "Billetera de criptomonedas" porque la app no
hace ninguna de las dos cosas, y declararlas afirmaría una actividad regulada que no
existe y para la que no podría presentar licencia alguna.

**Lo que la palabra "billetera" significa acá.** En CoinDen una "billetera" es solo una
etiqueta de registro que la persona crea y escribe a mano ("Efectivo", "Mi banco",
"Ahorros"), con un saldo que ella misma anota. No está conectada a ninguna cuenta real, no
contiene fondos y no puede transferir nada. Es el mismo concepto que una columna en una
planilla de Excel.

**Lo que la app no hace, de forma verificable:**

- No mueve dinero ni procesa pagos, transferencias, remesas ni cobros.
- No custodia ni almacena fondos ni criptomonedas: no hay claves privadas ni direcciones.
- No se conecta a bancos, exchanges, procesadores ni agregadores financieros. No hay
  Open Banking, ni Plaid, ni scraping de cuentas.
- No ofrece préstamos, crédito, adelantos de sueldo, inversiones, seguros ni
  asesoramiento financiero. No hay puntaje de crédito ni cobranza.
- No hay compras dentro de la app, suscripciones ni anuncios.

**Evidencia que pueden comprobar en el APK:**

1. **Tres permisos en total:** cámara (fotografiar un comprobante), micrófono (dictar un
   gasto) e internet. No pide SMS, contactos, ubicación ni teléfono — permisos que una app
   que de verdad mueve dinero o presta necesitaría.
2. **No hay cuentas ni servidores.** CoinDen no tiene backend. No hay registro, ni inicio
   de sesión, ni servidor al que subir datos. La app funciona completa en modo avión.
3. **El uso de internet es acotado y declarado:** las cotizaciones públicas de tipo de
   cambio que la persona configura, la descarga de paquetes de idioma opcionales, y las
   métricas técnicas que el SDK de reconocimiento de texto de Google (ML Kit) envía por su
   cuenta —versión, rendimiento e identificador por instalación—, que es exactamente lo que
   declaré en la sección de seguridad de los datos. Los montos, los textos y las fotos de la
   persona no se envían a ninguna parte.
4. **Los datos quedan cifrados en el teléfono** (SQLCipher) y se borran al desinstalar.
5. **El código es abierto y auditable:** https://github.com/jhoel0521/coinden

**Lo que pido.** Que se revise la declaración "Otro" como correcta. Si el equipo considera
que la categoría Finanzas es la que genera la ambigüedad, con gusto muevo la ficha a
"Productividad" o ajusto el texto: la app es una herramienta de anotación personal y no
tengo interés en presentarla como un servicio financiero.

Gracias por revisarlo.

---

## English

**Subject: CoinDen (com.coinden.app) — "Other" is the accurate declaration**

I am appealing the rejection for an inaccurate financial services declaration. I believe
the form is answered accurately, and that the confusion comes from my store category
rather than from what the app does.

**What CoinDen is.** It is a personal expense notebook. The user records what they have
**already paid** — by typing, by dictating, or by photographing a receipt — and the app
adds and subtracts those notes to show them their totals. Nothing else.

**Why I selected "Other."** I previously selected "My app does not provide any financial
features" and it was rejected as inaccurate, I understand because my listing sits in the
Finance category and mentions wallets and balances. I switched to "Other" and described the
exact function. I did not select "Mobile payments and digital wallets" or "Cryptocurrency
wallet" because the app does neither, and declaring them would assert a regulated activity
that does not exist and for which I could produce no license.

**What "wallet" means here.** In CoinDen a "wallet" is only a bookkeeping label that the
user creates and types in themselves ("Cash", "My bank", "Savings"), with a balance they
enter by hand. It is not linked to any real account, it holds no funds, and it cannot
transfer anything. It is the same concept as a column in a spreadsheet.

**What the app verifiably does not do:**

- It does not move money or process payments, transfers, remittances or collections.
- It does not custody or store funds or cryptocurrency: there are no private keys or
  addresses.
- It does not connect to banks, exchanges, processors or financial aggregators. There is no
  Open Banking, no Plaid, no account scraping.
- It does not offer loans, credit, cash advances, investments, insurance or financial
  advice. There is no credit scoring and no debt collection.
- There are no in-app purchases, subscriptions or ads.

**Evidence you can verify in the APK:**

1. **Three permissions in total:** camera (photograph a receipt), microphone (dictate an
   expense) and internet. It requests no SMS, contacts, location or phone permissions —
   the permissions an app that genuinely moves money or lends would require.
2. **There are no accounts and no servers.** CoinDen has no backend of its own. There is no
   sign-up, no login, and no server to upload data to. The app works fully in airplane mode.
3. **Internet use is narrow and disclosed:** the public exchange-rate quotes the user
   configures, optional language pack downloads, and the technical metrics that Google's
   text recognition SDK (ML Kit) sends on its own — version, performance and a per-install
   identifier — which is exactly what I declared in the Data Safety section. The user's
   amounts, text and photos are never sent anywhere.
4. **Data stays encrypted on the device** (SQLCipher) and is deleted on uninstall.
5. **The source code is open and auditable:** https://github.com/jhoel0521/coinden

**What I am asking.** That the "Other" declaration be reviewed as correct. If the team
believes the Finance category is what creates the ambiguity, I am glad to move the listing
to "Productivity" or adjust the wording: the app is a personal note-taking tool and I have
no interest in presenting it as a financial service.

Thank you for reviewing this.
