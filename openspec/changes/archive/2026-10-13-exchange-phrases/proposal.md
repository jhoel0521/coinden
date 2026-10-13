# Proposal: Cambios de Moneda por Entrada y Salida (Exchange Phrases)

## Why
En la calle nadie cambia dinero "a una tasa": en el librecambista se anuncia "a 11 y algo" y se cierra "por tus 100 dólares te doy 1190 Bs". Lo que el usuario sabe es **lo que entregó y lo que recibió**. El dominio ya registra los cambios así (la tasa se deriva exacta de los dos montos), pero el intérprete solo entiende la forma con tasa ("vendí 50 dólares a 11.88"): con "vendí 11 dólares, me dieron 112 bs" deja el monto recibido como nota y el borrador queda incompleto, y con "vendí 11 dólares y me dieron 112 bs" lo parte en dos movimientos y propone un gasto falso de 112 Bs. Es justo el caso en que fallan las apps de finanzas, y para Carlos, Facundo o Ana es de todas las semanas.

## What Changes
- **Monto recibido en la frase**: en un cambio de moneda, el intérprete reconoce el monto que se recibió después de un marcador de recepción ("me dieron", "recibí", "por" en "cambié 100 usd por 1190 bs"; "got", "received", "for" en inglés; "me deram", "recebi", "por" en portugués). Los marcadores viven en los paquetes de idioma como datos.
- **Lo recibido manda**: si la frase dice tanto una tasa como el monto recibido, se propone el monto recibido (es el trato real); la tasa dicha se ignora.
- **No partir el cambio**: un conector seguido de un marcador de recepción ("... y me dieron 112 bs") no separa la frase en dos movimientos.
- **Billeteras sugeridas por moneda**: el origen se sugiere con la única billetera activa en la moneda vendida, y el destino con la billetera por defecto si es de la moneda recibida o, si no, con la única billetera activa de esa moneda. Si hay varias o ninguna, el campo queda vacío para completar en la revisión.
- **Persona Carlos en el librecambista**: prueba de feature de punta a punta, de la frase a la confirmación, con la tasa derivada exacta (112/11, sin redondear a 10.18).

### Non-Goals
- Cambios sin verbo de cambio ("por mis 100 dólares me dieron 1190"): se sigue leyendo como hoy; puede llegar en otro cambio.
- Comisiones del cambio, cambios en cuotas o con varias monedas a la vez.
- Comprobantes de cambio (plantillas o lector genérico): solo frases escritas o dictadas.
- Validar que la tasa dicha coincida con los montos.
- Cambios en el dominio de dinero o del libro: ya derivan la tasa exacta.

## Capabilities

### New Capabilities
<!-- Ninguna. -->

### Modified Capabilities
- `text-interpreter`: se agregan la lectura del monto recibido en cambios de moneda (con prioridad sobre la tasa dicha y sin partir la frase) y la sugerencia de billeteras de origen y destino por moneda.

## Impact
- **`core/domain`**: `LanguagePack` (nuevo conjunto de marcadores de recepción en en, es y pt), `PhraseReader` (segmentación y lectura del cambio). Sin dependencias nuevas.
- **Pruebas**: `PhraseReaderTest` (es, en y pt) y una prueba de feature de Carlos en el librecambista; las existentes siguen pasando (incluida "vendi 50 dolares a 11.88").
- **App**: ninguna modificación; la demo de captura muestra el nuevo resultado automáticamente.
