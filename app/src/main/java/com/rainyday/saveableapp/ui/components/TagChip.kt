package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.PillShape
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

/** Color scheme for [TagChip]'s selected/unselected look — see [TagChipPaletteDefaults]. */
@Immutable
data class TagChipPalette(
    val selectedContainerColor: Color,
    val unselectedContainerColor: Color,
    val selectedContentColor: Color,
    val unselectedContentColor: Color,
    val selectedBorderColor: Color,
    val unselectedBorderColor: Color,
)

object TagChipPaletteDefaults {
    @Composable
    fun default(): TagChipPalette {
        val colors = MaterialTheme.colorScheme
        return TagChipPalette(
            selectedContainerColor = colors.primary.copy(alpha = AppAlpha.a16),
            unselectedContainerColor = colors.surface,
            selectedContentColor = colors.primary,
            unselectedContentColor = colors.onSurfaceVariant,
            selectedBorderColor = colors.primary.copy(alpha = AppAlpha.a40),
            unselectedBorderColor = colors.outlineVariant
        )
    }
}

/**
 * Small mono pill chip used for tags, e.g. "#security". [selected] switches the chip between a filled
 * accent look (chosen) and an outlined muted look (not chosen) — used by toggleable pickers to make the
 * two states distinguishable. Read-only chips (no [onClick], e.g. on a task row) default to selected
 * since they represent a tag that is actually applied.
 */
@Composable
fun TagChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    palette: TagChipPalette = TagChipPaletteDefaults.default()
) {
    val contentColor = if (selected) palette.selectedContentColor else palette.unselectedContentColor
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) palette.selectedContainerColor else palette.unselectedContainerColor)
            .border(
                width = Dimens.d1,
                color = if (selected) palette.selectedBorderColor else palette.unselectedBorderColor,
                shape = PillShape
            )
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = Dimens.d10, vertical = Dimens.d5),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
        if (onRemove != null) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.tag_chip_remove_cd, label),
                tint = contentColor,
                modifier = Modifier
                    .padding(start = Dimens.d4)
                    .size(Dimens.d12)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagChipPreview() {
    SaveableAppTheme {
        Row(modifier = Modifier.padding(Dimens.d12), horizontalArrangement = Arrangement.spacedBy(Dimens.d8)) {
            TagChip(label = "#security", selected = true, onClick = {}, onRemove = {})
            TagChip(label = "#backlog", selected = false, onClick = {})
        }
    }
}
