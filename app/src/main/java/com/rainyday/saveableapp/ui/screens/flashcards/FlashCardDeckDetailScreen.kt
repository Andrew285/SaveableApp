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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.local.FlashCardEntity
import com.rainyday.saveableapp.ui.components.DetailHeader
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardDeckDetailScreen(deckId: String, onBack: () -> Unit, onStudy: () -> Unit) {
    val viewModel: FlashCardViewModel = hiltViewModel()
    val deck by viewModel.deck.collectAsState()
    val cards by viewModel.cards.collectAsState()

    FlashCardDeckDetailScreenContent(
        onBack = onBack,
        onStudy = onStudy,
        deck = deck,
        cards = cards,
        onMoveCard = viewModel::moveCard,
        onCreateCard = viewModel::createCard,
        onUpdateCard = viewModel::updateCard,
        onDeleteCardWithUndo = viewModel::deleteCardWithUndo,
        onRestoreCard = viewModel::restoreCard
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashCardDeckDetailScreenContent(
    onBack: () -> Unit,
    onStudy: () -> Unit,
    deck: FlashCardDeckEntity?,
    cards: List<FlashCardEntity>,
    onMoveCard: (Int, Int) -> Unit = { _, _ -> },
    onCreateCard: (String, String) -> Unit = { _, _ -> },
    onUpdateCard: (FlashCardEntity, String, String) -> Unit = { _, _, _ -> },
    onDeleteCardWithUndo: suspend (FlashCardEntity) -> FlashCardEntity = { it },
    onRestoreCard: suspend (FlashCardEntity) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoActionLabel = stringResource(R.string.action_undo)
    val deletedCardMessage = stringResource(R.string.flashcards_deleted_card_message)
    val newCardLabel = stringResource(R.string.flashcards_new_card)
    val editCardLabel = stringResource(R.string.flashcards_edit_card)

    var showAddDialog by remember { mutableStateOf(false) }
    var cardPendingEdit by remember { mutableStateOf<FlashCardEntity?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveCard(from.index, to.index)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = newCardLabel)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DetailHeader(
                    onBack = onBack,
                    backLabel = stringResource(R.string.nav_cards),
                    modifier = Modifier.weight(1f)
                )
                if (cards.isNotEmpty()) {
                    IconButton(onClick = onStudy) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.flashcards_study_this_deck_cd))
                    }
                }
            }
            Text(
                text = deck?.name ?: stringResource(R.string.flashcards_deck_fallback_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = Dimens.d20)
            )
            if (cards.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Style,
                    title = stringResource(R.string.flashcards_empty_cards_title),
                    subtitle = stringResource(R.string.flashcards_empty_cards_subtitle),
                    actionLabel = newCardLabel,
                    onAction = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(Dimens.d20, Dimens.d8, Dimens.d20, Dimens.d96),
                    verticalArrangement = Arrangement.spacedBy(Dimens.d10)
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
            title = newCardLabel,
            onDismiss = { showAddDialog = false },
            onConfirm = { front, back ->
                onCreateCard(front, back)
                showAddDialog = false
            }
        )
    }

    cardPendingEdit?.let { card ->
        FlashCardEditDialog(
            title = editCardLabel,
            initialFront = card.front,
            initialBack = card.back,
            onDismiss = { cardPendingEdit = null },
            onConfirm = { front, back ->
                onUpdateCard(card, front, back)
                cardPendingEdit = null
            },
            onDelete = {
                cardPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = deletedCardMessage,
                        actionLabel = undoActionLabel,
                        delete = { onDeleteCardWithUndo(card) },
                        restore = { onRestoreCard(it) }
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
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.d0),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.d8, vertical = Dimens.d4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Dimens.d88)
                    .padding(horizontal = Dimens.d8)
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
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.flashcards_edit_card))
            }
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = stringResource(R.string.cd_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.then(dragHandle())
            )
        }
    }
}

private val previewDeck = FlashCardDeckEntity(id = "deck-1", name = "Spanish Vocabulary", icon = "language", colorHex = "#6750A4", createdAt = 0L, updatedAt = 0L)

private val previewCards = listOf(
    FlashCardEntity(id = "card-1", deckId = "deck-1", front = "Casa", back = "House", createdAt = 0L, updatedAt = 0L),
    FlashCardEntity(id = "card-2", deckId = "deck-1", front = "Perro", back = "Dog", createdAt = 0L, updatedAt = 0L),
    FlashCardEntity(id = "card-3", deckId = "deck-1", front = "Libro", back = "Book", createdAt = 0L, updatedAt = 0L)
)

@Preview(showBackground = true)
@Composable
fun FlashCardDeckDetailScreenPreview() {
    SaveableAppTheme {
        FlashCardDeckDetailScreenContent(
            onBack = {},
            onStudy = {},
            deck = previewDeck,
            cards = previewCards
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FlashCardDeckDetailScreenEmptyPreview() {
    SaveableAppTheme {
        FlashCardDeckDetailScreenContent(
            onBack = {},
            onStudy = {},
            deck = previewDeck,
            cards = emptyList()
        )
    }
}
