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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.rainyday.saveableapp.BuildConfig
import com.rainyday.saveableapp.data.prefs.ThemeMode
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.PinSetupDialog
import com.rainyday.saveableapp.ui.screens.todo.formatDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val container = appContainer()
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.preferencesRepository,
                    container.backupRepository,
                    container.driveBackupRepository,
                    container.firebaseAuthRepository,
                    container.autoBackupScheduler
                )
            }
        }
    )

    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsState()
    val infoLockEnabled by viewModel.infoLockEnabled.collectAsState()
    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    val appPinIsSet by viewModel.appPinIsSet.collectAsState()
    val driveLastBackupAt by viewModel.driveLastBackupAt.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()

    var showPinSetup by remember { mutableStateOf(false) }
    var driveAccount by remember { mutableStateOf(viewModel.driveBackupRepository.getSignedInAccount()) }
    var driveBusy by remember { mutableStateOf(false) }
    var showDriveRestoreConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                snackbarHostState.showSnackbar("Google sign-in failed (code $statusCode)")
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
                    snackbarHostState.showSnackbar(
                        if (success) "Backup saved" else "Export failed"
                    )
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
                    snackbarHostState.showSnackbar(
                        if (success) "Backup restored" else "Import failed — file may be invalid"
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            com.rainyday.saveableapp.ui.components.ScreenHeader(
                eyebrow = "// SETTINGS",
                title = "Settings",
                subtitle = "Appearance, security, and backup"
            )
            SettingsSectionTitle("Appearance")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.label()) }
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text("Match wallpaper colors") },
                    supportingContent = { Text("Use Material You dynamic color") },
                    trailingContent = {
                        Switch(checked = dynamicColorEnabled, onCheckedChange = viewModel::setDynamicColorEnabled)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("Security")
            ListItem(
                headlineContent = { Text("Lock app on launch") },
                supportingContent = { Text("Require biometrics or a PIN to open the app") },
                trailingContent = {
                    Switch(checked = appLockEnabled, onCheckedChange = viewModel::setAppLockEnabled)
                }
            )
            ListItem(
                headlineContent = { Text("Lock Info section") },
                supportingContent = { Text("Require biometrics or a PIN to open Info") },
                trailingContent = {
                    Switch(checked = infoLockEnabled, onCheckedChange = viewModel::setInfoLockEnabled)
                }
            )
            ListItem(
                headlineContent = { Text(if (appPinIsSet) "Change app PIN" else "Set app PIN") },
                supportingContent = { Text("Numeric PIN used as a fallback for the locks above") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPinSetup = true }
            )
            if (appPinIsSet) {
                ListItem(
                    headlineContent = { Text("Remove app PIN", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearAppPin() }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("AI Task Parsing")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = if (driveAccount != null) {
                        "Quick-add text is parsed automatically (priority, due date, list, and tags) " +
                            "using the same Google sign-in as Drive backup below. Free accounts get " +
                            "20 AI parses a day."
                    } else {
                        "Sign in with Google below (under Backup) to let quick-add parse your task text " +
                            "automatically — priority, due date, list, and tags. Free accounts get 20 AI " +
                            "parses a day."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("Backup")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = "Google Drive", style = MaterialTheme.typography.titleSmall)
                val account = driveAccount
                if (account == null) {
                    Text(
                        text = "Sign in to back up your data to Google Drive and restore it on another device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    OutlinedButton(
                        onClick = { driveSignInLauncher.launch(viewModel.driveBackupRepository.signInClient().signInIntent) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Sign in with Google")
                    }
                } else {
                    Text(
                        text = "Signed in as ${account.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = driveLastBackupAt?.let { "Last backed up ${formatDate(it)}" } ?: "Never backed up",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Automatic daily backup",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = autoBackupEnabled, onCheckedChange = viewModel::setAutoBackupEnabled)
                    }
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            enabled = !driveBusy,
                            onClick = {
                                driveBusy = true
                                viewModel.backupToDrive(account) { success, error ->
                                    driveBusy = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (success) "Backed up to Drive" else "Backup failed: ${error ?: "unknown error"}"
                                        )
                                    }
                                }
                            }
                        ) { Text("Back up now") }
                        OutlinedButton(
                            enabled = !driveBusy,
                            onClick = { showDriveRestoreConfirm = true }
                        ) { Text("Restore") }
                    }
                    TextButton(
                        onClick = {
                            viewModel.driveBackupRepository.signOut()
                            driveAccount = null
                            if (autoBackupEnabled) viewModel.setAutoBackupEnabled(false)
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) { Text("Sign out") }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = "Local file", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Export a backup file to keep a copy or move to another phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = { exportLauncher.launch("todo-backup.json") }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Export", modifier = Modifier.padding(start = 4.dp))
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Import", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsSectionTitle("About")
            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text(BuildConfig.VERSION_NAME) }
            )
        }
    }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onConfirm = { pin ->
                viewModel.setAppPin(pin)
                showPinSetup = false
            }
        )
    }

    if (showDriveRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showDriveRestoreConfirm = false },
            title = { Text("Restore from Drive?") },
            text = { Text("This replaces everything on this device with your Google Drive backup. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDriveRestoreConfirm = false
                    val account = driveAccount ?: return@TextButton
                    driveBusy = true
                    viewModel.restoreFromDrive(account) { success, error ->
                        driveBusy = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (success) "Restored from Drive" else "Restore failed: ${error ?: "unknown error"}"
                            )
                        }
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showDriveRestoreConfirm = false }) { Text("Cancel") }
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
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}
