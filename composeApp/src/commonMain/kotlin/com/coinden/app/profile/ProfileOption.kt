package com.coinden.app.profile

import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.categories_title
import coinden.composeapp.generated.resources.erase_title
import coinden.composeapp.generated.resources.home_privacy
import coinden.composeapp.generated.resources.models_title
import coinden.composeapp.generated.resources.settings_title
import org.jetbrains.compose.resources.StringResource

/** Las opciones de configuración de Perfil, en el orden en que se ven (antes de Acerca de). */
enum class ProfileOption { WALLETS, CATEGORIES, PRIVACY, VOICE_MODELS, ERASE_DATA }

fun ProfileOption.label(): StringResource =
    when (this) {
        ProfileOption.WALLETS -> Res.string.settings_title
        ProfileOption.CATEGORIES -> Res.string.categories_title
        ProfileOption.PRIVACY -> Res.string.home_privacy
        ProfileOption.VOICE_MODELS -> Res.string.models_title
        ProfileOption.ERASE_DATA -> Res.string.erase_title
    }
