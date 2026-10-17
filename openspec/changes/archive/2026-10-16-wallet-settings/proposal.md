# Propuesta

## Why

La persona necesita administrar sus billeteras y actualizar sus referencias después del primer inicio. Además, la revisión solicitada identifica textos de bienvenida fijados a español y la ausencia del icono de lanzamiento Android.

## What Changes

- Acceso «Billeteras y tasas» desde Inicio: crear, archivar, reactivar y elegir una billetera predeterminada; agregar monedas del catálogo y actualizar o quitar tasas manuales.
- Proteger la última billetera activa y conservar los eventos de las archivadas.
- Corregir el idioma de la bienvenida y la resolución de recursos sin reiniciar la navegación al cambiar de idioma.
- Agregar iconos Android vectoriales normal, adaptable y monocromático usando el isotipo Cobre existente.
- Auditorías de código y UX/UI documentadas, con pruebas unitarias e integración. Validación visual Android diferida a una issue, por petición expresa.

## Capabilities

### New Capabilities

- `wallet-settings`: gestión local de billeteras y referencias después del onboarding.

### Modified Capabilities

- `app-shell`: icono identificable al instalar y en el selector de apps.
- `app-onboarding`: el saludo principal sigue el idioma elegido; los nombres de los idiomas siguen siendo reconocibles en su propia lengua.

## Impact

Presentación y navegación de composeApp, recursos traducidos, manifiesto y recursos Android, y pruebas de presentadores y empaquetado. Se reutilizan puertos, casos de uso y almacenamiento cifrado; no se añaden dependencias, red ni migraciones.

## Fuera de alcance

Cotizaciones automáticas, cambio de moneda principal, edición del saldo o moneda de una billetera existente, eliminación de eventos, sync y nueva identidad de marca. Las auditorías describen los límites de la inspección estática; no simulan una prueba visual ejecutada.
