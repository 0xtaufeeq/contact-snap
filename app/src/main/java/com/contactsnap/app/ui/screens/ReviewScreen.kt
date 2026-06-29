package com.contactsnap.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.contactsnap.app.model.ParsedContact
import com.contactsnap.app.ui.ScanStatus
import com.contactsnap.app.ui.ScanUiState
import com.contactsnap.app.ui.components.EditableField
import com.contactsnap.app.ui.components.EditableList
import com.contactsnap.app.ui.components.ExpandableSection
import com.contactsnap.app.ui.components.glass
import com.contactsnap.app.util.NameFormat
import com.contactsnap.app.util.NameFormats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    state: ScanUiState,
    isSaving: Boolean,
    nameFormat: NameFormat,
    existingGroups: List<String>,
    onUpdate: ((ParsedContact) -> ParsedContact) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRetake: () -> Unit,
    onRetryExtraction: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val c = state.contact
    val warn = state.lowConfidence
    var moreOpen by remember { mutableStateOf(c.websites.isNotEmpty() || c.address.isNotBlank()) }
    var organizeOpen by remember { mutableStateOf(c.group.isNotBlank() || c.tags.isNotEmpty() || c.notes.isNotBlank()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Review", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onRetake) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (state.status == ScanStatus.Ready) {
                        IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, contentDescription = "Share") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp).navigationBarsPadding()) {
                    Button(
                        onClick = onSave,
                        enabled = !isSaving && state.status == ScanStatus.Ready,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Save to contacts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(0.dp))

                // Identity card
                SectionCard {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initials(c.name), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(14.dp))
                        EditableField(c.name, { v -> onUpdate { it.copy(name = v) } }, Modifier.weight(1f), placeholder = "Name", big = true, warn = "name" in warn)
                    }
                    val savedAs = NameFormats.format(nameFormat, c)
                    if (savedAs.isNotBlank()) {
                        Text(
                            "Saves as “$savedAs”",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 62.dp, bottom = 4.dp)
                        )
                    }
                    EditableField(c.jobTitle, { v -> onUpdate { it.copy(jobTitle = v) } }, label = "Title", placeholder = "Job title", warn = "jobTitle" in warn)
                    EditableField(c.company, { v -> onUpdate { it.copy(company = v) } }, label = "Company", placeholder = "Company", warn = "company" in warn)
                }

                // Channels card
                SectionCard {
                    EditableList(c.phones, { v -> onUpdate { it.copy(phones = v) } }, "phone", Icons.Rounded.Phone, warn = "phones" in warn)
                    EditableList(c.emails, { v -> onUpdate { it.copy(emails = v) } }, "email", Icons.Rounded.MailOutline, warn = "emails" in warn)
                }

                SectionCard {
                    ExpandableSection("More details", Icons.Rounded.LocationOn, moreOpen, { moreOpen = !moreOpen }) {
                        EditableList(c.websites, { v -> onUpdate { it.copy(websites = v) } }, "website", Icons.Rounded.Language, warn = "websites" in warn)
                        EditableField(c.address, { v -> onUpdate { it.copy(address = v) } }, label = "Address", placeholder = "Street, city", warn = "address" in warn)
                    }
                }

                SectionCard {
                    ExpandableSection("Group, tags, and note", Icons.Rounded.LocalOffer, organizeOpen, { organizeOpen = !organizeOpen }) {
                        EditableField(c.group, { v -> onUpdate { it.copy(group = v) } }, label = "Group · where you met", placeholder = "e.g. Web Summit 2026")
                        val others = existingGroups.filter { it.isNotBlank() && it != c.group }
                        if (others.isNotEmpty()) GroupSuggestions(others) { g -> onUpdate { it.copy(group = g) } }
                        TagField(c.tags) { v -> onUpdate { it.copy(tags = v) } }
                        EditableField(c.notes, { v -> onUpdate { it.copy(notes = v) } }, label = "Note", placeholder = "Saved to the contact")
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            if (state.status == ScanStatus.Processing) ProcessingOverlay()
            if (state.status == ScanStatus.Error) {
                ErrorOverlay(state.errorMessage ?: "Something went wrong.", onRetryExtraction, onOpenSettings)
            }
        }
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "+"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupSuggestions(groups: List<String>, onPick: (String) -> Unit) {
    FlowRow(Modifier.padding(top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.forEach { g -> SuggestionChip(onClick = { onPick(g) }, label = { Text(g) }) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagField(tags: List<String>, onChange: (List<String>) -> Unit) {
    var input by remember { mutableStateOf("") }
    fun add() {
        val t = input.trim()
        if (t.isNotEmpty() && tags.none { it.equals(t, ignoreCase = true) }) onChange(tags + t)
        input = ""
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("TAGS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onChange(tags - tag) },
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f).padding(vertical = 10.dp)
            )
            IconButton(onClick = { add() }) { Icon(Icons.Rounded.Add, contentDescription = "Add tag") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ProcessingOverlay() {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary, strokeWidth = 2.dp)
            Spacer(Modifier.height(20.dp))
            Text("Reading the card…", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f)).padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Couldn't read the card", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("Try again") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenSettings) { Text("Open settings", color = MaterialTheme.colorScheme.secondary) }
        }
    }
}
