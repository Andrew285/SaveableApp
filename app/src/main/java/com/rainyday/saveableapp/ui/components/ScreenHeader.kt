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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.ui.theme.EyebrowTextStyle

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
    actionContentDescription: String = "Search"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                modifier = Modifier.padding(top = if (eyebrow != null) 2.dp else 0.dp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (onActionClick != null) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(40.dp)
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
    backLabel: String = "Back",
    trailingLabel: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = backLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        if (trailingLabel != null) {
            Text(
                text = trailingLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
