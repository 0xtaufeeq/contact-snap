package com.contactsnap.app.util

import com.contactsnap.app.model.ParsedContact

/** Selectable formats for the saved contact's display name. */
enum class NameFormat(val id: String, val label: String) {
    NAME_TITLE_COMPANY("name_title_company", "Name - Designation, Company"),
    NAME_COMPANY("name_company", "Name - Company"),
    NAME_PAREN_COMPANY("name_paren_company", "Name (Company)"),
    COMPANY_NAME("company_name", "Company - Name"),
    NAME_ONLY("name_only", "Name only");

    companion object {
        val DEFAULT = NAME_TITLE_COMPANY
        fun fromId(id: String?): NameFormat = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

object NameFormats {

    /** Renders the display name for [c] in the chosen [fmt], dropping missing parts. */
    fun format(fmt: NameFormat, c: ParsedContact): String {
        val name = c.name.trim()
        val title = c.jobTitle.trim()
        val company = c.company.trim()
        return when (fmt) {
            NameFormat.NAME_ONLY -> name.ifEmpty { company }
            NameFormat.NAME_TITLE_COMPANY -> {
                val suffix = listOf(title, company).filter { it.isNotEmpty() }.joinToString(", ")
                nameThenSuffix(name, suffix, fallback = suffix)
            }
            NameFormat.NAME_COMPANY -> nameThenSuffix(name, company, fallback = company)
            NameFormat.NAME_PAREN_COMPANY -> when {
                name.isEmpty() -> company
                company.isEmpty() -> name
                else -> "$name ($company)"
            }
            NameFormat.COMPANY_NAME -> when {
                company.isEmpty() -> name
                name.isEmpty() -> company
                else -> "$company - $name"
            }
        }
    }

    /** A live example using sample data, for the settings picker. */
    fun example(fmt: NameFormat): String =
        format(fmt, ParsedContact(name = "Asha Rao", jobTitle = "Product Manager", company = "Acme Inc"))

    private fun nameThenSuffix(name: String, suffix: String, fallback: String): String = when {
        name.isEmpty() -> fallback
        suffix.isEmpty() -> name
        else -> "$name - $suffix"
    }
}
