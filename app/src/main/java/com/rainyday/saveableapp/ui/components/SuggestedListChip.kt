package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.PillShape
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

/**
 * Shown when the AI couldn't match anything existing but proposed a new list name instead of quietly
 * falling back to Uncategorized — lets the user create and switch to it, or dismiss the suggestion.
 */
@Composable
fun SuggestedListChip(
    suggestedName: String,
    onCreateAndUse: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.a10))
            .padding(start = Dimens.d10, end = Dimens.d10, top = Dimens.d4, bottom = Dimens.d4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.d16)
        )
        Text(
            text = stringResource(R.string.suggested_list_chip_message, suggestedName),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.d6)
        )
        TextButton(onClick = onCreateAndUse, modifier = Modifier.padding(end = Dimens.d10)) {
            Text(stringResource(R.string.suggested_list_chip_create_action), style = MaterialTheme.typography.labelLarge)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(Dimens.d28)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.suggested_list_chip_dismiss_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.d24)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestedListChipPreview() {
    SaveableAppTheme {
        SuggestedListChip(suggestedName = "Groceries", onCreateAndUse = {}, onDismiss = {})
    }
}
