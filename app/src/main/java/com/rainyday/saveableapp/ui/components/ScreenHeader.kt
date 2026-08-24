package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.EyebrowTextStyle
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

/**
 * Header used at the top of each bottom-tab screen's scrollable content: an optional small mono
 * eyebrow line, a large bold title, an optional gray subtitle, and a trailing circular action
 * button (search, by default — the mockups show a profile glyph there, repurposed since this app
 * has no profile feature).
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionContentDescription: String = stringResource(R.string.cd_search)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.d20, vertical = Dimens.d8),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = EyebrowTextStyle,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = if (eyebrow != null) Dimens.d2 else Dimens.d0)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Dimens.d2)
                )
            }
        }
        if (onActionClick != null) {
            Box(
                modifier = Modifier
                    .padding(start = Dimens.d12)
                    .size(Dimens.d40)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = actionContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Header for sub-screens: a mint "< Back" affordance plus an optional right-aligned mono label. */
@Composable
fun DetailHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backLabel: String = stringResource(R.string.cd_back),
    trailingLabel: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.d20, vertical = Dimens.d12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onBack)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = backLabel,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.d18)
            )
            Text(
                text = backLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Dimens.d6)
            )
        }
        if (trailingLabel != null) {
            Text(
                text = trailingLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Dimens.d12),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenHeaderPreview() {
    SaveableAppTheme {
        ScreenHeader(
            eyebrow = "// TASKS",
            title = "Active Tasks",
            subtitle = "Everything on your plate",
            onActionClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailHeaderPreview() {
    SaveableAppTheme {
        DetailHeader(onBack = {}, trailingLabel = "3 items")
    }
}
