# Design

## Context

Ver `proposal.md` (Why). Estado actual relevante, comprobado en el código:

- La base cifrada (`coinden.db`, SQLCipher) se abre en `CoinDenApplication.onCreate` y el contenedor (`AppContainer`) vive con el proceso. La passphrase está en `coinden.db.key`, cifrada con una llave del Android Keystore (alias `coinden.db`).
- Las fotos, PDF y audios de comprobantes van a `filesDir/attachments` (`EncryptedAttachmentStore`). Hay además `queue-failures.tsv`, restos en `cacheDir` (PDF y audios temporales), y datos **públicos** descargados: `models/`, `packs/`, `tesseract/`.
- Las preferencias (idioma, lector de Google, perfil) están dentro de la misma base (`LocalPreferencesRepository`), no en `SharedPreferences`.
- `allowBackup="false"`: Android no copia estos datos a la nube de Google.
- El bloqueo con huella o PIN existe (`AppLockController`, `DeviceAuth.authenticate()`); las pruebas usan un `DeviceAuth` falso.
- Los nombres visibles de los tipos de billetera salen de `composeResources/values*/strings.xml` (`wallet_type_*`, `kind_transfer`); el dominio solo tiene los enums `WalletType` y `MovementType`.
- El sitio se publica desde `site/` con `pages.yml` al fusionar a `main`. `ProjectSite.url(language)` ya abre la portada en el idioma de la app desde Perfil.

## Goals / Non-Goals

**Goals:**
- Un borrado total, comprobable y sin estados a medias.
- Que ningún texto de la interfaz nombre un producto financiero que CoinDen no ofrece, sin perder funciones.
- Una política de privacidad completa y fiel a lo que hace el código.

**Non-Goals:**
- No cambiar `WalletType`, `MovementType` ni nada en `core/domain` (regla: el dominio no tiene textos).
- No crear un servicio, cuenta ni consulta externa; la elección de idioma del sitio no usa IP ni país.
- No resolver aquí la sincronización P2P del borrado (aún no existe sync entre dispositivos).

## Decisions

### 1. El borrado se hace en un puerto de la app y reinicia el proceso
Un puerto `DataEraser` (commonMain) con una implementación Android. Un presentador `EraseDataPresenter` (commonMain, sin Compose) lleva los estados `Idle → Confirming → Authenticating → Erasing → Failed | Done`, y se prueba en la JVM con un `DataEraser` falso, como el resto de presentadores.

Al terminar, la implementación Android **cierra el driver, borra, y reinicia el proceso** (relanza `MainActivity` y mata el proceso). *Por qué:* el contenedor, los repositorios y la cola viven con el proceso y tienen la base abierta; reconstruirlos en caliente es frágil y fácil de dejar con datos viejos en memoria. Reiniciar garantiza que la app arranca "recién instalada".
*Alternativa descartada:* reconstruir `AppContainer` sin matar el proceso (más código, más riesgo de fugas de datos en memoria).

### 2. Qué se borra y en qué orden
1. Cancelar el trabajo en segundo plano (WorkManager) y vaciar la cola.
2. Cerrar la base y borrar `coinden.db` con su `-wal`, `-shm` y `-journal` (`context.deleteDatabase`).
3. Borrar `coinden.db.key` **y la entrada del Keystore** (`coinden.db`): sin ellas, lo que quedara en disco no se puede descifrar, y el próximo inicio crea una llave nueva.
4. Borrar `attachments/`, `queue-failures.tsv` y los restos de `cacheDir`.
5. Reiniciar.
*No se borran* `models/`, `packs/` ni `tesseract/` (datos públicos descargados; ver Non-Goals del proposal).

El orden pone la base y la llave antes que las fotos a propósito: si se interrumpe a medias, la base ya es ilegible y no hay datos parciales visibles.

### 3. Sin estados a medias
Se deja un marcador `erase.pending` en `filesDir` **antes** de empezar. Al iniciar, si existe, la app **termina el borrado** antes de abrir la base y lo quita al acabar. Así una interrupción (batería, cierre forzado) no deja datos parciales, y "reintentar" es simplemente volver a ejecutar el mismo borrado, que es idempotente.

### 4. Autenticación solo si el bloqueo está activo
Se reutiliza `DeviceAuth.authenticate()`. Si el bloqueo está apagado, no se pide nada extra: la confirmación explícita ya es la barrera. Si está activo, una autenticación fallida o cancelada deja todo intacto.

### 5. El borrado solo afecta a este dispositivo (decisión por defecto)
Hoy no existe sincronización entre dispositivos, así que no hay nada que propagar. Se decide que, cuando exista, un borrado local **no** enviará órdenes a otros dispositivos: borrar un teléfono no debe poder destruir en silencio los datos del otro. La pantalla de confirmación lo dice. *Si en el futuro se quiere "borrar en todos mis dispositivos", será un cambio nuevo con su propio spec.*

