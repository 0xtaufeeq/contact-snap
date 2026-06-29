package com.contactsnap.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.groupColorDataStore by preferencesDataStore(name = "contactsnap_group_colors")

/** Persists per-group color overrides (group name -> ARGB int) as a JSON map. */
class GroupColorStore(private val context: Context) {

    private val key = stringPreferencesKey("colors")

    val colorsFlow: Flow<Map<String, Int>> =
        context.groupColorDataStore.data.map { decode(it[key].orEmpty()) }

    suspend fun setColor(group: String, argb: Int) {
        context.groupColorDataStore.edit { prefs ->
            val map = decode(prefs[key].orEmpty()).toMutableMap()
            map[group] = argb
            prefs[key] = encode(map)
        }
    }

    suspend fun rename(old: String, new: String) {
        context.groupColorDataStore.edit { prefs ->
            val map = decode(prefs[key].orEmpty()).toMutableMap()
            map.remove(old)?.let { map[new] = it }
            prefs[key] = encode(map)
        }
    }

    suspend fun remove(group: String) {
        context.groupColorDataStore.edit { prefs ->
            val map = decode(prefs[key].orEmpty()).toMutableMap()
            map.remove(group)
            prefs[key] = encode(map)
        }
    }

    private fun encode(map: Map<String, Int>): String {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        return o.toString()
    }

    private fun decode(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { o.getInt(it) }
        }.getOrDefault(emptyMap())
    }
}
