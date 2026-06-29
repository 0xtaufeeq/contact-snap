package com.contactsnap.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A field that reads as a calm row and turns into an inline editor on tap.
 * Commits live via [onValueChange]; exits on IME done or focus loss.
 */
@Composable
fun EditableField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    big: Boolean = false,
    warn: Boolean = false,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var editing by remember { mutableStateOf(false) }
    var hasFocused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val valueStyle = if (big) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge

    Row(
        modifier
            .fillMaxWidth()
            .clickable { editing = true }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            if (label != null) {
                Text(
                    if (warn) "${label.uppercase()} · CHECK" else label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (warn) WarnColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(2.dp))
            }
            if (editing) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = valueStyle.copy(color = MaterialTheme.colorScheme.onBackground),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { editing = false }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .onFocusChanged { if (it.isFocused) hasFocused = true else if (hasFocused) editing = false }
                )
                androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
                HorizontalDivider(
                    Modifier.padding(top = 4.dp),
                    thickness = 1.dp,
                    color = if (warn) WarnColor else MaterialTheme.colorScheme.secondary
                )
            } else {
                Text(
                    value.ifBlank { placeholder },
                    style = valueStyle,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (warn && !editing) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(7.dp).background(WarnColor, CircleShape))
        }
    }
}

/** A tap-to-edit list of values (phones, emails…) with per-row remove and an add row. */
@Composable
fun EditableList(
    values: List<String>,
    onChange: (List<String>) -> Unit,
    addLabel: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    warn: Boolean = false,
) {
    Column(Modifier.fillMaxWidth()) {
        values.forEachIndexed { index, v ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                EditableField(
                    value = v,
                    onValueChange = { nv -> onChange(values.toMutableList().also { it[index] = nv }) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = leadingIcon,
                    placeholder = addLabel,
                    keyboardType = keyboardType,
                    warn = warn && index == 0
                )
                IconButton(onClick = { onChange(values.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clickable { onChange(values + "") }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text("Add $addLabel", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** A collapsible section: a tappable header row that reveals its content. */
@Composable
fun ExpandableSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                content()
            }
        }
    }
}
