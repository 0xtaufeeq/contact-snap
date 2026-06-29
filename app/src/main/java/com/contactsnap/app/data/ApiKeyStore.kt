package com.contactsnap.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "contactsnap_settings")

/** Persists the user's Gemini API key locally (app-private DataStore). */
class ApiKeyStore(private val context: Context) {

    private val keyPref = stringPreferencesKey("gemini_api_key")
    private val nameFormatPref = stringPreferencesKey("name_format")

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { it[keyPref].orEmpty() }
    val nameFormatFlow: Flow<String> = context.dataStore.data.map { it[nameFormatPref].orEmpty() }

    suspend fun get(): String = apiKeyFlow.first()

    suspend fun set(value: String) {
        context.dataStore.edit { it[keyPref] = value.trim() }
    }

    suspend fun getNameFormat(): String = nameFormatFlow.first()

    suspend fun setNameFormat(id: String) {
        context.dataStore.edit { it[nameFormatPref] = id }
    }
}
