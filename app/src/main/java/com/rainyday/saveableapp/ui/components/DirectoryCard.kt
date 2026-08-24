package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

/** Grid card for the Lists/Vault "directory" screens: a colored icon panel standing in for a
 *  cover image (the data model has no image field), a title, and an item-count subtitle. */
@Composable
fun DirectoryCard(
    title: String,
    itemCount: Int,
    icon: ImageVector,
    accentHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {}
) {
    val accent = parseHexColor(accentHex)
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .background(accent.copy(alpha = AppAlpha.a16)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(Dimens.d40)
            )
        }
        Column(modifier = Modifier.padding(Dimens.d12)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (itemCount == 1) {
                    stringResource(R.string.directory_card_item_count_singular)
                } else {
                    stringResource(R.string.directory_card_item_count_plural, itemCount)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.d2)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DirectoryCardPreview() {
    SaveableAppTheme {
        DirectoryCard(
            title = "Recipes",
            itemCount = 12,
            icon = Icons.Filled.Folder,
            accentHex = "#6750A4",
            onClick = {},
            modifier = Modifier.width(Dimens.d160)
        )
    }
}
