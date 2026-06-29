package com.contactsnap.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.contactsnap.app.ui.HistoryViewModel
import com.contactsnap.app.ui.ScanViewModel
import com.contactsnap.app.ui.SettingsViewModel
import com.contactsnap.app.ui.screens.CameraScreen
import com.contactsnap.app.ui.screens.HistoryScreen
import com.contactsnap.app.ui.screens.HomeScreen
import com.contactsnap.app.ui.screens.ReviewScreen
import com.contactsnap.app.ui.screens.SettingsScreen
import com.contactsnap.app.ui.screens.SuccessScreen
import com.contactsnap.app.ui.theme.ContactSnapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactSnapTheme {
                AppRoot()
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val REVIEW = "review"
    const val SUCCESS = "success"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val vm: ScanViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val historyVm: HistoryViewModel = viewModel()
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val apiKey by settingsVm.apiKey.collectAsState()
    val nameFormat by settingsVm.nameFormat.collectAsState()
    val history by historyVm.entries.collectAsState()
    val existingGroups = remember(history) {
        history.map { it.contact.group }.filter { it.isNotBlank() }.distinct()
    }
    var isSaving by remember { mutableStateOf(false) }
    var pendingDuplicates by remember { mutableStateOf<List<String>?>(null) }

    fun requireKeyThen(action: () -> Unit) {
        if (apiKey.isBlank()) {
            Toast.makeText(context, "Add your Gemini API key in Settings first.", Toast.LENGTH_SHORT).show()
            nav.navigate(Routes.SETTINGS)
        } else action()
    }

    // Run duplicate check, then either save or surface a warning dialog.
    fun runSave() {
        vm.checkDuplicates { matches ->
            if (matches.isEmpty()) {
                doSave(vm, context, onSaving = { isSaving = it }) { nav.navigate(Routes.SUCCESS) }
            } else {
                pendingDuplicates = matches
            }
        }
    }

    val contactsPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.WRITE_CONTACTS] == true) runSave()
        else Toast.makeText(context, "Contacts permission is needed to save.", Toast.LENGTH_SHORT).show()
    }

    fun onSaveTapped() {
        val canWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        val canRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (canWrite && canRead) runSave()
        else contactsPermissions.launch(
            arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        )
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) nav.navigate(Routes.CAMERA)
        else Toast.makeText(context, "Camera permission is needed to scan.", Toast.LENGTH_SHORT).show()
    }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            vm.onImageCaptured(uri)
            nav.navigate(Routes.REVIEW)
        }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onScan = {
                    requireKeyThen {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) nav.navigate(Routes.CAMERA)
                        else cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                },
                onPickFromGallery = {
                    requireKeyThen {
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenHistory = { nav.navigate(Routes.HISTORY) }
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onCaptured = { uri ->
                    vm.onImageCaptured(uri)
                    nav.navigate(Routes.REVIEW) { popUpTo(Routes.HOME) }
                },
                onClose = { nav.popBackStack() }
            )
        }

        composable(Routes.REVIEW) {
            ReviewScreen(
                state = state,
                isSaving = isSaving,
                nameFormat = nameFormat,
                existingGroups = existingGroups,
                onUpdate = { transform -> vm.updateContact(transform) },
                onSave = { onSaveTapped() },
                onRetake = {
                    vm.syncHistory()
                    nav.popBackStack(Routes.HOME, inclusive = false)
                    vm.reset()
                },
                onRetryExtraction = { vm.retry() },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SUCCESS) {
            SuccessScreen(
                name = state.contact.name.ifBlank { "Contact" },
                onScanAnother = {
                    vm.reset()
                    nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                },
                onDone = {
                    vm.reset()
                    nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                savedKey = apiKey,
                currentFormat = nameFormat,
                onSelectFormat = { settingsVm.setNameFormat(it) },
                onSave = { settingsVm.save(it) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                entries = history,
                nameFormat = nameFormat,
                onOpen = { entry ->
                    vm.loadFromHistory(entry)
                    nav.navigate(Routes.REVIEW)
                },
                onDelete = { historyVm.delete(it) },
                onClear = { historyVm.clear() },
                onBack = { nav.popBackStack() }
            )
        }
    }

    pendingDuplicates?.let { matches ->
        AlertDialog(
            onDismissRequest = { pendingDuplicates = null },
            title = { Text("Possible duplicate") },
            text = {
                Text(
                    "You may already have this contact:\n\n" +
                        matches.take(5).joinToString("\n") { "•  $it" } +
                        "\n\nSave a new contact anyway?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDuplicates = null
                    doSave(vm, context, onSaving = { isSaving = it }) { nav.navigate(Routes.SUCCESS) }
                }) { Text("Save anyway") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDuplicates = null }) { Text("Cancel") }
            }
        )
    }
}

private fun doSave(
    vm: ScanViewModel,
    context: android.content.Context,
    onSaving: (Boolean) -> Unit,
    onSuccess: () -> Unit
) {
    onSaving(true)
    vm.save { ok ->
        onSaving(false)
        if (ok) onSuccess()
        else Toast.makeText(context, "Couldn't save the contact.", Toast.LENGTH_SHORT).show()
    }
}
