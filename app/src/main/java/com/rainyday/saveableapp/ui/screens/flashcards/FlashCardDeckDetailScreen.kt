package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.local.FlashCardEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardDeckDetailScreen(deckId: Long, onBack: () -> Unit, onStudy: () -> Unit) {
    val container = appContainer()
    val viewModel: FlashCardViewModel = viewModel(
        factory = viewModelFactory { initializer { FlashCardViewModel(deckId, container.flashCardsRepository) } }
    )
    val deck by viewModel.deck.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var cardPendingEdit by remember { mutableStateOf<FlashCardEntity?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveCard(from.index, to.index)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New card")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.rainyday.saveableapp.ui.components.DetailHeader(
                    onBack = onBack,
                    backLabel = "Cards",
                    modifier = Modifier.weight(1f)
                )
                if (cards.isNotEmpty()) {
                    IconButton(onClick = onStudy) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Study this deck")
                    }
                }
            }
            Text(
                text = deck?.name ?: "Deck",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            if (cards.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Style,
                    title = "No cards yet",
                    subtitle = "Add a card with a front and back side, then tap it to flip and study.",
                    actionLabel = "New card",
                    onAction = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(cards, key = { _, card -> card.id }) { _, card ->
                        ReorderableItem(reorderableState, key = card.id) { _ ->
                            FlipCardRow(
                                card = card,
                                onEdit = { cardPendingEdit = card },
                                dragHandle = { Modifier.draggableHandle() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        FlashCardEditDialog(
            title = "New card",
            onDismiss = { showAddDialog = false },
            onConfirm = { front, back ->
                viewModel.createCard(front, back)
                showAddDialog = false
            }
        )
    }

    cardPendingEdit?.let { card ->
        FlashCardEditDialog(
            title = "Edit card",
            initialFront = card.front,
            initialBack = card.back,
            onDismiss = { cardPendingEdit = null },
            onConfirm = { front, back ->
                viewModel.updateCard(card, front, back)
                cardPendingEdit = null
            },
            onDelete = {
                cardPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = "Deleted card",
                        delete = { viewModel.deleteCardWithUndo(card) },
                        restore = { viewModel.restoreCard(it) }
                    )
                }
            }
        )
    }
}

@Composable
private fun FlipCardRow(
    card: FlashCardEntity,
    onEdit: () -> Unit,
    dragHandle: @Composable () -> Modifier
) {
    var showBack by remember(card.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (showBack) 180f else 0f,
        animationSpec = tween(350),
        label = "flip"
    )

    Card(
        onClick = { showBack = !showBack },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp)
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                if (rotation <= 90f) {
                    Text(
                        text = card.front,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = card.back,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.graphicsLayer { rotationY = 180f }
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit card")
            }
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.then(dragHandle())
            )
        }
    }
}
