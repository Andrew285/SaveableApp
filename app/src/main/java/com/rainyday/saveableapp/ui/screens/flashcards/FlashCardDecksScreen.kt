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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.PillButtonPrimary
import com.rainyday.saveableapp.ui.components.ScreenHeader
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch

@Composable
fun FlashCardDecksScreen(
    onOpenDeck: (Long) -> Unit,
    onOpenSearch: () -> Unit
) {
    val container = appContainer()
    val viewModel: FlashCardDecksViewModel = viewModel(
        factory = viewModelFactory { initializer { FlashCardDecksViewModel(container.flashCardsRepository) } }
    )
    val decks by viewModel.decks.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var deckPendingEdit by remember { mutableStateOf<FlashCardDeckEntity?>(null) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val currentDecks = decks
            when {
                currentDecks == null -> LoadingIndicator(modifier = Modifier.weight(1f))
                currentDecks.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Style,
                    title = "No decks yet",
                    subtitle = "Create a deck to start collecting flashcards for anything you want to memorize.",
                    actionLabel = "New deck",
                    onAction = { showCreateDialog = true },
                    modifier = Modifier.weight(1f)
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            ScreenHeader(
                                eyebrow = "// CARDS",
                                title = "Study Decks",
                                subtitle = "Spaced-repetition memory",
                                onActionClick = onOpenSearch
                            )
                        }
                        items(currentDecks, key = { it.deck.id }) { entry ->
                            DeckRow(
                                entry = entry,
                                onClick = { onOpenDeck(entry.deck.id) },
                                onLongClick = { deckPendingEdit = entry.deck },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                    PillButtonPrimary(
                        text = "+ New Deck",
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        EditListDialog(
            title = "New deck",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { result ->
                viewModel.createDeck(result.name, result.icon, result.colorHex)
                showCreateDialog = false
            }
        )
    }

    deckPendingEdit?.let { deck ->
        EditListDialog(
            title = "Edit deck",
            initialName = deck.name,
            initialIcon = deck.icon,
            initialColorHex = deck.colorHex,
            onDismiss = { deckPendingEdit = null },
            onConfirm = { result ->
                viewModel.updateDeck(deck, result.name, result.icon, result.colorHex)
                deckPendingEdit = null
            },
            onDelete = {
                deckPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = "Deleted \"${deck.name}\"",
                        delete = { viewModel.deleteDeckWithUndo(deck) },
                        restore = { viewModel.restoreDeck(it) }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (entry.dueCount > 0) {
                    Text(
                        text = "${entry.dueCount} DUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "TOTAL CARDS: ${entry.cardCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(4.dp)
                    .clip(CircleShape)
            )
        }
    }
}
