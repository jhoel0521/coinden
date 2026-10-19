## MODIFIED Requirements

### Requirement: Origen de Cada Campo Propuesto
El intérprete DEBE (MUST) indicar para cada campo propuesto si salió de la evidencia (etiqueta o plantilla), lo dijo el usuario, se infirió por una regla de lectura o se sugirió por defecto, sin alterar el texto original: montos, fechas y nombres se proponen tal como aparecen. Una frase sin ningún verbo que nombra su billetera es un gasto dicho por el usuario (la forma corta que enseña la app: nota, monto y billetera); sin verbo y sin billetera nombrada, o con un verbo leído con error de tipeo, el tipo queda inferido.

#### Scenario: Orígenes de una frase
- **WHEN** se interpreta "page 3 bs para trasmporte" sin billetera nombrada
- **THEN** el monto y la moneda son dichos, el tipo es inferido y la billetera es sugerida

#### Scenario: Nombres sin traducir
- **WHEN** un comprobante en español tiene el beneficiario "POLLOS COPACABANA"
- **THEN** la nota propuesta es exactamente "POLLOS COPACABANA"

#### Scenario: Forma corta sin verbo
- **WHEN** se interpreta "almuerzo 25 bnb" y "bnb" es alias de "BNB Bs"
- **THEN** el tipo gasto, el monto y la billetera son dichos, y el borrador queda Listo con confianza alta

#### Scenario: Frase de otro idioma sin su paquete
- **WHEN** se interpreta "gaste 2 para un semillita" con solo el paquete en inglés
- **THEN** el tipo gasto queda inferido, porque el verbo no se entiende y la frase no nombra billetera

#### Scenario: Taxi en forma corta
- **WHEN** Carlos dice "taxi 10 bnb"
- **THEN** el borrador queda en "Listos" junto con los demás gastos seguros
