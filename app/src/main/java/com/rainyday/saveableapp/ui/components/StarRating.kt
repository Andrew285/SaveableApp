package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val defaultStarColor = parseHexColor("#E8B23A")

/** Tappable 1..maxStars picker. Tapping the currently-selected star clears the rating to 0. */
@Composable
fun StarRatingInput(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    color: Color = defaultStarColor
) {
    Row(modifier = modifier) {
        for (i in 1..maxStars) {
            IconButton(onClick = { onRatingChange(if (rating == i) 0 else i) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i star${if (i == 1) "" else "s"}",
                    tint = if (i <= rating) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Read-only star display, e.g. for showing a rating value on an item row. */
@Composable
fun StarRatingDisplay(
    rating: Int,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 14.dp,
    color: Color = defaultStarColor
) {
    Row(modifier = modifier) {
        for (i in 1..maxStars) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(starSize)
            )
        }
    }
}
