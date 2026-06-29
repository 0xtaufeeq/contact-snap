package com.contactsnap.app.ui.screens

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

private const val LAST_STEP = 3

@Composable
fun OnboardingScreen(
    onSaveKey: (String) -> Unit,
    onGetStarted: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var key by remember { mutableStateOf("") }

    fun finish() {
        if (key.isNotBlank()) onSaveKey(key.trim())
        onGetStarted()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        // Progress dots + skip
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(LAST_STEP + 1) { i ->
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(if (i == step) 20.dp else 6.dp)
                            .background(
                                if (i == step) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                CircleShape
                            )
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (step < LAST_STEP) {
                TextButton(onClick = { step = LAST_STEP }) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Crossfade(targetState = step, animationSpec = tween(250), label = "onboarding", modifier = Modifier.weight(1f)) { s ->
            when (s) {
                0 -> StepPane(Icons.Rounded.AutoAwesome, "Welcome to ContactSnap", "Snap a business card and it becomes a contact — name, title, phones, emails and more, read for you.")
                1 -> StepPane(Icons.Rounded.CameraAlt, "Point and shoot", "Frame a card (or import a photo). You can capture the front and back together for bilingual cards.")
                2 -> StepPane(Icons.Rounded.Contacts, "Review, then save", "The AI fills the fields — you confirm, organise by where you met, and save straight to your contacts.")
                else -> KeyPane(key, onKeyChange = { key = it })
            }
        }

        // Back / Next
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) {
                TextButton(onClick = { step-- }) { Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (step < LAST_STEP) step++ else finish() },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    if (step < LAST_STEP) "Next" else "Get started",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun StepPane(icon: ImageVector, title: String, body: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(76.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun KeyPane(key: String, onKeyChange: (String) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Key, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Add your API key", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(10.dp))
        Text(
            "ContactSnap reads cards with a free AI model. Create a key, paste it below — it's stored only on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = key,
            onValueChange = onKeyChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            label = { Text("API key") },
            placeholder = { Text("AIza…") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://aistudio.google.com/app/apikey".toUri())) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Get a free key")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "You can also add it later from Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
