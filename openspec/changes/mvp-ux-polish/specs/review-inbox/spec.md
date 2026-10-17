## ADDED Requirements

### Requirement: Carga Recuperable de Bandeja e Historial
La bandeja y el historial DEBEN (MUST) distinguir carga, resultado vacío y fallo de lectura. Un fallo DEBE ofrecer reintento sin reiniciar la app y nunca mostrarse como ausencia de movimientos. Si quedan datos anteriores visibles durante una recarga fallida, DEBEN indicarse como no actualizados. Las respuestas antiguas no DEBEN reemplazar una carga más reciente.

#### Scenario: Carga pendiente
- **WHEN** Carlos abre la bandeja o el historial y la consulta aún no termina
- **THEN** ve un aviso de carga, no un mensaje de lista vacía

#### Scenario: Fallo y reintento
- **WHEN** falla la lectura de la bandeja o el historial y Carlos toca Reintentar
- **THEN** ve el error y después la nueva carga; al recuperarse aparecen los datos sin reiniciar

#### Scenario: Sin movimientos tras lectura correcta
- **WHEN** la lectura termina correctamente sin elementos
- **THEN** aparece el mensaje vacío específico de esa pantalla

#### Scenario: Recarga fallida con datos previos
- **WHEN** Carlos tenía datos cargados y falla la actualización
- **THEN** cualquier dato conservado se identifica como no actualizado y se ofrece reintento

#### Scenario: Respuesta tardía
- **WHEN** termina una consulta vieja después de una consulta más reciente
- **THEN** la pantalla conserva el resultado más reciente

### Requirement: Nombre de Categorías Propias en el Recorrido
Las tarjetas de borrador, el historial y el formulario de revisión o corrección DEBEN (MUST) mostrar el nombre guardado de cada categoría propia, sin traducirlo ni sustituirlo por una etiqueta genérica. El formulario DEBE permitir conservar la categoría propia seleccionada. Si el nombre no está disponible, DEBE usar una alternativa localizada sin exponer IDs.

#### Scenario: Categoría propia visible
- **WHEN** Carlos tiene un gasto con la categoría propia «Mascotas» y lo consulta en tarjeta, historial o formulario
- **THEN** ve «Mascotas» y no «Categoría personalizada»

#### Scenario: Cambio de idioma
- **WHEN** John cambia a inglés y ve su categoría propia «Casa de mamá»
- **THEN** el nombre se conserva exactamente y las etiquetas del sistema siguen en inglés

#### Scenario: Categoría archivada y categoría desconocida
- **WHEN** Sophie consulta movimientos con una categoría propia archivada o con un nombre ya no disponible
- **THEN** la archivada conserva su nombre y la desconocida usa una alternativa localizada sin mostrar su ID

#### Scenario: Conservar categoría al corregir
- **WHEN** Carlos cambia solo el monto de un gasto con categoría propia y confirma
- **THEN** se conserva la categoría propia y su nombre se muestra también en la comparación del valor anterior
