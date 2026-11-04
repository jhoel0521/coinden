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

# LiteRT-LM (modelo de lenguaje): el AAR no trae reglas propias. Su parte nativa (JNI) busca las
# clases y los métodos por nombre, y usa gson y reflexión: si R8 los renombra, el motor no carga.
-keep class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**
