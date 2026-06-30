package com.contactsnap.app.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.contactsnap.app.data.HistoryEntry
import com.contactsnap.app.model.ParsedContact
import com.contactsnap.app.util.ContactMerge
import com.contactsnap.app.util.GroupColors
import com.contactsnap.app.util.NameFormat
import com.contactsnap.app.util.NameFormats
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    nameFormat: NameFormat,
    groupColors: Map<String, Int>,
    onOpen: (HistoryEntry) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit,
    onManageGroups: () -> Unit,
    onExportAll: () -> Unit,
    onMerge: (keepId: String, otherId: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var mergeMode by remember { mutableStateOf(false) }
    // Order matters: the first picked is offered as the primary by default.
    var picked by remember { mutableStateOf<List<String>>(emptyList()) }
    var confirmMerge by remember { mutableStateOf(false) }

    fun exitMerge() {
        mergeMode = false
        picked = emptyList()
    }

    fun togglePick(id: String) {
        picked = when {
            picked.contains(id) -> picked - id
            picked.size < 2 -> picked + id
            else -> picked // already two chosen
        }
    }

    val groups = remember(entries) {
        entries.map { it.contact.group }.filter { it.isNotBlank() }.distinct()
    }
    val shown = remember(entries, selectedGroup, query) {
        entries
            .filter { selectedGroup == null || it.contact.group == selectedGroup }
            .filter { e -> query.isBlank() || e.contact.matches(query) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (mergeMode) "Select 2 to merge" else "Recent scans",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (mergeMode) exitMerge() else onBack() }) {
                        Icon(
                            if (mergeMode) Icons.Rounded.Close else Icons.Rounded.ArrowBack,
                            contentDescription = if (mergeMode) "Cancel merge" else "Back"
                        )
                    }
                },
                actions = {
                    if (!mergeMode) {
                        if (entries.size >= 2) {
                            IconButton(onClick = { mergeMode = true }) {
                                Icon(Icons.Rounded.CallMerge, contentDescription = "Merge contacts")
                            }
                        }
                        if (entries.isNotEmpty()) {
                            IconButton(onClick = onExportAll) {
                                Icon(Icons.Rounded.Share, contentDescription = "Export all as vCard")
                            }
                        }
                        IconButton(onClick = onManageGroups) {
                            Icon(Icons.Rounded.LocalOffer, contentDescription = "Manage groups")
                        }
                        if (entries.isNotEmpty()) {
                            IconButton(onClick = { confirmClear = true }) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear all")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (entries.isNotEmpty() && !mergeMode) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    placeholder = { Text("Search name, company, group…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            if (mergeMode) {
                Text(
                    "Tap two scans to combine into one. Their numbers, emails and notes are merged.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            if (groups.isNotEmpty() && !mergeMode) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedGroup == null,
                        onClick = { selectedGroup = null },
                        label = { Text("All") }
                    )
                    groups.forEach { g ->
                        FilterChip(
                            selected = selectedGroup == g,
                            onClick = { selectedGroup = if (selectedGroup == g) null else g },
                            label = { Text(g) },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .background(GroupColors.effective(g, groupColors[g]), CircleShape)
                                )
                            }
                        )
                    }
                }
            }

            if (shown.isEmpty()) {
                val message = if (entries.isEmpty()) "No scans yet.\nCards you scan will show up here."
                else "No matches."
                Box(
                    Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown, key = { it.id }) { entry ->
                        HistoryRow(
                            entry = entry,
                            nameFormat = nameFormat,
                            groupColor = GroupColors.effective(entry.contact.group, groupColors[entry.contact.group]),
                            mergeMode = mergeMode,
                            selected = picked.contains(entry.id),
                            onOpen = {
                                if (mergeMode) togglePick(entry.id) else onOpen(entry)
                            },
                            onDelete = { onDelete(entry.id) }
                        )
                    }
                }
            }

            if (mergeMode && picked.size == 2) {
                Button(
                    onClick = { confirmMerge = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Rounded.CallMerge, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Merge 2 contacts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (confirmMerge && picked.size == 2) {
        val first = entries.firstOrNull { it.id == picked[0] }
        val second = entries.firstOrNull { it.id == picked[1] }
        if (first != null && second != null) {
            MergeContactsDialog(
                first = first,
                second = second,
                nameFormat = nameFormat,
                onConfirm = { keepId ->
                    val otherId = if (keepId == first.id) second.id else first.id
                    onMerge(keepId, otherId)
                    confirmMerge = false
                    exitMerge()
                },
                onDismiss = { confirmMerge = false }
            )
        } else {
            confirmMerge = false
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all scans?") },
            text = { Text("This permanently removes all ${entries.size} recent scans and their images. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClear() }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MergeContactsDialog(
    first: HistoryEntry,
    second: HistoryEntry,
    nameFormat: NameFormat,
    onConfirm: (keepId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var keepId by remember { mutableStateOf(first.id) }
    val primary = if (keepId == first.id) first else second
    val secondary = if (keepId == first.id) second else first
    val merged = remember(keepId) { ContactMerge.combine(primary.contact, secondary.contact) }

    val nameOf = { e: HistoryEntry ->
        NameFormats.format(nameFormat, e.contact).ifBlank { "(unnamed)" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge contacts") },
        text = {
            Column {
                Text(
                    "Keep which details when they differ?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                listOf(first, second).forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().clickable { keepId = e.id }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = keepId == e.id,
                            onClick = { keepId = e.id },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                        )
                        Text(nameOf(e), color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val nCount = merged.phones.size
                val numbers = "$nCount ${if (nCount == 1) "number" else "numbers"}"
                val eCount = merged.emails.size
                val emailPart = if (eCount > 0) " and $eCount ${if (eCount == 1) "email" else "emails"}" else ""
                Text(
                    "The result keeps $numbers$emailPart. The other scan will be removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(keepId) }) { Text("Merge") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    nameFormat: NameFormat,
    groupColor: Color,
    mergeMode: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val title = NameFormats.format(nameFormat, entry.contact).ifBlank { "Untitled scan" }
    val sub = entry.contact.phones.firstOrNull()
        ?: entry.contact.emails.firstOrNull()
        ?: entry.contact.company
    val when_ = DateUtils.getRelativeTimeSpanString(
        entry.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
    ).toString()

    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .let {
            if (selected) it.border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(14.dp)) else it
        }
        .clickable(onClick = onOpen)
        .padding(8.dp)

    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Thumbnail(entry.imagePath)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Text(
                listOfNotNull(sub.takeIf { it.isNotBlank() }, when_).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.contact.group.isNotBlank()) {
                Spacer(Modifier.size(4.dp))
                Text(
                    entry.contact.group,
                    style = MaterialTheme.typography.labelMedium,
                    color = groupColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(groupColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        if (mergeMode) {
            Icon(
                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (selected) "Selected" else "Not selected",
                tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        } else {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun ParsedContact.matches(query: String): Boolean {
    val q = query.trim().lowercase()
    return listOf(name, company, jobTitle, group, address, notes)
        .any { it.lowercase().contains(q) } ||
        phones.any { it.contains(q) } ||
        emails.any { it.lowercase().contains(q) } ||
        tags.any { it.lowercase().contains(q) }
}

@Composable
private fun Thumbnail(path: String) {
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (path.isNotBlank() && File(path).exists()) {
            AsyncImage(
                model = File(path),
                contentDescription = "Scan image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
