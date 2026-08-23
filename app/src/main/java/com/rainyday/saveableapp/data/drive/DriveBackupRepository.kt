package com.rainyday.saveableapp.data.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.repository.BackupRepository
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val BACKUP_FILE_NAME = "saveable_app_backup.json"
private const val MIME_TYPE_JSON = "application/json"

/**
 * Backs up/restores the same [com.rainyday.saveableapp.data.backup.BackupPayload] used for local
 * export, but to/from a single app-created file in the signed-in user's Google Drive (drive.file
 * scope: this app can only see files it created itself, not the rest of the user's Drive).
 */
class DriveBackupRepository(
    private val context: Context,
    private val backupRepository: BackupRepository,
    private val preferencesRepository: PreferencesRepository
) {
    private val driveScope = Scope(DriveScopes.DRIVE_FILE)

    fun signInClient(): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            // Also requests an ID token so this same sign-in can be linked to a Firebase Auth
            // session (see FirebaseAuthRepository), which authenticates AI-parsing calls.
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestScopes(driveScope)
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** The signed-in account, or null if no account is signed in or it hasn't granted Drive access. */
    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return if (GoogleSignIn.hasPermissions(account, driveScope)) account else null
    }

    fun signOut() {
        signInClient().signOut()
    }

    suspend fun backup(account: GoogleSignInAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService(account)
            val text = backupRepository.encode(backupRepository.buildPayload())
            val content = ByteArrayContent(MIME_TYPE_JSON, text.toByteArray(Charsets.UTF_8))
            val existingFileId = resolveBackupFileId(drive)
            val fileId = if (existingFileId != null) {
                drive.files().update(existingFileId, null, content).execute()
                existingFileId
            } else {
                val metadata = DriveFile().setName(BACKUP_FILE_NAME)
                drive.files().create(metadata, content).setFields("id").execute().id
            }
            preferencesRepository.setDriveBackupFileId(fileId)
            preferencesRepository.setDriveLastBackupAt(System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(describeDriveError(e), e))
        }
    }

    suspend fun restore(account: GoogleSignInAccount): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService(account)
            val fileId = resolveBackupFileId(drive) ?: error("No backup found in Google Drive yet.")
            val output = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(output)
            val payload = backupRepository.decode(output.toString(Charsets.UTF_8.name()))
            backupRepository.restorePayload(payload)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(describeDriveError(e), e))
        }
    }

    /** Turns a raw Drive API exception into an actionable message instead of a bare HTTP status. */
    private fun describeDriveError(e: Exception): String {
        val details = (e as? GoogleJsonResponseException)?.details ?: return e.message ?: e.toString()
        val reason = details.errors?.firstOrNull()?.reason
        return when (reason) {
            "accessNotConfigured" ->
                "The Google Drive API isn't enabled for this app's Google Cloud project. " +
                    "Enable it in Cloud Console (APIs & Services > Library > Google Drive API), then try again."
            "insufficientPermissions", "forbidden", "insufficientFilePermissions" ->
                "Google didn't grant this app Drive access. Make sure the drive.file scope is added " +
                    "on the OAuth consent screen and this account is listed as a test user, then sign out, " +
                    "sign back in, and try again."
            else -> details.message ?: e.message ?: e.toString()
        }
    }

    /** Finds this app's backup file, preferring the cached id but falling back to a name search. */
    private suspend fun resolveBackupFileId(drive: Drive): String? {
        val cached = preferencesRepository.driveBackupFileId.first()
        if (cached != null && driveFileExists(drive, cached)) return cached

        val found = drive.files().list()
            .setSpaces("drive")
            .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
            .setFields("files(id)")
            .setPageSize(1)
            .execute()
            .files
            .firstOrNull()
            ?.id
        if (found != null) preferencesRepository.setDriveBackupFileId(found)
        return found
    }

    private fun driveFileExists(drive: Drive, fileId: String): Boolean = runCatching {
        drive.files().get(fileId).setFields("id").execute()
        true
    }.getOrDefault(false)

    private fun driveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account.account
        return Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("SaveableApp")
            .build()
    }
}
