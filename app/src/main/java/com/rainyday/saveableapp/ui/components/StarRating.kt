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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

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
            IconButton(onClick = { onRatingChange(if (rating == i) 0 else i) }, modifier = Modifier.size(Dimens.d36)) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = pluralStringResource(R.plurals.star_rating_cd, i, i),
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
    starSize: Dp = Dimens.d14,
    color: Color = defaultStarColor
) {
    Row(modifier = modifier) {
        for (i in 1..maxStars) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppAlpha.a40),
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StarRatingInputPreview() {
    SaveableAppTheme {
        StarRatingInput(rating = 3, onRatingChange = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StarRatingDisplayPreview() {
    SaveableAppTheme {
        StarRatingDisplay(rating = 4)
    }
}
