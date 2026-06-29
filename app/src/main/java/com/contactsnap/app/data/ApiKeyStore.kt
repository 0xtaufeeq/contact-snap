package com.contactsnap.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.contactsnap.app.util.Crypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "contactsnap_settings")

/** Persists the user's Gemini API key locally (app-private DataStore). */
class ApiKeyStore(private val context: Context) {

    private val keyPref = stringPreferencesKey("gemini_api_key")          // legacy plaintext
    private val encKeyPref = stringPreferencesKey("gemini_api_key_enc")   // Keystore-encrypted
    private val nameFormatPref = stringPreferencesKey("name_format")
    private val themePref = stringPreferencesKey("theme_mode")
    private val onboardingPref = booleanPreferencesKey("onboarding_seen")

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[encKeyPref]?.takeIf { it.isNotBlank() }?.let { Crypto.decrypt(it) }
            ?: prefs[keyPref].orEmpty()
    }
    val nameFormatFlow: Flow<String> = context.dataStore.data.map { it[nameFormatPref].orEmpty() }
    val themeFlow: Flow<String> = context.dataStore.data.map { it[themePref].orEmpty() }
    val onboardingSeenFlow: Flow<Boolean> = context.dataStore.data.map { it[onboardingPref] ?: false }

    suspend fun get(): String = apiKeyFlow.first()

    suspend fun set(value: String) {
        val encrypted = Crypto.encrypt(value.trim())
        context.dataStore.edit { prefs ->
            if (encrypted.isNotBlank()) {
                prefs[encKeyPref] = encrypted
                prefs.remove(keyPref) // drop any legacy plaintext copy
            } else {
                // Keystore unavailable — fall back to plaintext rather than losing the key.
                prefs[keyPref] = value.trim()
            }
        }
    }

    suspend fun getNameFormat(): String = nameFormatFlow.first()

    suspend fun setNameFormat(id: String) {
        context.dataStore.edit { it[nameFormatPref] = id }
    }

    suspend fun setTheme(id: String) {
        context.dataStore.edit { it[themePref] = id }
    }

    suspend fun setOnboardingSeen() {
        context.dataStore.edit { it[onboardingPref] = true }
    }
}
