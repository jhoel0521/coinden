# Propuesta

## Por qué

CoinDen ya captura, revisa y confirma gastos, pero aún no permite responder desde la app «¿en qué se me fue el dinero este mes?». Mostrar el reporte que el dominio ya calcula cierra esa promesa central del MVP.

## Qué cambia

- Acceso «Gastos del mes» desde Inicio a un resumen del mes actual, con navegación entre meses.
- Total confirmado y categorías ordenadas por gasto, incluyendo «Sin categoría», con desglose por moneda y equivalencias según las tasas de referencia configuradas.
- Aviso visible cuando el total es parcial por falta de tasas, sin ocultar los montos originales.
- Estados de carga, mes sin gastos y error recuperable; consulta totalmente local.
- Etiquetas y fechas en español, inglés y portugués, siguiendo el tema existente.

## Capacidades

### Capacidades nuevas

- `spending-insights`: consulta mensual de gastos desde la app, selección del período y presentación transparente de los importes consolidados y sin convertir.

### Capacidades modificadas

Ninguna. Se consume el contrato vigente de `expense-categorization`; sus reglas contables no cambian.

## Impacto

- Presentación, navegación, recursos y pruebas de `composeApp`; conexión del caso de uso de reporte en su contenedor.
- Reutilización del reporte de dominio y sus pruebas; sin cambios de persistencia ni dependencias nuevas previstas.
- Actualización del estado de esta capacidad en README al implementar.

## Fuera de alcance (Non-Goals)

- Cotizaciones automáticas, administración de billeteras/tasas/categorías y cambios al patrimonio de Inicio.
- Presupuestos, dinero libre, metas, ingresos frente a gastos, comparación entre meses, exportación y sincronización.
- Gráficos, porcentajes y navegación al detalle de una categoría en esta primera entrega.
- Tasas históricas: las equivalencias usan las referencias configuradas al consultar y se identifican como estimaciones.
