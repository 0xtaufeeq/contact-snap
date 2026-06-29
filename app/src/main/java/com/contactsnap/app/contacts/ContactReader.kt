package com.contactsnap.app.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import com.contactsnap.app.model.ParsedContact

/**
 * Finds existing contacts that match a parsed one by phone, email, or name —
 * returning their contact id so the user can merge instead of duplicating.
 * Requires READ_CONTACTS.
 */
object ContactReader {

    data class Match(val contactId: Long, val displayName: String)

    fun findMatches(context: Context, contact: ParsedContact): List<Match> {
        val out = LinkedHashMap<Long, String>()

        contact.phones.forEach { phone ->
            if (phone.count(Char::isDigit) >= 7) {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone)
                )
                query(context, uri, null, null, out)
            }
        }

        contact.emails.forEach { email ->
            val uri = Uri.withAppendedPath(
                CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(email)
            )
            query(context, uri, null, null, out)
        }

        if (contact.name.isNotBlank()) {
            query(
                context,
                ContactsContract.Contacts.CONTENT_URI,
                "${ContactsContract.Contacts.DISPLAY_NAME} = ?",
                arrayOf(contact.name.trim()),
                out
            )
        }

        return out.map { Match(it.key, it.value) }
    }

    private fun query(
        context: Context,
        uri: Uri,
        selection: String?,
        args: Array<String>?,
        into: MutableMap<Long, String>
    ) {
        runCatching {
            context.contentResolver.query(uri, null, selection, args, null)?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                // Data/PhoneLookup rows expose contact_id; the Contacts table uses _id.
                val cidIdx = c.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                    .let { if (it >= 0) it else c.getColumnIndex(ContactsContract.Contacts._ID) }
                if (cidIdx < 0) return
                while (c.moveToNext()) {
                    val id = c.getLong(cidIdx)
                    val name = if (nameIdx >= 0) c.getString(nameIdx).orEmpty() else ""
                    if (id > 0) into.putIfAbsent(id, name.ifBlank { "(unnamed)" })
                }
            }
        }
    }
}
