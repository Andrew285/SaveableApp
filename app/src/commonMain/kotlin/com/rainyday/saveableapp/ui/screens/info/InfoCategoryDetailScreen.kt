package com.rainyday.saveableapp.ui.screens.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.local.InfoBlockEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.platform.SecureScreenEffect
import com.rainyday.saveableapp.platform.nowMillis
import com.rainyday.saveableapp.platform.rememberAppLockAuthenticator
import com.rainyday.saveableapp.ui.screens.todo.formatDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCategoryDetailScreen(categoryId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val viewModel: InfoBlockViewModel = viewModel(
        factory = viewModelFactory { initializer { InfoBlockViewModel(categoryId, container.infoRepository) } }
    )
    val category by viewModel.category.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val lockEnabled by container.preferencesRepository.infoLockEnabled.collectAsState(initial = true)

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
        topBar = {
            TopAppBar(
                title = { Text(category?.name ?: "Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New entry")
            }
        }
    ) { padding ->
        if (blocks.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Lock,
                title = "Nothing saved here yet",
                subtitle = "Add sizes, IDs, or any detail you want available at a glance.",
                actionLabel = "New entry",
                onAction = { showAddDialog = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            val expiringSoonCount = blocks.count { it.expiryDate != null && isExpiringSoon(it.expiryDate) }
            LazyColumn(
                contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 96.dp),
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
                        onClick = { blockPendingEdit = block },
                        onToggleFavorite = { viewModel.setFavorite(block, !block.isFavorite) },
                        onCopy = { clipboard.copyText(block.content) }
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockedGate(categoryName: String, onUnlocked: () -> Unit, onBack: () -> Unit) {
    val authenticator = rememberAppLockAuthenticator()
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
                text = "Locked",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Verify it's you to view this information.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Button(
                onClick = {
                    authenticator.authenticate(
                        title = "Unlock $categoryName",
                        onSuccess = onUnlocked,
                        onError = { error = it }
                    )
                },
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Unlock")
            }
        }
    }
}

@Composable
private fun InfoBlockRow(
    block: InfoBlockEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit
) {
    var revealed by remember(block.id) { mutableStateOf(!block.isSensitive) }

    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
    val daysLeft = (expiryDate - nowMillis()) / 86_400_000L
    return daysLeft <= withinDays
}

private fun expiryLabel(expiryDate: Long): String {
    val daysLeft = (expiryDate - nowMillis()) / 86_400_000L
    return when {
        daysLeft < 0 -> "Expired ${formatDate(expiryDate)}"
        daysLeft == 0L -> "Expires today"
        else -> "Expires ${formatDate(expiryDate)}"
    }
}

@Composable
private fun expiryColor(expiryDate: Long): Color {
    val daysLeft = (expiryDate - nowMillis()) / 86_400_000L
    return when {
        daysLeft < 0 -> MaterialTheme.colorScheme.error
        daysLeft <= 30 -> parseHexColor("#E8B23A")
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
