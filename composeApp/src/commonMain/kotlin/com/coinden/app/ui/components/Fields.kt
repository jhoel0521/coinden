package com.coinden.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
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
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        modifier = modifier.fillMaxWidth(),
    )
}
