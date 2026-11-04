# Spec Delta

## Purpose

Define lo que el sitio público del proyecto debe decir y hacer para que cualquier persona entienda, en su idioma, qué datos guarda CoinDen, cómo los protege y cómo borrarlos.

## ADDED Requirements

### Requirement: Política de Privacidad Completa
La página de privacidad DEBE (MUST) explicar qué datos guarda la app (movimientos, billeteras, y fotos, PDF y audios de comprobantes, que pueden contener números de cuenta y de transacción), dónde (solo en el dispositivo), cómo se protegen (cifrado de la base y de los adjuntos, llave protegida por el sistema) y que no hay cuenta, servidor ni copia en la nube de Google.

#### Scenario: Qué se guarda
- **WHEN** Carlos lee la política de privacidad
- **THEN** encuentra que la app guarda fotos de comprobantes que pueden contener números de cuenta, que se guardan cifradas solo en su teléfono y que nada se envía a un servidor de CoinDen

#### Scenario: Sin copia en la nube
- **WHEN** John busca si sus datos se respaldan automáticamente
- **THEN** la política dice que Android no respalda los datos de la app y que no existe respaldo de CoinDen

### Requirement: Qué Sale del Teléfono y Quién lo Recibe
La política DEBE (MUST) listar todo lo que puede salir del dispositivo, siempre por decisión de la persona: cotizaciones públicas que ella configuró, descargas opcionales de paquetes y modelos, y el lector de comprobantes de Google (apagado por defecto) con lo que recibe y lo que nunca recibe.

#### Scenario: Lector de Google
- **WHEN** Ana lee la sección sobre el lector de comprobantes de Google
- **THEN** ve que está apagado por defecto, que solo recibe métricas técnicas de uso y que nunca recibe imágenes, textos ni montos

### Requirement: Cómo Borrar los Datos y Derechos de la Persona
La política DEBE (MUST) explicar cómo borrar todos los datos desde la app (Perfil → Borrar mis datos) y que desinstalar la app también los elimina, y DEBE indicar que, como CoinDen no tiene los datos de la persona en ningún servidor, el acceso, la corrección y el borrado los ejerce ella misma en su dispositivo, e incluir un medio de contacto.

#### Scenario: Pasos para borrar
- **WHEN** Facundo busca cómo borrar su información
- **THEN** la política le indica los pasos en la app y le aclara que no hay copia en servidores que también deba borrarse

### Requirement: Política en Español, Inglés y Portugués
La política DEBE (MUST) existir con el mismo contenido en español, inglés y portugués, y cada versión DEBE enlazar a las otras dos.

#### Scenario: Cambiar de idioma
- **WHEN** Ana lee la política en portugués y toca EN
- **THEN** ve la misma política en inglés

### Requirement: Portada en el Idioma del Navegador
La portada del sitio (español) DEBE (MUST) llevar a la persona a la versión en inglés o portugués cuando el idioma de su navegador sea uno de esos, una sola vez por visita. Si el idioma no es uno de los tres, se queda en español.

#### Scenario: Navegador en inglés de Estados Unidos
- **WHEN** John abre la portada con el navegador en inglés
- **THEN** llega a la versión en inglés

#### Scenario: Navegador en un idioma no soportado
- **WHEN** Wei abre la portada con el navegador en chino
- **THEN** se queda en la versión en español

### Requirement: La Elección Manual se Respeta
Si la persona elige un idioma con los enlaces ES, EN o PT, el sitio DEBE (MUST) respetar esa elección y no volver a redirigirla por el idioma del navegador.

#### Scenario: Volver a español a propósito
- **WHEN** John, con el navegador en inglés, toca ES en la versión en inglés
- **THEN** ve la versión en español y la portada no lo vuelve a llevar al inglés

### Requirement: Sin Detección por País ni Datos de la Persona
La elección de idioma DEBE (MUST) usar únicamente el idioma que el navegador declara, sin consultar la dirección IP, el país ni ningún servicio externo, y sin guardar datos de la visita en un servidor propio.

#### Scenario: Ninguna consulta externa
- **WHEN** Carlos abre la portada
- **THEN** el sitio no hace solicitudes a servicios de geolocalización ni de terceros para elegir el idioma
