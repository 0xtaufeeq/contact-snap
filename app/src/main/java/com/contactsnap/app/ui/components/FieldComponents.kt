package com.contactsnap.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/** A single labeled, editable line. */
// Amber used to flag fields Gemini was unsure about.
val WarnColor = Color(0xFFB08900)

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    warn: Boolean = false,
    leading: (@Composable () -> Unit)? = null
) {
    Column(modifier.fillMaxWidth()) {
        FieldLabel(label, warn)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = leading,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (warn) WarnColor else MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = if (warn) WarnColor else MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

/** A list of values (phones, emails, …) the user can edit, remove, or extend. */
@Composable
fun MultiValueField(
    label: String,
    values: List<String>,
    onChange: (List<String>) -> Unit,
    addLabel: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    warn: Boolean = false
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label, warn)
        values.forEachIndexed { index, v ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = v,
                    onValueChange = { nv -> onChange(values.toMutableList().also { it[index] = nv }) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                IconButton(onClick = { onChange(values.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(20.dp))
                }
            }
        }
        TextButton(onClick = { onChange(values + "") }) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  $addLabel")
        }
    }
}

@Composable
private fun FieldLabel(label: String, warn: Boolean = false) {
    Text(
        text = if (warn) "${label.uppercase()} · DOUBLE-CHECK" else label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = if (warn) WarnColor else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}
