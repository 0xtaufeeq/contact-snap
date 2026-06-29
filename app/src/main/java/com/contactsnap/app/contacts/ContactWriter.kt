package com.contactsnap.app.contacts

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import com.contactsnap.app.model.ParsedContact
import com.contactsnap.app.util.NameFormat
import com.contactsnap.app.util.NameFormats
import com.contactsnap.app.util.Phones

/**
 * Writes a [ParsedContact] into the device's contacts via a batch of
 * ContentProvider operations against a fresh RawContact. Requires the
 * WRITE_CONTACTS permission (requested at runtime before this is called).
 */
object ContactWriter {

    /** @return true on success. */
    fun save(context: Context, contact: ParsedContact, nameFormat: NameFormat = NameFormat.DEFAULT): Boolean {
        val ops = ArrayList<ContentProviderOperation>()

        // Index 0: the RawContact every data row attaches to (local/device account).
        ops += ContentProviderOperation
            .newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build()

        val displayName = NameFormats.format(nameFormat, contact)
        if (displayName.isNotBlank()) {
            ops += dataInsert()
                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                .build()
        }

        if (contact.company.isNotBlank() || contact.jobTitle.isNotBlank()) {
            ops += dataInsert()
                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Organization.COMPANY, contact.company)
                .withValue(CommonDataKinds.Organization.TITLE, contact.jobTitle)
                .withValue(CommonDataKinds.Organization.TYPE, CommonDataKinds.Organization.TYPE_WORK)
                .build()
        }

        contact.phones.forEach { phone ->
            val number = Phones.normalize(phone)
            if (number.isNotEmpty()) {
                ops += dataInsert()
                    .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.Phone.NUMBER, number)
                    .withValue(CommonDataKinds.Phone.TYPE, CommonDataKinds.Phone.TYPE_WORK)
                    .build()
            }
        }

        contact.emails.forEach { email ->
            ops += dataInsert()
                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Email.ADDRESS, email)
                .withValue(CommonDataKinds.Email.TYPE, CommonDataKinds.Email.TYPE_WORK)
                .build()
        }

        contact.websites.forEach { site ->
            ops += dataInsert()
                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Website.URL, site)
                .build()
        }

        if (contact.address.isNotBlank()) {
            ops += dataInsert()
                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, contact.address)
                .withValue(CommonDataKinds.StructuredPostal.TYPE, CommonDataKinds.StructuredPostal.TYPE_WORK)
                .build()
        }

        if (contact.notes.isNotBlank()) {
            ops += dataInsert()
                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Note.NOTE, contact.notes)
                .build()
        }

        return try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Each data row back-references the RawContact created at op index 0.
    private fun dataInsert(): ContentProviderOperation.Builder =
        ContentProviderOperation
            .newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

    /**
     * Appends new phones/emails/websites (and a note) to an existing contact,
     * skipping values that contact already has. @return true on success.
     */
    fun merge(context: Context, contactId: Long, contact: ParsedContact): Boolean {
        val rawId = firstRawContactId(context, contactId) ?: return false

        val existingPhones = existingValues(context, contactId, CommonDataKinds.Phone.CONTENT_ITEM_TYPE, CommonDataKinds.Phone.NUMBER)
            .map { it.filter(Char::isDigit) }.toSet()
        val existingEmails = existingValues(context, contactId, CommonDataKinds.Email.CONTENT_ITEM_TYPE, CommonDataKinds.Email.ADDRESS)
            .map { it.lowercase() }.toSet()
        val existingSites = existingValues(context, contactId, CommonDataKinds.Website.CONTENT_ITEM_TYPE, CommonDataKinds.Website.URL).toSet()

        val ops = ArrayList<ContentProviderOperation>()

        contact.phones.map { Phones.normalize(it) }
            .filter { it.isNotEmpty() && it.filter(Char::isDigit) !in existingPhones }
            .forEach { ops += rowInto(rawId, CommonDataKinds.Phone.CONTENT_ITEM_TYPE, CommonDataKinds.Phone.NUMBER, it, CommonDataKinds.Phone.TYPE, CommonDataKinds.Phone.TYPE_WORK) }

        contact.emails
            .filter { it.isNotBlank() && it.lowercase() !in existingEmails }
            .forEach { ops += rowInto(rawId, CommonDataKinds.Email.CONTENT_ITEM_TYPE, CommonDataKinds.Email.ADDRESS, it, CommonDataKinds.Email.TYPE, CommonDataKinds.Email.TYPE_WORK) }

        contact.websites
            .filter { it.isNotBlank() && it !in existingSites }
            .forEach { ops += rowInto(rawId, CommonDataKinds.Website.CONTENT_ITEM_TYPE, CommonDataKinds.Website.URL, it, null, null) }

        if (contact.notes.isNotBlank()) {
            ops += rowInto(rawId, CommonDataKinds.Note.CONTENT_ITEM_TYPE, CommonDataKinds.Note.NOTE, contact.notes, null, null)
        }

        if (ops.isEmpty()) return true // nothing new to add, but not a failure
        return try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun rowInto(
        rawId: Long, mime: String, valueCol: String, value: String, typeCol: String?, type: Int?
    ): ContentProviderOperation {
        val b = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
            .withValue(ContactsContract.Data.MIMETYPE, mime)
            .withValue(valueCol, value)
        if (typeCol != null && type != null) b.withValue(typeCol, type)
        return b.build()
    }

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

    private fun existingValues(context: Context, contactId: Long, mime: String, col: String): List<String> {
        val out = ArrayList<String>()
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(col),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(contactId.toString(), mime),
            null
        )?.use { c -> while (c.moveToNext()) c.getString(0)?.let { out.add(it) } }
        return out
    }
}
