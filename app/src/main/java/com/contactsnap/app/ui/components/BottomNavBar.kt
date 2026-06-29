package com.contactsnap.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

object NavRoutes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val FIX = "fix_contacts"
}

@Composable
fun BottomNavBar(
    current: String?,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onScan: () -> Unit,
    onSettings: () -> Unit,
    onFix: () -> Unit
) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(28.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab(Icons.Outlined.Home, "Home", current == NavRoutes.HOME, onHome, Modifier.weight(1f))
            NavTab(Icons.Outlined.History, "Recent", current == NavRoutes.HISTORY, onHistory, Modifier.weight(1f))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { ScanButton(onScan) }
            NavTab(Icons.Outlined.Settings, "Settings", current == NavRoutes.SETTINGS, onSettings, Modifier.weight(1f))
            NavTab(Icons.Outlined.PersonSearch, "Fix", current == NavRoutes.FIX, onFix, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val tint by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tint"
    )
    val scale by animateFloatAsState(if (selected) 1.18f else 1f, spring(dampingRatio = 0.45f), label = "scale")
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.clickable(interaction, indication = null, onClick = onClick).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp).scale(scale))
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.4f), label = "press")
    Box(
        Modifier
            .scale(scale)
            .size(46.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.CameraAlt,
            contentDescription = "Scan a card",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(23.dp)
        )
    }
}
