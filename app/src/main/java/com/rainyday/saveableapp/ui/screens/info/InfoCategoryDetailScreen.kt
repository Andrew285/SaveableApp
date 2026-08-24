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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.InfoBlockEntity
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.ui.rememberAppEntryPoint
import com.rainyday.saveableapp.ui.components.DetailHeader
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.ui.screens.todo.formatDate
import com.rainyday.saveableapp.ui.security.LockGateContent
import com.rainyday.saveableapp.ui.security.SecureScreenEffect
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
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
            categoryName = category?.name ?: stringResource(R.string.info_category_fallback_name),
            onUnlocked = { unlocked = true },
            onBack = onBack
        )
        return
    }

    SecureScreenEffect()

    InfoCategoryDetailScreenContent(
        category = category,
        blocks = blocks,
        onBack = onBack,
        onSetFavorite = viewModel::setFavorite,
        onCreateBlock = viewModel::createBlock,
        onUpdateBlock = viewModel::updateBlock,
        onDeleteBlockWithUndo = viewModel::deleteBlockWithUndo,
        onRestoreBlock = viewModel::restoreBlock
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoCategoryDetailScreenContent(
    category: InfoCategoryEntity?,
    blocks: List<InfoBlockEntity>,
    onBack: () -> Unit,
    onSetFavorite: (InfoBlockEntity, Boolean) -> Unit = { _, _ -> },
    onCreateBlock: (String, String, Boolean, Long?) -> Unit = { _, _, _, _ -> },
    onUpdateBlock: (InfoBlockEntity, String, String, Boolean, Long?) -> Unit = { _, _, _, _, _ -> },
    onDeleteBlockWithUndo: suspend (InfoBlockEntity) -> InfoBlockEntity = { it },
    onRestoreBlock: suspend (InfoBlockEntity) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var blockPendingEdit by remember { mutableStateOf<InfoBlockEntity?>(null) }
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoActionLabel = stringResource(R.string.action_undo)
    val newEntryLabel = stringResource(R.string.info_new_entry)
    val vaultLabel = stringResource(R.string.info_vault_label)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = newEntryLabel)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            DetailHeader(onBack = onBack, backLabel = vaultLabel)
            Text(
                text = category?.name ?: vaultLabel,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = Dimens.d20)
            )
        if (blocks.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.info_empty_blocks_title),
                subtitle = stringResource(R.string.info_empty_blocks_subtitle),
                actionLabel = newEntryLabel,
                onAction = { showAddDialog = true },
                modifier = Modifier.weight(1f)
            )
        } else {
            val expiringSoonCount = blocks.count { it.expiryDate != null && isExpiringSoon(it.expiryDate) }
            LazyColumn(
                contentPadding = PaddingValues(Dimens.d20, Dimens.d8, Dimens.d20, Dimens.d96),
                verticalArrangement = Arrangement.spacedBy(Dimens.d8)
            ) {
                if (expiringSoonCount > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(Dimens.d12),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text(
                                    text = pluralStringResource(R.plurals.info_expiring_count, expiringSoonCount, expiringSoonCount),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(start = Dimens.d8)
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
                        onToggleFavorite = { onSetFavorite(block, !block.isFavorite) },
                        onCopy = { clipboard.copyText(block.content) }
                    )
                }
            }
        }
        }
    }

    if (showAddDialog) {
        InfoBlockEditDialog(
            title = newEntryLabel,
            templates = infoBlockTemplates(),
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content, sensitive, expiryDate ->
                onCreateBlock(title, content, sensitive, expiryDate)
                showAddDialog = false
            }
        )
    }

    blockPendingEdit?.let { block ->
        val deletedBlockMessage = stringResource(R.string.deleted_named_item, block.title)
        InfoBlockEditDialog(
            title = stringResource(R.string.info_edit_entry),
            initialTitle = block.title,
            initialContent = block.content,
            initialSensitive = block.isSensitive,
            initialExpiryDate = block.expiryDate,
            onDismiss = { blockPendingEdit = null },
            onConfirm = { title, content, sensitive, expiryDate ->
                onUpdateBlock(block, title, content, sensitive, expiryDate)
                blockPendingEdit = null
            },
            onDelete = {
                blockPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = deletedBlockMessage,
                        actionLabel = undoActionLabel,
                        delete = { onDeleteBlockWithUndo(block) },
                        restore = { onRestoreBlock(it) }
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
        topBar = { DetailHeader(onBack = onBack, backLabel = stringResource(R.string.info_vault_label)) }
    ) { padding ->
        LockGateContent(
            title = stringResource(R.string.info_locked_title),
            subtitle = stringResource(R.string.info_locked_subtitle),
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
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.d0),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.d8, horizontal = Dimens.d14),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.d36)
                    .clip(MaterialTheme.shapes.small)
                    .background(accent.copy(alpha = AppAlpha.a16)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(Dimens.d18))
            }
            Column(modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.d12)) {
                Text(text = block.title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = if (revealed) block.content else "•".repeat(block.content.length.coerceIn(4, 12)),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = Dimens.d2)
                )
                if (block.expiryDate != null) {
                    Text(
                        text = expiryLabel(block.expiryDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = expiryColor(block.expiryDate),
                        modifier = Modifier.padding(top = Dimens.d2)
                    )
                }
            }
            if (block.isSensitive) {
                IconButton(onClick = { revealed = !revealed }) {
                    Icon(
                        if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (revealed) stringResource(R.string.cd_hide) else stringResource(R.string.cd_reveal)
                    )
                }
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.cd_copy))
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (block.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (block.isFavorite) stringResource(R.string.cd_unfavorite) else stringResource(R.string.cd_favorite),
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

@Composable
private fun expiryLabel(expiryDate: Long): String {
    val daysLeft = TimeUnit.MILLISECONDS.toDays(expiryDate - System.currentTimeMillis())
    return when {
        daysLeft < 0 -> stringResource(R.string.info_expired_on, formatDate(expiryDate))
        daysLeft == 0L -> stringResource(R.string.info_expires_today)
        else -> stringResource(R.string.info_expires_on, formatDate(expiryDate))
    }
}

private val previewInfoCategory = InfoCategoryEntity(id = "cat-1", name = "Passports", icon = "badge", colorHex = "#6750A4", position = 0, updatedAt = 0L)

private val previewInfoBlocks = listOf(
    InfoBlockEntity(id = "block-1", categoryId = "cat-1", title = "Passport number", content = "X1234567", isSensitive = true, createdAt = 0L, updatedAt = 0L),
    InfoBlockEntity(id = "block-2", categoryId = "cat-1", title = "Expiry", content = "Renew before travel", isFavorite = true, createdAt = 0L, updatedAt = 0L, expiryDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10))
)

@Preview(showBackground = true)
@Composable
fun InfoCategoryDetailScreenPreview() {
    SaveableAppTheme {
        InfoCategoryDetailScreenContent(
            category = previewInfoCategory,
            blocks = previewInfoBlocks,
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InfoCategoryDetailScreenEmptyPreview() {
    SaveableAppTheme {
        InfoCategoryDetailScreenContent(
            category = previewInfoCategory,
            blocks = emptyList(),
            onBack = {}
        )
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
