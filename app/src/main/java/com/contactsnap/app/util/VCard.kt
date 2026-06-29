package com.contactsnap.app.util

import com.contactsnap.app.model.ParsedContact

/** Builds vCard 3.0 text for sharing/exporting contacts. */
object VCard {

    fun forContact(c: ParsedContact): String = buildString {
        val name = c.name.ifBlank { c.company }
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("N:;${esc(name)};;;")
        appendLine("FN:${esc(name)}")
        if (c.company.isNotBlank()) appendLine("ORG:${esc(c.company)}")
        if (c.jobTitle.isNotBlank()) appendLine("TITLE:${esc(c.jobTitle)}")
        c.phones.forEach { appendLine("TEL;TYPE=WORK,VOICE:${esc(Phones.normalize(it))}") }
        c.emails.forEach { appendLine("EMAIL;TYPE=WORK:${esc(it)}") }
        c.websites.forEach { appendLine("URL:${esc(it)}") }
        if (c.address.isNotBlank()) appendLine("ADR;TYPE=WORK:;;${esc(c.address)};;;;")
        val categories = (listOf(c.group).filter { it.isNotBlank() } + c.tags).distinct()
        if (categories.isNotEmpty()) appendLine("CATEGORIES:${categories.joinToString(",") { esc(it) }}")
        if (c.notes.isNotBlank()) appendLine("NOTE:${esc(c.notes)}")
        appendLine("END:VCARD")
    }

    fun forMany(contacts: List<ParsedContact>): String =
        contacts.joinToString("\r\n") { forContact(it) }

    // vCard escaping: backslash, comma, semicolon, and newlines.
    private fun esc(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")
}
