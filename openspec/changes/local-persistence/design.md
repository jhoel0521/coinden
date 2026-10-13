# Design: Datos Guardados y Cifrados en el Teléfono

## Context
Ver proposal.md (Why) y los specs `local-persistence` y `app-shell`. Hoy la app usa `core/data-memory` (5 repositorios con `Mutex`) armados en `AppContainer`; `CoinDenApplication` genera un id de dispositivo y un dueño nuevos en cada arranque y siembra la demo siempre. Los puertos de almacenamiento son `WalletRepositoryPort`, `DraftRepositoryPort`, `WalletAliasRepositoryPort`, `CategorizationRepositoryPort` y `CurrencyProfileRepositoryPort`; todos son `suspend` y sus consultas filtran por id, billetera, borrador, movimiento o dueño, nunca por montos. El dominio no tiene dependencias de frameworks y no debe tenerlas. `Money` y `ExchangeRate` encapsulan su representación interna (`minorUnits`, `numerator`/`denominator` son `internal`); `Money` ya tiene forma canónica pública (`parseCanonical`, `toNumericString`), `ExchangeRate` no.

## Goals / Non-Goals

**Goals:**
- Un adaptador persistente intercambiable con el de memoria, verificado con la misma batería de contrato.
- Un formato de eventos que se pueda enviar tal cual por sync P2P y respaldo, y que tolere versiones mezcladas.
- Pruebas en la JVM sin emulador para todo salvo el cifrado y la llave, que se verifican en el emulador.

**Non-Goals:**
- Migraciones de datos entre versiones de esquema (la versión 1 es la primera; las siguientes usarán `.sqm`).
- Rendimiento con cientos de miles de eventos (snapshots y tablas de lectura quedan para cuando se midan).
- Consultas SQL sobre montos o categorías: los reportes siguen calculándose con el dominio.

## Decisions

### D1. Bibliotecas
| Pieza | Versión | Motivo |
|---|---|---|
| SQLDelight | 2.0.2 | KMP (Android hoy, Desktop/iOS después), SQL explícito y tipado, migraciones `.sqm` |
| SQLCipher for Android | `net.zetetic:sqlcipher-android` 4.6.x | AES-256 de toda la base, licencia BSD, usado por Signal; se conecta a SQLDelight vía `SupportOpenHelperFactory` |
| AndroidX SQLite | 2.4.x | requerido por SQLCipher y por el driver Android de SQLDelight |
| kotlinx.serialization JSON | 1.7.3 + plugin `org.jetbrains.kotlin.plugin.serialization` 2.0.21 | JSON del contenido de los eventos, multiplataforma |
| Driver JVM de SQLDelight | `app.cash.sqldelight:sqlite-driver` 2.0.2 | pruebas de contrato en la JVM (sin cifrado) |

Alternativas descartadas: Room KMP (más acoplado a Android y su soporte multiplataforma es más reciente), Realm (discontinuado), ObjectBox (el cifrado es comercial) y archivos JSON propios (habría que inventar atomicidad, índices y cifrado). Si alguna versión no compila con Kotlin 2.0.21/AGP 8.5.2 se ajusta la versión menor y se documenta aquí.

### D2. Módulo `core/data-local`
KMP con `jvm` y `androidTarget`; depende de `:core:domain`. Contiene el esquema `.sq`, los repositorios persistentes, el mapeo dominio ↔ JSON y, en `androidMain`, la apertura de la base cifrada y la llave. `jvmTest` depende además de `:core:data-memory` para correr la batería de contrato contra ambos. El dominio no se entera: sigue viendo puertos.

