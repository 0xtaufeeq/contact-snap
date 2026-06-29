package com.contactsnap.app.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.contactsnap.app.model.ParsedContact
import com.contactsnap.app.ui.ScanStatus
import com.contactsnap.app.ui.ScanUiState
import com.contactsnap.app.ui.components.LabeledField
import com.contactsnap.app.ui.components.MultiValueField
import com.contactsnap.app.ui.components.WarnColor
import com.contactsnap.app.util.NameFormat
import com.contactsnap.app.util.NameFormats

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Review contact", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onRetake) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Retake")
                    }
                },
                actions = {
                    if (state.status == ScanStatus.Ready) {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share as vCard")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
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
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Spacer(Modifier.height(2.dp))

                state.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Captured card",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.7f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                SaveAsPreview(NameFormats.format(nameFormat, c))

                if (state.lowConfidence.isNotEmpty()) LowConfidenceBanner()

                val warn = state.lowConfidence
                LabeledField("Name", c.name, { v -> onUpdate { it.copy(name = v) } }, placeholder = "Full name", warn = "name" in warn)
                LabeledField("Job title", c.jobTitle, { v -> onUpdate { it.copy(jobTitle = v) } }, placeholder = "e.g. Product Manager", warn = "jobTitle" in warn)
                LabeledField("Company", c.company, { v -> onUpdate { it.copy(company = v) } }, placeholder = "Company name", warn = "company" in warn)

                MultiValueField("Phone", c.phones, { v -> onUpdate { it.copy(phones = v) } }, "Add phone", KeyboardType.Phone, warn = "phones" in warn)
                MultiValueField("Email", c.emails, { v -> onUpdate { it.copy(emails = v) } }, "Add email", KeyboardType.Email, warn = "emails" in warn)
                MultiValueField("Website", c.websites, { v -> onUpdate { it.copy(websites = v) } }, "Add website", KeyboardType.Uri, warn = "websites" in warn)

                LabeledField("Address", c.address, { v -> onUpdate { it.copy(address = v) } }, placeholder = "Street, city", warn = "address" in warn)

                GroupField(
                    value = c.group,
                    suggestions = existingGroups,
                    onValueChange = { v -> onUpdate { it.copy(group = v) } }
                )

                TagField(
                    tags = c.tags,
                    onChange = { v -> onUpdate { it.copy(tags = v) } }
                )

                NotesField(
                    value = c.notes,
                    onValueChange = { v -> onUpdate { it.copy(notes = v) } }
                )

                Spacer(Modifier.height(8.dp))
            }

            if (state.status == ScanStatus.Processing) {
                ProcessingOverlay()
            }
            if (state.status == ScanStatus.Error) {
                ErrorOverlay(
                    message = state.errorMessage ?: "Something went wrong.",
                    onRetry = onRetryExtraction,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Couldn't read the card",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Try again") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenSettings) {
                Text("Open settings", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun LowConfidenceBanner() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarnColor.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = WarnColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Gemini wasn't fully sure about the highlighted fields — give them a quick check.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagField(tags: List<String>, onChange: (List<String>) -> Unit) {
    var input by remember { mutableStateOf("") }
    fun addTag() {
        val t = input.trim()
        if (t.isNotEmpty() && tags.none { it.equals(t, ignoreCase = true) }) onChange(tags + t)
        input = ""
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            "TAGS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onChange(tags - tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Add a tag (e.g. investor, follow-up)") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addTag() }),
            trailingIcon = {
                IconButton(onClick = { addTag() }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add tag")
                }
            }
        )
    }
}

@Composable
private fun NotesField(value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "NOTES",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Saved to the contact's notes") }
        )
    }
}

@Composable
private fun SaveAsPreview(displayName: String) {
    if (displayName.isBlank()) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            "WILL SAVE AS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            displayName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupField(
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LabeledField(
            label = "Group · where you met",
            value = value,
            onValueChange = onValueChange,
            placeholder = "e.g. Web Summit 2026"
        )
        val others = suggestions.filter { it.isNotBlank() && it != value }
        if (others.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                others.forEach { group ->
                    SuggestionChip(
                        onClick = { onValueChange(group) },
                        label = { Text(group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingOverlay() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary, strokeWidth = 2.dp)
            Spacer(Modifier.height(20.dp))
            Text("Reading the card…", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text("Extracting name, phone and email", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
