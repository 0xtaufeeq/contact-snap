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
}
