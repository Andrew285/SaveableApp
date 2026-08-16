package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.repository.CardRating
import com.rainyday.saveableapp.data.repository.SpacedRepetition
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.parseHexColor
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardStudyScreen(deckId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val viewModel: FlashCardStudyViewModel = viewModel(
        factory = viewModelFactory { initializer { FlashCardStudyViewModel(deckId, container.flashCardsRepository) } }
    )
    val deck by viewModel.deck.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val index by viewModel.index.collectAsState()
    var revealed by remember { mutableStateOf(false) }

    val currentQueue = queue
    val currentCard = currentQueue?.getOrNull(index)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deck?.name?.let { "Study: $it" } ?: "Study") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            currentQueue == null -> LoadingIndicator(modifier = Modifier.padding(padding))
            currentCard == null -> StudyCompleteState(
                studiedAnyCards = currentQueue.isNotEmpty(),
                onStudyAll = {
                    revealed = false
                    viewModel.studyAllCards()
                },
                onDone = onBack,
                modifier = Modifier.padding(padding)
            )
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                LinearProgressIndicator(
                    progress = { index.toFloat() / currentQueue.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${index + 1} of ${currentQueue.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StudyCard(
                        front = currentCard.front,
                        back = currentCard.back,
                        revealed = revealed,
                        onClick = { revealed = !revealed }
                    )
                }

                if (revealed) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RatingButton(
                            label = "Hard",
                            intervalLabel = formatInterval(SpacedRepetition.schedule(currentCard, CardRating.HARD).intervalDays),
                            containerColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                viewModel.rate(currentCard, CardRating.HARD)
                                revealed = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        RatingButton(
                            label = "Medium",
                            intervalLabel = formatInterval(SpacedRepetition.schedule(currentCard, CardRating.MEDIUM).intervalDays),
                            containerColor = MaterialTheme.colorScheme.primary,
                            onClick = {
                                viewModel.rate(currentCard, CardRating.MEDIUM)
                                revealed = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        RatingButton(
                            label = "Easy",
                            intervalLabel = formatInterval(SpacedRepetition.schedule(currentCard, CardRating.EASY).intervalDays),
                            containerColor = parseHexColor("#6FB668"),
                            onClick = {
                                viewModel.rate(currentCard, CardRating.EASY)
                                revealed = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Button(
                        onClick = { revealed = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Show Answer") }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    intervalLabel: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(vertical = 10.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = intervalLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Compact "next review in" label, e.g. 1d, 6d, 2mo, 1y. */
private fun formatInterval(days: Int): String = when {
    days <= 1 -> "1d"
    days < 30 -> "${days}d"
    days < 365 -> "${(days / 30f).roundToInt().coerceAtLeast(1)}mo"
    else -> "${(days / 365f).roundToInt().coerceAtLeast(1)}y"
}

@Composable
private fun StudyCard(front: String, back: String, revealed: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(350),
        label = "study-flip"
    )

    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                },
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                Text(text = front, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            } else {
                Text(
                    text = back,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                )
            }
        }
    }
}

@Composable
private fun StudyCompleteState(
    studiedAnyCards: Boolean,
    onStudyAll: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Text(
            text = if (studiedAnyCards) "All caught up!" else "Nothing due right now",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = if (studiedAnyCards) {
                "You've reviewed every card that was due. Nice work."
            } else {
                "No cards in this deck are due for review yet."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Button(onClick = onDone, modifier = Modifier.padding(top = 20.dp)) {
            Text("Done")
        }
        TextButton(onClick = onStudyAll, modifier = Modifier.padding(top = 4.dp)) {
            Text("Study all cards anyway")
        }
    }
}

