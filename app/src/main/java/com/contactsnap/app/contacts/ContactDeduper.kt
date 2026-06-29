package com.contactsnap.app.contacts

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import com.contactsnap.app.util.Phones

data class DupContact(
    val contactId: Long,
    val displayName: String,
    val phones: List<String>,
    val emails: List<String>
)

data class DupCluster(
    val contacts: List<DupContact>,
    val names: List<String>,          // distinct non-blank display names in the cluster
    val mergedPhones: List<String>,   // normalized + deduped (spaces/hyphens ignored)
    val mergedEmails: List<String>
) {
    val id: Long get() = contacts.minOf { it.contactId }
}

/**
 * Finds and merges duplicate device contacts. Two contacts are considered the
 * same if they share a name OR a phone number — where numbers are compared by
 * their digits only, so "+91 98765-43210" and "9876543210" match.
 */
object ContactDeduper {

    /** Comparable key for a number: digits only, last 10 (ignores spaces, hyphens, country code). */
    fun phoneKey(raw: String): String? {
        val d = raw.filter(Char::isDigit)
        if (d.length < 7) return null
        return if (d.length > 10) d.takeLast(10) else d
    }

    private fun nameKey(name: String): String? =
        name.trim().lowercase().replace(Regex("\\s+"), " ").ifBlank { null }

    fun findClusters(context: Context): List<DupCluster> {
        val resolver = context.contentResolver
        val names = HashMap<Long, String>()
        val phones = HashMap<Long, MutableSet<String>>()
        val emails = HashMap<Long, MutableSet<String>>()

        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            val idI = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nI = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            while (c.moveToNext()) {
                val id = c.getLong(idI)
                names[id] = c.getString(nI).orEmpty()
                phones.getOrPut(id) { mutableSetOf() }
                emails.getOrPut(id) { mutableSetOf() }
            }
        }

        resolver.query(
            CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(CommonDataKinds.Phone.CONTACT_ID, CommonDataKinds.Phone.NUMBER),
            null, null, null
        )?.use { c ->
            val idI = c.getColumnIndexOrThrow(CommonDataKinds.Phone.CONTACT_ID)
            val nI = c.getColumnIndexOrThrow(CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val id = c.getLong(idI)
                c.getString(nI)?.takeIf { it.isNotBlank() }?.let { phones.getOrPut(id) { mutableSetOf() }.add(it) }
            }
        }

        resolver.query(
            CommonDataKinds.Email.CONTENT_URI,
            arrayOf(CommonDataKinds.Email.CONTACT_ID, CommonDataKinds.Email.ADDRESS),
            null, null, null
        )?.use { c ->
            val idI = c.getColumnIndexOrThrow(CommonDataKinds.Email.CONTACT_ID)
            val aI = c.getColumnIndexOrThrow(CommonDataKinds.Email.ADDRESS)
            while (c.moveToNext()) {
                val id = c.getLong(idI)
                c.getString(aI)?.takeIf { it.isNotBlank() }?.let { emails.getOrPut(id) { mutableSetOf() }.add(it) }
            }
        }

        val ids = names.keys.toList()
        val parent = HashMap<Long, Long>().apply { ids.forEach { put(it, it) } }
        fun find(x: Long): Long {
            var r = x
            while (parent[r] != r) r = parent[r]!!
            var cur = x
            while (parent[cur] != r) { val nx = parent[cur]!!; parent[cur] = r; cur = nx }
            return r
        }
        fun union(a: Long, b: Long) { val ra = find(a); val rb = find(b); if (ra != rb) parent[ra] = rb }

        val byPhone = HashMap<String, MutableList<Long>>()
        for (id in ids) for (p in phones[id].orEmpty()) phoneKey(p)?.let { byPhone.getOrPut(it) { mutableListOf() }.add(id) }
        byPhone.values.forEach { g -> for (i in 1 until g.size) union(g[0], g[i]) }

        val byName = HashMap<String, MutableList<Long>>()
        for (id in ids) nameKey(names[id].orEmpty())?.let { byName.getOrPut(it) { mutableListOf() }.add(id) }
        byName.values.forEach { g -> for (i in 1 until g.size) union(g[0], g[i]) }

        val groups = HashMap<Long, MutableList<Long>>()
        for (id in ids) groups.getOrPut(find(id)) { mutableListOf() }.add(id)

        return groups.values.filter { it.size > 1 }.map { memberIds ->
            val members = memberIds.map { id ->
                DupContact(id, names[id].orEmpty(), phones[id].orEmpty().toList(), emails[id].orEmpty().toList())
            }
            DupCluster(
                contacts = members.sortedByDescending { it.phones.size + it.emails.size },
                names = members.map { it.displayName }.filter { it.isNotBlank() }.distinct(),
                mergedPhones = dedupePhones(members.flatMap { it.phones }),
                mergedEmails = members.flatMap { it.emails }.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
            )
        }.sortedByDescending { it.contacts.size }
    }

    private fun dedupePhones(raws: List<String>): List<String> {
        val seen = LinkedHashMap<String, String>()
        for (r in raws) {
            val norm = Phones.normalize(r)
            if (norm.isBlank()) continue
            val key = phoneKey(r) ?: norm
            val existing = seen[key]
            if (existing == null || (!existing.startsWith("+") && norm.startsWith("+"))) seen[key] = norm
        }
        return seen.values.toList()
    }

    /** Merge a cluster into one contact keeping [keepName]; deletes the others. @return true on success. */
    fun merge(context: Context, cluster: DupCluster, keepName: String): Boolean {
        val target = cluster.contacts.firstOrNull { it.displayName.equals(keepName, true) } ?: cluster.contacts.first()
        val targetRawId = firstRawContactId(context, target.contactId) ?: return false

        val ops = ArrayList<ContentProviderOperation>()

        // Replace the target's name / phone / email rows with the clean merged set.
        ops += ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
            .withSelection(
                "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} IN (?,?,?)",
                arrayOf(
                    targetRawId.toString(),
                    CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                    CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
            )
            .build()

        ops += dataInsert(targetRawId, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, keepName)
            .build()

        cluster.mergedPhones.forEach { p ->
            ops += dataInsert(targetRawId, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Phone.NUMBER, p)
                .withValue(CommonDataKinds.Phone.TYPE, CommonDataKinds.Phone.TYPE_MOBILE)
                .build()
        }
        cluster.mergedEmails.forEach { e ->
            ops += dataInsert(targetRawId, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Email.ADDRESS, e)
                .withValue(CommonDataKinds.Email.TYPE, CommonDataKinds.Email.TYPE_WORK)
                .build()
        }

        cluster.contacts.filter { it.contactId != target.contactId }.forEach { other ->
            ops += ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(other.contactId.toString()))
                .build()
        }

        return try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun dataInsert(rawId: Long, mime: String) =
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
            .withValue(ContactsContract.Data.MIMETYPE, mime)

    private fun firstRawContactId(context: Context, contactId: Long): Long? {
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { c -> if (c.moveToFirst()) return c.getLong(0) }
        return null
    }
}
