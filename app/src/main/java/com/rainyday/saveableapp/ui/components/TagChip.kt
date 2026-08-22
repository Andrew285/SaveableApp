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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.ui.theme.PillShape

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
    onRemove: (() -> Unit)? = null
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                shape = PillShape
            )
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 10.dp, vertical = 5.dp),
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
                contentDescription = "Remove $label",
                tint = contentColor,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(12.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}
