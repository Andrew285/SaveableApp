package com.rainyday.saveableapp.ui.screens.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.rainyday.saveableapp.BuildConfig
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.prefs.ThemeMode
import com.rainyday.saveableapp.ui.components.PinSetupDialog
import com.rainyday.saveableapp.ui.components.ScreenHeader
import com.rainyday.saveableapp.ui.screens.todo.formatDate
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = hiltViewModel()

    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsState()
    val infoLockEnabled by viewModel.infoLockEnabled.collectAsState()
    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    val appPinIsSet by viewModel.appPinIsSet.collectAsState()
    val driveLastBackupAt by viewModel.driveLastBackupAt.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val lastSyncAt by viewModel.lastSyncAt.collectAsState()

    var driveAccount by remember { mutableStateOf(viewModel.driveBackupRepository.getSignedInAccount()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val signInFailedPattern = stringResource(R.string.settings_google_signin_failed)
    val backupSavedMessage = stringResource(R.string.settings_backup_saved)
    val exportFailedMessage = stringResource(R.string.settings_export_failed)
    val backupRestoredMessage = stringResource(R.string.settings_backup_restored)
    val importFailedMessage = stringResource(R.string.settings_import_failed)

    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val signInResult = runCatching { task.getResult(ApiException::class.java) }
        val account = signInResult.getOrNull()
        driveAccount = account
        if (account == null) {
            val statusCode = (signInResult.exceptionOrNull() as? ApiException)?.statusCode
            scope.launch {
                snackbarHostState.showSnackbar(signInFailedPattern.format(statusCode.toString()))
            }
        } else {
            // Also links this sign-in to Firebase Auth so it can authenticate AI-parsing calls.
            viewModel.signInToFirebase(account)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportData(context, uri) { success ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (success) backupSavedMessage else exportFailedMessage)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importData(context, uri) { success ->
                scope.launch {
                    snackbarHostState.showSnackbar(if (success) backupRestoredMessage else importFailedMessage)
                }
            }
        }
    }

    SettingsScreenContent(
        themeMode = themeMode,
        dynamicColorEnabled = dynamicColorEnabled,
        infoLockEnabled = infoLockEnabled,
        appLockEnabled = appLockEnabled,
        appPinIsSet = appPinIsSet,
        driveLastBackupAt = driveLastBackupAt,
        autoBackupEnabled = autoBackupEnabled,
        lastSyncAt = lastSyncAt,
        driveAccount = driveAccount,
        snackbarHostState = snackbarHostState,
        onSetThemeMode = viewModel::setThemeMode,
        onSetDynamicColorEnabled = viewModel::setDynamicColorEnabled,
        onSetAppLockEnabled = viewModel::setAppLockEnabled,
        onSetInfoLockEnabled = viewModel::setInfoLockEnabled,
        onSetAppPin = viewModel::setAppPin,
        onClearAppPin = viewModel::clearAppPin,
        onSetAutoBackupEnabled = viewModel::setAutoBackupEnabled,
        onSignInWithGoogleRequested = {
            driveSignInLauncher.launch(viewModel.driveBackupRepository.signInClient().signInIntent)
        },
        onSignOutOfDrive = {
            viewModel.driveBackupRepository.signOut()
            driveAccount = null
            if (autoBackupEnabled) viewModel.setAutoBackupEnabled(false)
        },
        onBackupToDrive = viewModel::backupToDrive,
        onRestoreFromDrive = viewModel::restoreFromDrive,
        onExportRequested = { exportLauncher.launch("todo-backup.json") },
        onImportRequested = { importLauncher.launch(arrayOf("application/json")) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    infoLockEnabled: Boolean,
    appLockEnabled: Boolean,
    appPinIsSet: Boolean,
    driveLastBackupAt: Long?,
    autoBackupEnabled: Boolean,
    lastSyncAt: Long?,
    driveAccount: GoogleSignInAccount?,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSetThemeMode: (ThemeMode) -> Unit = {},
    onSetDynamicColorEnabled: (Boolean) -> Unit = {},
    onSetAppLockEnabled: (Boolean) -> Unit = {},
    onSetInfoLockEnabled: (Boolean) -> Unit = {},
    onSetAppPin: (String) -> Unit = {},
    onClearAppPin: () -> Unit = {},
    onSetAutoBackupEnabled: (Boolean) -> Unit = {},
    onSignInWithGoogleRequested: () -> Unit = {},
    onSignOutOfDrive: () -> Unit = {},
    onBackupToDrive: (GoogleSignInAccount, (Boolean, String?) -> Unit) -> Unit = { _, onDone -> onDone(false, null) },
    onRestoreFromDrive: (GoogleSignInAccount, (Boolean, String?) -> Unit) -> Unit = { _, onDone -> onDone(false, null) },
    onExportRequested: () -> Unit = {},
    onImportRequested: () -> Unit = {}
) {
    var showPinSetup by remember { mutableStateOf(false) }
    var driveBusy by remember { mutableStateOf(false) }
    var showDriveRestoreConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val unknownError = stringResource(R.string.settings_unknown_error)
    val backedUpSuccessMessage = stringResource(R.string.settings_backed_up_success)
    val backupFailedPattern = stringResource(R.string.settings_backup_failed)
    val restoreFailedPattern = stringResource(R.string.settings_restore_failed)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeader(
                eyebrow = stringResource(R.string.settings_eyebrow),
                title = stringResource(R.string.settings_screen_title),
                subtitle = stringResource(R.string.settings_screen_subtitle)
            )
            SettingsSectionTitle(stringResource(R.string.settings_section_appearance))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.d16, vertical = Dimens.d8),
                horizontalArrangement = Arrangement.spacedBy(Dimens.d8)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onSetThemeMode(mode) },
                        label = { Text(mode.label()) }
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_color_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_color_subtitle)) },
                    trailingContent = {
                        Switch(checked = dynamicColorEnabled, onCheckedChange = onSetDynamicColorEnabled)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.d8))

            SettingsSectionTitle(stringResource(R.string.settings_section_security))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_app_lock_title)) },
                supportingContent = { Text(stringResource(R.string.settings_app_lock_subtitle)) },
                trailingContent = {
                    Switch(checked = appLockEnabled, onCheckedChange = onSetAppLockEnabled)
                }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_info_lock_title)) },
                supportingContent = { Text(stringResource(R.string.settings_info_lock_subtitle)) },
                trailingContent = {
                    Switch(checked = infoLockEnabled, onCheckedChange = onSetInfoLockEnabled)
                }
            )
            ListItem(
                headlineContent = {
                    Text(
                        if (appPinIsSet) {
                            stringResource(R.string.settings_change_pin)
                        } else {
                            stringResource(R.string.settings_set_pin)
                        }
                    )
                },
                supportingContent = { Text(stringResource(R.string.settings_pin_subtitle)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPinSetup = true }
            )
            if (appPinIsSet) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_remove_pin), color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearAppPin() }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.d8))

            SettingsSectionTitle(stringResource(R.string.settings_section_ai_parsing))
            Column(modifier = Modifier.padding(horizontal = Dimens.d16)) {
                Text(
                    text = if (driveAccount != null) {
                        stringResource(R.string.settings_ai_parsing_connected)
                    } else {
                        stringResource(R.string.settings_ai_parsing_disconnected)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.d8))

            SettingsSectionTitle(stringResource(R.string.settings_section_sync))
            Column(modifier = Modifier.padding(horizontal = Dimens.d16)) {
                if (driveAccount != null) {
                    Text(
                        text = stringResource(R.string.settings_sync_connected, driveAccount.email ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = lastSyncAt?.let { stringResource(R.string.settings_last_synced, formatDate(it)) }
                            ?: stringResource(R.string.settings_not_synced),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Dimens.d4)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_sync_disconnected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.d8))

            SettingsSectionTitle(stringResource(R.string.settings_section_backup))
            Column(modifier = Modifier.padding(horizontal = Dimens.d16)) {
                Text(text = stringResource(R.string.settings_backup_drive_title), style = MaterialTheme.typography.titleSmall)
                val account = driveAccount
                if (account == null) {
                    Text(
                        text = stringResource(R.string.settings_drive_signin_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Dimens.d4)
                    )
                    OutlinedButton(
                        onClick = onSignInWithGoogleRequested,
                        modifier = Modifier.padding(top = Dimens.d8)
                    ) {
                        Text(stringResource(R.string.settings_sign_in_with_google))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settings_signed_in_as, account.email ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Dimens.d4)
                    )
                    Text(
                        text = driveLastBackupAt?.let { stringResource(R.string.settings_last_backed_up, formatDate(it)) }
                            ?: stringResource(R.string.settings_never_backed_up),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.d8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_auto_backup_title),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = autoBackupEnabled, onCheckedChange = onSetAutoBackupEnabled)
                    }
                    Row(
                        modifier = Modifier.padding(top = Dimens.d8),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.d12)
                    ) {
                        OutlinedButton(
                            enabled = !driveBusy,
                            onClick = {
                                driveBusy = true
                                onBackupToDrive(account) { success, error ->
                                    driveBusy = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (success) {
                                                backedUpSuccessMessage
                                            } else {
                                                backupFailedPattern.format(error ?: unknownError)
                                            }
                                        )
                                    }
                                }
                            }
                        ) { Text(stringResource(R.string.settings_backup_now)) }
                        OutlinedButton(
                            enabled = !driveBusy,
                            onClick = { showDriveRestoreConfirm = true }
                        ) { Text(stringResource(R.string.action_restore)) }
                    }
                    TextButton(
                        onClick = onSignOutOfDrive,
                        modifier = Modifier.padding(top = Dimens.d4)
                    ) { Text(stringResource(R.string.settings_sign_out)) }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.d8, horizontal = Dimens.d16))

            Column(modifier = Modifier.padding(horizontal = Dimens.d16)) {
                Text(text = stringResource(R.string.settings_local_file_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.settings_export_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.d4)
                )
                Row(
                    modifier = Modifier.padding(top = Dimens.d8),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.d12)
                ) {
                    OutlinedButton(onClick = onExportRequested) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(Dimens.d18))
                        Text(stringResource(R.string.settings_export_action), modifier = Modifier.padding(start = Dimens.d4))
                    }
                    OutlinedButton(onClick = onImportRequested) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(Dimens.d18))
                        Text(stringResource(R.string.settings_import_action), modifier = Modifier.padding(start = Dimens.d4))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.d8))

            SettingsSectionTitle(stringResource(R.string.settings_section_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version_label)) },
                supportingContent = { Text(BuildConfig.VERSION_NAME) }
            )
        }
    }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onConfirm = { pin ->
                onSetAppPin(pin)
                showPinSetup = false
            }
        )
    }

    if (showDriveRestoreConfirm) {
        val restoredSuccessMessage = stringResource(R.string.settings_restored_success)
        AlertDialog(
            onDismissRequest = { showDriveRestoreConfirm = false },
            title = { Text(stringResource(R.string.settings_drive_restore_title)) },
            text = { Text(stringResource(R.string.settings_drive_restore_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDriveRestoreConfirm = false
                    val account = driveAccount ?: return@TextButton
                    driveBusy = true
                    onRestoreFromDrive(account) { success, error ->
                        driveBusy = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) {
                                    restoredSuccessMessage
                                } else {
                                    restoreFailedPattern.format(error ?: unknownError)
                                }
                            )
                        }
                    }
                }) { Text(stringResource(R.string.action_restore)) }
            },
            dismissButton = {
                TextButton(onClick = { showDriveRestoreConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Dimens.d16, top = Dimens.d16, bottom = Dimens.d4)
    )
}

@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    }
)

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SaveableAppTheme {
        SettingsScreenContent(
            themeMode = ThemeMode.SYSTEM,
            dynamicColorEnabled = true,
            infoLockEnabled = true,
            appLockEnabled = false,
            appPinIsSet = false,
            driveLastBackupAt = null,
            autoBackupEnabled = false,
            lastSyncAt = null,
            driveAccount = null
        )
    }
}
