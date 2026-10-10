package com.coinden.core.domain.port.output

/**
 * Puerto de Salida (SPI) que entrega la hora actual en milisegundos epoch.
 */
interface ClockPort {
    fun nowEpochMs(): Long
}
