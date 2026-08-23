package com.rainyday.saveableapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val INFO_LOCK_ENABLED = booleanPreferencesKey("info_lock_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_PIN_HASH = stringPreferencesKey("app_pin_hash")
        val APP_PIN_SALT = stringPreferencesKey("app_pin_salt")
        val DRIVE_BACKUP_FILE_ID = stringPreferencesKey("drive_backup_file_id")
        val DRIVE_LAST_BACKUP_AT = longPreferencesKey("drive_last_backup_at")
        val LAST_USED_TODO_LIST_ID = longPreferencesKey("last_used_todo_list_id")
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR] ?: true
    }

    val infoLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.INFO_LOCK_ENABLED] ?: true
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_LOCK_ENABLED] ?: false
    }

    /** Whether a PIN has been configured as a fallback for biometric locks. */
    val appPinIsSet: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_PIN_HASH] != null
    }

    /** Id of the backup file this app previously created in the signed-in user's Drive, if any. */
    val driveBackupFileId: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.DRIVE_BACKUP_FILE_ID] }

    val driveLastBackupAt: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[Keys.DRIVE_LAST_BACKUP_AT] }

    /** Last to-do list a task was added to, used to default the quick-add bar's destination list. */
    val lastUsedTodoListId: Flow<Long?> = context.dataStore.data.map { prefs -> prefs[Keys.LAST_USED_TODO_LIST_ID] }

    /** API key for the Groq chat completions API, used to parse quick-add task text. Testing only. */
    val groqApiKey: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.GROQ_API_KEY] }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[Keys.AUTO_BACKUP_ENABLED] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setInfoLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INFO_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppPin(pin: String) {
        val salt = generateSalt()
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_PIN_SALT] = salt
            prefs[Keys.APP_PIN_HASH] = hashPin(pin, salt)
        }
    }

    suspend fun clearAppPin() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.APP_PIN_HASH)
            prefs.remove(Keys.APP_PIN_SALT)
        }
    }

    suspend fun verifyAppPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val hash = prefs[Keys.APP_PIN_HASH] ?: return false
        val salt = prefs[Keys.APP_PIN_SALT] ?: return false
        return hashPin(pin, salt) == hash
    }

    suspend fun setDriveBackupFileId(fileId: String) {
        context.dataStore.edit { it[Keys.DRIVE_BACKUP_FILE_ID] = fileId }
    }

    suspend fun setDriveLastBackupAt(millis: Long) {
        context.dataStore.edit { it[Keys.DRIVE_LAST_BACKUP_AT] = millis }
    }

    suspend fun setLastUsedTodoListId(listId: Long) {
        context.dataStore.edit { it[Keys.LAST_USED_TODO_LIST_ID] = listId }
    }

    suspend fun setGroqApiKey(key: String) {
        context.dataStore.edit { prefs ->
            if (key.isBlank()) prefs.remove(Keys.GROQ_API_KEY) else prefs[Keys.GROQ_API_KEY] = key.trim()
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
