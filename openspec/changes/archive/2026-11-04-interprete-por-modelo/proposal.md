# Proposal

## Why

Entender lo que una persona dice es un problema de lenguaje, y hoy lo resuelven listas de palabras: 18 listas por idioma que hay que escribir a mano para cada idioma que la app soporte. Esas listas reconocen "gasté", pero no entienden **intenciones**: qué audio explica qué comprobante, a quién se le pagó, si "los 60 de la comida" es el mismo pago que ya se mencionó. Cada caso nuevo pide otra lista, y ninguna lista alcanza.

El Manifiesto ya dice adónde va esto (principio 4): *"Cuando hay un modelo de lenguaje disponible, interpreta primero lo que dice la persona; las reglas deterministas verifican lo que propone y son el respaldo cuando no hay modelo."* Pero el spec vigente de `text-interpreter` dice lo contrario — *"sin usar la red ni modelos"* — y el código siguió al spec. **Spec y manifiesto se contradicen, y se corrige el spec** (Manifiesto §1).

El cambio `contexto-de-captura` lo dejó en evidencia: para entender un audio junto a dos comprobantes hacían falta conectores, frases de referencia, verbos de pago a una persona y palabras vacías, todo por idioma. Ese camino no escala a "mundial desde el día 1".

## What Changes

- **El modelo de lenguaje local interpreta primero.** Corre solo en el teléfono, con un prompt de sistema fijo, temperatura 0 y un formato de salida cerrado. Recibe el texto o el paquete de captura y devuelve las mismas propuestas tipadas que hoy devuelven las reglas.
- **Las reglas dejan de adivinar el lenguaje y pasan a verificar.** Comprueban que lo que el modelo propone esté respaldado: el monto aparece en el texto o en el comprobante, la moneda y la billetera existen, y ningún hecho del comprobante fue cambiado.
- **Si el modelo y las reglas no coinciden, el borrador queda para revisar**, con el motivo. Nunca se registra en silencio.
- **Sin modelo, la app sigue completa con un respaldo mínimo:** monto y moneda por el locale, billetera por alias, medio de pago, fecha y hora, y los comprobantes por plantilla. Es menos listo, no menos usable.
- **Se eliminan las listas de lenguaje natural** (verbos de movimiento, conectores, preposiciones de nota, palabras de categoría, marcadores de lo recibido). Quedan como datos solo las que no son lenguaje: etiquetas de comprobante, meses, palabras de moneda y medios de pago.
- **El modelo se descarga como los de voz:** opcional, con su tamaño a la vista, verificado por huella y sin enviar nada de la persona.
- **BREAKING:** `text-interpreter` deja de ser determinista de punta a punta. El determinismo se exige ahora al validador y al respaldo, no a la interpretación.

## Capabilities

### New Capabilities
- `intent-interpreter`: la interpretación de intenciones por un modelo de lenguaje local. Cubre el contrato de entrada y salida, el prompt fijo y la temperatura 0, el formato cerrado, qué hace el validador determinista, qué pasa cuando modelo y reglas no coinciden, y qué queda cuando no hay modelo.

### Modified Capabilities
- `text-interpreter`: "Interpretación Determinista en el Dispositivo" se reemplaza por el respaldo mínimo determinista; "Interpretación de Frases" y "Paquetes de Idioma como Datos" se recortan a lo que no es lenguaje natural.
- `expense-categorization`: la categoría la propone el modelo a partir de la nota; sin modelo quedan las reglas aprendidas de la persona.
- `evidence-perception`: "Modelos Descargables y Verificados" y "Modelo Recomendado según el Teléfono" se extienden al modelo de lenguaje, con sus propios mínimos de memoria.

## Non-Goals

- **Un modelo en la nube.** Nunca. Si el teléfono no puede, hay respaldo, no servidor.
- **Que el modelo registre movimientos.** Propone; la persona confirma (Manifiesto, principio 4 y 5).
- **Entrenar o afinar un modelo.** Se usa uno publicado, verificado por huella.
- **Reemplazar la lectura de comprobantes.** Las plantillas y el lector genérico siguen: un comprobante son hechos, no lenguaje.
- **El repaso del día**, que sigue siendo un cambio aparte.

## Meta de la 0.2.0

- **Criterio 2** ("el modelo descargable entiende la intención y las reglas lo verifican"): es el objeto entero de este cambio. Los casos del comprobante sin glosa, el ambulante, la comida y el somó y el comprobante equivocado se prueban contra el modelo, con el validador verificando cada propuesta.
- **Criterio 3** ("sin modelo la app sigue completa"): el requisito "Respaldo Mínimo sin Modelo" y las tareas 2.1 a 2.5. Las mismas pruebas corren sin modelo y lo dudoso va a "Para revisar".
- **Criterio 6** ("funciona en el piso"): la tarea 6.1 mide en un teléfono real de 4 GB arm64 y no se da por terminada sin el número. El piso de la app (Android 8, 2 GB) ya se cumple con `minSdk = 26`; el modelo simplemente no se ofrece ahí.
- **Criterio 1**: completa lo que empezó `paquetes-como-datos`, borrando de los archivos las listas que el modelo reemplaza.

## Impact

- **`core/domain`:** puerto del modelo, contrato cerrado de entrada y salida, validador determinista y respaldo mínimo. Se borran de `Readers.kt` las partes que adivinan lenguaje.
- **`composeApp`:** adaptador del modelo sobre un motor on-device, descarga y verificación, y la pantalla que explica qué hace el modelo y permite borrarlo.
- **Dependencia nueva:** un motor de inferencia on-device. Se evalúa en el diseño.
- **`contexto-de-captura`:** su intérprete del paquete pasa a ser el modelo con reglas que validan. Lo ya hecho (paquete, reconciliación, prueba de actualización) se mantiene; se quitan de su plan `ContextPhraseReader` y las palabras de contexto.
- **Nada toca la base de datos:** el modelo no persiste nada. `UpdateFromReleaseTest` y `verifySqlDelightMigration` no se ven afectados.
