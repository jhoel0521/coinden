# Spec Delta

## MODIFIED Requirements

### Requirement: Presentación de Borradores
La tarjeta de un borrador DEBE (MUST) mostrar su estado, el monto, la billetera y el medio de pago por separado, la categoría con su motivo y la confianza. Un campo sugerido DEBE distinguirse sin depender solo del color (borde punteado y la palabra "sugerida"), y la confianza DEBE mostrarse con color, ícono y texto (alta, media o baja). En la bandeja la tarjeta es compacta: nota, monto, billetera y, en "Para revisar", el motivo por el que hay que revisarla en lugar del estado; el detalle completo se ve en el formulario.

#### Scenario: Borrador por QR sugerido
- **WHEN** se muestra el borrador de "almuerzo 45 qr" con la billetera "BNB Bs" sugerida y confianza baja
- **THEN** la tarjeta muestra "BNB Bs" con borde punteado y la palabra "sugerida", el medio de pago QR aparte, la categoría comida con su motivo y la confianza baja con su ícono y su texto

#### Scenario: Borrador con alias
- **WHEN** se muestra el borrador de "almuerzo 45 bnb" con confianza media
- **THEN** la billetera "BNB Bs" se muestra sin borde punteado y la confianza media con su ícono y su texto

#### Scenario: Borrador capturado sin propuesta
- **WHEN** se muestra el borrador de "hola"
- **THEN** la tarjeta indica que quedó capturado sin propuesta y muestra la evidencia original, sin campos vacíos

#### Scenario: Tarjeta compacta en Para revisar
- **WHEN** la bandeja muestra "page 3 bs para trasmporte" con la billetera sugerida
- **THEN** la tarjeta ocupa dos líneas con la nota, 3,00 Bs, "Billetera" sugerida y el motivo "Billetera sugerida", y no dice "Listo para confirmar"
