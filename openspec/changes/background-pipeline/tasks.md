# Tasks: Procesamiento en Segundo Plano

## 1. Cola común

- [ ] 1.1 Escribir las pruebas de `QueueRunner` con lectores falsos (procesa en orden de captura y de a uno; un corte a mitad retoma solo lo que falta; no repite lo ya leído; una falla pasajera se reintenta y la segunda vez funciona; tres fallas dejan el borrador Capturado y siguen los demás; al vaciarse llama a `release`; emite cada borrador procesado) e implementar D1 y D4 hasta que pasen con `:composeApp:testDebugUnitTest`.
- [ ] 1.2 Escribir las pruebas de `QuickCapture` (texto compartido → borrador interpretado; imagen, PDF y audio → borrador Capturado con archivo y huella y `kick()` llamado) y mover `CapturePresenter.shared` y `photo` a usarlo, refrescando la tarjeta con `processed` (D5, D6).

## 2. Android

- [ ] 2.1 Agregar WorkManager, `PerceptionWorker` y `ProcessingQueue` (D2); mover los lectores a `CoinDenApplication` con `release()` y la liberación a los 60 s (D3); prueba instrumentada con `work-testing` que encola una foto inventada y verifica que el borrador queda leído.
- [ ] 2.2 `ShareActivity` sin interfaz con aviso y regreso a la otra app; si no terminó el onboarding, abrir `MainActivity` (D5); retirar "Leer capturas guardadas" (D6); `kick()` al abrir la app, al terminar el onboarding y al descargar un modelo; textos en es, en y pt; verificar que `npm test`, `npm run lint` y `npm run build:android` pasan.

## 3. Verificación en el dispositivo

- [ ] 3.1 En el emulador: tomar una foto y cerrar la app de inmediato, y verificar al reabrir que se leyó; compartir un PDF y una nota de voz desde Archivos y verificar el aviso y que se vuelve a Archivos; forzar el cierre del proceso con capturas pendientes y verificar que se retoman; verificar con `adb shell dumpsys meminfo` que la memoria baja al vaciarse la cola; guardar capturas como evidencia.

## Workflow follow-up

- Archivar `background-pipeline` con `opsx-archive` tras la revisión.
- Siguientes: `review-inbox-ui` y `local-llm-interpreter` (un paso más de la cola).
