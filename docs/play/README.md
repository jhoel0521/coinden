# Publicar CoinDen en Google Play

Material de la ficha: [ficha.md](ficha.md) (textos en es, en y pt), [graphics/](graphics/) (ícono de 512
y gráfico destacado, generados con `python scripts/brand/export_brand.py`) y [screenshots/](screenshots/) (una carpeta por idioma: `es-419`, `en-US`, `pt-BR`).

## 1. La llave de subida (una sola vez, en tu computadora)

La llave firma cada versión que subes. **Nunca entra al repositorio** (`.gitignore` ignora `*.jks`,
`*.keystore` y `keystore.properties`). Guárdala junto con sus contraseñas en un lugar seguro y con
respaldo: con Play App Signing, si la pierdes, Google puede cambiarla, pero el trámite es lento.

```bash
keytool -genkeypair -v -keystore C:/Users/SISTEMA/llaves/coinden-upload.jks -alias coinden -keyalg RSA -keysize 4096 -validity 10000
```

Para compilar en tu computadora, crea `keystore.properties` en la raíz del repo (ignorado por git):

```properties
storeFile=C:/Users/SISTEMA/llaves/coinden-upload.jks
storePassword=la-contraseña-del-almacén
keyAlias=coinden
keyPassword=la-contraseña-de-la-llave
```

Con ese archivo, `./gradlew :composeApp:bundleRelease -PversionName=0.1.0 -PversionCode=100` deja el AAB
firmado en `composeApp/build/outputs/bundle/release/composeApp-release.aab`. Sin el archivo, la release se
firma con la llave de depuración: sirve para probar en el teléfono, **no para subir a Play**.

## 2. El AAB desde GitHub (opcional, recomendado)

En GitHub → Settings → Secrets and variables → Actions, crea estos secretos del repositorio:

| Secreto | Valor |
|---|---|
| `COINDEN_KEYSTORE_BASE64` | el `.jks` en base64: `base64 -w0 coinden-upload.jks` (en Git Bash) |
| `COINDEN_KEYSTORE_PASSWORD` | contraseña del almacén |
| `COINDEN_KEY_ALIAS` | `coinden` |
| `COINDEN_KEY_PASSWORD` | contraseña de la llave |

Después, cada etiqueta `vX.Y.Z` arma el AAB firmado (workflow `release.yml`) y lo deja en la pestaña
Actions como artefacto `coinden-X.Y.Z-aab`:

```bash
git tag v0.1.0 && git push origin v0.1.0
```

El `versionCode` sale de la etiqueta: `X*10000 + Y*100 + Z` (v0.1.0 → 100). Play exige que cada subida
tenga un `versionCode` mayor que la anterior.

## 3. Play Console

1. Crea la cuenta de desarrollador en https://play.google.com/console (pago único de USD 25 y
   verificación de identidad).
2. **Crear app:** nombre "CoinDen", idioma predeterminado español (Latinoamérica), App, Gratis.
3. **Prueba cerrada obligatoria** (cuentas personales nuevas): sube el AAB a una prueba cerrada, invita a
   **al menos 12 testers** (una lista de correos o un Grupo de Google) y mantenlos con la app instalada
   **14 días seguidos**. Recién después se habilita pedir el acceso a Producción.
4. Al subir el primer AAB, acepta **Play App Signing** (Google guarda la llave de la app; tú solo
   conservas la de subida).
5. Completa **Presencia en la tienda** con [ficha.md](ficha.md) y los gráficos.
6. Completa **Contenido de la app** con las respuestas de abajo.

## 4. Respuestas para "Contenido de la app"

**Política de privacidad:** https://jhoel0521.github.io/coinden/privacidad/

**Acceso a la app:** "Toda la funcionalidad está disponible sin restricciones especiales". No hay cuenta
ni inicio de sesión; el bloqueo es opcional, viene apagado y usa la huella o el PIN del propio teléfono.

**Anuncios:** No, la app no contiene anuncios.

**Clasificación de contenido:** categoría "Utilidad, productividad, comunicación u otra". Responde "No"
a violencia, sexualidad, lenguaje, drogas, apuestas y contenido generado por usuarios; la app no comparte
la ubicación ni permite comprar bienes digitales. Resultado esperado: apta para todo público.

**Público objetivo:** 18 años o más. La app no está dirigida a niños; así no aplican las reglas de Familias.

**Seguridad de los datos:**

