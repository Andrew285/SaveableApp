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
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.ui.screens.todo.formatDate
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

/** Small colored chip showing one custom field's value on an item row. */
@Composable
fun FieldValueChip(field: FieldDefinitionEntity, rawValue: String, modifier: Modifier = Modifier) {
    val accent = parseHexColor(field.colorHex)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.d8))
            .background(accent.copy(alpha = AppAlpha.a14))
            .padding(horizontal = Dimens.d8, vertical = Dimens.d4),
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
            FieldType.RATING -> StarRatingDisplay(rating = rawValue.toIntOrNull() ?: 0, starSize = Dimens.d12, color = accent)
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

@Preview(showBackground = true)
@Composable
private fun FieldValueChipRatingPreview() {
    SaveableAppTheme {
        FieldValueChip(
            field = FieldDefinitionEntity(
                id = "field-1",
                listId = "list-1",
                name = "Rating",
                type = FieldType.RATING,
                colorHex = "#F9A825",
                createdAt = 0L,
                updatedAt = 0L
            ),
            rawValue = "4"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FieldValueChipTextPreview() {
    SaveableAppTheme {
        FieldValueChip(
            field = FieldDefinitionEntity(
                id = "field-2",
                listId = "list-1",
                name = "Author",
                type = FieldType.TEXT,
                colorHex = "#6750A4",
                createdAt = 0L,
                updatedAt = 0L
            ),
            rawValue = "Jane Austen"
        )
    }
}
