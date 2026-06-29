package com.contactsnap.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.contactsnap.app.contacts.DupCluster
import com.contactsnap.app.ui.FixUiState
import com.contactsnap.app.ui.components.glass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixContactsScreen(
    state: FixUiState,
    onScan: () -> Unit,
    onMerge: (DupCluster, String, (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var merging by remember { mutableStateOf<DupCluster?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Fix contacts", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Centered { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary, strokeWidth = 2.dp) }

                !state.scanned -> IntroPane(onScan)

                state.clusters.isEmpty() -> Centered {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Done, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No duplicates found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.height(4.dp))
                        Text("Your contacts look clean.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${state.clusters.size} possible ${if (state.clusters.size == 1) "duplicate" else "duplicates"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(state.clusters, key = { it.id }) { cluster ->
                        ClusterCard(cluster) { merging = cluster }
                    }
                }
            }
        }
    }

    merging?.let { cluster ->
        MergeDialog(
            cluster = cluster,
            onConfirm = { name -> onMerge(cluster, name) { merging = null } },
            onDismiss = { merging = null }
        )
    }
}

@Composable
private fun IntroPane(onScan: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(76.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PersonSearch, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Find duplicate contacts", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "We'll group contacts that share a name or number — even when a number only differs by spaces or hyphens — so you can merge them.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) { Text("Scan for duplicates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ClusterCard(cluster: DupCluster, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                cluster.names.firstOrNull()?.ifBlank { "(unnamed)" } ?: "(unnamed)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${cluster.contacts.size} contacts · ${cluster.mergedPhones.size} ${if (cluster.mergedPhones.size == 1) "number" else "numbers"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MergeDialog(cluster: DupCluster, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val nameChoices = cluster.names.ifEmpty { listOf("") }
    var chosen by remember { mutableStateOf(nameChoices.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge ${cluster.contacts.size} contacts") },
        text = {
            Column {
                if (nameChoices.size > 1) {
                    Text("Keep which name?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    nameChoices.forEach { name ->
                        Row(
                            Modifier.fillMaxWidth().clickable { chosen = name }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = chosen == name,
                                onClick = { chosen = name },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                            )
                            Text(name.ifBlank { "(unnamed)" }, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                val nCount = cluster.mergedPhones.size
                val numbers = "$nCount ${if (nCount == 1) "number" else "numbers"}"
                val emailPart = cluster.mergedEmails.size.let { e ->
                    if (e > 0) " and $e ${if (e == 1) "email" else "emails"}" else ""
                }
                val others = cluster.contacts.size - 1
                Text(
                    "Combines $numbers$emailPart into one contact. The other ${if (others == 1) "contact" else "$others contacts"} will be removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(chosen) }) { Text("Merge") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) { content() }
}
