# Tasks

## 1. Nombres neutros en la interfaz (wallet-settings, app-onboarding)

- [x] 1.1 Escribir primero la prueba que recorre `WalletType` y `MovementType` en es, en y pt y falla si un texto visible contiene "Banco", "Cripto", "crédito" o "Transferencia" como nombre de tipo o de movimiento; verificar que falla antes del cambio
- [x] 1.2 Cambiar `wallet_type_*` y `kind_transfer` en `values`, `values-en` y `values-pt` según la tabla del design; verificar que la prueba de 1.1 pasa
- [x] 1.3 Revisar y ajustar las pruebas de presentadores y de formato que usan los nombres viejos (escenario "Tipos al crear una billetera" y "Nombres en los tres idiomas" de wallet-settings; "Tipos con nombres neutros" de app-onboarding); verificar con `:composeApp:testDebugUnitTest` en verde
- [x] 1.4 Probar con Carlos que una billetera de tipo banco ya guardada aparece como "Cuenta" con el mismo saldo, y con Facundo que "Tarjeta" admite saldo inicial -25.00 ARS; verificar con pruebas de feature en `core/domain` y de presentador
- [x] 1.5 Probar el "Traspaso" de 50.00 USD a 594.00 Bs de Carlos en el historial; verificar que los montos y saldos no cambian

## 2. Borrar mis datos: lógica y borrado en Android (data-erasure, local-persistence)

- [x] 2.1 Escribir las pruebas de `EraseDataPresenter` con un `DataEraser` y un `DeviceAuth` falsos: abrir y cancelar sin borrar, bloqueo activo con autenticación correcta, cancelada y sin bloqueo, falla a mitad con reintento; verificar que fallan antes de implementar
- [x] 2.2 Implementar el puerto `DataEraser` y `EraseDataPresenter` (estados `Idle`, `Confirming`, `Authenticating`, `Erasing`, `Failed`, `Done`); verificar que las pruebas de 2.1 pasan
- [x] 2.3 Implementar el borrado en Android en el orden del design (cancelar trabajo, cerrar y borrar la base y sus archivos, borrar la llave y la entrada del Keystore, borrar adjuntos, cola y restos de caché) con el marcador `erase.pending` que completa un borrado interrumpido al siguiente inicio; verificar con una prueba instrumentada que, tras borrar, no existen la base, la llave, los adjuntos ni los restos, y que los modelos y paquetes siguen
- [x] 2.4 Probar con una prueba instrumentada que una base nueva tiene una llave distinta a la anterior, que la copia de la base vieja sigue sin poder leerse sin la llave, y que no existe ninguna operación para borrar o modificar un solo evento; verificar que pasa en el emulador (`:core:data-local:connectedDebugAndroidTest`)
- [ ] 2.5 Reiniciar el proceso al terminar y volver a la bienvenida; verificar con el emulador que tras borrar se ve la bienvenida y que, al completar el onboarding, la app tiene una base vacía

## 3. Borrar mis datos: pantallas y textos (data-erasure, app-navigation, app-onboarding)

- [x] 3.1 Agregar la fila "Borrar mis datos" en Perfil entre "Modelos de voz" y "Acerca de" y la pantalla de confirmación, con el aviso de que solo afecta a este teléfono y que no se envía nada a ningún lado; verificar con una prueba de presentador de Perfil (escenarios "Lo que hay en Perfil", "Borrar mis datos desde Perfil" y "Aviso en la confirmación")
- [x] 3.2 Agregar los textos de la confirmación y del aviso en es, en y pt sin textos fijos en la UI; verificar que no hay textos nuevos fuera de `strings.xml`
- [x] 3.3 Agregar a la pantalla de privacidad del onboarding la línea sobre borrar todo desde Perfil; verificar con la prueba del escenario "Cómo borrar todo"
- [ ] 3.4 Probar con Carlos, en el emulador y con el bloqueo activo, el recorrido completo de "Borrar mis datos" (abrir, cancelar, confirmar, autenticar); verificar que cada escenario de `data-erasure` se cumple y que `openspec validate privacidad-y-nombres-neutros` sigue en verde

## 4. Política de privacidad y sitio (project-site)

- [x] 4.1 Reescribir `site/privacidad/index.html` con las secciones del spec: qué se guarda (incluidas fotos con números de cuenta), cifrado, sin respaldo en la nube de Google, qué sale del teléfono, lector de Google, cómo borrar y derechos, contacto; verificar contra el código (cifrado, `allowBackup="false"`, qué datos salen) y con una lectura completa
- [x] 4.2 Traducir la política a `site/en/privacy/index.html` y `site/pt/privacidade/index.html` con el mismo contenido y los enlaces ES, EN y PT; verificar con `grep` que las tres versiones tienen las mismas secciones
- [x] 4.3 Agregar la redirección por idioma del navegador en la portada (`navigator.language`, una sola vez, respeta la elección manual, `try/catch` sin almacenamiento, sin IP ni servicios externos); verificar con un script de Node que simula `navigator.language` en es, en, pt y zh y la elección manual
- [x] 4.4 Comprobar el sitio en el navegador local con idioma en inglés y portugués; verificar que la portada redirige y que ES vuelve al español sin rebote
- [ ] 4.5 Publicar con el PR (el flujo `pages.yml` publica al fusionar); verificar que la página pública muestra el contenido nuevo

## 5. Documentación, versión y verificación de la 0.2.1

- [x] 5.1 Actualizar `docs/play/ficha.md` (categoría, nombres, línea de borrado) y `docs/play/README.md` (declaración de datos, borrado y nombres); verificar que los textos coinciden con la app y con el sitio
- [x] 5.2 Volver a tomar las capturas de la ficha (`docs/play/screenshots/`) con los nombres nuevos; verificar que ninguna muestra "Banco", "Cripto" ni "Tarjeta de crédito"
- [x] 5.3 Ejecutar `npm test`, `npm run lint` y `npm run build:android`; verificar los tres en verde
- [ ] 5.4 Compilar el AAB de la 0.2.1 (`bundleRelease`, código de versión 201) y probar con la persona el camino feliz en el emulador: crear una billetera "Cuenta", capturar un gasto, borrar todos los datos y volver a la bienvenida; verificar que no se ve ningún dato anterior
- [ ] 5.5 Revisar con `openspec validate privacidad-y-nombres-neutros` y `openspec status` que todo está completo antes de archivar; verificar la salida

## Workflow follow-up

- Subir el AAB de la 0.2.1 a la prueba cerrada Alpha como actualización de la 0.2.0, mantener "Mi app no ofrece ninguna función financiera" y reenviar a revisión (lo hace la persona o se pide aprobación explícita).
- Archivar el cambio con `opsx-archive` cuando la persona lo acepte, y sincronizar los deltas a los specs principales.
