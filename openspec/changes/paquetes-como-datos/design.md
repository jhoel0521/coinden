# Design

## Context

- **El puerto ya existe.** `InstalledPacksPort` (`core/domain/port/output/InterpreterPorts.kt`) expone `languagePacks()` y `receiptTemplates()`. El dominio ya recibe los paquetes por parámetro: `Readers.kt`, `WalletNameHints.kt` y `Categorizer.kt` leen de `LanguagePack`, nunca de una constante global.
- **La deuda está en los adaptadores y en las constantes.** `LanguagePacks.ENGLISH/SPANISH/PORTUGUESE` (320 líneas de palabras) y `ReceiptTemplates.all` (5 plantillas) son constantes de Kotlin en `core/domain`. El adaptador real (`composeApp/.../AppLanguage.kt:25-32`) solo elige entre esas constantes con un `when`.
- **Consecuencia:** el dominio, que según el Manifiesto §9 no debe tener textos de idioma, hoy contiene cientos de palabras en es, en y pt.

## Inventario: qué hace hoy cada lista y quién la usa

| Lista | Para qué sirve | Quién la lee |
|---|---|---|
| `amountLabels`, `counterpartyLabels`, `accountLabels`, `noteLabels`, `referenceLabels` | Encontrar los campos en un comprobante sin plantilla | `Readers.kt` (lector genérico) |
| `expenseWords`, `incomeWords`, `withdrawalWords`, `exchangeWords` | El tipo de movimiento de una frase | `Readers.kt` (lector de frases) |
| `notePrepositions`, `conjunctions`, `rateMarkers`, `receivedMarkers` | Cortar la frase: nota, varios movimientos, tasa, lo recibido | `Readers.kt` |
| `months` | Leer fechas escritas con el mes en letras | `Readers.kt` (`DateTimeReader`) |
| `currencyWords` | "dolares" → USD; null = la del locale | `Readers.kt`, `WalletNameHints.kt` |
| `paymentMethodWords` | "qr", "efectivo" → filtrar billeteras candidatas | `Readers.kt`, `WalletNameHints.kt` |
| `genericWalletWords` | "banco", "cuenta": no sirven como apodo de billetera | `WalletNameHints.kt` |
| `categoryWords` | "almuerzo" → Comida | `Categorizer.kt` |
| `ReceiptTemplates` (5) | Yape, y tres formatos del Banco Económico | `TemplateReader` en `Readers.kt` |

**Ninguna se elimina en este cambio**: todas pasan tal cual de Kotlin a JSON, con los mismos valores. Este cambio no decide qué listas sobreviven a largo plazo; eso lo decide `interprete-por-modelo`.

## Goals / Non-Goals

**Goals:**
- Que el dominio no contenga ni una palabra de ningún idioma.
- Que agregar un idioma o el formato de un banco sea agregar un archivo.
- Comportamiento idéntico: las mismas 408 pruebas pasan con los mismos valores.

**Non-Goals:**
- Cambiar qué entiende el intérprete, ni agregar o quitar palabras.
- El modelo de lenguaje y el validador: son del cambio `interprete-por-modelo`.
- La interfaz para explorar y descargar paquetes: aquí solo se define el formato, la carga y la verificación.

## Decisions

### 1. Formato: JSON con versión

Un archivo por paquete (`es.json`, `en.json`) y uno por plantilla o grupo de plantillas (`bo-banco-economico.json`). Cada archivo abre con la versión del formato:

```json
{
  "formatVersion": 1,
  "code": "es",
  "amountLabels": ["importe", "monto", "total"],
  "months": { "ene": 1, "enero": 1 },
  "currencyWords": { "dolares": "USD", "pesos": null },
  "categoryWords": { "almuerzo": "FOOD" }
}
```

