package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.background
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

/** Small mono pill chip used for tags, e.g. "#security". */
@Composable
fun TagChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRemove != null) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove $label",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(12.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}
