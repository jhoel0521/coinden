package com.coinden.core.domain.port.output

/**
 * Puerto de Salida (SPI) para realizar peticiones HTTP a fuentes externas de cotización.
 *
 * Desacopla la lógica de dominio de librerías como Ktor, OkHttp o navegadores embebidos.
 */
interface RemoteWebFetcherPort {
    /**
     * Realiza un fetch HTTP (GET) y devuelve el cuerpo de la respuesta como texto plano (JSON o HTML).
     *
     * @param url Dirección web del recurso.
     * @return Contenido recibido en texto.
     * @throws Exception si hay error de conectividad o HTTP status no exitoso.
     */
    suspend fun fetchUrl(url: String): String
}
