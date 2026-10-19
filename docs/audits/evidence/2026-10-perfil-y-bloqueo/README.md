# Evidencia: pestañas, Mi perfil y bloqueo (2026-10-19)

Emulador `Android14_API34` (1080×2400), APK de depuración de la rama `feat/perfil-y-bloqueo`.
Datos inventados: Billetera 100 BOB y BNB Bs 500 BOB; USD a 10,20. Gastos de prueba: "almuerzo 45 bnb" y "taxi 20 bnb".
El PIN del emulador lo configuró y lo escribió la persona en el aviso del sistema.

| Captura | Qué verifica |
|---------|--------------|
| 01 | Splash con la marca. |
| 02 | Privacidad en el onboarding: tarjeta "Bloquear con huella o PIN", apagada; sin bloqueo de pantalla en el teléfono, avisa por qué no se puede usar. |
| 03, 11 | Inicio solo con patrimonio y billeteras; ícono transparente en claro y oscuro; barra de 5 pestañas con Capturar al centro y etiquetas alineadas. |
| 04 | Capturar abre una hoja con Dictar, Foto y Escribir sin cambiar de pestaña. |
| 07 | Revisar como pestaña, sin botón de cerrar. |
| 08, 09 | Movimientos con el segmentado Historial / Gastos del mes. |
| 10 | Mi perfil: Seguridad, Idioma, Configuración (Billeteras y tasas, Privacidad, Modelos de voz) y Acerca de. |
| 12, 13 | Sin bloqueo de pantalla: el botón "Configurar bloqueo de pantalla" abre los ajustes de seguridad del teléfono. |

## Recorrido con el bloqueo activo (leído de la pantalla)

Con el bloqueo activo, la ventana de CoinDen lleva `FLAG_SECURE`: Android no permite capturas de pantalla ni muestra su miniatura en apps recientes. Por eso esta parte no tiene imágenes; se leyó la pantalla con `uiautomator` y el estado de la ventana con `dumpsys`.

1. Al volver a la app con el bloqueo activo, Inicio y Perfil muestran "Desbloquea para ver tus saldos" y "Desbloquear", sin ningún monto.
2. Capturar → Escribir → "taxi 20 bnb" → Guardar: "Guardado como borrador", sin pedir el PIN.
3. Confirmar en el resultado: se abre el aviso del sistema "Desbloquear CoinDen — Usa tu huella o el bloqueo de tu teléfono" (`mCurrentFocus=BiometricPrompt`).
4. Con el PIN de la persona: "Confirmado: ya cuenta en tu saldo." e Inicio muestra BNB Bs en 435,00 BOB (500 − 45 − 20).
5. 30 segundos en segundo plano: al volver, Inicio sigue mostrando el patrimonio.
6. 65 segundos en segundo plano: al volver, "Desbloquea para ver tus saldos".
7. `dumpsys window`: la ventana de `MainActivity` tiene `SECURE`; la captura de la vista de apps recientes sale vacía.

## Corregido durante esta verificación

- El contenido quedaba bajo la barra de estado y la barra de pestañas bajo los gestos del sistema: la raíz respeta los bordes.
- "Capturar" quedaba más abajo que las otras etiquetas: el círculo sobresale hacia arriba ocupando el mismo alto.
- El logo y el ícono de la app pasan a ser transparentes (sin placa oscura), con sus contornos.
- Sin bloqueo de pantalla, el aviso no llevaba a ningún lado: ahora tiene el botón para configurarlo y el interruptor se habilita solo al volver.

## Pendiente

- Capturas en oscuro y letra 1,3 de cada pestaña: con el bloqueo activo el sistema no las permite; se toman con el bloqueo apagado.
- Huella simulada (`adb -e emu finger touch 1`): el emulador solo tiene PIN.
