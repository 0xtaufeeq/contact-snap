package com.contactsnap.app.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import com.contactsnap.app.model.ParsedContact

/**
 * Looks for existing contacts that match a parsed one by phone, email, or name,
 * so the user can be warned before creating a duplicate. Requires READ_CONTACTS.
 */
object ContactReader {

    /** @return display names of existing contacts that look like a match. */
    fun findMatches(context: Context, contact: ParsedContact): List<String> {
        val matches = linkedSetOf<String>()

        contact.phones.forEach { phone ->
            if (phone.count(Char::isDigit) >= 7) {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone)
                )
                queryName(context, uri, ContactsContract.PhoneLookup.DISPLAY_NAME, null, matches)
            }
        }

        contact.emails.forEach { email ->
            val uri = Uri.withAppendedPath(
                CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(email)
            )
            queryName(context, uri, ContactsContract.Contacts.DISPLAY_NAME, null, matches)
        }

        if (contact.name.isNotBlank()) {
            queryName(
                context,
                ContactsContract.Contacts.CONTENT_URI,
                ContactsContract.Contacts.DISPLAY_NAME,
                contact.name.trim(),
                matches
            )
        }

        return matches.toList()
    }

    private fun queryName(
        context: Context,
        uri: Uri,
        nameColumn: String,
        nameEquals: String?,
        into: MutableSet<String>
    ) {
        val selection = nameEquals?.let { "$nameColumn = ?" }
        val args = nameEquals?.let { arrayOf(it) }
        runCatching {
            context.contentResolver.query(uri, arrayOf(nameColumn), selection, args, null)?.use { c ->
                val idx = c.getColumnIndex(nameColumn)
                while (c.moveToNext()) {
                    if (idx >= 0) c.getString(idx)?.takeIf { it.isNotBlank() }?.let { into.add(it) }
                }
            }
        }
    }
}
