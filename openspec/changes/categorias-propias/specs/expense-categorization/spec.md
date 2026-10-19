## MODIFIED Requirements

### Requirement: Categorías Base y Propias
El sistema DEBE (MUST) ofrecer 12 categorías base identificadas sin idioma (comida, mercado, transporte, salud, suscripciones, servicios, hogar, salidas, compras, educación, personas y comisiones) y permitir que cada usuario cree categorías propias con el nombre que escriba, las renombre, las archive sin borrarlas y las reactive. Un gasto sin categoría queda como "Sin categoría".

#### Scenario: Categoría propia
- **WHEN** Carlos crea la categoría "Semillitas"
- **THEN** puede asignarla a sus gastos igual que una categoría base, y se guarda con el nombre tal como lo escribió

#### Scenario: Archivar una categoría propia
- **WHEN** Carlos archiva "Semillitas"
- **THEN** ya no se sugiere ni se ofrece para gastos nuevos, pero los gastos que ya la tenían la conservan en los reportes

#### Scenario: Renombrar una categoría propia
- **WHEN** Carlos tiene dos gastos en "Mascota" y la renombra a "Mascotas"
- **THEN** los dos gastos, el historial y el reporte muestran "Mascotas"

#### Scenario: Reactivar una categoría propia
- **WHEN** Carlos reactiva "Semillitas", que estaba archivada
- **THEN** vuelve a ofrecerse para gastos nuevos

#### Scenario: Nombre vacío
- **WHEN** Carlos intenta crear o renombrar una categoría con el nombre vacío
- **THEN** la app no la guarda y le pide un nombre