### D3. Esquema: sobre consultable + contenido JSON
```
events(
  seq        INTEGER PRIMARY KEY AUTOINCREMENT,  -- solo orden local de inserción; nunca sale del dispositivo ni identifica nada
  id         TEXT NOT NULL UNIQUE,               -- UUID del evento (identidad real, sync-ready)
  stream     TEXT NOT NULL,                      -- 'ledger' | 'draft' | 'classification'
  owner_id   TEXT,
  device_id  TEXT NOT NULL,
  occurred_at INTEGER, recorded_at INTEGER NOT NULL,
  type       TEXT NOT NULL,                      -- 'ledger.expense', 'draft.interpreted', ...
  version    INTEGER NOT NULL,                   -- versión del formato del contenido
  payload    TEXT NOT NULL                       -- JSON del evento completo
)
event_refs(event_id TEXT NOT NULL, ref TEXT NOT NULL, PRIMARY KEY(event_id, ref))  -- billetera(s), borrador o movimiento
wallets(id TEXT PRIMARY KEY, owner_id TEXT NOT NULL, payload TEXT NOT NULL)
wallet_aliases(owner_id, alias, wallet_id, PRIMARY KEY(owner_id, alias))
default_wallets(owner_id TEXT PRIMARY KEY, wallet_id TEXT)
category_rules(owner_id, rule_key, payload, PRIMARY KEY(owner_id, rule_key))
custom_categories(id TEXT PRIMARY KEY, owner_id TEXT NOT NULL, payload TEXT NOT NULL)
currency_profiles(user_id TEXT PRIMARY KEY, payload TEXT NOT NULL)
app_identity(name TEXT PRIMARY KEY, value TEXT NOT NULL)                         -- device_id, owner_id
```
- `event_refs` resuelve que una transferencia pertenezca a dos billeteras (`eventsForWallet` busca por ref) sin columnas nulas por tipo.
- Las consultas leen por `seq` ascendente: devuelven los eventos en orden de inserción, como el adaptador en memoria.
- Sobre `events` y `event_refs` solo existen sentencias `INSERT` y `SELECT`; no hay `UPDATE` ni `DELETE` en el `.sq`. Un id repetido viola `UNIQUE` y se traduce al mismo rechazo que en memoria. El evento y sus refs se insertan en una transacción.
- Billeteras, alias, reglas, categorías y perfil son estado configurable que el dominio ya reemplaza por completo (`saveWallet`, `setAliases`, `saveRule`...): se guardan con *upsert*, igual que hoy en memoria.

Alternativas: tablas por tipo de evento (descartada: `DraftEvent.Interpreted` anida una `MovementProposal` de 6 tipos; cada cambio del dominio sería una migración SQL en varias tablas y un dispositivo viejo no podría guardar tipos nuevos) y una sola tabla clave-valor sin sobre (descartada: sin índices por billetera o borrador).

### D4. Mapeo dominio ↔ JSON con DTOs propios
El dominio no lleva anotaciones de serialización (no puede depender de kotlinx.serialization). `core/data-local` define DTOs `@Serializable` por tipo de evento y por objeto (billetera, propuesta, perfil, estrategia de tasa) y funciones `toDto()`/`toDomain()`. El `type` y la `version` van en el sobre, no dentro del JSON, para poder filtrar sin parsear.
- **Dinero**: `{"value": "45.00", "currency": {...}}`, con `value` de `Money.toNumericString()` y lectura con `Money.parseCanonical`. La moneda va completa (código, decimales y, si es personalizada, símbolo y nombre) porque el código solo no basta para una moneda propia.
- **Tasas**: el dominio gana `ExchangeRate.toCanonical(): String` y `ExchangeRate.parseCanonical(text, from, to)` con la fracción exacta de enteros, reducida: "vendí 11 USD, me dieron 112 BOB" se guarda como `"112/11"`, nunca como el decimal `10.1818…`. Es un string, como `Money`: la representación interna sigue encapsulada.
- **Versión 1** para todos los tipos. Una versión futura agrega su lector (*upcasting*) en el adaptador; las pruebas guardan JSON de ejemplo de cada versión para no romper la lectura de datos viejos.

### D5. Eventos desconocidos
Al leer, una fila cuyo `type` no está en el registro de lectores o cuya `version` es mayor que la soportada se omite del resultado (no se lanza error) y la fila no se toca. Así un dispositivo viejo podrá guardar y reenviar eventos nuevos cuando llegue el sync. Se prueba insertando una fila cruda con un tipo inventado.

### D6. Llave y apertura en Android
- Al primer inicio: 32 bytes de `SecureRandom` como passphrase de SQLCipher. Se cifra con una llave AES-256-GCM no exportable creada en `AndroidKeyStore` (alias `coinden.db`) y se guarda `iv + ciphertext` en un archivo del almacenamiento privado (`files/db.key`). En los inicios siguientes se descifra con la misma llave del Keystore.
- La base se abre con `AndroidSqliteDriver(CoinDenDatabase.Schema, context, "coinden.db", factory = SupportOpenHelperFactory(passphrase))` y la passphrase se limpia del arreglo después de abrir.
- `allowBackup=false` ya está en el manifiesto: ni la base ni la llave cifrada salen en respaldos automáticos (y la llave del Keystore no se podría restaurar en otro equipo).
- Alternativas: `EncryptedSharedPreferences` (androidx.security-crypto está deprecada) y derivar la llave de un PIN (es el cambio aparte del PIN).

