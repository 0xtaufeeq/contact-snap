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
    val rawText: String = ""
)
