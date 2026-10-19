# Reglas de R8 para la versión de publicación de CoinDen.
# Las librerías (SQLCipher, ML Kit, Tesseract, pdfbox, kotlinx.serialization, Compose) traen sus
# propias reglas; aquí va solo lo que el código de CoinDen necesita.

# Whisper: whisper_jni.c busca estas funciones nativas por nombre de clase y de método.
-keepclasseswithmembernames class com.coinden.app.perception.WhisperNative {
    native <methods>;
}
-keep class com.coinden.app.perception.WhisperNative { *; }

# Mensajes de error con números de línea útiles en los reportes de Play (sin exponer nombres).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# pdfbox: el decodificador JPEG 2000 es opcional y CoinDen no lo incluye.
-dontwarn com.gemalto.jp2.**