### 6. Nombres neutros: solo textos, con una tabla única
Los cambios son únicamente en `strings.xml` (es, en, pt):

| Clave | es | en | pt |
|---|---|---|---|
| `wallet_type_cash` | Efectivo | Cash | Dinheiro |
| `wallet_type_bank` | **Cuenta** | **Account** | **Conta** |
| `wallet_type_digital` | Digital | Digital | Digital |
| `wallet_type_crypto` | **Personalizada** | **Custom** | **Personalizada** |
| `wallet_type_credit` | **Tarjeta** | **Card** | **Cartão** |
| `kind_transfer` | **Traspaso** | **Move** | **Repasse** |

Una prueba recorre todos los valores de `WalletType` y de `MovementType` y falla si alguno de los textos visibles contiene una de las palabras retiradas ("Banco", "Cripto", "crédito", "Transferencia" como tipo de movimiento). Así los nombres no vuelven por accidente.
*El medio de pago "Transferencia o QR" (`method_transfer`) no cambia:* describe cómo pagó la persona, igual que lo dice el comprobante, y no es un servicio de CoinDen. Queda marcado como pregunta abierta.

### 7. Política y sitio
- `site/privacidad`, `site/en/privacy` y `site/pt/privacidade` se reescriben con las secciones del spec `project-site`. Los hechos (cifrado, `allowBackup="false"`, qué sale del teléfono) salen de este diseño y del código, no de memoria.
- La portada lleva un script corto que lee `navigator.language`, redirige a `/en/` o `/pt/` una sola vez y guarda la elección manual en `localStorage` (con `try/catch`: si falla, simplemente no redirige). Los enlaces ES/EN/PT guardan la elección antes de navegar.
- Sin IP, sin país, sin servicios externos: se elige por el idioma que declara el navegador.
*Alternativa descartada:* redirigir por país con un servicio de geolocalización: exige una consulta externa y contradice el principio de no enviar datos a terceros.

### 8. Documentación y Play
`docs/play/ficha.md` y `docs/play/README.md` se actualizan (nombres, declaración de datos y el borrado). Las capturas de la ficha se vuelven a tomar con los nombres nuevos, porque las actuales muestran "Banco" y "Cripto" (el plan por captura está en la decisión 9).

### 9. Vocabulario visible sin términos financieros
La app dice lo que es: una libreta de gastos. Se retiran los términos que suenan a producto financiero, solo en el texto que ve la persona (`strings.xml` es/en/pt, `site/`, `docs/play/`). Los nombres internos no cambian: clases, enums, claves de recursos, paquetes y alias del intérprete.

**Qué cambia**

| Concepto | es | en | pt |
|---|---|---|---|
| billetera / billeteras | **fuente / fuentes** | **source / sources** | **fonte / fontes** |
| Patrimonio total / parcial | **Total anotado** / **Total anotado parcial** | **Recorded total** / **Partial recorded total** | **Total anotado** / **Total anotado parcial** |
| saldo, saldo de hoy, ajuste de saldo | **total**, **total de hoy**, **ajuste** | **total**, **today's total**, **adjustment** | **total**, **total de hoje**, **ajuste** |
| tasa, cotización, "cambio" de moneda | **equivalencia(s)** | **equivalence(s)** | **equivalência(s)** |
| Billeteras y tasas (Perfil) | **Fuentes y equivalencias** | **Sources and equivalences** | **Fontes e equivalências** |
| ¿Dónde guardas tu dinero? (paso 3) | **¿De dónde salen tus gastos?** | **Where do your expenses come from?** | **De onde saem seus gastos?** |
| Crea una billetera por cada lugar y pon cuánto tiene hoy | **Crea una fuente por cada lugar y anota con cuánto empiezas** | **Create a source for each place and note what you start with** | **Crie uma fonte para cada lugar e anote com quanto começa** |
| pago, pagos (¿De qué fue este pago?, Dos pagos…, Este pago no tiene comprobante) | **gasto, gastos** | **expense, expenses** | **gasto, gastos** |
| Pistas de nombre "BNB Bs, Billetera, Yape…" | **Mi efectivo, Mi cuenta…** | **My cash, My account…** | **Meu dinheiro, Minha conta…** |
| "Cotizaciones públicas" (privacidad, ficha, sitio) | **equivalencias públicas** | **public equivalences** | **equivalências públicas** |

Los textos largos que contienen esas palabras ("No cambia tus saldos hasta que lo confirmes", "Desbloquea para ver tus saldos", "No alcanza el saldo…", "Ya cuenta en tu saldo") se reescriben con la misma regla: saldo → total, billetera → fuente.

**Qué NO cambia, y por qué**

