# Proposal: Datos Guardados y Cifrados en el Teléfono (Local Persistence)

## Why
La app ya corre en Android, pero olvida todo al cerrarse: las billeteras, los borradores del día y lo que la categorización aprendió viven solo en memoria. Sin guardar datos no se puede capturar de día y revisar de noche, que es el corazón de CoinDen. Como son datos financieros personales, deben quedar en el teléfono del usuario **y cifrados**, para que ni otra app ni alguien que copie el archivo pueda leerlos. Y como el sync P2P y el respaldo vienen después, el formato que se guarde hoy debe poder viajar mañana entre dispositivos con versiones distintas de la app.

## What Changes
- **Nueva base de datos local cifrada**: todos los puertos de almacenamiento del dominio (billeteras y libro, borradores, alias y billetera por defecto, clasificaciones, reglas y categorías propias, perfil de monedas) se guardan en una base SQLite del teléfono, cifrada por completo con AES-256 (SQLCipher).
- **Llave sin contraseña**: la llave de cifrado se genera al azar en el primer inicio y queda protegida por el almacén seguro del sistema (Android Keystore). No se pide cuenta ni contraseña; el PIN opcional es un cambio aparte.
- **Eventos como objetos con formato versionado**: cada evento (del libro, de borradores y de clasificación) se guarda con un sobre consultable (id, flujo al que pertenece, dispositivo, fechas, tipo y versión de formato) y su contenido completo como JSON. El dinero se guarda en su forma canónica exacta (`"45.00 BOB"`), nunca como flotante.
- **Eventos desconocidos no se pierden**: si la base contiene un evento de un tipo o versión que esta versión de la app no entiende (por ejemplo, llegado de un dispositivo más nuevo), se conserva intacto y se ignora al proyectar.
- **Libro de solo agregar también en disco**: los eventos solo se insertan; repetir un id se rechaza y no existen operaciones para editar o borrar eventos.
- **Identidad estable**: el id del dispositivo y el del usuario local se generan una vez (UUID) y se conservan entre sesiones, como exige el sync.
- **Las billeteras de ejemplo solo la primera vez**: la demostración siembra sus billeteras únicamente si la base está vacía.
- **Dominio**: `ExchangeRate` gana una forma canónica pública para guardarse sin perder su fracción exacta (hoy solo expone una lectura decimal).
- **Principio 6 aclarado**: el manifiesto y las reglas del proyecto dicen a la vez "enteros en unidades mínimas" y "forma canónica"; se unifica la redacción en "dinero exacto: nunca flotantes; se persiste en forma canónica exacta, equivalente a unidades mínimas".

### Non-Goals
- PIN o contraseña opcional para la llave (cambio aparte).
- Sincronización P2P y respaldo en la nube (`device-sync`, `cloud-backup`); aquí solo se deja el formato listo para ellos.
- Tablas de lectura o cachés de saldos: los saldos se siguen calculando con las proyecciones del dominio.
- Exportar o importar datos y migrar datos de versiones anteriores (no hay usuarios todavía).
- Persistencia en Desktop e iOS: el módulo es multiplataforma, pero en este cambio solo se activa Android.
- Pantallas nuevas: la demo de captura sigue igual, solo que ahora recuerda.

## Capabilities

### New Capabilities
- `local-persistence`: almacenamiento local cifrado de todos los datos del usuario, con eventos de solo agregar en formato versionado, conservación de eventos desconocidos e identidad estable del dispositivo y del usuario local.

### Modified Capabilities
- `app-shell`: el requisito "Datos en Memoria Temporales" pasa a ser "Datos Guardados y Billeteras de Ejemplo": la app usa la base local, los datos sobreviven al cierre y las billeteras de ejemplo se crean solo si la base está vacía.

## Impact
- **Nuevo módulo** `core/data-local` (SQLDelight, SQLCipher, kotlinx.serialization) con los repositorios persistentes y sus pruebas en la JVM.
- **`core/data-memory`** se mantiene para pruebas y como referencia: ambos adaptadores pasan la misma batería de pruebas de contrato.
- **`core/domain`**: forma canónica de `ExchangeRate` (sin dependencias nuevas; el dominio sigue sin frameworks).
- **`composeApp`**: `AppContainer` recibe los repositorios persistentes; `CoinDenApplication` abre la base cifrada y conserva las identidades.
- **Build**: `gradle/libs.versions.toml` (SQLDelight, SQLCipher para Android, AndroidX SQLite, kotlinx.serialization y su plugin). El APK crece algunos MB por las librerías nativas de SQLCipher.
- **Docs**: `MANIFIESTO.md` (principio 6), `AGENTS.md` (regla 3, arquitectura y comandos), `openspec/config.yaml` y la sección "Desarrollo" del README.