### D7. Identidad estable
`app_identity` guarda `device_id` y `owner_id` (UUID) dentro de la base cifrada; se crean la primera vez y se leen siempre después. `FixedDeviceIdentity` recibe ese valor y `AppContainer` recibe el `ownerId` en lugar de generarlo. Si la base se pierde, se pierden con ella, lo que es coherente: un dispositivo nuevo.

### D8. Concurrencia
Los drivers de SQLDelight son síncronos; cada método del repositorio corre en un `CoroutineDispatcher` inyectado (`Dispatchers.IO` en Android, el de prueba en la JVM). Las escrituras de varias filas usan `transaction {}`.

### D9. App y demo
- `CoinDenApplication` abre la base, lee o crea la identidad, arma `AppContainer` con los repositorios persistentes y siembra la demo solo si el dueño no tiene billeteras. Abrir y sembrar se hace con `runBlocking` al iniciar (milisegundos con una base pequeña); pasar a carga asíncrona queda para `onboarding-wallets-ui`.
- `CaptureDemoPresenter` agrega `load()`: al abrir lee los borradores pendientes (`GetDraftInboxUseCase`) y los muestra, más recientes primero.
- `AppContainer` recibe los repositorios por constructor, así las pruebas siguen usando los de memoria.

### D10. Pruebas
- **Contrato** (`core/data-local/jvmTest`): clases abstractas por puerto con los casos del spec (orden, id repetido, upsert, alias, eventos por billetera incluida la transferencia), con dos implementaciones: memoria y SQLDelight sobre `JdbcSqliteDriver` en memoria.
- **Ida y vuelta**: un evento de cada tipo y perfiles con cada estrategia de tasa, incluidos satoshis y la tasa 1/3.
- **Reapertura**: base en archivo temporal; se cierra el driver y se abre otro sobre el mismo archivo (datos, identidad y orden se conservan).
- **Eventos desconocidos**: fila cruda con tipo inventado.
- **Feature Carlos**: el día de Carlos (billeteras, capturas, clasificación) con la base persistente, cerrando y reabriendo entre la mañana y la noche.
- **Cifrado y llave** (`androidInstrumentedTest`, en el emulador, no en CI): el archivo no empieza con `SQLite format 3`, no contiene en texto plano una nota capturada ni el nombre de una billetera, y la app reabre la base con la llave guardada.

### D11. Principio 6
Se unifica la redacción en `MANIFIESTO.md` (principio 6), `AGENTS.md` (regla 3) y `openspec/config.yaml`: "Dinero exacto: nunca flotantes; en memoria el dinero son enteros en unidades mínimas y se persiste en forma canónica exacta (dígitos y moneda), equivalente a esas unidades".

## Risks / Trade-offs
- [Las migraciones del contenido pasan a Kotlin] → `version` por evento desde el día 1 y pruebas con JSON de cada versión.
- [SQL no valida el contenido JSON] → el dominio valida antes de guardar; las pruebas de ida y vuelta cubren todos los tipos.
- [SQLCipher agrega algunos MB al APK y un costo de CPU pequeño] → aceptable para datos financieros; se mide el tamaño del APK en la verificación.
- [El cifrado no se prueba en CI] → prueba instrumentada local y verificación en el emulador antes de archivar.
- [Si se borran los datos de la app o se pierde el Keystore, los datos son irrecuperables] → es el comportamiento esperado sin respaldo; lo resuelve `cloud-backup`.
- [`runBlocking` al iniciar] → base pequeña y local; se revisa al llegar las pantallas reales.
- [`saveWallet` reemplaza la billetera (no es evento)] → igual que hoy; si el sync lo necesita, `device-sync` convertirá los cambios de billetera en eventos.

## Migration Plan
No hay usuarios ni datos que migrar. Al instalar esta versión sobre la demo anterior se crea la base vacía y se siembra la demo. Volver atrás es reinstalar la versión anterior (los datos en memoria nunca existieron en disco).

## Open Questions
- Ubicación de la base cuando lleguen Desktop e iOS (cada plataforma tendrá su driver y su almacén de llaves; no cambia el formato).