| Se queda | Motivo |
|---|---|
| Medio de pago "Transferencia o QR" (`method_transfer`), "Efectivo", "Tarjeta" y "Digital" | Describen cómo pagó la persona, como lo dice su comprobante; no son un servicio de CoinDen. Decisión de la persona. |
| Tipos "Cuenta", "Personalizada", "Tarjeta" y el movimiento "Traspaso" | Ya decididos en el grupo 1. |
| "Monto", "Gasto", "Ingreso", "Confirmar", "Borrador" | Son el vocabulario de la libreta, no de un servicio financiero. |
| Códigos de moneda (BOB, USD, ARS…) | La app sirve a quien usa varias monedas; son datos del mundo real. |
| "Comprobante" y "evidencia" | Describen la foto o el PDF que la persona adjunta. |
| Nombres que escribe la persona ("Mi efectivo", "BNB Bs") y alias del intérprete ("bnb", "yape", "meru") | Son datos de la persona o de los paquetes de idioma, no texto de la app. |
| "Sin cuenta, sin registro" | "Cuenta" aquí significa que no hay inicio de sesión: es parte de la promesa de privacidad. |
| Clases, enums, claves de recursos y paquetes del código | No los ve la persona; renombrarlos solo agrega riesgo y ruido. |

**Guarda contra regresiones.** La prueba de nombres neutros se amplía: lee todos los `strings.xml` (es, en, pt) y falla si algún texto contiene una palabra retirada (billetera/wallet/carteira, patrimonio/net worth/patrimônio, saldo/balance, tasa/rate/taxa, cotiza/quote/cotação, pago/payment/pagamento, "guardas tu dinero"), salvo una lista corta y explicada de excepciones (por ejemplo `method_transfer`).

**Capturas de la ficha.** Se vuelven a tomar las que muestran texto cambiado, con la app 0.2.1 recién instalada, en 1080x1920 (9:16; Play no admite más de 2:1):

| Captura | Qué muestra | ¿Se retoma? |
|---|---|---|
| 01 bienvenida | Idioma y "Sin cuenta, sin registro" | No cambia: se revisa que no tenga palabras retiradas |
| 02 privacidad | Qué sale del teléfono, con la línea de borrar y "equivalencias" | **Sí** |
| 03 contexto | "Lo que mandaste" con una nota "Gasto hormiga…" y la pregunta "¿De qué fue este gasto?" | **Sí** |
| 04 fuentes | Paso 3: "¿De dónde salen tus gastos?", tipos y las fuentes creadas | **Sí** |
| 05 escribir | Formulario con "Fuente" y los medios | **Sí** |
| 06 borrador listo | Resultado con "Fuente" y el aviso "No cambia tus totales…" | **Sí** |
| 07 inicio | "Total anotado" y "Fuentes" | **Sí** |
| 08 movimientos | Historial | No cambia: se revisa que no tenga palabras retiradas |

Google Play admite hasta 8 capturas de teléfono por idioma, así que el conjunto queda en 8.

## Risks / Trade-offs

- **[El rechazo de Play puede no deberse a los nombres]** → Se declara en el proposal que no hay garantía. El borrado y la política valen por sí mismos; si Play sigue rechazando, el ticket de soporte ya enviado es el siguiente paso.
- **["Personalizada" para cripto resta claridad a quien tiene BTC]** → La función no cambia: la moneda de la billetera (BTC) sigue visible. Si molesta, se puede cambiar el texto sin tocar nada más.
- **[Borrar es irreversible y no hay respaldo]** → Doble barrera: confirmación explícita y autenticación si hay bloqueo. El texto lo dice con claridad.
- **[Reiniciar el proceso corta una operación en curso]** → El borrado cancela antes el trabajo en segundo plano; el marcador `erase.pending` asegura que se completa al siguiente inicio.
- **[El script de idioma falla en navegadores con almacenamiento bloqueado]** → Envuelto en `try/catch`; sin almacenamiento, no redirige y la portada queda en español.

## Migration Plan

- Las billeteras existentes conservan su tipo (los códigos no cambian): solo cambia el texto que se ve. No hay migración de datos.
- Versión **0.2.1** (código de versión 201): se compila el AAB firmado con la etiqueta `v0.2.1`, se sube a la prueba cerrada Alpha como actualización de la 0.2.0.
- Reversión: volver a los textos anteriores en `strings.xml` y publicar otra versión; el borrado no deja datos que revertir.

## Open Questions

- ¿Los nombres de la tabla de la decisión 6 están bien, o prefieres otros (por ejemplo "Activo" o "Otra" en lugar de "Personalizada")? Se pueden ajustar antes de aplicar sin cambiar specs ni tareas.
- Resuelta: el medio de pago "Transferencia o QR" se mantiene tal cual (decisión de la persona).
- Resuelta: "billetera" pasa a "fuente" (en "source", pt "fonte"). Si más adelante se prefiere otra palabra, es un solo cambio de tabla en la decisión 9 y de textos.
- Resuelta: los specs principales no se reescriben; se agregó `openspec/glosario.md`, que traduce los términos del spec y del dominio al vocabulario visible, y `AGENTS.md` lo enlaza.
