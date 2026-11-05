## ADDED Requirements

### Requirement: Modelo del Sistema como Adaptador Opcional
La app DEBE (MUST) consultar el modelo de lenguaje que el sistema operativo ofrece en el teléfono, para proponer la interpretación de una captura, únicamente si el teléfono lo tiene disponible y la persona aceptó el motor de Google. Sin alguna de las dos cosas, mandan las reglas.

#### Scenario: Teléfono sin modelo del sistema
- **WHEN** Carlos captura "almuerzo 45 en efectivo" en un teléfono donde el modelo del sistema no está disponible
- **THEN** la interpretación la hacen las reglas, no aparece ningún aviso de error y el borrador queda igual que sin esta función

#### Scenario: Sin aceptar el motor de Google
- **WHEN** el teléfono tiene el modelo del sistema pero Carlos eligió seguir sin el motor de Google
- **THEN** no se consulta el modelo del sistema y mandan las reglas

#### Scenario: Modelo disponible y aceptado
- **WHEN** el teléfono tiene el modelo del sistema, Carlos aceptó el motor de Google y captura una frase
- **THEN** el modelo propone pagos con temperatura 0, el validador los verifica contra el texto y las reglas, y el borrador queda para confirmar sin cambiar ningún total

### Requirement: El Modelo del Sistema Solo Propone
El modelo DEBE (MUST) solo proponer: el validador determinista verifica cada propuesta y la app DEBE (MUST) seguir completa con las reglas cuando el modelo no está, no responde a tiempo o responde fuera de formato. La app NO DEBE (MUST NOT) descargar ni incluir un modelo ni un motor de inferencia propios para esto.

#### Scenario: Respuesta fuera de formato
- **WHEN** el modelo del sistema responde algo que no es el JSON pedido
- **THEN** se descarta la respuesta y mandan las reglas

#### Scenario: Monto inventado
- **WHEN** el modelo propone 2500.00 Bs para un comprobante que dice 25.00 Bs
- **THEN** el validador rechaza el monto y el borrador queda para revisar con el motivo

#### Scenario: Sin descargas propias
- **WHEN** Carlos abre Perfil
- **THEN** no hay pantalla ni botón para descargar modelos de lenguaje: el sistema gestiona el suyo
