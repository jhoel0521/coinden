# Spec Delta

## MODIFIED Requirements

### Requirement: Unir con un Movimiento Confirmado
Si el borrador que queda ya está confirmado, el sistema DEBE (MUST) no registrar otro movimiento ni cambiar sus datos por su cuenta. Si el otro borrador viene de un comprobante real (foto, PDF o texto del banco), "Son el mismo" crea un cambio pendiente con lo que diría el movimiento al unirlos, que la persona aprueba o descarta en el formulario de corrección, incluso si el comprobante indica otra billetera. Si viene de texto escrito, dictado o audio, solo se agrega su evidencia.

#### Scenario: Unir con el taxi confirmado
- **WHEN** Carlos responde "Son el mismo" entre el gasto confirmado "taxi" de 40.00 Bs en "BNB Bs" y el comprobante de 40.00 Bs de "BNB Bs" con referencia "400000777"
- **THEN** se crea un cambio pendiente que agrega la referencia "400000777", y el taxi sigue sin referencia y el saldo no cambia hasta que Carlos lo aprueba

#### Scenario: Billetera distinta
- **WHEN** el gasto confirmado de 40.00 Bs está en "Billetera", el comprobante dice "BNB Bs" y Carlos responde "Son el mismo"
- **THEN** el cambio pendiente muestra la billetera "Billetera" → "BNB Bs" para aprobarla, y nada cambia hasta entonces

#### Scenario: Dictado unido a un confirmado
- **WHEN** Carlos responde "Son el mismo" entre el taxi confirmado y su dictado "gasté 40 en taxi"
- **THEN** el taxi suma el audio como evidencia, sus datos no cambian y no se crea un cambio pendiente
