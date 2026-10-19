package com.coinden.app.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.back
import coinden.composeapp.generated.resources.categories_add
import coinden.composeapp.generated.resources.categories_empty
import coinden.composeapp.generated.resources.categories_error_empty
import coinden.composeapp.generated.resources.categories_hint
import coinden.composeapp.generated.resources.categories_new
import coinden.composeapp.generated.resources.categories_not_found
import coinden.composeapp.generated.resources.categories_rename
import coinden.composeapp.generated.resources.categories_rename_title
import coinden.composeapp.generated.resources.categories_title
import coinden.composeapp.generated.resources.dialog_cancel
import coinden.composeapp.generated.resources.settings_archive_action
import coinden.composeapp.generated.resources.settings_archived
import coinden.composeapp.generated.resources.settings_reactivate
import coinden.composeapp.generated.resources.settings_save
import coinden.composeapp.generated.resources.settings_storage_error
import com.coinden.app.theme.CoinDenIcon
import com.coinden.app.ui.components.ActionRole
import com.coinden.app.ui.components.BadgeTone
import com.coinden.app.ui.components.CardTone
import com.coinden.app.ui.components.CdActionSpec
import com.coinden.app.ui.components.CdActions
import com.coinden.app.ui.components.CdBadge
import com.coinden.app.ui.components.CdCard
import com.coinden.app.ui.components.CdDialog
import com.coinden.app.ui.components.CdField
import com.coinden.app.ui.components.CdNotice
import com.coinden.app.ui.components.CdTopBar
import com.coinden.app.ui.components.NoticeKind
import com.coinden.core.domain.model.category.CustomCategory
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Categorías propias: crear con el nombre que la persona escriba, renombrar, archivar y reactivar. */
@Composable
fun CustomCategoriesScreen(
    presenter: CustomCategoriesPresenter,
    onBack: () -> Unit,
) {
    val state by presenter.state.collectAsState()
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<CustomCategory?>(null) }
    LaunchedEffect(presenter) { presenter.load() }
    val add: () -> Unit = { scope.launch { if (presenter.create(name)) name = "" } }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        CdTopBar(stringResource(Res.string.categories_title), stringResource(Res.string.back), onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                CdCard(tone = CardTone.Raised) {
                    CdField(
                        name,
                        { name = it },
                        label = stringResource(Res.string.categories_new),
                        onImeAction = add,
                        enabled = !state.busy,
                    )
                    CdActions(
                        listOf(
                            CdActionSpec(
                                stringResource(Res.string.categories_add),
                                add,
                                enabled = !state.busy,
                                icon = CoinDenIcon.PLUS.vector,
                            ),
                        ),
                    )
                }
            }
            state.error?.let { error -> item { CdNotice(NoticeKind.Error, errorText(error)) } }
            if (state.loaded && state.categories.isEmpty()) {
                item { Text(stringResource(Res.string.categories_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.categories, key = { it.id }) { category ->
                CdCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(CoinDenIcon.TAG.vector, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text(category.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (category.archived) CdBadge(stringResource(Res.string.settings_archived), BadgeTone.Neutral)
                    }
                    val toggle =
                        if (category.archived) {
                            CdActionSpec(
                                stringResource(Res.string.settings_reactivate),
                                { scope.launch { presenter.reactivate(category.id) } },
                                enabled = !state.busy,
                            )
                        } else {
                            CdActionSpec(
                                stringResource(Res.string.settings_archive_action),
                                { scope.launch { presenter.archive(category.id) } },
                                ActionRole.Danger,
                                enabled = !state.busy,
                            )
                        }
                    CdActions(
                        listOf(
                            CdActionSpec(stringResource(Res.string.categories_rename), { renaming = category }, enabled = !state.busy),
                            toggle,
                        ),
                    )
                }
            }
            item {
                Text(
                    stringResource(Res.string.categories_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    renaming?.let { category ->
        var newName by remember(category.id) { mutableStateOf(category.name) }
        val save: () -> Unit = { scope.launch { if (presenter.rename(category.id, newName)) renaming = null } }
        CdDialog(
            title = stringResource(Res.string.categories_rename_title),
            onDismiss = { renaming = null },
            confirm = CdActionSpec(stringResource(Res.string.settings_save), save, ActionRole.Primary, enabled = !state.busy),
            dismiss = CdActionSpec(stringResource(Res.string.dialog_cancel), { renaming = null }),
        ) {
            CdField(newName, { newName = it }, label = stringResource(Res.string.categories_new), onImeAction = save)
        }
    }
}

@Composable
private fun errorText(error: CategoriesError): String =
    when (error) {
        CategoriesError.EMPTY_NAME -> stringResource(Res.string.categories_error_empty)
        CategoriesError.NOT_FOUND -> stringResource(Res.string.categories_not_found)
        CategoriesError.STORAGE -> stringResource(Res.string.settings_storage_error)
    }
