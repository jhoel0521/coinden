# Publicar CoinDen en Google Play

Material de la ficha: [ficha.md](ficha.md) (textos en es, en y pt), [graphics/](graphics/) (ícono de 512
y gráfico destacado, generados con `python scripts/brand/export_brand.py`) y [screenshots/](screenshots/).

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
  audio y textos se quedan cifrados en el teléfono. Las cotizaciones y los modelos de voz se descargan
  sin enviar datos de la persona.
- ¿Los datos se encriptan en tránsito? **Sí** (HTTPS).
- ¿Se pueden borrar? No hay cuenta ni servidores de CoinDen; borrar la app borra todo.
- Tipos de datos a declarar (según la [divulgación de ML Kit](https://developers.google.com/ml-kit/android-data-disclosure)),
  todos **recopilados, no compartidos**, para **Estadísticas** (diagnóstico), **opcionales** para la persona:
  - Información y rendimiento de la app → **Diagnóstico** (latencia, errores, tamaño de entrada y salida, versión).
  - Dispositivo u otros identificadores → identificador por instalación (la versión incluida de ML Kit no
    usa el identificador del dispositivo).
- Nada más: sin ubicación, contactos, datos financieros, fotos, audio ni mensajes fuera del teléfono.

**Funciones financieras:** la app solo lleva un registro personal de gastos. No ofrece préstamos, pagos,
transferencias, banca, inversiones ni criptomonedas. Si la lista no tiene una opción de "presupuesto o
seguimiento de gastos", marca "Mi app no ofrece ninguna de estas funciones financieras".

**App gubernamental, de salud, de noticias:** No.

**Permisos:** cámara (fotografiar comprobantes) y micrófono (dictar gastos), ambos solo cuando la persona
toca el botón. Ninguno requiere declaración especial.

## 5. Antes de cada versión

- `npm test`, `npm run lint` y `bundleRelease` en verde.
- Instalar la release en un teléfono y probar el onboarding, una captura por voz, una foto de comprobante
  y el bloqueo: R8 reduce el código y una regla faltante solo aparece en la release.
