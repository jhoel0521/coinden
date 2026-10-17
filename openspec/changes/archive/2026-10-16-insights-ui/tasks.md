# Tareas

Cada grupo comienza con pruebas que fallen antes de implementar. Las referencias de requisitos corresponden a `specs/spending-insights/spec.md`; no se alteran las reglas vigentes de `expense-categorization`.

## 1. Período mensual

- [x] 1.1 Escribir pruebas de `SpendingMonth` con reloj controlado: mes inicial local, enero/diciembre, febrero bisiesto, intervalo inclusivo/exclusivo y prohibición de avanzar al futuro. Verificar que fallen antes de implementar. Requisitos: Acceso al Resumen Mensual; Selección de Mes Civil.
- [x] 1.2 Implementar la utilidad de período en presentación y probar los límites con desfases negativos y positivos, incluido un instante cuyo mes local difiere del UTC; verificar que pasen las pruebas de 1.1 y documentar el uso del desfase actual, sin zona histórica. Requisito: Selección de Mes Civil.

## 2. Consulta y estado del resumen

- [x] 2.1 Escribir pruebas del presentador con el contenedor real en memoria de Carlos: total 225.41 BOB y orden, Sin categoría, exclusión de pendientes/ingresos/cambios, corrección 35→32 y gasto en categoría/billetera archivadas. Verificar los fallos iniciales y preservar los nombres propios. Requisito: Gastos Confirmados por Categoría.
- [x] 2.2 Conectar el caso de uso existente en `AppContainer` sin tasas cripto de ejemplo e implementar el estado/consulta del presentador; verificar 2.1 y las pruebas existentes de gasto por categoría. Requisitos: Gastos Confirmados por Categoría; Equivalencias Transparentes.
- [x] 2.3 Escribir primero pruebas de 12 USD→134.40 BOB, cambio de referencia→120 BOB, mezcla BOB/BRL sin tasa, todo sin convertir y BTC sin referencia; implementar el estado parcial, conservando originales y diferenciando cero convertido válido de falta de tasa. Verificar que pasen y que consultar no agregue eventos ni cambie saldos. Requisitos: Equivalencias Transparentes; Monedas sin Tasa Visibles.
- [x] 2.4 Escribir primero pruebas controladas de carga, mes vacío de John, error/reintento en el mismo mes, cancelación y respuesta tardía de septiembre después de agosto; implementar el control de solicitudes y comprobar que ningún dato obsoleto aparece bajo otro mes. Requisito: Estados de Consulta Local.

## 3. Pantalla y navegación

- [x] 3.1 Preparar pruebas de presentación de mes/estados y recursos para es/en/pt, incluyendo inglés de John, portugués, nombres propios, textos de estimación y avisos parciales; agregar recursos y mapeos reutilizando categorías y formato monetario existentes. Verificar las pruebas de recursos/traducciones y formato. Requisito: Presentación Localizada y Accesible.
- [x] 3.2 Implementar la pantalla con total, lista ordenada, desglose nativo, mes anterior/siguiente, carga, vacío y error con reintento. Verificar contra 2.1–2.4, sin gráficos ni porcentajes; comprobar que el caso todo sin convertir conserva sus filas y aviso. Requisitos: Gastos Confirmados por Categoría; Equivalencias Transparentes; Monedas sin Tasa Visibles; Estados de Consulta Local.
- [ ] 3.3 Agregar acceso «Gastos del mes» y ruta desde Inicio, carga al entrar y retorno; verificar en Android el mes actual, retroceso/avance y recarga después de confirmar/corregir. Incluir una prueba automatizada del contrato de selección/recarga del presentador. Requisitos: Acceso al Resumen Mensual; Selección de Mes Civil; Gastos Confirmados por Categoría.
- [ ] 3.4 Verificar controles de al menos 48 dp, semántica/descripciones, avisos textuales, texto ampliado y tema claro/oscuro. Cubrir automáticamente los mapeos de etiquetas/estados y comprobar lectura y navegación con lector de pantalla en Android. Requisito: Presentación Localizada y Accesible.
- [x] 3.5 Actualizar la fila `insights-ui` del README y describir tasas actuales, totales parciales y navegación mensual; verificar que lo documentado corresponda a la pantalla implementada. Requisitos: Acceso al Resumen Mensual; Equivalencias Transparentes; Monedas sin Tasa Visibles.

## 4. Validación integrada

- [x] 4.1 Ejecutar `npm test`, `npm run lint` y `npm run build:android` con JDK 21; verificar éxito y registrar cualquier fallo real, conservando las pruebas existentes. Requisitos: todos.
- [ ] 4.2 En dispositivo/emulador, recorrer con Carlos captura→confirmación→resumen→corrección→resumen, cambiar de mes y repetir consulta en modo avión; comprobar el resultado tras reabrir la app. Registrar resultados y dejar explícitamente pendientes las comprobaciones que no puedan ejecutarse. Requisitos: Acceso al Resumen Mensual; Gastos Confirmados por Categoría; Estados de Consulta Local.
- [x] 4.3 Revisar la correspondencia de cada escenario del spec con su prueba automatizada y las comprobaciones Android complementarias; ejecutar `openspec validate insights-ui --strict` y verificar que pase antes de dar la implementación por terminada. Requisitos: todos.
