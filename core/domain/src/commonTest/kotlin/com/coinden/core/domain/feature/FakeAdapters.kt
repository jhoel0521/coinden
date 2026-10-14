package com.coinden.core.domain.feature

import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.port.output.CurrencyProfileRepositoryPort
import com.coinden.core.domain.port.output.RemoteWebFetcherPort

/**
 * Adaptador en memoria de prueba para el repositorio de perfiles de divisa.
 */
class InMemoryCurrencyProfileRepository : CurrencyProfileRepositoryPort {
    private val profiles = mutableMapOf<String, UserCurrencyProfile>()

    override suspend fun saveProfile(profile: UserCurrencyProfile) {
        profiles[profile.userId] = profile
    }

    override suspend fun getProfile(userId: String): UserCurrencyProfile? = profiles[userId]

    fun clear() {
        profiles.clear()
    }
}

/**
 * Adaptador falso de prueba para simular respuestas remotas HTTP sin red externa.
 */
class FakeWebFetcher : RemoteWebFetcherPort {
    private val responses = mutableMapOf<String, String>()

    fun setResponse(
        url: String,
        content: String,
    ) {
        responses[url] = content
    }

    override suspend fun fetchUrl(url: String): String = responses[url] ?: throw IllegalStateException("404 Not Found simulado para: $url")
}
