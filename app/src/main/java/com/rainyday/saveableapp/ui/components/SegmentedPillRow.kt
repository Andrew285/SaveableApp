package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.PillShape
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

/** A row of mutually-exclusive pill options (e.g. priority, sync frequency). Selected = tinted
 *  background + colored border + colored text; unselected = subtle surface + muted text. */
@Composable
fun <T> SegmentedPillRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    accentColor: @Composable (T) -> Color = { MaterialTheme.colorScheme.primary }
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.d8)) {
        options.forEach { option ->
            val isSelected = option == selected
            val accent = accentColor(option)
            var chipModifier = Modifier
                .clip(PillShape)
                .background(if (isSelected) accent.copy(alpha = AppAlpha.a14) else MaterialTheme.colorScheme.surfaceVariant)
            if (isSelected) {
                chipModifier = chipModifier.border(BorderStroke(Dimens.d1, accent), PillShape)
            }
            Text(
                text = label(option),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = chipModifier
                    .clickable { onSelect(option) }
                    .wrapContentWidth()
                    .padding(horizontal = Dimens.d16, vertical = Dimens.d10)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedPillRowPreview() {
    SaveableAppTheme {
        SegmentedPillRow(
            options = listOf("Low", "Medium", "High", "Urgent"),
            selected = "Medium",
            onSelect = {},
            label = { it }
        )
    }
}
