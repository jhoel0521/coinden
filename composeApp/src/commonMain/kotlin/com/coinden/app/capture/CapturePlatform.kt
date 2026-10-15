package com.coinden.app.capture

import androidx.compose.runtime.Composable

/**
 * Lo que solo la plataforma sabe hacer: el reconocimiento de voz local, los permisos y la cámara.
 * La UI compartida lo recibe ya armado (en Android lo da `MainActivity`).
 */
interface CapturePlatform {
    val dictation: DictationPort

    /**
     * Devuelve una acción que pide el micrófono si hace falta y luego llama a [onResult] con
     * true si está concedido.
     */
    @Composable
    fun rememberMicrophoneRequest(onResult: (granted: Boolean) -> Unit): () -> Unit

    /**
     * Cámara a pantalla completa con disparador y botón de galería. [onPhoto] recibe un JPEG
     * reducido a 2000 px por lado; [onDenied] se llama si no hay permiso de cámara.
     */
    @Composable
    fun Camera(
        onPhoto: (bytes: ByteArray, mimeType: String) -> Unit,
        onDenied: () -> Unit,
        onClose: () -> Unit,
    )
}
