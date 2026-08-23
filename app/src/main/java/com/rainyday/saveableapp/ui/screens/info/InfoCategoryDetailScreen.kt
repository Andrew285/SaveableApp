package com.rainyday.saveableapp.ui.screens.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.data.local.InfoBlockEntity
import com.rainyday.saveableapp.ui.rememberAppEntryPoint
import com.rainyday.saveableapp.ui.components.DetailHeader
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.ui.screens.todo.formatDate
import com.rainyday.saveableapp.ui.security.LockGateContent
import com.rainyday.saveableapp.ui.security.SecureScreenEffect
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCategoryDetailScreen(categoryId: String, onBack: () -> Unit) {
    val viewModel: InfoBlockViewModel = hiltViewModel()
    val preferencesRepository = rememberAppEntryPoint().preferencesRepository()
    val category by viewModel.category.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val lockEnabled by preferencesRepository.infoLockEnabled.collectAsState(initial = true)

    var unlocked by remember { mutableStateOf(false) }
    LaunchedEffect(lockEnabled) {
        if (!lockEnabled) unlocked = true
    }

    if (!unlocked) {
        LockedGate(
            categoryName = category?.name ?: "Info",
            onUnlocked = { unlocked = true },
            onBack = onBack
        )
        return
    }

    SecureScreenEffect()

    var showAddDialog by remember { mutableStateOf(false) }
    var blockPendingEdit by remember { mutableStateOf<InfoBlockEntity?>(null) }
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New entry")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            DetailHeader(onBack = onBack, backLabel = "Vault")
            Text(
                text = category?.name ?: "Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        if (blocks.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Lock,
                title = "Nothing saved here yet",
                subtitle = "Add sizes, IDs, or any detail you want available at a glance.",
                actionLabel = "New entry",
                onAction = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            )
        } else {
            val expiringSoonCount = blocks.count { it.expiryDate != null && isExpiringSoon(it.expiryDate) }
            LazyColumn(
                contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (expiringSoonCount > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text(
                                    text = if (expiringSoonCount == 1) "1 entry expiring or expired" else "$expiringSoonCount entries expiring or expired",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
                items(blocks, key = { it.id }) { block ->
                    InfoBlockRow(
                        block = block,
                        icon = IconCatalog.resolve(category?.icon ?: IconCatalog.defaultKey),
                        accentHex = category?.colorHex ?: "#2EE6A8",
                        onClick = { blockPendingEdit = block },
                        onToggleFavorite = { viewModel.setFavorite(block, !block.isFavorite) },
                        onCopy = { clipboard.copyText(block.content) }
                    )
                }
            }
        }
        }
    }

    if (showAddDialog) {
        InfoBlockEditDialog(
            title = "New entry",
            templates = infoBlockTemplates,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, sensitive, expiryDate ->
                viewModel.createBlock(title, content, sensitive, expiryDate)
                showAddDialog = false
            }
        )
    }

    blockPendingEdit?.let { block ->
        InfoBlockEditDialog(
            title = "Edit entry",
            initialTitle = block.title,
            initialContent = block.content,
            initialSensitive = block.isSensitive,
            initialExpiryDate = block.expiryDate,
            onDismiss = { blockPendingEdit = null },
            onConfirm = { title, content, sensitive, expiryDate ->
                viewModel.updateBlock(block, title, content, sensitive, expiryDate)
                blockPendingEdit = null
            },
            onDelete = {
                blockPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = "Deleted \"${block.title}\"",
                        delete = { viewModel.deleteBlockWithUndo(block) },
                        restore = { viewModel.restoreBlock(it) }
                    )
                }
            }
        )
    }
}

private fun ClipboardManager.copyText(text: String) = setText(AnnotatedString(text))

@Composable
private fun LockedGate(categoryName: String, onUnlocked: () -> Unit, onBack: () -> Unit) {
    val preferencesRepository = rememberAppEntryPoint().preferencesRepository()

    Scaffold(
        topBar = { DetailHeader(onBack = onBack, backLabel = "Vault") }
    ) { padding ->
        LockGateContent(
            title = "Locked",
            subtitle = "Verify it's you to view this information.",
            preferencesRepository = preferencesRepository,
            onUnlocked = onUnlocked,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun InfoBlockRow(
    block: InfoBlockEntity,
    icon: ImageVector,
    accentHex: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit
) {
    var revealed by remember(block.id) { mutableStateOf(!block.isSensitive) }
    val accent = parseHexColor(accentHex)

    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)) {
                Text(text = block.title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = if (revealed) block.content else "•".repeat(block.content.length.coerceIn(4, 12)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (block.expiryDate != null) {
                    Text(
                        text = expiryLabel(block.expiryDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = expiryColor(block.expiryDate),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (block.isSensitive) {
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (revealed) "Hide" else "Reveal"
                    )
                }
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (block.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (block.isFavorite) "Unfavorite" else "Favorite",
                    tint = if (block.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun isExpiringSoon(expiryDate: Long, withinDays: Long = 30): Boolean {
    val daysLeft = TimeUnit.MILLISECONDS.toDays(expiryDate - System.currentTimeMillis())
    return daysLeft <= withinDays
}

private fun expiryLabel(expiryDate: Long): String {
    val daysLeft = TimeUnit.MILLISECONDS.toDays(expiryDate - System.currentTimeMillis())
    return when {
        daysLeft < 0 -> "Expired ${formatDate(expiryDate)}"
        daysLeft == 0L -> "Expires today"
        else -> "Expires ${formatDate(expiryDate)}"
    }
}

@Composable
private fun expiryColor(expiryDate: Long): Color {
    val daysLeft = TimeUnit.MILLISECONDS.toDays(expiryDate - System.currentTimeMillis())
    return when {
        daysLeft < 0 -> MaterialTheme.colorScheme.error
        daysLeft <= 30 -> parseHexColor("#E8B23A")
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
