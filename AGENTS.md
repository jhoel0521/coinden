# AGENTS.md

Instrucciones para cualquier agente de IA (Claude Code, Antigravity, Codex, etc.) que trabaje en este repo.

Antes de nada, lee [MANIFIESTO.md](MANIFIESTO.md) para conocer las reglas del proceso y los principios del producto, y [README.md](README.md) para saber qué es CoinDen.

## Restricciones duras

Si una tarea las contradice, detente y pregunta.

1. **Sin servidores de CoinDen ni de terceros.** No agregues backend propio, telemetría o analítica propias, publicidad ni APIs de IA remotas. Un SDK de terceros que envíe métricas técnicas de uso (nunca imágenes, textos, montos ni datos del usuario) solo se usa como adaptador opcional, activado por la persona tras un aviso claro, y declarado en la pantalla de privacidad. La red solo se usa para: (a) consultar las cotizaciones públicas que el usuario configuró, (b) sincronizar P2P entre sus propios dispositivos, (c) en el futuro, el respaldo cifrado opcional en la nube del propio usuario, y (d) descargar paquetes de idioma, plantillas de comprobantes y modelos on-device (datos públicos y opcionales; nunca se envían datos del usuario). Las opciones (b) y (c) son siempre explícitas y van cifradas de extremo a extremo.
2. **El dominio es determinista.** Parser, categorizador y cálculos son lógica pura que se puede probar en `core/domain`. Los modelos (voz a texto, OCR y modelos de lenguaje en el teléfono, del sistema o descargados) se usan solo como adaptadores detrás de un puerto. Voz y OCR producen texto para el `TextDraftInterpreter`; un modelo de lenguaje solo puede devolver propuestas con el mismo contrato del intérprete, que el dominio valida igual que las de las reglas; interpreta primero cuando está disponible, las reglas validan sus propuestas y son el respaldo, y si no coinciden el borrador queda para revisar. Las palabras de cada idioma y los formatos de cada banco viven en paquetes y plantillas como datos, nunca en el código.
3. **Dinero exacto y encapsulado.** Opera solo con `Money`: `Money + Money` y `Money - Money` en la misma moneda, y `Money * ExchangeRate` para cruzar monedas (por ejemplo `bs + (usd * usdToBs)`). Nunca leas su representación interna ni uses `Double` para dinero o tasas; un desbordamiento lanza `ArithmeticException`. Para persistir, usa la forma canónica exacta: el monto con su moneda (`"1188.00"` + `BOB`) y la tasa como fracción de unidades mínimas (`ExchangeRate.toCanonical()`, por ejemplo `"112/11"`).
4. **Categoría ≠ medio de pago ≠ billetera.** La *categoría* dice en qué se gastó (Comida). El *medio* ("QR", "Pix", "efectivo", "tarjeta") no nombra una billetera: filtra las candidatas por tipo y moneda (todos los bancos cobran por QR). Solo un *alias* ("bnb", "yape", "meru") o un comprobante identifican la billetera.
5. **Capturar ≠ confirmar.** Un borrador no afecta saldos hasta que el usuario lo confirma.
6. **Sync-ready.** Las entidades usan IDs UUID (nunca autoincrementales); los movimientos son append-only y una corrección es un nuevo evento, no se pisa el anterior. Nada del dominio puede asumir un solo dispositivo.
7. **Dominio sin idioma.** En `core/domain` no hay textos para el usuario ni formatos regionales (separador decimal, símbolo, fechas): solo enums, códigos e IDs. Traducir y formatear es tarea de la capa de presentación. Los nombres que escribe el usuario, como el de una billetera, se guardan tal cual.

## Flujo SDD con OpenSpec

```
  opsx-explore  -->  opsx-propose  -->  opsx-apply  -->  opsx-archive
  (pensar)           (proposal,         (TDD sobre       (deltas -> spec
                      specs, design,     tasks.md)        principal)
                      tasks)
```

- No implementes nada sin un cambio activo en `openspec/changes/`.
- Los specs vigentes están en `openspec/specs/`. Léelos con `openspec show <id> --type spec`.
- Escribe los artefactos de OpenSpec **en español**.
- Las skills de OpenSpec están en `.agents/skills/`. Claude Code las ve a través de la junction local `.claude/skills → .agents/skills` (está en `.gitignore`; en un clon nuevo hay que volver a crearla).

## Arquitectura

```
core/domain/src/
  commonMain/kotlin/com/coinden/core/domain/
    model/          value objects y entidades (Money, Currency, ...)
    extractor/      lógica pura de extracción (JSON, HTML)
    port/output/    SPI: repositorios, fetchers (interfaces)
    usecase/        puertos de entrada / casos de uso
  commonTest/kotlin/com/coinden/core/domain/
    unit/           pruebas de lógica pura
    feature/        pruebas end-to-end por persona + FakeAdapters
```

```
core/data-memory/   adaptadores en memoria de los puertos (pruebas y referencia)
core/data-local/    base local cifrada: SQLDelight + SQLCipher, eventos versionados (sobre + JSON)
  jvmTest/          batería de contrato que corren memoria y base local por igual
composeApp/         app Compose Multiplatform (hoy solo Android)
  commonMain/       UI, presentadores sin Compose y AppContainer (DI manual)
  androidMain/      Application, MainActivity
  commonTest/       pruebas de presentadores y formato (corren en la JVM)
```

- `core/domain` no depende de Android, Ktor, SQLite ni de ningún framework. Solo usa stdlib y coroutines.
- La UI no tiene textos fijos: todo sale de `composeResources/values/strings.xml`.
- Las dependencias nuevas se declaran en `gradle/libs.versions.toml`.

## Pruebas (TDD)

- Primero la prueba, después el código.
- Cada escenario WHEN/THEN de un spec tiene su prueba.
- Las pruebas de feature usan personas: John (USA), Carlos (Bolivia), Facundo (Argentina), Sophie (Francia) y Wei (China). Si una capacidad nueva lo pide, agrega casos a la persona que corresponda en lugar de inventar datos sueltos.
- Los adaptadores falsos viven en `feature/FakeAdapters.kt`.

## Comandos

| Acción | Comando |
|--------|---------|
| Pruebas | `npm test` (= `./gradlew :core:domain:jvmTest :core:data-memory:jvmTest :core:data-local:jvmTest :core:data-local:verifySqlDelightMigration :composeApp:testDebugUnitTest`) |
| Pruebas de cifrado (emulador) | `./gradlew :core:data-local:connectedDebugAndroidTest` |
| APK de depuración | `npm run build:android` (= `./gradlew :composeApp:assembleDebug`) |
| Lint | `npm run lint` (= `./gradlew ktlintCheck`) |
| Formato | `npm run format` (= `./gradlew ktlintFormat`) |

En Windows usa `.\gradlew.bat`.

## Commits

- Usa Conventional Commits **en español**: `tipo(área): descripción`.
- Tipos permitidos: `feat fix refactor docs test chore perf style ci revert`.
- El hook `pre-commit` formatea Kotlin y el hook `commit-msg` valida el mensaje con commitlint. No los saltes con `--no-verify`.
- Haz commit solo cuando la persona lo pida.
