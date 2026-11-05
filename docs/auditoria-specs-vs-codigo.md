# Auditoría: specs contra código

Fecha: 4 de noviembre de 2026. Rama de trabajo: `release/0.2.2` (sobre `main` en `603beee`, la 0.2.1).

## Alcance y método

- **Specs revisados:** los 21 de `openspec/specs/` (169 requisitos, 499 escenarios) y los 5 cambios abiertos de `openspec/changes/`.
- **Código:** `core/domain`, `core/data-local`, `core/data-memory`, `composeApp`, recursos de texto, workflows de CI y `site/`.
- **Comprobaciones hechas:**
  1. Las 7 restricciones duras de `AGENTS.md`, con búsquedas en el código.
  2. Validación estructural de los specs (`openspec validate --specs --strict`).
  3. Trazabilidad escenario → prueba, por nombre de prueba y por contenido (script en `build/trace*.json`, no versionado).
  4. Comprobación manual de 18 escenarios dudosos y de las constantes numéricas que prometen los specs.
  5. Specs de cambios abiertos contra los specs principales.
- **Límite:** la trazabilidad por nombre es una heurística. Una prueba con otro nombre no aparece. Lo que dice "sin evidencia" significa "no la encontré", no "no existe".

## Resumen

| Tema | Resultado |
|---|---|
| Restricciones duras de `AGENTS.md` | 7 de 7 se cumplen (con 2 notas) |
| Escenarios con prueba evidente | 402 de 499 (81 %) |
| Escenarios sin prueba por nombre | 97; 69 tampoco por contenido; de 18 revisados a mano, 16 sí tienen prueba y los 2 restantes **ya tienen prueba** (ver Cierre) |
| Specs que pasan `--strict` | 14 de 21 (los 7 restantes solo tienen avisos de estilo, ningún error) |
| Capacidades en el código sin spec principal | 5 (por cambios sin archivar) |
| Incumplimientos reales de un spec | 2 (texto cortado en inglés y portugués); **corregidos en la 0.2.2** |

## 1. Restricciones duras (`AGENTS.md`)

| # | Regla | Resultado | Evidencia |
|---|---|---|---|
| 1 | Sin servidores ni telemetría propios | Cumple | Sin Firebase, Crashlytics, Sentry ni analítica en el código. `INTERNET` se usa para descargas públicas (`VerifiedDownload`, `PackStore`). `allowBackup="false"`. |
| 2 | Dominio determinista; idiomas como datos | Cumple, con una nota | No hay palabras de idioma en el código del dominio, solo en comentarios. Nota: `VerifiedSourceDirectory` (`AnchorCurrencies.kt`) deja en `core/domain` las direcciones de DolarApi, Frankfurter, CoinGecko y DolarBlueBolivia. Son datos, no lógica; ningún código las consulta. |
| 3 | Dinero exacto | Cumple | Cero `Double`, `Float`, `toDouble` o `toFloat` en `core/domain/commonMain`. |
| 4 | Categoría ≠ medio ≠ billetera | Cumple | Cubierto por `payment-method-hints` y pruebas de `capture-matching`. |
| 5 | Capturar ≠ confirmar | Cumple | El borrador no cambia totales hasta confirmar; se vio en el emulador en es, en y pt. |
| 6 | Sync-ready | Cumple, con una nota | `events.id` es UUID. `events.seq AUTOINCREMENT` es solo orden local y el propio `CoinDen.sq` lo dice. |
| 7 | Dominio sin idioma | Cumple | Sin imports de `android`, `ktor`, `sqldelight`, `Locale` ni `NumberFormat` en `core/domain/commonMain`. |

Constantes que prometen los specs y que existen en el código: bloqueo a 1 minuto (`RELOCK_AFTER_MS = 60_000`), 3 reintentos (`QueueRunner.maxAttempts = 3`), audios de 5 minutos (`MAX_AUDIO_SECONDS`), recomendación de modelo por memoria (`ModelRecommender`, 4 GB y 8 GB, con 7 pruebas).

## 2. Cambios abiertos y specs desactualizados

Los specs principales no reflejan lo que ya hace la app porque 5 cambios no se archivaron. Su código sí existe.