- ¿Recopila o comparte datos del usuario? **Sí**, solo por el lector de comprobantes de Google (ML Kit),
  que manda métricas técnicas a Google. CoinDen no envía nada propio: los movimientos, montos, fotos,
  audio y textos se quedan cifrados en el teléfono. Los modelos de voz y los paquetes se descargan sin
  enviar datos de la persona; la app todavía no consulta equivalencias.
- ¿Los datos se encriptan en tránsito? **Sí** (HTTPS).
- ¿Se pueden borrar? Dentro de la app, **sí**: Perfil → Borrar mis datos elimina de ese teléfono la base,
  su llave, las fotos, PDF y audios, la cola y las preferencias (con huella o PIN si el bloqueo está
  activo), y la app vuelve a la bienvenida. Desinstalar también borra todo. No hay cuenta ni servidores de
  CoinDen, así que no hay copia que borrar fuera del teléfono. Las métricas técnicas que Google ya recibió
  por el lector opcional no las guarda CoinDen: se rigen por las condiciones de ML Kit.
- Tipos de datos a declarar (según la [divulgación de ML Kit](https://developers.google.com/ml-kit/android-data-disclosure)),
  todos **recopilados, no compartidos**, para **Estadísticas** (diagnóstico), **opcionales** para la persona:
  - Información y rendimiento de la app → **Diagnóstico** (latencia, errores, tamaño de entrada y salida, versión).
  - Dispositivo u otros identificadores → identificador por instalación (la versión incluida de ML Kit no
    usa el identificador del dispositivo).
- Nada más: sin ubicación, contactos, datos financieros, fotos, audio ni mensajes fuera del teléfono.

**Funciones financieras:** marca **"Mi app no ofrece ninguna función financiera"** y deja la categoría en
**Productividad**. CoinDen solo anota lo que la persona ya gastó: no mueve dinero, no guarda fondos, no se
conecta a bancos y no presta ni asesora. Google rechazó la revisión con cada combinación probada
(ninguna función u "Otro", con categoría Finanzas o Productividad) sin decir qué fallaba; por eso la app,
el sitio y la ficha usan el vocabulario de una libreta de gastos (sección 5) y no hablan de billeteras,
saldos, tasas ni cripto. No marques "Pagos móviles y billeteras digitales" ni "Billetera de
criptomonedas": la app no hace eso, y marcarlas pediría licencias que no aplican.

Si Play pide describir la función, el texto de respaldo es:

> CoinDen es un registro personal de gastos (una libreta). La persona anota a mano, por voz o con la foto
> de un comprobante lo que ya gastó, y ve un total anotado por "fuente", que es solo una etiqueta de
> registro (efectivo, cuenta o tarjeta) que la persona crea y escribe a mano. La app NO mueve dinero, NO
> hace pagos ni transferencias, NO guarda fondos, NO se conecta a bancos, NO ofrece préstamos, crédito,
> inversiones ni asesoramiento financiero. Todos los datos quedan cifrados en el teléfono; no hay cuentas
> ni servidores.

En el paso "Documentación", Google indicó que por ahora no hace falta adjuntar nada.

**App gubernamental, de salud, de noticias:** No.

**Permisos:** cámara (fotografiar comprobantes) y micrófono (dictar gastos), ambos solo cuando la persona
toca el botón. Ninguno requiere declaración especial.

## 5. Nombres visibles en la app

Desde la 0.2.1, la app, el sitio y la ficha usan el vocabulario de una libreta de gastos, no el de un
servicio financiero:

| Antes | Ahora (es · en · pt) |
|---|---|
| billetera | fuente · source · fonte |
| patrimonio, saldo | total anotado, total · recorded total, total · total anotado, total |
| tasa, cotización | equivalencia · equivalence · equivalência |
| pago | gasto · expense · gasto |
| Banco, Cripto, Tarjeta de crédito (tipos) | Cuenta, Personalizada, Tarjeta · Account, Custom, Card · Conta, Personalizada, Cartão |
| Transferencia (entre fuentes) | Traspaso · Move · Repasse |

El medio de pago "Transferencia o QR" no cambia: describe cómo pagó la persona, como lo dice su
comprobante. Los nombres que escribe la persona y los alias del dictado no son texto de la app. Una prueba
(`VisibleVocabularyTest`) falla si alguna palabra retirada vuelve a un `strings.xml`. La ficha, el sitio y las
capturas deben usar estos mismos nombres.

## 6. Antes de cada versión

- `npm test`, `npm run lint` y `bundleRelease` en verde.
- Instalar la release en un teléfono y probar el onboarding, una captura por voz, una foto de comprobante
  y el bloqueo: R8 reduce el código y una regla faltante solo aparece en la release.
