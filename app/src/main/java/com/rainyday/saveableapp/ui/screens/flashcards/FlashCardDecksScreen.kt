package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.components.ListRow
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FlashCardDecksScreen(
    onOpenDeck: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flashcards") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New deck")
            }
        }
    ) { padding ->
        val currentDecks = decks
        when {
            currentDecks == null -> LoadingIndicator(modifier = Modifier.padding(padding))
            currentDecks.isEmpty() -> EmptyState(
                icon = Icons.Filled.Style,
                title = "No decks yet",
                subtitle = "Create a deck to start collecting flashcards for anything you want to memorize.",
                actionLabel = "New deck",
                onAction = { showCreateDialog = true },
                modifier = Modifier.padding(padding)
            )
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentDecks, key = { it.deck.id }) { entry ->
                        val deck = entry.deck
                        ListRow(
                            title = deck.name,
                            subtitle = if (entry.cardCount > 0) {
                                "${entry.cardCount} card${if (entry.cardCount == 1) "" else "s"}"
                            } else null,
                            icon = IconCatalog.resolve(deck.icon),
                            accentHex = deck.colorHex,
                            onClick = { onOpenDeck(deck.id) },
                            onLongClick = { deckPendingEdit = deck }
                        )
                    }
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
