package com.coinden.app.lock

/** Si el teléfono puede autenticar a la persona (huella, rostro, PIN o patrón). */
enum class AuthAvailability { Available, NoScreenLock, Unsupported }

/** Resultado del aviso de autenticación del sistema. */
enum class AuthResult { Success, Cancelled, Failed }

/**
 * Puerto hacia la autenticación del propio teléfono. CoinDen nunca recibe la huella ni el PIN:
 * solo sabe si la persona se autenticó.
 */
interface DeviceAuth {
    fun availability(): AuthAvailability

    suspend fun authenticate(): AuthResult

    /** Abre los ajustes del teléfono donde se configura el bloqueo de pantalla. */
    fun openScreenLockSettings()
}

/** Sin autenticación disponible (pruebas y plataformas sin adaptador): el bloqueo no se ofrece. */
object NoDeviceAuth : DeviceAuth {
    override fun availability(): AuthAvailability = AuthAvailability.Unsupported

    override suspend fun authenticate(): AuthResult = AuthResult.Failed

    override fun openScreenLockSettings() = Unit
}
