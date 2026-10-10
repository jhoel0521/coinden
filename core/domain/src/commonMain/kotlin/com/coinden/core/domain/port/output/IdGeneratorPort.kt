package com.coinden.core.domain.port.output

/**
 * Puerto de Salida (SPI) que genera identificadores globales únicos en el dispositivo.
 *
 * En producción entrega UUIDs; en pruebas, secuencias deterministas.
 */
interface IdGeneratorPort {
    fun newId(): String
}