| Cambio | Estado | Capacidad que falta en `openspec/specs/` | Specs que modifica |
|---|---|---|---|
| `paquetes-como-datos` | Completo, sin archivar | `language-packs` | `text-interpreter` |
| `contexto-de-captura` | 24 de 25 (falta 8.1, prueba en emulador) | `capture-bundle` | 6 specs |
| `eleccion-del-lector` | 13 de 15 (faltan 2.4 y 5.2) | — | `app-onboarding`, `evidence-perception` |
| `interprete-por-modelo` | 11 de 28, en curso | `intent-interpreter` | 3 specs |
| `privacidad-y-nombres-neutros` | 25 de 34 | `data-erasure`, `project-site` | 5 specs |

Acción recomendada, en este orden: archivar `paquetes-como-datos` ya (está completo); cerrar los pasos de emulador de `contexto-de-captura` y `eleccion-del-lector` y archivarlos; terminar `privacidad-y-nombres-neutros` tras la 0.2.2; dejar `interprete-por-modelo` abierto porque es trabajo a medias. Ninguno se archiva sin tu aceptación (`MANIFIESTO.md`, punto 4).

## 3. Código que incumple un spec

1. **Texto cortado en botones.** `ui-components` dice que la etiqueta "se lea entera, sin cortarse con «…»". Incumplen:
   - `CdToolbar` (`Actions.kt`) usa `maxLines = 1` con elipsis y reparte 4 acciones en una fila: en portugués se ve "Dit…", "Es…", "Ad…".
   - El botón "Quitar" de la pantalla de contexto recibe el 25 % del ancho: en inglés se parte en "Remov / e" y en portugués en "Remov / er".
   Lo corrige la 0.2.2 (tareas 7.2 y 7.3, ya redactadas en el cambio).
2. **Apóstrofe con barra visible en inglés** (`Today\'s total`, 5 textos). No tenía spec; ahora lo tiene (`ui-components`, escenario "Textos con apóstrofe") y una prueba que lo vigila.

## 4. Spec que dice otra cosa que el código

