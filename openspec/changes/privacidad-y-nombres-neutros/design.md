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
`docs/play/ficha.md` y `docs/play/README.md` se actualizan (nombres, declaración de datos y el borrado). Las capturas de la ficha se vuelven a tomar con los nombres nuevos, porque las actuales muestran "Banco" y "Cripto".

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
- ¿Se mantiene el medio de pago "Transferencia o QR" tal cual? Es lo que dice el comprobante; cambiarlo afecta lo que el usuario reconoce al capturar.
