package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.ui.screens.todo.formatDate

/** Small colored chip showing one custom field's value on an item row. */
@Composable
fun FieldValueChip(field: FieldDefinitionEntity, rawValue: String, modifier: Modifier = Modifier) {
    val accent = parseHexColor(field.colorHex)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${field.name}: ",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        when (field.type) {
            FieldType.RATING -> StarRatingDisplay(rating = rawValue.toIntOrNull() ?: 0, starSize = 12.dp, color = accent)
            FieldType.DATE -> Text(
                text = rawValue.toLongOrNull()?.let { formatDate(it) } ?: rawValue,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            FieldType.TEXT, FieldType.NUMBER -> Text(
                text = rawValue,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
