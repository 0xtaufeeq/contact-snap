package com.contactsnap.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.contactsnap.app.model.ParsedContact
import com.contactsnap.app.util.ContactMerge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One past scan kept in local history. */
data class HistoryEntry(
    val id: String,
    val timestamp: Long,
    val contact: ParsedContact,
    val imagePath: String = ""
)

private val Context.historyDataStore by preferencesDataStore(name = "contactsnap_history")

/** Stores recent scans as a JSON array in DataStore (newest first, capped). */
class HistoryStore(private val context: Context) {

    private val key = stringPreferencesKey("entries")

    val historyFlow: Flow<List<HistoryEntry>> =
        context.historyDataStore.data.map { prefs -> decode(prefs[key].orEmpty()) }

    /** Insert a new entry or update an existing one (matched by id). Keeps the
     *  previous image path when the incoming entry doesn't carry one. */
    suspend fun upsert(entry: HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val current = decode(prefs[key].orEmpty())
            val existing = current.firstOrNull { it.id == entry.id }
            val merged = entry.copy(
                imagePath = entry.imagePath.ifBlank { existing?.imagePath.orEmpty() }
            )
            val without = current.filterNot { it.id == entry.id }
            prefs[key] = encode((listOf(merged) + without).take(MAX_ENTRIES))
        }
    }

    /** Merge the [otherId] scan into [keepId], combining their fields, and
     *  remove the [otherId] entry. The kept entry retains its id, timestamp and
     *  image. No-op if either id is missing. */
    suspend fun merge(keepId: String, otherId: String) {
        if (keepId == otherId) return
        context.historyDataStore.edit { prefs ->
            val current = decode(prefs[key].orEmpty())
            val keep = current.firstOrNull { it.id == keepId } ?: return@edit
            val other = current.firstOrNull { it.id == otherId } ?: return@edit
            val merged = keep.copy(contact = ContactMerge.combine(keep.contact, other.contact))
            deleteImage(other.imagePath)
            prefs[key] = encode(current.mapNotNull { e ->
                when (e.id) {
                    keepId -> merged
                    otherId -> null
                    else -> e
                }
            })
        }
    }

    suspend fun delete(id: String) {
        context.historyDataStore.edit { prefs ->
            val current = decode(prefs[key].orEmpty())
            current.firstOrNull { it.id == id }?.let { deleteImage(it.imagePath) }
            prefs[key] = encode(current.filterNot { it.id == id })
        }
    }

    suspend fun clear() {
        context.historyDataStore.edit { prefs ->
            decode(prefs[key].orEmpty()).forEach { deleteImage(it.imagePath) }
            prefs[key] = encode(emptyList())
        }
    }

    private fun deleteImage(path: String) {
        if (path.isNotBlank()) runCatching { File(path).delete() }
    }

    /** Rename a group across every scan that uses it. */
    suspend fun renameGroup(old: String, new: String) {
        val target = new.trim()
        context.historyDataStore.edit { prefs ->
            val updated = decode(prefs[key].orEmpty()).map {
                if (it.contact.group == old) it.copy(contact = it.contact.copy(group = target)) else it
            }
            prefs[key] = encode(updated)
        }
    }

    /** Remove a group label from every scan that uses it (contacts are kept). */
    suspend fun deleteGroup(name: String) {
        context.historyDataStore.edit { prefs ->
            val updated = decode(prefs[key].orEmpty()).map {
                if (it.contact.group == name) it.copy(contact = it.contact.copy(group = "")) else it
            }
            prefs[key] = encode(updated)
        }
    }

    private fun encode(entries: List<HistoryEntry>): String {
        val arr = JSONArray()
        entries.forEach { e ->
            val c = e.contact
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("ts", e.timestamp)
                    .put("img", e.imagePath)
                    .put("name", c.name)
                    .put("jobTitle", c.jobTitle)
                    .put("company", c.company)
                    .put("phones", JSONArray(c.phones))
                    .put("emails", JSONArray(c.emails))
                    .put("websites", JSONArray(c.websites))
                    .put("address", c.address)
                    .put("group", c.group)
                    .put("notes", c.notes)
                    .put("tags", JSONArray(c.tags))
            )
        }
        return arr.toString()
    }

    private fun decode(raw: String): List<HistoryEntry> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                HistoryEntry(
                    id = o.optString("id"),
                    timestamp = o.optLong("ts"),
                    imagePath = o.optString("img"),
                    contact = ParsedContact(
                        name = o.optString("name"),
                        jobTitle = o.optString("jobTitle"),
                        company = o.optString("company"),
                        phones = o.optJSONArray("phones").toList(),
                        emails = o.optJSONArray("emails").toList(),
                        websites = o.optJSONArray("websites").toList(),
                        address = o.optString("address"),
                        group = o.optString("group"),
                        notes = o.optString("notes"),
                        tags = o.optJSONArray("tags").toList()
                    )
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun JSONArray?.toList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() } }
    }

    companion object { private const val MAX_ENTRIES = 100 }
}
