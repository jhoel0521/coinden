package com.coinden.core.domain.port.output

/**
 * Puerto de Salida (SPI) que identifica al dispositivo donde se registra un evento.
 */
interface DeviceIdentityPort {
    fun deviceId(): String
}
