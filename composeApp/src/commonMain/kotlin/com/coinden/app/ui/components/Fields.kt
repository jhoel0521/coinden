package com.coinden.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** El campo de texto que tiene el foco, para que "Siguiente" salte opciones y mosaicos. */
private object FocusedField {
    var id: Any? = null
}

/** Cuántos elementos enfocables puede saltar "Siguiente" buscando el próximo campo. */
private const val MAX_HOPS = 30

/**
 * Campo de texto de CoinDen: ocupa el ancho de su padre y muestra debajo el valor anterior
 * ([before], "antes: …") o el error ([error]); [suffix] muestra la moneda junto al monto.
 */
@Composable
fun CdField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    before: String? = null,
    error: String? = null,
    warning: Boolean = false,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onImeAction: (() -> Unit)? = null,
    search: Boolean = false,
    enabled: Boolean = true,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun close() {
        focus.clearFocus()
        keyboard?.hide()
    }
    val key = ImeRule.of(hasAction = onImeAction != null, search = search)
    val id = remember { Any() }

    // Siguiente avanza hasta el próximo campo de texto, saltando opciones y mosaicos; si no hay
    // otro, cierra el teclado.
    fun next() {
        repeat(MAX_HOPS) {
            if (!focus.moveFocus(FocusDirection.Next)) return close()
            val now = FocusedField.id
            if (now != null && now !== id) return
        }
        close()
    }
    OutlinedTextField(
        value = value,
        // En un campo de varias líneas con acción, Enter (también el del teclado físico) no salta
        // de línea: ejecuta la acción, igual que "Listo".
        onValueChange = { text ->
            if (!singleLine && onImeAction != null && '\n' in text && '\n' !in value) {
                onValueChange(text.replace("\n", ""))
                onImeAction()
                close()
            } else {
                onValueChange(text)
            }
        },
        enabled = enabled,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp)) } },
        isError = error != null || warning,
        supportingText =
            when {
                error != null -> {
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                }
                before != null -> {
                    { CdChangedText(before) }
                }
                else -> null
            },
        singleLine = singleLine,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = keyboardType,
                imeAction =
                    when (key) {
                        ImeKey.Next -> ImeAction.Next
                        ImeKey.Done -> ImeAction.Done
                        ImeKey.Search -> ImeAction.Search
                    },
            ),
        // Siguiente pasa al próximo campo (o cierra el teclado en el último); Listo y Buscar
        // ejecutan la acción del formulario, si la hay, y cierran el teclado.
        keyboardActions =
            KeyboardActions(
                onNext = { next() },
                onDone = {
                    onImeAction?.invoke()
                    close()
                },
                onSearch = {
                    onImeAction?.invoke()
                    close()
                },
            ),
        modifier =
            modifier.fillMaxWidth().onFocusChanged {
                if (it.isFocused) {
                    FocusedField.id = id
                } else if (FocusedField.id === id) {
                    FocusedField.id = null
                }
            },
    )
}