- **Por qué JSON y no otra cosa:** ya hay que leer JSON en el proyecto (los eventos de la base se guardan como JSON), es legible para quien traduzca y no agrega un formato más.
- **`formatVersion`:** un archivo con una versión mayor que la que la app entiende se ignora, en vez de fallar. Es lo que permite publicar paquetes nuevos sin romper apps viejas.
- **Las palabras se guardan normalizadas** (sin tildes, en minúscula), como hoy. Una prueba lo verifica para cada archivo, así un paquete mal escrito se detecta al construir, no en el teléfono.

### 2. Dónde vive cada archivo

| Origen | Qué trae | Cuándo está |
|---|---|---|
| Recursos de la app | `en.json` y las plantillas base | Siempre, desde el primer arranque, sin red |
| Carpeta de datos de la app | Los paquetes y plantillas que la persona descargó | Tras descargarlos y verificar su huella |

El paquete inglés va en recursos porque las specs vigentes lo dan por siempre disponible ("El paquete inglés está siempre disponible").

### 3. Carga: un adaptador, no el dominio

`InstalledPacksPort` no cambia de forma. Lo que cambia es quién lo implementa:

```
Recursos (en.json, plantillas base)  ─┐
                                      ├─> PackLoader ─> InstalledPacksPort ─> dominio
Descargados (es.json, pt.json, ...)  ─┘   (parsea, valida, descarta inválidos)
```

**Dónde vive:** en un módulo nuevo, `core/data-packs`, al lado de `core/data-memory` y `core/data-local`. No puede vivir en `core/domain` porque `AGENTS.md` prohíbe que el dominio dependa de un framework, y leer JSON necesita `kotlinx.serialization`. El dominio solo conserva las `data class` y el puerto.

Las pruebas de `core/domain` que hoy usan las constantes cargan los archivos reales con `testImplementation(project(":core:data-packs"))`: es una dependencia **de prueba**, así que el dominio de producción sigue sin framework.

- El `PackLoader` parsea, verifica `formatVersion` y descarta lo que no entiende, registrando el motivo.
- El dominio sigue recibiendo `List<LanguagePack>` y `List<ReceiptTemplate>`: **no se entera de que ahora vienen de archivos**. Por eso las pruebas de `Readers.kt` no cambian.
- `LanguagePack.kt` y `ReceiptTemplate.kt` conservan sus `data class` y pierden los `object LanguagePacks` y `object ReceiptTemplates`.

### 4. Descarga y verificación: el camino que ya existe

Los modelos de voz ya resolvieron esto en `evidence-perception` ("Modelos Descargables y Verificados"): se pide, se muestra el tamaño, se descarga de una dirección pública fija, se verifica la huella y un archivo que no coincide se borra. Los paquetes y plantillas usan **el mismo mecanismo y las mismas garantías**, sin inventar otro.

### 5. Las pruebas son la red de seguridad

El riesgo real de este cambio es perder una palabra en la mudanza. Se cubre así:

- Las pruebas que hoy usan `LanguagePacks.SPANISH` pasan a cargar `es.json` **y no cambian ninguna aserción**. Si falta una palabra, fallan.
- Una prueba compara el contenido de cada archivo contra la lista esperada de claves, para que un `.json` incompleto no pase desapercibido.
- Una prueba verifica que ningún archivo de `core/domain` contenga listas de palabras, para que la deuda no vuelva a entrar.

## Risks / Trade-offs

- **Perder palabras al mudarlas:** se mitiga con las pruebas de arriba; la mudanza es mecánica y verificable.
- **Leer archivos al arrancar cuesta tiempo:** son archivos de decenas de KB y se leen una vez. Se cargan al iniciar y se mantienen en memoria, como hoy.
- **Un paquete descargado puede traer palabras malas** (una traducción errónea). No afecta datos: el intérprete solo propone, y nada toca saldos sin que la persona confirme.
- **Dependencia nueva de serialización:** se declara en `gradle/libs.versions.toml`; es la misma que ya se usa para los eventos.