| Spec | Dice | El código hace |
|---|---|---|
| `app-navigation`, "Lo que hay en Perfil" | "Billeteras y tasas" y sin "Borrar mis datos" | "Fuentes y equivalencias" y con "Borrar mis datos" (el delta está en el cambio sin archivar) |
| `app-shell`, "Unir un pull request" | La CI no vuelve a correr al unir | Hay un workflow de CI nuevo por tipo de cambio (PR #19) y otro de publicación; **no hubo cambio de OpenSpec** para ese trabajo |
| Varios specs | billetera, saldo, tasa | fuente, total, equivalencia (`openspec/glosario.md` lo traduce; los specs no se reescribieron) |
| `app-onboarding`, "Cambiar el saludo sin perder la pantalla" | Los textos siguen la selección | Tras reiniciar a mitad del onboarding, el texto sale en inglés pero el selector marca "Español" |

## 5. Escenarios sin prueba evidente

De los 69 sin evidencia por nombre ni por contenido:

- **Se verifican en emulador o con el dispositivo, no con prueba unitaria** (la mayoría): compilar el APK, máscara redonda del ícono, "sin llave en la máquina", la versión optimizada, modo avión, nota de voz de WhatsApp, PDF de la banca, tema oscuro, tamaño de letra, rotar con el formulario a medias.
- **De comportamiento, revisados a mano y con prueba** (nombres distintos al del escenario): bloqueo con la app, doble toque al guardar, selector web caído, cotización sin coma flotante, copia del archivo sin la llave, inicios siguientes, apagar el lector, categorizar una transferencia, ajuste posterior sin hueco.
- **Para confirmar** (no encontré prueba por ninguno de los dos caminos): `movement-correction` "Lectura que nadie aprobó" (cambio pendiente no mueve el saldo) y `capture-ui` "Salida durante guardado".

## 6. Hallazgos para Play y la percepción

- **Cripto en el código:** la interfaz no dice "cripto" en ningún texto, pero `WalletSettingsPresenter` acepta escribir `BTC` en "Agregar otra moneda", el dominio define `Currency.BTC` y una fuente CoinGecko, y el spec `currency-configuration` tiene un requisito de criptos. Es funcional, no visible.
- **Paquetes públicos con bancos reales:** `site/packs/*.json` incluye `yape`, `nubank`, `pix` y plantillas de bancos bolivianos (`template-bo-be-*`). Son datos públicos y coherentes con el spec; el riesgo de percepción es bajo.
- **README raíz:** menciona Meru, bancos, tasas y billeteras, pero es documentación de desarrollo.

## 7. Acciones sugeridas

1. Hacer la 0.2.2 (en curso): tareas 7.2 y 7.3 del cambio activo.
2. Archivar `paquetes-como-datos`; cerrar y archivar `contexto-de-captura` y `eleccion-del-lector`.
3. Abrir un cambio de OpenSpec para la CI por tipo de cambio y actualizar el escenario de `app-shell`.
4. Añadir la prueba de "cambio pendiente no mueve el saldo" y la de "salida durante guardado" si no existen.
5. Decidir si `BTC` sigue siendo escribible desde "Agregar otra moneda".
6. Reducir los avisos de estilo de los 7 specs que fallan `--strict`.

## 8. Cierre en la 0.2.2 (4 de noviembre de 2026)

| Pendiente de la auditoría | Estado |
|---|---|
| Texto cortado en botones (sección 3) | **Cerrado.** "Quitar/Remove/Remover" se lee en una línea en es, en y pt; la barra de captura reparte máximo 3 botones por fila (`CaptureBarLayoutTest`). Verificado en el emulador (1080x1920, 420 dpi). |
| Apóstrofe con barra en inglés | **Cerrado.** Prueba en `VisibleVocabularyTest`. |
| Archivar `paquetes-como-datos` | **Hecho.** Archivado; nacen `language-packs` y cambia `text-interpreter`. |
| Archivar `contexto-de-captura` y `eleccion-del-lector` | **Hecho**, con la nota "No verificado por adb: compartir imagen/audio con ShareActivity…" en su `tasks.md`. Siguen pendientes de prueba manual en un teléfono real: compartir imagen y audio, reabrir desde Revisar y Separar (8.1), abrir el enlace de datos de Google (2.4) y medir que no se encolen eventos sin el lector (5.2). |
| `interprete-por-modelo` | Sin tocar (11 de 28), fuera de la 0.2.2. |
| `privacidad-y-nombres-neutros` | **Abierto**: faltan 5.4 (la hace la persona al compilar el AAB), 5.5 (validar antes de archivar) y 7.4 (subir las capturas 03 de en y pt a Play). Las capturas ya están en `docs/play/screenshots/`. |
| Cambio de OpenSpec para la CI (PR #19 y #21) | **Hecho**: `ci-por-tipo-de-cambio`. Aclaración: el escenario "Unir un pull request" **sí coincide** con el código (`ci.yml` solo escucha `pull_request`); lo que faltaba eran los escenarios de solo documentación, borrador y versión por etiqueta. |
| `movement-correction` "Lectura que nadie aprobó" | **Con prueba**: `lectura que nadie aprobo de 35 a 32 no mueve el saldo de BNB Bs` (`CarlosSameMovementFeatureTest`). El comportamiento ya existía, así que la prueba nació en verde. |
| `capture-ui` "Salida durante guardado" | **Con prueba**: `Carlos sale durante el guardado y el progreso sigue hasta que termina la escritura` (`DraftFormWriteTest`). Ya existía el comportamiento (`FormExit.BLOCKED`). |
| "Borrar mis datos" (2.5 y 3.4) | **Verificado en el emulador con el bloqueo de CoinDen apagado**: cancelar no borra, confirmar vuelve a la bienvenida y, tras el onboarding, la base está vacía. La rama con autenticación solo está cubierta por las pruebas de `EraseDataPresenter`. |

### Hallazgos nuevos (no corregidos, fuera de alcance)

- **Versión en Perfil:** el APK de depuración muestra "CoinDen 0.1.0" porque `versionName` toma su valor por omisión; el AAB de la etiqueta lo recibe con `-PversionName` (el texto usa `%1$s`). Conviene confirmarlo en el AAB real.
- **Dictado "taxi 20 bs":** el borrador queda "Sin monto" (el parser no toma "20 bs" sin verbo ni medio). No es un incumplimiento de spec, pero conviene revisarlo con el contenido de `text-interpreter`.
- **Pruebas de `core/domain`:** la prueba nueva de "Lectura que nadie aprobó" vive en `core/domain/commonTest`; no se tocó código de dominio.
