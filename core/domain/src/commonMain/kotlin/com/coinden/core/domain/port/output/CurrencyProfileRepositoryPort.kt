package com.coinden.core.domain.port.output

import com.coinden.core.domain.model.UserCurrencyProfile

/**
 * Puerto de Salida (SPI) para persistir y consultar el perfil de divisas del usuario.
 */
interface CurrencyProfileRepositoryPort {
    suspend fun saveProfile(profile: UserCurrencyProfile)

    suspend fun getProfile(userId: String): UserCurrencyProfile?
}
