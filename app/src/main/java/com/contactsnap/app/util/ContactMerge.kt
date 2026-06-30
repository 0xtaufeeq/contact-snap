package com.contactsnap.app.util

import com.contactsnap.app.model.ParsedContact

/** Combines two scanned contacts into one. */
object ContactMerge {

    /**
     * Merges [secondary] into [primary]. Single-value fields (name, job title,
     * company, address, group) keep [primary]'s value and fall back to
     * [secondary] only when [primary]'s is blank. List fields (phones, emails,
     * websites, tags) are unioned with duplicates removed, and notes from both
     * are concatenated.
     */
    fun combine(primary: ParsedContact, secondary: ParsedContact): ParsedContact =
        ParsedContact(
            name = primary.name.ifBlank { secondary.name },
            jobTitle = primary.jobTitle.ifBlank { secondary.jobTitle },
            company = primary.company.ifBlank { secondary.company },
            phones = unionPhones(primary.phones, secondary.phones),
            emails = unionCaseInsensitive(primary.emails, secondary.emails),
            websites = unionCaseInsensitive(primary.websites, secondary.websites),
            address = primary.address.ifBlank { secondary.address },
            group = primary.group.ifBlank { secondary.group },
            notes = listOf(primary.notes, secondary.notes)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n\n"),
            tags = unionCaseInsensitive(primary.tags, secondary.tags),
            rawText = listOf(primary.rawText, secondary.rawText)
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        )

    /** Keeps original formatting but drops numbers that normalize to a duplicate. */
    private fun unionPhones(first: List<String>, second: List<String>): List<String> {
        val seen = HashSet<String>()
        return (first + second).filter { phone ->
            phone.isNotBlank() && seen.add(Phones.normalize(phone))
        }
    }

    private fun unionCaseInsensitive(first: List<String>, second: List<String>): List<String> {
        val seen = HashSet<String>()
        return (first + second).filter { value ->
            value.isNotBlank() && seen.add(value.trim().lowercase())
        }
    }
}
