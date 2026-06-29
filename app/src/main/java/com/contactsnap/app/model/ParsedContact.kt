package com.contactsnap.app.model

/**
 * Structured contact extracted from a scanned image. All fields are editable
 * by the user on the review screen before saving.
 */
data class ParsedContact(
    val name: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val websites: List<String> = emptyList(),
    val address: String = "",
    /** Contact group / label, e.g. where you met them ("Web Summit 2026"). */
    val group: String = "",
    /** Free-form note saved with the contact. */
    val notes: String = "",
    /** Free-form labels for in-app organising (not written to phone contacts). */
    val tags: List<String> = emptyList(),
    val rawText: String = ""
)
