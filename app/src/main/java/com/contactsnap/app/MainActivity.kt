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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.contactsnap.app.contacts.ContactReader
import com.contactsnap.app.ui.components.BottomNavBar
import com.contactsnap.app.ui.FixContactsViewModel
import com.contactsnap.app.ui.HistoryViewModel
import com.contactsnap.app.ui.ScanViewModel
import com.contactsnap.app.ui.SettingsViewModel
import com.contactsnap.app.ui.screens.AboutScreen
import com.contactsnap.app.ui.screens.CameraScreen
import com.contactsnap.app.ui.screens.FixContactsScreen
import com.contactsnap.app.ui.screens.HistoryScreen
import com.contactsnap.app.ui.screens.HomeScreen
import com.contactsnap.app.ui.screens.ManageGroupsScreen
import com.contactsnap.app.ui.screens.OnboardingScreen
import com.contactsnap.app.ui.screens.ReviewScreen
import com.contactsnap.app.ui.screens.SettingsScreen
import com.contactsnap.app.ui.screens.SuccessScreen
import com.contactsnap.app.ui.theme.ContactSnapTheme
import com.contactsnap.app.util.Sharing
import com.contactsnap.app.util.ThemeMode
import com.contactsnap.app.util.VCard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val theme by settingsVm.theme.collectAsState()
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AMOLED -> true
            }
            ContactSnapTheme(darkTheme = dark, amoled = theme == ThemeMode.AMOLED) {
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
    const val MANAGE_GROUPS = "manage_groups"
    const val ABOUT = "about"
    const val ONBOARDING = "onboarding"
    const val FIX_CONTACTS = "fix_contacts"
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val vm: ScanViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val historyVm: HistoryViewModel = viewModel()
    val fixVm: FixContactsViewModel = viewModel()
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val apiKey by settingsVm.apiKey.collectAsState()
    val nameFormat by settingsVm.nameFormat.collectAsState()
    val appTheme by settingsVm.theme.collectAsState()
    val onboardingSeen by settingsVm.onboardingSeen.collectAsState()
    val history by historyVm.entries.collectAsState()
    val groupCounts by historyVm.groups.collectAsState()
    val groupColors by historyVm.groupColors.collectAsState()
    val existingGroups = remember(history) {
        history.map { it.contact.group }.filter { it.isNotBlank() }.distinct()
    }
    val fixState by fixVm.state.collectAsState()
    var isSaving by remember { mutableStateOf(false) }
    var pendingDuplicates by remember { mutableStateOf<List<ContactReader.Match>?>(null) }
    var pendingFixMerge by remember { mutableStateOf<(() -> Unit)?>(null) }
    var batchMode by remember { mutableStateOf(false) }
    var batchCount by remember { mutableStateOf(0) }

    fun requireKeyThen(action: () -> Unit) {
        if (apiKey.isBlank()) {
            Toast.makeText(context, "Add your API key in Settings first.", Toast.LENGTH_SHORT).show()
            nav.navigate(Routes.SETTINGS)
        } else action()
    }

    fun afterSaved() {
        batchCount++
        nav.navigate(Routes.SUCCESS)
    }

    // Run duplicate check, then either save or surface a warning dialog.
    fun runSave() {
        vm.checkDuplicates { matches ->
            if (matches.isEmpty()) {
                doSave(vm, context, onSaving = { isSaving = it }) { afterSaved() }
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

    val fixReadPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fixVm.scan()
        else Toast.makeText(context, "Contacts permission is needed to scan.", Toast.LENGTH_SHORT).show()
    }

    val fixWritePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingFixMerge?.invoke()
        else Toast.makeText(context, "Contacts permission is needed to merge.", Toast.LENGTH_SHORT).show()
        pendingFixMerge = null
    }

    fun startFixScan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
            fixVm.scan()
        else fixReadPermission.launch(Manifest.permission.READ_CONTACTS)
    }

    fun doMerge(cluster: com.contactsnap.app.contacts.DupCluster, name: String, onResult: (Boolean) -> Unit) {
        val run = {
            fixVm.merge(cluster, name) { ok ->
                onResult(ok)
                if (!ok) Toast.makeText(context, "Couldn't merge — try again.", Toast.LENGTH_SHORT).show()
            }
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) run()
        else { pendingFixMerge = run; fixWritePermission.launch(Manifest.permission.WRITE_CONTACTS) }
    }

    fun startScan() {
        requireKeyThen {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                nav.navigate(Routes.CAMERA)
            else cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun startGallery() {
        requireKeyThen {
            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun switchTab(route: String) {
        nav.navigate(route) {
            popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
            launchSingleTop = true
        }
    }

    // Wait until the onboarding flag has loaded so we start on the right screen.
    if (onboardingSeen == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    val topLevel = setOf(Routes.HOME, Routes.HISTORY, Routes.SETTINGS, Routes.FIX_CONTACTS)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in topLevel) {
                BottomNavBar(
                    current = currentRoute,
                    onHome = { switchTab(Routes.HOME) },
                    onHistory = { switchTab(Routes.HISTORY) },
                    onScan = { startScan() },
                    onSettings = { switchTab(Routes.SETTINGS) },
                    onFix = { switchTab(Routes.FIX_CONTACTS) }
                )
            }
        }
    ) { scaffoldPadding ->
    NavHost(
        navController = nav,
        startDestination = if (onboardingSeen == false) Routes.ONBOARDING else Routes.HOME,
        modifier = Modifier.padding(scaffoldPadding)
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onSaveKey = { settingsVm.save(it) },
                onGetStarted = {
                    settingsVm.completeOnboarding()
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onScan = { startScan() },
                onPickFromGallery = { startGallery() },
                onOpenHistory = { switchTab(Routes.HISTORY) },
                batchMode = batchMode,
                onToggleBatch = { batchMode = it }
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onCaptured = { uris ->
                    vm.onImagesCaptured(uris)
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
                onShare = {
                    val c = state.contact
                    runCatching {
                        Sharing.shareVcf(
                            context,
                            baseName = c.name.ifBlank { "contact" },
                            vcardText = VCard.forContact(c)
                        )
                    }
                },
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
                batchMode = batchMode,
                savedCount = batchCount,
                onScanNext = {
                    vm.reset()
                    nav.navigate(Routes.CAMERA) { popUpTo(Routes.HOME) }
                },
                onScanAnother = {
                    vm.reset()
                    nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                },
                onDone = {
                    batchCount = 0
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
                currentTheme = appTheme,
                onSelectTheme = { settingsVm.setTheme(it) },
                onSave = { settingsVm.save(it) },
                onOpenAbout = { nav.navigate(Routes.ABOUT) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            val versionName = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: "1.0"
            }
            AboutScreen(versionName = versionName, onBack = { nav.popBackStack() })
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                entries = history,
                nameFormat = nameFormat,
                groupColors = groupColors,
                onOpen = { entry ->
                    vm.loadFromHistory(entry)
                    nav.navigate(Routes.REVIEW)
                },
                onDelete = { historyVm.delete(it) },
                onClear = { historyVm.clear() },
                onManageGroups = { nav.navigate(Routes.MANAGE_GROUPS) },
                onExportAll = {
                    if (history.isNotEmpty()) runCatching {
                        Sharing.shareVcf(
                            context,
                            baseName = "contactsnap-export",
                            vcardText = VCard.forMany(history.map { it.contact }),
                            title = "Export ${history.size} contacts"
                        )
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.MANAGE_GROUPS) {
            ManageGroupsScreen(
                groups = groupCounts,
                groupColors = groupColors,
                onRename = { old, new -> historyVm.renameGroup(old, new) },
                onSetColor = { name, color -> historyVm.setGroupColor(name, color) },
                onDelete = { historyVm.deleteGroup(it) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.FIX_CONTACTS) {
            FixContactsScreen(
                state = fixState,
                onScan = { startFixScan() },
                onMerge = { cluster, name, onResult -> doMerge(cluster, name, onResult) },
                onBack = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
            )
        }
    }
    }

    pendingDuplicates?.let { matches ->
        AlertDialog(
            onDismissRequest = { pendingDuplicates = null },
            title = { Text("Possible duplicate") },
            text = {
                Column {
                    Text("You may already have this contact. Add the scanned details to an existing one, or save a new contact.")
                    Spacer(Modifier.height(12.dp))
                    matches.take(5).forEach { match ->
                        Text(
                            "Add to ${match.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val id = match.contactId
                                    pendingDuplicates = null
                                    isSaving = true
                                    vm.merge(id) { ok ->
                                        isSaving = false
                                        if (ok) afterSaved()
                                        else Toast.makeText(context, "Couldn't merge the contact.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDuplicates = null
                    doSave(vm, context, onSaving = { isSaving = it }) { afterSaved() }
                }) { Text("Save as new") }
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
