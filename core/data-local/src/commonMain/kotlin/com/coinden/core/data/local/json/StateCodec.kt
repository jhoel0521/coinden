package com.coinden.core.data.local.json

import com.coinden.core.domain.model.ExchangeRateStrategy
import com.coinden.core.domain.model.ResponseFormat
import com.coinden.core.domain.model.SavedRateSelector
import com.coinden.core.domain.model.SelectorStatus
import com.coinden.core.domain.model.UserCurrencyProfile
import com.coinden.core.domain.model.category.CategoryRule
import com.coinden.core.domain.model.category.CustomCategory
import com.coinden.core.domain.model.wallet.Wallet
import com.coinden.core.domain.model.wallet.WalletStatus
import com.coinden.core.domain.model.wallet.WalletType
import kotlinx.serialization.Serializable

/** Formato JSON del estado configurable (billeteras, reglas, categorías propias y perfil de monedas). */
object StateCodec {
    fun encodeWallet(wallet: Wallet): String =
        CoinDenJson.encodeToString(
            WalletDto.serializer(),
            with(wallet) { WalletDto(id, ownerId, name, currency.toDto(), type.name, allowsNegative, status.name) },
        )

    fun decodeWallet(json: String): Wallet =
        CoinDenJson.decodeFromString(WalletDto.serializer(), json).let {
            Wallet(
                it.id,
                it.ownerId,
                it.name,
                it.currency.toDomain(),
                WalletType.valueOf(it.type),
                it.allowsNegative,
                WalletStatus.valueOf(it.status),
            )
        }

    fun encodeRule(rule: CategoryRule): String =
        CoinDenJson.encodeToString(
            RuleDto.serializer(),
            RuleDto(rule.ownerId, rule.key, rule.category.toDto()),
        )

    fun decodeRule(json: String): CategoryRule =
        CoinDenJson.decodeFromString(RuleDto.serializer(), json).let { CategoryRule(it.ownerId, it.key, it.category.toDomain()) }

    fun encodeCustomCategory(category: CustomCategory): String =
        CoinDenJson.encodeToString(CustomCategoryDto.serializer(), with(category) { CustomCategoryDto(id, ownerId, name, archived) })

    fun decodeCustomCategory(json: String): CustomCategory =
        CoinDenJson.decodeFromString(CustomCategoryDto.serializer(), json).let { CustomCategory(it.id, it.ownerId, it.name, it.archived) }

    fun encodeProfile(profile: UserCurrencyProfile): String =
        CoinDenJson.encodeToString(
            ProfileDto.serializer(),
            ProfileDto(
                profile.userId,
                profile.anchorCurrency.toDto(),
                profile.localCurrency.toDto(),
                profile.strategy.toDto(),
                profile.currencies.map { it.toDto() },
                profile.referenceRates.mapValues { it.value.toDto() },
            ),
        )

    fun decodeProfile(json: String): UserCurrencyProfile =
        CoinDenJson.decodeFromString(ProfileDto.serializer(), json).let {
            UserCurrencyProfile(
                it.userId,
                it.anchor.toDomain(),
                it.local.toDomain(),
                it.strategy.toDomain(),
                it.currencies.map { currency -> currency.toDomain() },
                it.referenceRates.mapValues { rate -> rate.value.toDomain() },
            )
        }
}

@Serializable
internal data class WalletDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val currency: CurrencyDto,
    val type: String,
    val allowsNegative: Boolean,
    val status: String,
)

@Serializable
internal data class RuleDto(
    val ownerId: String,
    val key: String,
    val category: CategoryRefDto,
)

@Serializable
internal data class CustomCategoryDto(
    val id: String,
    val ownerId: String,
    val name: String,
    val archived: Boolean = false,
)

@Serializable
internal data class ProfileDto(
    val userId: String,
    val anchor: CurrencyDto,
    val local: CurrencyDto,
    val strategy: StrategyDto,
    /** Otras monedas que usa (opcional: los perfiles guardados antes no lo traen). */
    val currencies: List<CurrencyDto> = emptyList(),
    /** Tasa de referencia por código de moneda (opcional, igual que [currencies]). */
    val referenceRates: Map<String, StrategyDto> = emptyMap(),
)

@Serializable
internal data class StrategyDto(
    val kind: String,
    val rate: RateDto? = null,
    val updatedAt: Long? = null,
    val sourceId: String? = null,
    val selector: SelectorDto? = null,
)

@Serializable
internal data class SelectorDto(
    val id: String,
    val targetCurrency: CurrencyDto,
    val sourceUrl: String,
    val format: String,
    val selectorExpression: String,
    val lastKnownRate: RateDto,
    val lastSuccessfulFetchAt: Long,
    val status: String,
)

private object StrategyKinds {
    const val FIXED = "fixed"
    const val MANUAL_DAILY = "manual_daily"
    const val VERIFIED = "verified"
    const val CUSTOM_SELECTOR = "custom_selector"
}

private fun ExchangeRateStrategy.toDto(): StrategyDto =
    when (this) {
        is ExchangeRateStrategy.Fixed -> StrategyDto(StrategyKinds.FIXED, rate.toDto())
        is ExchangeRateStrategy.ManualDaily -> StrategyDto(StrategyKinds.MANUAL_DAILY, rate.toDto(), updatedAtEpochMs)
        is ExchangeRateStrategy.Verified -> StrategyDto(StrategyKinds.VERIFIED, currentRate.toDto(), sourceId = sourceId)
        is ExchangeRateStrategy.CustomSelector ->
            StrategyDto(
                StrategyKinds.CUSTOM_SELECTOR,
                selector =
                    with(selector) {
                        SelectorDto(
                            id,
                            targetCurrency.toDto(),
                            sourceUrl,
                            format.name,
                            selectorExpression,
                            lastKnownRate.toDto(),
                            lastSuccessfulFetchEpochMs,
                            status.name,
                        )
                    },
            )
    }

private fun StrategyDto.toDomain(): ExchangeRateStrategy =
    when (kind) {
        StrategyKinds.FIXED -> ExchangeRateStrategy.Fixed(requireNotNull(rate).toDomain())
        StrategyKinds.MANUAL_DAILY -> ExchangeRateStrategy.ManualDaily(requireNotNull(rate).toDomain(), requireNotNull(updatedAt))
        StrategyKinds.VERIFIED -> ExchangeRateStrategy.Verified(requireNotNull(sourceId), requireNotNull(rate).toDomain())
        StrategyKinds.CUSTOM_SELECTOR ->
            with(requireNotNull(selector)) {
                ExchangeRateStrategy.CustomSelector(
                    SavedRateSelector(
                        id,
                        targetCurrency.toDomain(),
                        sourceUrl,
                        ResponseFormat.valueOf(format),
                        selectorExpression,
                        lastKnownRate.toDomain(),
                        lastSuccessfulFetchAt,
                        SelectorStatus.valueOf(status),
                    ),
                )
            }
        else -> error("Estrategia de tasa desconocida: $kind")
    }
