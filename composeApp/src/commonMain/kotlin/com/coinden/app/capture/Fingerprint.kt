package com.coinden.app.capture

/** Huella SHA-256 en hexadecimal: reconoce el mismo archivo enviado dos veces sin guardarlo legible. */
expect fun sha256Hex(bytes: ByteArray): String
