package com.rainyday.saveableapp.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.data.links.LinkPreview
import com.rainyday.saveableapp.data.links.linkHostLabel
import com.rainyday.saveableapp.ui.rememberAppEntryPoint
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

/**
 * Static preview card for a link — a thumbnail plus title, resolved off-thread and cached in memory.
 * Tapping it opens [url] in the browser. Shows a plain fallback (host name, link icon) while loading
 * or if no metadata could be resolved.
 */
@Composable
fun LinkPreviewCard(url: String, modifier: Modifier = Modifier) {
    val linkPreviewRepository = rememberAppEntryPoint().linkPreviewRepository()
    val context = LocalContext.current
    var preview by remember(url) { mutableStateOf<LinkPreview?>(null) }

    LaunchedEffect(url) {
        preview = linkPreviewRepository.preview(url)
    }

    val title = preview?.title?.takeIf { it.isNotBlank() } ?: linkHostLabel(url) ?: url

    LinkPreviewCardContent(
        title = title,
        hostLabel = linkHostLabel(url) ?: url,
        thumbnailUrl = preview?.imageUrl,
        onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        },
        modifier = modifier
    )
}

@Composable
private fun LinkPreviewCardContent(
    title: String,
    hostLabel: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.d10))
            .background(MaterialTheme.colorScheme.surface)
            .border(Dimens.d1, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Dimens.d10))
            .clickable(onClick = onClick)
            .padding(Dimens.d8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteThumbnail(
            url = thumbnailUrl,
            modifier = Modifier
                .size(Dimens.d44)
                .clip(RoundedCornerShape(Dimens.d8))
        )
        Column(
            modifier = Modifier
                .padding(start = Dimens.d10)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hostLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Dimens.d2)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LinkPreviewCardContentPreview() {
    SaveableAppTheme {
        LinkPreviewCardContent(
            title = "Kotlin Coroutines on Android",
            hostLabel = "developer.android.com",
            thumbnailUrl = null,
            onClick = {}
        )
    }
}
