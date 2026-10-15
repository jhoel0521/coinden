# Spec Delta

## ADDED Requirements

### Requirement: Lectura Posterior de la Evidencia
Un borrador Capturado DEBE (MUST) poder recibir después el texto leído de su foto, PDF o audio como un evento nuevo de su historial, con el lector que lo produjo y sin perder la evidencia original. Ese texto pasa por el intérprete como cualquier captura. Un borrador que la persona ya completó, confirmó o descartó no cambia.

#### Scenario: Comprobante con dos pagos
- **WHEN** la lectura de una foto encuentra dos gastos
- **THEN** el borrador queda con el primero y el segundo se guarda como otro borrador con la misma foto

#### Scenario: Foto leída después de capturarla
- **WHEN** Carlos toma la foto de un comprobante de 25.00 Bs y la lectura termina un segundo después
- **THEN** el borrador conserva la foto, guarda el texto leído y pasa a proponer el gasto de 25.00 Bs

#### Scenario: Borrador ya completado a mano
- **WHEN** Carlos completó a mano un borrador con foto y después termina su lectura
- **THEN** la propuesta de Carlos no cambia

#### Scenario: Lectura en otro dispositivo
- **WHEN** el celular lee una foto y el historial se sincroniza con la PC
- **THEN** la PC muestra la misma propuesta sin volver a leer la foto

#### Scenario: Lectura sin resultado
- **WHEN** la lectura de un audio no produce texto
- **THEN** el borrador sigue Capturado con su audio
