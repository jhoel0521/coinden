package com.coinden.app.capture

import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.category_bills
import coinden.composeapp.generated.resources.category_education
import coinden.composeapp.generated.resources.category_fees
import coinden.composeapp.generated.resources.category_food
import coinden.composeapp.generated.resources.category_groceries
import coinden.composeapp.generated.resources.category_health
import coinden.composeapp.generated.resources.category_housing
import coinden.composeapp.generated.resources.category_leisure
import coinden.composeapp.generated.resources.category_people
import coinden.composeapp.generated.resources.category_shopping
import coinden.composeapp.generated.resources.category_subscriptions
import coinden.composeapp.generated.resources.category_transport
import coinden.composeapp.generated.resources.confidence_high
import coinden.composeapp.generated.resources.confidence_low
import coinden.composeapp.generated.resources.confidence_medium
import coinden.composeapp.generated.resources.kind_adjustment
import coinden.composeapp.generated.resources.kind_expense
import coinden.composeapp.generated.resources.kind_income
import coinden.composeapp.generated.resources.kind_not_a_movement
import coinden.composeapp.generated.resources.kind_transfer
import coinden.composeapp.generated.resources.status_captured
import coinden.composeapp.generated.resources.status_incomplete
import coinden.composeapp.generated.resources.status_ready
import coinden.composeapp.generated.resources.status_unsupported
import com.coinden.app.theme.CoinDenIcon
import com.coinden.core.domain.model.category.BaseCategory
import com.coinden.core.domain.model.draft.Confidence
import org.jetbrains.compose.resources.StringResource

/*
 * Traducción de enums del dominio y de la vista a recursos. Los `when` son exhaustivos:
 * un valor nuevo sin texto no compila.
 */

fun BaseCategory.label(): StringResource =
    when (this) {
        BaseCategory.FOOD -> Res.string.category_food
        BaseCategory.GROCERIES -> Res.string.category_groceries
        BaseCategory.TRANSPORT -> Res.string.category_transport
        BaseCategory.HEALTH -> Res.string.category_health
        BaseCategory.SUBSCRIPTIONS -> Res.string.category_subscriptions
        BaseCategory.BILLS -> Res.string.category_bills
        BaseCategory.HOUSING -> Res.string.category_housing
        BaseCategory.LEISURE -> Res.string.category_leisure
        BaseCategory.SHOPPING -> Res.string.category_shopping
        BaseCategory.EDUCATION -> Res.string.category_education
        BaseCategory.PEOPLE -> Res.string.category_people
        BaseCategory.FEES -> Res.string.category_fees
    }

/** Ícono de cada categoría base; las propias usan [CoinDenIcon.TAG]. */
fun BaseCategory.icon(): CoinDenIcon =
    when (this) {
        BaseCategory.FOOD -> CoinDenIcon.FOOD
        BaseCategory.GROCERIES -> CoinDenIcon.GROCERIES
        BaseCategory.TRANSPORT -> CoinDenIcon.BUS
        BaseCategory.HEALTH -> CoinDenIcon.HEALTH
        BaseCategory.SUBSCRIPTIONS -> CoinDenIcon.REPEAT
        BaseCategory.BILLS -> CoinDenIcon.BOLT
        BaseCategory.HOUSING -> CoinDenIcon.HOUSE
        BaseCategory.LEISURE -> CoinDenIcon.STAR
        BaseCategory.SHOPPING -> CoinDenIcon.BAG
        BaseCategory.EDUCATION -> CoinDenIcon.BOOK
        BaseCategory.PEOPLE -> CoinDenIcon.PEOPLE
        BaseCategory.FEES -> CoinDenIcon.PERCENT
    }

fun MovementKindView.label(): StringResource =
    when (this) {
        MovementKindView.EXPENSE -> Res.string.kind_expense
        MovementKindView.INCOME -> Res.string.kind_income
        MovementKindView.TRANSFER -> Res.string.kind_transfer
        MovementKindView.ADJUSTMENT -> Res.string.kind_adjustment
        MovementKindView.NOT_A_MOVEMENT -> Res.string.kind_not_a_movement
    }

fun DraftStatusView.label(): StringResource =
    when (this) {
        DraftStatusView.CAPTURED -> Res.string.status_captured
        DraftStatusView.INCOMPLETE -> Res.string.status_incomplete
        DraftStatusView.READY -> Res.string.status_ready
        DraftStatusView.UNSUPPORTED -> Res.string.status_unsupported
    }

fun Confidence.label(): StringResource =
    when (this) {
        Confidence.LOW -> Res.string.confidence_low
        Confidence.MEDIUM -> Res.string.confidence_medium
        Confidence.HIGH -> Res.string.confidence_high
    }
