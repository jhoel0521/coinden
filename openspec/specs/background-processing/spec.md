# background-processing Specification

## Purpose
Procesa en segundo plano lo que la persona captura (leer fotos, PDF y audios, y después la IA), para que pueda mandar y salir: el trabajo sigue aunque cierre la app, de a uno, sin duplicar ni perder nada, y libera la memoria de los modelos al terminar.

## Requirements

### Requirement: Cola que Sigue sin la App
Lo pendiente de cada captura DEBE (MUST) procesarse en segundo plano aunque la persona salga o cierre la app, y retomarse solo si el sistema corta el trabajo o el teléfono se reinicia. Capturar nunca espera a la cola.

#### Scenario: Mandar y salir
- **WHEN** Carlos toma la foto de un comprobante y cierra la app en seguida
- **THEN** la foto se lee igual y, al volver a abrir la app, el borrador ya propone el gasto

#### Scenario: Reinicio del teléfono
- **WHEN** el teléfono de Carlos se reinicia con dos audios pendientes de transcribir
- **THEN** después del reinicio la cola los transcribe sin que Carlos abra la app

### Requirement: Lo Pendiente Sale de los Datos
La cola DEBE (MUST) procesar los borradores Capturados con archivo que todavía no se leyeron, sin una lista aparte. Un borrador ya leído, completado, confirmado o descartado no se vuelve a procesar.

#### Scenario: Cola cortada a mitad
- **WHEN** el sistema corta la cola después de leer la primera de tres fotos
- **THEN** al retomarse lee solo las dos que faltan

#### Scenario: Lectura hecha en otro dispositivo
- **WHEN** el celular ya leyó una foto y ese historial llegó a la PC
- **THEN** la cola de la PC no vuelve a leerla

#### Scenario: Capturas de antes de la lectura
- **WHEN** Carlos abre la app y tiene borradores Capturados con fotos guardadas antes de que existiera la lectura
- **THEN** la cola los lee solos, sin tocar ningún botón

#### Scenario: Audios que esperaban un modelo de voz
- **WHEN** Carlos tenía una nota de voz sin transcribir y descarga un modelo de voz
- **THEN** la cola la transcribe sola

### Requirement: Un Trabajo a la Vez
La cola DEBE (MUST) procesar un borrador por vez y nunca tener dos modelos trabajando al mismo tiempo.

#### Scenario: Varias capturas seguidas
- **WHEN** Carlos manda tres fotos y un audio en un minuto
- **THEN** se procesan uno después de otro, en el orden en que se capturaron

### Requirement: Liberar Memoria al Terminar
Cuando la cola queda vacía, la app DEBE (MUST) descargar de la memoria los modelos que usó (voz, lector de Google y, más adelante, la IA). Con la app abierta, los libera también tras un minuto sin usarlos.

#### Scenario: Cola vacía
- **WHEN** la cola termina de transcribir el último audio pendiente
- **THEN** el modelo de voz se descarga de la memoria

#### Scenario: Volver a usarlo
- **WHEN** después de liberarlo llega otro audio
- **THEN** el modelo se vuelve a cargar y el audio se transcribe

### Requirement: Reintentos sin Perder la Captura
Si procesar un borrador falla, la cola DEBE (MUST) reintentarlo más tarde hasta 3 veces; si sigue fallando, el borrador queda Capturado con su archivo para revisarlo a mano, y la cola sigue con los demás.

#### Scenario: Falla pasajera
- **WHEN** la lectura de una foto falla una vez por falta de memoria y la segunda vez funciona
- **THEN** el borrador queda con el gasto propuesto

#### Scenario: Falla persistente
- **WHEN** un audio dañado falla tres veces
- **THEN** su borrador queda Capturado con el audio y la cola sigue con el siguiente
