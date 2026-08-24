package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.FlashCardDeckEntity
import com.rainyday.saveableapp.data.local.FlashCardEntity
import com.rainyday.saveableapp.data.repository.CardRating
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.EyebrowTextStyle
import com.rainyday.saveableapp.ui.theme.PillShape
import com.rainyday.saveableapp.ui.theme.PriorityColors
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

@Composable
fun FlashCardStudyScreen(deckId: String, onBack: () -> Unit) {
    val viewModel: FlashCardStudyViewModel = hiltViewModel()
    val deck by viewModel.deck.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val index by viewModel.index.collectAsState()

    FlashCardStudyScreenContent(
        onBack = onBack,
        deck = deck,
        queue = queue,
        index = index,
        onStudyAllCards = viewModel::studyAllCards,
        onRate = viewModel::rate
    )
}

@Composable
private fun FlashCardStudyScreenContent(
    onBack: () -> Unit,
    deck: FlashCardDeckEntity?,
    queue: List<FlashCardEntity>?,
    index: Int,
    onStudyAllCards: () -> Unit = {},
    onRate: (FlashCardEntity, CardRating) -> Unit = { _, _ -> }
) {
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
                    onStudyAllCards()
                },
                onDone = onBack,
                modifier = Modifier.padding(padding)
            )
            else -> Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = Dimens.d20)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.d12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onBack)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.flashcards_exit_study_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.d18)
                        )
                        Text(
                            text = stringResource(R.string.flashcards_exit_study_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Dimens.d6)
                        )
                    }
                    Text(
                        text = stringResource(R.string.flashcards_reviewed_progress, index, currentQueue.size),
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
                        .padding(vertical = Dimens.d16),
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
                        text = stringResource(R.string.flashcards_rate_difficulty),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Dimens.d10),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.d8),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Dimens.d24)
                    ) {
                        RatingButton(
                            label = stringResource(R.string.flashcards_rating_again),
                            intervalLabel = stringResource(R.string.flashcards_interval_again),
                            palette = RatingButtonPaletteDefaults.again(),
                            onClick = { onRate(currentCard, CardRating.AGAIN); revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                        RatingButton(
                            label = stringResource(R.string.flashcards_rating_hard),
                            intervalLabel = stringResource(R.string.flashcards_interval_hard),
                            palette = RatingButtonPaletteDefaults.hard(),
                            onClick = { onRate(currentCard, CardRating.HARD); revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                        RatingButton(
                            label = stringResource(R.string.flashcards_rating_good),
                            intervalLabel = stringResource(R.string.flashcards_interval_good),
                            palette = RatingButtonPaletteDefaults.good(),
                            onClick = { onRate(currentCard, CardRating.GOOD); revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                        RatingButton(
                            label = stringResource(R.string.flashcards_rating_easy),
                            intervalLabel = stringResource(R.string.flashcards_interval_easy),
                            palette = RatingButtonPaletteDefaults.easy(),
                            onClick = { onRate(currentCard, CardRating.EASY); revealed = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Dimens.d24),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { revealed = true }) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = null, modifier = Modifier.size(Dimens.d16))
                            Text(stringResource(R.string.flashcards_tap_to_flip_hint), modifier = Modifier.padding(start = Dimens.d4))
                        }
                    }
                }
            }
        }
    }
}

/** Color for a single spaced-repetition rating pill — see [RatingButtonPaletteDefaults]. */
@Immutable
data class RatingButtonPalette(val accentColor: Color)

object RatingButtonPaletteDefaults {
    @Composable
    fun again(): RatingButtonPalette = RatingButtonPalette(MaterialTheme.colorScheme.error)

    @Composable
    fun hard(): RatingButtonPalette = RatingButtonPalette(PriorityColors.high)

    @Composable
    fun good(): RatingButtonPalette = RatingButtonPalette(MaterialTheme.colorScheme.primary)

    @Composable
    fun easy(): RatingButtonPalette = RatingButtonPalette(PriorityColors.medium)
}

@Composable
private fun RatingButton(
    label: String,
    intervalLabel: String,
    palette: RatingButtonPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = PillShape,
        border = BorderStroke(Dimens.d1, palette.accentColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accentColor),
        contentPadding = PaddingValues(vertical = Dimens.d10),
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
            .padding(Dimens.d24),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (eyebrow.isNotBlank()) {
                Text(
                    text = stringResource(R.string.flashcards_study_card_eyebrow, eyebrow.uppercase()),
                    style = EyebrowTextStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = Dimens.d20)
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
            .padding(Dimens.d32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(Dimens.d56),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.a60)
        )
        Text(
            text = if (studiedAnyCards) {
                stringResource(R.string.flashcards_study_complete_title)
            } else {
                stringResource(R.string.flashcards_study_nothing_due_title)
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Dimens.d16)
        )
        Text(
            text = if (studiedAnyCards) {
                stringResource(R.string.flashcards_study_complete_subtitle)
            } else {
                stringResource(R.string.flashcards_study_nothing_due_subtitle)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.d4)
        )
        Button(onClick = onDone, modifier = Modifier.padding(top = Dimens.d20)) {
            Text(stringResource(R.string.action_done))
        }
        TextButton(onClick = onStudyAll, modifier = Modifier.padding(top = Dimens.d4)) {
            Text(stringResource(R.string.flashcards_study_all_anyway))
        }
    }
}

private val previewStudyDeck = FlashCardDeckEntity(id = "deck-1", name = "Spanish Vocabulary", icon = "language", colorHex = "#6750A4", createdAt = 0L, updatedAt = 0L)

private val previewStudyQueue = listOf(
    FlashCardEntity(id = "card-1", deckId = "deck-1", front = "Casa", back = "House", createdAt = 0L, updatedAt = 0L),
    FlashCardEntity(id = "card-2", deckId = "deck-1", front = "Perro", back = "Dog", createdAt = 0L, updatedAt = 0L)
)

@Preview(showBackground = true)
@Composable
fun FlashCardStudyScreenPreview() {
    SaveableAppTheme {
        FlashCardStudyScreenContent(
            onBack = {},
            deck = previewStudyDeck,
            queue = previewStudyQueue,
            index = 0
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FlashCardStudyScreenCompletePreview() {
    SaveableAppTheme {
        FlashCardStudyScreenContent(
            onBack = {},
            deck = previewStudyDeck,
            queue = previewStudyQueue,
            index = previewStudyQueue.size
        )
    }
}
