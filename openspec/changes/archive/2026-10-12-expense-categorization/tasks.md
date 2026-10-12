# Tasks: Categorización de Gastos (TDD: primero la prueba)

## 1. Modelo y categorizador

- [x] 1.1 Definir `BaseCategory`, `CategoryRef`, `CustomCategory`, `CategoryOrigin`, `CategoryRule`, `CategorySuggestion` y `CategorizationError` en `model/category/`, y agregar `categoryWords` a `LanguagePack` con datos en `en`, `es` y `pt`; verificar que compila y que el modelo no contiene textos de usuario.
- [x] 1.2 Escribir `CategorizerTest` ("almuerzo" → comida MEDIA por paquete; "salteña" con regla a salidas → ALTA; "Mariela Quispe Rojas" → null; "lunch" en inglés y "almoço" en portugués → comida; regla contenida como palabras completas; gana la regla más larga; regla hacia categoría archivada ignorada; "trasmporte" sin regla → null). Luego implementar `Categorizer` hasta que pase.

## 2. Clasificación append-only

- [x] 2.1 Escribir `ClassificationProjectionTest` (la vigente es la más reciente en orden cronológico; una descripción null conserva la anterior; el historial guarda todo) e implementar `ClassificationEvent` y `ClassificationProjection` hasta que pase.
- [x] 2.2 Definir `CategorizationRepositoryPort` e implementar el fake en memoria; verificar con una prueba mínima que las clasificaciones son append-only.

## 3. Casos de uso de categorías

- [x] 3.1 Implementar `CreateCustomCategoryUseCase` y `ArchiveCustomCategoryUseCase` con pruebas (nombre tal cual, archivada no se sugiere pero se conserva en clasificaciones previas).
- [x] 3.2 Implementar `ClassifyExpenseUseCase` (reclasificar o clasificar un gasto directo con descripción opcional) con pruebas: aprende la regla, no toca el libro, rechaza transferencias con `NotAnExpense` y gastos inexistentes con `NotFound`.

## 4. Integración con draft-inbox

- [x] 4.1 Agregar el `CategorizationContext` opcional a `DraftContext`, la categoría opcional a `ConfirmDraftUseCase` (clasificación con la nota, regla aprendida, sin duplicar en la confirmación idempotente) y las sugerencias a `GetDraftInboxUseCase`; verificar los escenarios "confirmar con categoría", "confirmar sin categoría" y "categoría sugerida en la bandeja", y que las pruebas previas de draft-inbox siguen iguales.

## 5. Reporte

- [x] 5.1 Extraer `ProfileRates.converterFor` desde `GetConsolidatedPortfolioUseCase` sin cambiar su comportamiento; verificar que las pruebas de las 5 personas siguen pasando.
- [x] 5.2 Escribir `SpendingByCategoryTest` (período, anulados excluidos, transferencias y retiros fuera, Sin categoría incluida, multimoneda consolidada con 1 USD = 11.20 BOB, orden descendente) e implementar `SpendingByCategoryUseCase` hasta que pase.

## 6. Pruebas de feature

- [x] 6.1 `CarlosCategoriesFeatureTest`: con el intérprete real, un mes de Carlos (comprobantes anonimizados y frases); el paquete sugiere "almuerzo" y "StreamingPremium"; Carlos elige comida para "POLLOS DEL VALLE" y transporte para "trasmporte" y la siguiente vez se sugieren solos con ALTA; reporte de octubre con suscripciones 118.71, comida 72.00, salud 31.70, transporte 3.00, total 225.41 Bs, y el retiro fuera.
- [x] 6.2 `WorldCategoriesFeatureTest`: John ("lunch", "uber") en inglés y Ana ("almoço", "uber") en portugués reciben sugerencias de su paquete y su reporte del mes en USD y BRL.

## 7. Verificación

- [x] 7.1 Ejecutar `npm test` y `npm run lint` en verde, confirmar que las 210 pruebas previas siguen pasando y marcar `expense-categorization` como hecho en el README.

## Workflow follow-up

- Archivar `expense-categorization` con `opsx-archive` tras la revisión.
- Siguiente del MVP: `android-app-shell` y `local-persistence`.
