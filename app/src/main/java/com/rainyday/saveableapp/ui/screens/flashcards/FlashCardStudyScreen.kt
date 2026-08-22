package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.repository.CardRating
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.theme.EyebrowTextStyle
import com.rainyday.saveableapp.ui.theme.PillShape

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

    Scaffold { padding ->
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
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onBack)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Exit study",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Exit Study",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Text(
                        text = "$index / ${currentQueue.size} reviewed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
                LinearProgressIndicator(
                    progress = { index.toFloat() / currentQueue.size },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StudyCard(
                        eyebrow = deck?.name.orEmpty(),
                        front = currentCard.front,
                        back = currentCard.back,
                        revealed = revealed,
                        onClick = { revealed = true }
                    )
                }

                if (revealed) {
                    Text(
                        text = "RATE RETRIEVAL DIFFICULTY:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        RatingButton(
                            label = "Again",
                            intervalLabel = "<1m",
                            accent = MaterialTheme.colorScheme.error,
                            onClick = { viewModel.rate(currentCard, CardRating.AGAIN); revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                        RatingButton(
                            label = "Hard",
                            intervalLabel = "12h",
                            accent = com.rainyday.saveableapp.ui.theme.PriorityColors.high,
                            onClick = { viewModel.rate(currentCard, CardRating.HARD); revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                        RatingButton(
                            label = "Good",
                            intervalLabel = "4d",
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = { viewModel.rate(currentCard, CardRating.GOOD); revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                        RatingButton(
                            label = "Easy",
                            intervalLabel = "8d",
                            accent = com.rainyday.saveableapp.ui.theme.PriorityColors.medium,
                            onClick = { viewModel.rate(currentCard, CardRating.EASY); revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { revealed = true }) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" Tap to flip hint", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    intervalLabel: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        shape = PillShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
        contentPadding = PaddingValues(vertical = 10.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = intervalLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StudyCard(eyebrow: String, front: String, back: String, revealed: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (eyebrow.isNotBlank()) {
                Text(
                    text = "// ${eyebrow.uppercase()}",
                    style = EyebrowTextStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
            Text(
                text = if (revealed) back else front,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = if (revealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
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
