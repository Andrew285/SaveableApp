package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.repository.FlashCardDeckSnapshot
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.PillButtonPrimary
import com.rainyday.saveableapp.ui.components.ScreenHeader
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import kotlinx.coroutines.launch

@Composable
fun FlashCardDecksScreen(
    onOpenDeck: (String) -> Unit,
    onOpenSearch: () -> Unit
) {
    val viewModel: FlashCardDecksViewModel = hiltViewModel()
    val decks by viewModel.decks.collectAsState()

    FlashCardDecksScreenContent(
        onOpenDeck = onOpenDeck,
        onOpenSearch = onOpenSearch,
        decks = decks,
        onCreateDeck = viewModel::createDeck,
        onUpdateDeck = viewModel::updateDeck,
        onDeleteDeckWithUndo = viewModel::deleteDeckWithUndo,
        onRestoreDeck = viewModel::restoreDeck
    )
}

@Composable
private fun FlashCardDecksScreenContent(
    onOpenDeck: (String) -> Unit,
    onOpenSearch: () -> Unit,
    decks: List<FlashCardDeckUiModel>?,
    onCreateDeck: (String, String, String) -> Unit = { _, _, _ -> },
    onUpdateDeck: (FlashCardDeckEntity, String, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteDeckWithUndo: suspend (FlashCardDeckEntity) -> FlashCardDeckSnapshot = { FlashCardDeckSnapshot(it, emptyList()) },
    onRestoreDeck: suspend (FlashCardDeckSnapshot) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoActionLabel = stringResource(R.string.action_undo)

    var showCreateDialog by remember { mutableStateOf(false) }
    var deckPendingEdit by remember { mutableStateOf<FlashCardDeckEntity?>(null) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val currentDecks = decks
            when {
                currentDecks == null -> LoadingIndicator(modifier = Modifier.weight(1f))
                currentDecks.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Style,
                    title = stringResource(R.string.flashcards_empty_decks_title),
                    subtitle = stringResource(R.string.flashcards_empty_decks_subtitle),
                    actionLabel = stringResource(R.string.flashcards_new_deck),
                    onAction = { showCreateDialog = true },
                    modifier = Modifier.weight(1f)
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = Dimens.d16),
                        verticalArrangement = Arrangement.spacedBy(Dimens.d12)
                    ) {
                        item {
                            ScreenHeader(
                                eyebrow = stringResource(R.string.flashcards_screen_eyebrow),
                                title = stringResource(R.string.flashcards_screen_title),
                                subtitle = stringResource(R.string.flashcards_screen_subtitle),
                                onActionClick = onOpenSearch
                            )
                        }
                        items(currentDecks, key = { it.deck.id }) { entry ->
                            DeckRow(
                                entry = entry,
                                onClick = { onOpenDeck(entry.deck.id) },
                                onLongClick = { deckPendingEdit = entry.deck },
                                modifier = Modifier.padding(horizontal = Dimens.d20)
                            )
                        }
                    }
                    PillButtonPrimary(
                        text = stringResource(R.string.flashcards_add_deck_button),
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.d20, vertical = Dimens.d12)
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        EditListDialog(
            title = stringResource(R.string.flashcards_new_deck),
            confirmLabel = stringResource(R.string.action_create),
            onDismiss = { showCreateDialog = false },
            onConfirm = { result ->
                onCreateDeck(result.name, result.icon, result.colorHex)
                showCreateDialog = false
            }
        )
    }

    deckPendingEdit?.let { deck ->
        val deletedDeckMessage = stringResource(R.string.deleted_named_item, deck.name)
        EditListDialog(
            title = stringResource(R.string.flashcards_edit_deck),
            initialName = deck.name,
            initialIcon = deck.icon,
            initialColorHex = deck.colorHex,
            onDismiss = { deckPendingEdit = null },
            onConfirm = { result ->
                onUpdateDeck(deck, result.name, result.icon, result.colorHex)
                deckPendingEdit = null
            },
            onDelete = {
                deckPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = deletedDeckMessage,
                        actionLabel = undoActionLabel,
                        delete = { onDeleteDeckWithUndo(deck) },
                        restore = { onRestoreDeck(it) }
                    )
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DeckRow(
    entry: FlashCardDeckUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (entry.cardCount > 0) (entry.cardCount - entry.dueCount).toFloat() / entry.cardCount else 0f
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.d0),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(Dimens.d16)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (entry.dueCount > 0) {
                    Text(
                        text = stringResource(R.string.flashcards_due_count, entry.dueCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.d8))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.a12))
                            .padding(horizontal = Dimens.d10, vertical = Dimens.d4)
                    )
                }
            }
            Text(
                text = stringResource(R.string.flashcards_total_cards, entry.cardCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.d2)
            )
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.d10)
                    .height(Dimens.d4)
                    .clip(CircleShape)
            )
        }
    }
}

private val previewDecks = listOf(
    FlashCardDeckUiModel(
        deck = FlashCardDeckEntity(id = "deck-1", name = "Spanish Vocabulary", icon = "language", colorHex = "#6750A4", createdAt = 0L, updatedAt = 0L),
        cardCount = 42,
        dueCount = 7
    ),
    FlashCardDeckUiModel(
        deck = FlashCardDeckEntity(id = "deck-2", name = "Anatomy Terms", icon = "school", colorHex = "#1E88E5", createdAt = 0L, updatedAt = 0L),
        cardCount = 18,
        dueCount = 0
    )
)

@Preview(showBackground = true)
@Composable
fun FlashCardDecksScreenPreview() {
    SaveableAppTheme {
        FlashCardDecksScreenContent(
            onOpenDeck = {},
            onOpenSearch = {},
            decks = previewDecks
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FlashCardDecksScreenEmptyPreview() {
    SaveableAppTheme {
        FlashCardDecksScreenContent(
            onOpenDeck = {},
            onOpenSearch = {},
            decks = emptyList()
        )
    }
}
