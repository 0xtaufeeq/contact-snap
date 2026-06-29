package com.contactsnap.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.groupDataStore by preferencesDataStore(name = "contactsnap_groups")

/** Remembers group names the user has used, most-recent first, for quick re-selection. */
class GroupStore(private val context: Context) {

    private val key = stringPreferencesKey("groups")

    val groupsFlow: Flow<List<String>> =
        context.groupDataStore.data.map { decode(it[key].orEmpty()) }

    suspend fun remember(group: String) {
        val name = group.trim()
        if (name.isEmpty()) return
        context.groupDataStore.edit { prefs ->
            val current = decode(prefs[key].orEmpty())
            val updated = (listOf(name) + current.filterNot { it.equals(name, ignoreCase = true) })
                .take(MAX_GROUPS)
            prefs[key] = JSONArray(updated).toString()
        }
    }

    private fun decode(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotEmpty() } }
        }.getOrDefault(emptyList())
    }

    companion object { private const val MAX_GROUPS = 20 }
}
